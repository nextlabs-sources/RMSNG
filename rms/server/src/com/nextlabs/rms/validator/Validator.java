package com.nextlabs.rms.validator;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.MembershipException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.rs.UserMgmt;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.Nvl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

public final class Validator {

    public static boolean isValidDirectoryPath(String fileName, File parent) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        File file = new File(parent, fileName);
        try {
            return file.getCanonicalPath().startsWith(parent.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    public static RMSUserPrincipal setDeviceInUserPrincipal(String deviceId, Integer platformId,
        RMSUserPrincipal principal,
        HttpServletRequest request) throws ValidateException {
        platformId = Nvl.nvl(platformId, DeviceType.WEB.getLow());
        try {
            deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : HTTPUtil.getRemoteAddress(request);
        } catch (UnsupportedEncodingException e) { //NOPMD
        }
        DeviceType deviceType = DeviceType.getDeviceType(platformId);
        if (deviceType == null) {
            throw new ValidateException(400, "Unknown platform.");
        }
        principal.setDeviceId(deviceId);
        principal.setDeviceType(deviceType);
        return principal;
    }

    public static void validateFilePath(String filePath) {
        if (!StringUtils.hasText(filePath) || !StringUtils.startsWith(filePath, "/")) {
            throw new ValidateException(400, "Missing download file path parameter.");
        }
    }

    public static Constants.DownloadType validateDownloadType(int downloadTypeOrdinal, Constants.SPACETYPE spaceType) {
        if (Constants.SPACETYPE.PROJECTSPACE.equals(spaceType)) {
            if (downloadTypeOrdinal < 0 || downloadTypeOrdinal > 3) {
                throw new ValidateException(400, "Missing/Wrong download type.");
            }
        } else if (Constants.SPACETYPE.SHAREDWORKSPACE.equals(spaceType)) {
            if (downloadTypeOrdinal < 0 || downloadTypeOrdinal > 3) {
                throw new ValidateException(400, "Missing/Wrong download type.");
            }
        } else {
            if (downloadTypeOrdinal < 0 || downloadTypeOrdinal > 2) {
                throw new ValidateException(400, "Missing/Wrong download type.");
            }
        }
        return Constants.DownloadType.values()[downloadTypeOrdinal];
    }

    public static Project validateProject(int projectId, UserSession us) {
        Project project;
        try (DbSession session = DbSession.newSession()) {
            project = ProjectService.getProject(session, us, projectId);
            if (project == null) {
                throw new ValidateException(403, "Invalid Project.");
            }
        }
        return project;
    }

    public static RepositoryContent validateFileMetadata(String filePath, DefaultRepositoryTemplate repository)
            throws RepositoryException {
        RepositoryContent fileMetadata = repository.getFileMetadata(filePath);
        if (fileMetadata == null) {
            throw new FileNotFoundException("Missing File.");
        }
        return fileMetadata;

    }

    public static Membership validateMembership(String originalMembership) throws MembershipException {
        Membership ownerMembership;
        try (DbSession session = DbSession.newSession()) {
            ownerMembership = session.get(Membership.class, originalMembership);
            if (ownerMembership == null) {
                if (!UserMgmt.validateDynamicMembership(originalMembership)) {
                    throw new MembershipException("Access Denied");
                }
                ownerMembership = new Membership();
                ownerMembership.setName(originalMembership);
                User owner = new User();
                owner.setId(Integer.parseInt(originalMembership.substring("user".length(), originalMembership.indexOf('@'))));
                ownerMembership.setUser(owner);
            }
        }
        return ownerMembership;

    }

    public static boolean validateProjectName(String projectName) {
        Matcher matcher = RegularExpressions.CC_POLICY_SUPPORT_CHAR_PATTERN.matcher(projectName);
        return matcher.matches();
    }

    public static boolean validateMembership(NxlFile nxlMetadata, int projectId) {
        try (DbSession session = DbSession.newSession()) {
            Project project = session.get(Project.class, projectId);
            String tokenGroupName = StringUtils.substringAfter(nxlMetadata.getOwner(), "@");
            return project != null && project.getKeystore().getTokenGroupName().equals(tokenGroupName);
        }
    }

    private Validator() {
        throw new UnsupportedOperationException();
    }

    public static boolean isPartialDownloadAllowed(DownloadType downloadType) {
        return !(DownloadType.NORMAL.equals(downloadType) || DownloadType.FOR_SYSTEMBUCKET.equals(downloadType));
    }
}

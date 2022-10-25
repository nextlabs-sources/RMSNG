package com.nextlabs.rms.command;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.manager.NxlMetaData;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.JsonUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RMSRestHelper;
import com.nextlabs.rms.shared.UploadFileRequest;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetNXLFileInfoCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        RMSUserPrincipal userPrincipal = authenticate(request);
        if (userPrincipal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String path = HTTPUtil.getInternalURI(request);
        UploadFileRequest fileRequest = null;
        NxlMetaData nxlSharable = null;
        GetNXLInfoResponse result = new GetNXLInfoResponse();
        Operations operation = Operations.SHARE;
        File tempDir = RepositoryFileUtil.getTempOutputFolder();
        int thresholdSize = UploadUtil.THRESHOLD_SIZE;
        long requestSize = UploadUtil.REQUEST_SIZE;
        try {
            fileRequest = UploadUtil.readFile(request, thresholdSize, requestSize);
            String originalName = fileRequest.getFileName();
            byte[] content = fileRequest.getBytes();

            String destFilePath = tempDir + File.separator + originalName;
            File file = UploadUtil.writeFileToDisk(content, destFilePath);
            nxlSharable = SharedFileManager.getNxlSharableMetadata(file, path, userPrincipal);
            if (nxlSharable.getTgType() != Constants.TokenGroupType.TOKENGROUP_TENANT) {
                if (!nxlSharable.isOwner()) {
                    operation = Operations.RESHARE;
                }
                if (StringUtils.hasText(nxlSharable.getDuid()) && (nxlSharable.isRevoked() || !nxlSharable.isAllowedToShare())) {
                    RMSRestHelper.sendActivityLogToRMS(path, userPrincipal.getTicket(), nxlSharable.getDuid(), nxlSharable.getOwnerMembership(), userPrincipal.getUserId(), userPrincipal.getClientId(), operation, null, null, null, AccessResult.DENY, request, null, null, AccountType.PERSONAL);
                }
            }
            nxlSharable.setProtectionType(ProtectionType.ADHOC.ordinal());
            result.setMetadata(nxlSharable);
        } catch (FileUploadException e) {
            result.setError("Error occured while getting the rights on the file");
            result.setSuccess(false);
            logger.error(e.getMessage(), e);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } catch (Exception e) {
            result.setError("Error occured while getting the rights on the file");
            result.setSuccess(false);
            logger.error(e.getMessage(), e);
            JsonUtil.writeJsonToResponse(result, response);
            return;
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }

        result.setSuccess(true);
        JsonUtil.writeJsonToResponse(result, response);
    }

    static class GetNXLInfoResponse {

        private NxlMetaData metadata;
        private boolean success;
        private String error;

        public NxlMetaData getMetadata() {
            return metadata;
        }

        public void setMetadata(NxlMetaData metadata) {
            this.metadata = metadata;
        }

        public boolean getSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}

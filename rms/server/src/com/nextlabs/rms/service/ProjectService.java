package com.nextlabs.rms.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonProject;
import com.nextlabs.common.shared.JsonProjectFile;
import com.nextlabs.common.shared.JsonProjectFileInfo;
import com.nextlabs.common.shared.JsonProjectFileList;
import com.nextlabs.common.shared.JsonProjectInvitation;
import com.nextlabs.common.shared.JsonProjectInvitationList;
import com.nextlabs.common.shared.JsonProjectMember;
import com.nextlabs.common.shared.JsonProjectMemberDetails;
import com.nextlabs.common.shared.JsonProjectMemberList;
import com.nextlabs.common.shared.JsonSharedProject;
import com.nextlabs.common.shared.JsonUser;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.AuthUtils;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.FilePolicy.Policy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.ProjectStorageExceededException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.CustomerAccount;
import com.nextlabs.rms.hibernate.model.CustomerAccount.AccountType;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Project.Status;
import com.nextlabs.rms.hibernate.model.ProjectInvitation;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.SharingRecipientProject;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserPreferences;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.mail.Mail;
import com.nextlabs.rms.mail.Sender;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryProject;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.IRMSRepositorySearcher;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.RMSProjectDBSearcherImpl;
import com.nextlabs.rms.repository.defaultrepo.RMSRepositorySearchException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.rs.TenantMgmt;
import com.nextlabs.rms.rs.UserMgmt;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.task.BackupKeyStore;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

public final class ProjectService {

    public static final int INVITATION_EXPIRY_DURATION_IN_DAYS = 7;
    private static final byte[] KEY = "/4gu5t1n0r0ck5akrIfLBXk7tmFjNF1B+ZlvW9ZnSOFmq4=".getBytes(StandardCharsets.UTF_8);
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_DESCRIPTION = "projectDescription";
    public static final String INVITER = "inviter";
    public static final String INVITER_DISPLAY = "inviterDisplay";
    public static final String INVITEE = "invitee";
    public static final String SEARCH_FIELD_EMAIL = "email";
    public static final String SEARCH_FIELD_USER_NAME = "name";
    public static final String SEARCH_FIELD_FILENAME = "name";
    public static final Long PROJECT_TRIAL_DURATION = TimeUnit.DAYS.toMillis(Long.parseLong(WebConfig.getInstance().getProperty(WebConfig.PROJECT_TRIAL_DURATION, "0")));
    public static final Long MY_PROJECT_DEFAULT_QUOTA = 26843545600L;
    public static final String PROJECT_QUOTA = "quota";
    private static final String PROJECT_FILE_LANDING_PAGE = "/main#/app/projects/%s/files";
    private static final String DEFAULT_EXPIRY = "{\"option\":0}";
    private static final String DEFAULT_WATERMARK = "$(User)$(Break)$(Date) $(Time)";

    public static final String FILTER_OPTION_ALLSHARED = "allShared";
    public static final String FILTER_OPTION_ALL = "allFiles";
    public static final String FILTER_OPTION_REVOKED = "revoked";

    @SuppressWarnings("unchecked")
    private static List<JsonProjectMember> getActiveMembers(DbSession session, int projectId, int page, int size,
        List<Order> orders, Boolean showPicture, List<String> searchFieldList, String searchString) {
        DetachedCriteria dc = DetachedCriteria.forClass(Membership.class, "m").createCriteria("user", "u");
        dc.add(Restrictions.eq("m.project.id", projectId));
        dc.add(Restrictions.eq("m.status", Membership.Status.ACTIVE));
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                dc.addOrder(order);
            }
        }
        if (searchFieldList != null && !searchFieldList.isEmpty() && StringUtils.hasText(searchString)) {
            dc.add(getUserSearchDisjunction(searchFieldList, searchString));
        }
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        if (size > 0) {
            criteria.setMaxResults(size);
            criteria.setFirstResult((page - 1) * size);
        }
        List<Membership> list = criteria.list();
        List<JsonProjectMember> result = new ArrayList<>(list.size());
        JsonProjectMember member;
        for (Membership membership : list) {
            member = new JsonProjectMember();
            result.add(getJsonProjectMember(session, member, membership, showPicture));
        }
        return result;
    }

    private static Disjunction getUserSearchDisjunction(List<String> searchFieldList, String searchString) {
        Disjunction disjunction = Restrictions.disjunction();
        for (String searchField : searchFieldList) {
            if (StringUtils.equals(searchField, SEARCH_FIELD_USER_NAME)) {
                disjunction.add(EscapedLikeRestrictions.ilike("u.displayName", searchString, MatchMode.ANYWHERE));
            } else if (StringUtils.equals(searchField, SEARCH_FIELD_EMAIL)) {
                disjunction.add(EscapedLikeRestrictions.ilike("u.email", searchString, MatchMode.ANYWHERE));
            }
        }
        return disjunction;
    }

    public static DefaultRepositoryTemplate getRepository(RMSUserPrincipal principal, int projectId)
            throws UnauthorizedRepositoryException, InvalidDefaultRepositoryException {
        DefaultRepositoryTemplate repository;
        try (DbSession session = DbSession.newSession()) {
            repository = ProjectService.getProjectRepository(session, principal, projectId);
        }
        return repository;
    }

    public static boolean isMemberProjectOwner(Membership member) {
        if (member == null) {
            return false;
        }
        return member.getProject().getOwner().equalsIgnoreCase(member.getUser().getEmail());
    }

    public static String validateFileDuid(int projectId, String filePath) throws FileNotFoundException {
        String duid = ProjectService.getProjectFileDUID(projectId, filePath);
        if (!StringUtils.hasText(duid)) {
            throw new FileNotFoundException("Missing File.");
        }
        return duid;
    }

    public static boolean pathExists(String projectId, String searchPath) {
        boolean result = false;
        try {
            IRMSRepositorySearcher searcher = new RMSProjectDBSearcherImpl();
            result = searcher.pathExists(projectId, searchPath);
        } catch (RMSRepositorySearchException e) {
            LOGGER.error("Error occurred while deleting files in project {} : {}", projectId, e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred while deleting files in project {} : {}", projectId, e.getMessage(), e);
        }
        return result;
    }

    public static JsonProjectFileList getJsonFileList(DbSession session, UserSession us, int projectId, Integer page,
        Integer size, List<Order> orders, String parentPath, List<String> searchFieldList, String searchString,
        String filter) {
        Project project = getProject(session, us, projectId);
        if (project == null) {
            return null;
        }
        JsonProjectFileList result = new JsonProjectFileList();
        long totalFiles = getTotalFiles(session, project, parentPath, searchFieldList, searchString, filter);
        result.setTotalFiles(totalFiles);
        List<JsonProjectFile> fileInfo = totalFiles > 0 ? getFileInformation(session, project, page, size, orders, parentPath, searchString, filter, us) : Collections.<JsonProjectFile> emptyList();
        result.setFiles(fileInfo);
        return result;
    }

    public static boolean checkFolderUpdated(DbSession session, int userId, int projectId, String folderPath,
        long lastModified) {
        Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
        criteria.add(Restrictions.and(Restrictions.eq("project.id", projectId), Restrictions.eq("filePath", folderPath), Restrictions.gt("lastModified", new Date(lastModified))));
        ProjectSpaceItem folder = (ProjectSpaceItem)criteria.uniqueResult();
        return folder != null;
    }

    private static DetachedCriteria getProjectSpaceItemDetachedCriteria(Project project, String searchString,
        String parentPath, String filter) {
        DetachedCriteria dc = DetachedCriteria.forClass(ProjectSpaceItem.class, "p");
        dc.add(Restrictions.eq("project.id", project.getId()));
        if (!StringUtils.hasText(searchString)) {
            if (StringUtils.hasText(parentPath)) {
                dc.add(Restrictions.eq("fileParentPath", parentPath));
            } else {
                dc.add(Restrictions.eq("directory", false));
            }
        }

        if (FILTER_OPTION_ALLSHARED.equals(filter)) {
            dc.add(Restrictions.eq("p.status", ProjectSpaceItem.Status.SHARED));
        } else if (FILTER_OPTION_REVOKED.equals(filter)) {
            dc.add(Restrictions.eq("p.status", ProjectSpaceItem.Status.REVOKED));
        }

        return dc;
    }

    private static Criteria getProjectSpaceItemCriteria(DbSession session, Project project, String searchString,
        String parentPath, List<String> searchFieldList, String filter) {
        DetachedCriteria dc = getProjectSpaceItemDetachedCriteria(project, searchString, parentPath, filter);
        if (searchFieldList != null && !searchFieldList.isEmpty() && StringUtils.hasText(searchString)) {
            Disjunction disjunction = Restrictions.disjunction();
            for (String searchField : searchFieldList) {
                if (StringUtils.equals(searchField, SEARCH_FIELD_FILENAME)) {
                    disjunction.add(EscapedLikeRestrictions.ilike("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
                }
            }
            dc.add(disjunction);
        }
        return dc.getExecutableCriteria(session.getSession());
    }

    @SuppressWarnings("unchecked")
    private static List<JsonProjectFile> getFileInformation(DbSession session, Project project, Integer page,
        Integer size, List<Order> orders, String parentPath, String searchString, String filter, UserSession us) {
        DetachedCriteria dc = getProjectSpaceItemDetachedCriteria(project, searchString, parentPath, filter);
        if (StringUtils.hasText(searchString)) {
            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add(EscapedLikeRestrictions.ilike("filePathSearchSpace", searchString.toLowerCase(), MatchMode.ANYWHERE));
            dc.add(disjunction);
        }
        dc.setFetchMode("user", FetchMode.JOIN);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                dc.addOrder(order);
            }
        }
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        if (size > 0) {
            criteria.setMaxResults(size);
            criteria.setFirstResult((page - 1) * size);
        }
        List<ProjectSpaceItem> list = criteria.list();
        List<JsonProjectFile> result = new ArrayList<>(list.size());
        for (ProjectSpaceItem item : list) {
            User user = item.getUser();
            User lastUser = item.getLastModifiedUser();
            JsonProjectMember owner = new JsonProjectMember();
            owner.setUserId(user.getId());
            owner.setDisplayName(user.getDisplayName());
            owner.setEmail(user.getEmail());
            JsonProjectMember lastModifiedUser = null;
            if (lastUser != null) {
                lastModifiedUser = new JsonProjectMember();
                lastModifiedUser.setUserId(lastUser.getId());
                lastModifiedUser.setDisplayName(lastUser.getDisplayName());
                lastModifiedUser.setEmail(lastUser.getEmail());
            }
            JsonProjectFile file = new JsonProjectFile();
            file.setCreationTime(item.getCreationTime().getTime());
            file.setSize(item.getSize());
            file.setFolder(item.isDirectory());
            Date lastModified = item.getLastModified();
            if (lastModified != null) {
                file.setLastModified(lastModified.getTime());
            }
            file.setId(item.getId());
            file.setDuid(item.getDuid());
            file.setPathDisplay(item.getFilePathDisplay());
            file.setPathId(item.getFilePath());
            if (!item.isDirectory()) {
                file.setName(StringUtils.substringAfterLast(item.getFilePathDisplay(), "/"));
                file.setFileType(RepositoryFileUtil.getOriginalFileExtension(file.getName()));
            } else {
                String filePathDisplay = item.getFilePathDisplay();
                String fileName = getFileName(filePathDisplay);
                file.setName(fileName);
            }
            file.setLastModifiedUser(lastModifiedUser);
            file.setOwner(owner);
            boolean isShared = item.getStatus().ordinal() == 1 ? true : false;
            file.setShared(isShared);
            file.setRevoked(ProjectSpaceItem.Status.REVOKED.equals(item.getStatus()) ? true : false);
            result.add(file);
        }

        // get Shared Recipient list for the projects -> projects
        if (!result.isEmpty()) {
            Map<String, Set<Integer>> recipientMap = getRecipientMapProjects(session, result);
            List<JsonProject> allProjects = getAllProjects(session, us, us.getLoginTenant());

            Map<Integer, JsonProject> projectMap = new HashMap<Integer, JsonProject>();
            for (JsonProject thisProject : allProjects) {
                projectMap.put(thisProject.getId(), thisProject);
            }

            for (JsonProjectFile projectfile : result) {
                Set<Integer> recipientIds = recipientMap.get(projectfile.getDuid());
                projectfile.setShareWithProject(recipientIds);

                if (recipientIds != null) {
                    Set<String> recipientNames = new LinkedHashSet<>();
                    for (Integer recipientId : recipientIds) {
                        recipientNames.add(projectMap.get(recipientId).getName());
                    }
                    projectfile.setShareWithProjectName(recipientNames);
                }
            }
        }
        // setShareWithPersonal & setShareWithEnterprise when we share Project -> Personal & Enterprise respectively
        return result;
    }

    private static String getFileName(String filePathDisplay) {
        String fileName = "";
        if (filePathDisplay.endsWith("/")) { //to display correct folder name on the ui create folder api path is /test whereas list files api is /test/
            fileName = StringUtils.substringAfterLast(filePathDisplay.substring(0, filePathDisplay.length() - 1), "/");
        } else {
            fileName = StringUtils.substringAfterLast(filePathDisplay.substring(0, filePathDisplay.length()), "/");
        }
        return fileName;
    }

    private static long getTotalFiles(DbSession session, Project project, String parentPath,
        List<String> searchFieldList, String searchString, String filter) {
        Criteria criteria = getProjectSpaceItemCriteria(session, project, searchString, parentPath, searchFieldList, filter);
        criteria.setProjection(Projections.rowCount());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    public static JsonProjectMemberList getJsonActiveMembers(DbSession session, UserSession us, int projectId, int page,
        int size, List<Order> orders, Boolean showPicture, List<String> searchFieldList, String searchString) {
        Project project = getProject(session, us, projectId);
        if (project == null) {
            return null;
        }
        JsonProjectMemberList result = new JsonProjectMemberList();
        long totalActiveMembers = getTotalActiveMembers(session, project, searchFieldList, searchString);
        result.setTotalMembers(totalActiveMembers);
        List<JsonProjectMember> activeMembers = getActiveMembers(session, projectId, page, size, orders, showPicture, searchFieldList, searchString);
        result.setMembers(activeMembers);
        return result;
    }

    public static JsonProjectMemberDetails getJsonMemberDetails(DbSession session, int userId, Project project,
        Boolean showPicture) {

        JsonProjectMemberDetails memberDetails = new JsonProjectMemberDetails();
        DetachedCriteria dc = DetachedCriteria.forClass(Membership.class, "m");
        dc.add(Restrictions.eq("project.id", project.getId()));
        dc.add(Restrictions.eq("status", Membership.Status.ACTIVE));
        dc.add(Restrictions.eq("user.id", userId));
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        Membership member = (Membership)criteria.uniqueResult();
        if (member == null) {
            return null;
        }

        if (!isMemberProjectOwner(member)) {
            User inviter = member.getInviter();
            if (inviter != null) {
                memberDetails.setInviterDisplayName(inviter.getDisplayName());
                memberDetails.setInviterEmail(inviter.getEmail());
            }
        }
        return (JsonProjectMemberDetails)getJsonProjectMember(session, memberDetails, member, showPicture);
    }

    private static JsonProjectMember getJsonProjectMember(DbSession session, JsonProjectMember jsonProjectMember,
        Membership member, Boolean showPicture) {
        final Gson gson = GsonUtils.GSON;
        User user = member.getUser();
        UserPreferences userPreferences = session.get(UserPreferences.class, user.getId());
        String preferences = userPreferences.getPreferences();
        jsonProjectMember.setCreationTime(member.getCreationTime().getTime());
        jsonProjectMember.setDisplayName(user.getDisplayName());
        jsonProjectMember.setEmail(user.getEmail());
        jsonProjectMember.setUserId(user.getId());
        if (showPicture != null && showPicture.booleanValue() && StringUtils.hasText(preferences)) {
            Map<String, Object> prefs = gson.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);
            String picture = (String)prefs.get(UserMgmt.PROFILE_PICTURE);
            jsonProjectMember.setPicture(picture);
        }
        return jsonProjectMember;
    }

    @SuppressWarnings("unchecked")
    public static JsonProjectInvitationList getJsonPendingInvitations(DbSession session, int projectId, int page,
        int size, List<Order> orders, String searchField, String searchString) {
        Criteria criteria = session.createCriteria(ProjectInvitation.class);
        criteria.add(Restrictions.eq("project.id", projectId));
        criteria.add(Restrictions.in("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING }));
        if (size > 0) {
            criteria.setMaxResults(size);
            criteria.setFirstResult((page - 1) * size);
        }
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }
        if (StringUtils.hasText(searchString)) {
            criteria.add(EscapedLikeRestrictions.ilike("inviteeEmail", searchString, MatchMode.ANYWHERE));
        }
        List<ProjectInvitation> invitationList = criteria.list();
        List<JsonProjectInvitation> pendingList = new ArrayList<JsonProjectInvitation>(invitationList.size());
        for (ProjectInvitation invitation : invitationList) {
            JsonProjectInvitation invitationJson = new JsonProjectInvitation();
            invitationJson.setInvitationId(invitation.getId());
            invitationJson.setInviteeEmail(invitation.getInviteeEmail());
            invitationJson.setInviterDisplay(invitation.getInviter().getDisplayName());
            invitationJson.setInviterEmail(invitation.getInviter().getEmail());
            invitationJson.setInviteTime(invitation.getInviteTime().getTime());
            pendingList.add(invitationJson);
        }
        JsonProjectInvitationList result = new JsonProjectInvitationList();
        Long totalNumber = getTotalPendingMembers(session, projectId, searchString);
        result.setTotalInvitations(totalNumber);
        result.setInvitations(pendingList);
        return result;
    }

    public static List<JsonProjectInvitation> getJsonPendingInvitationsForUser(DbSession session, String userEmail)
            throws GeneralSecurityException {
        boolean showDefaultInvitationMsg = false;
        boolean fromListProjects = false;
        Criteria criteria = session.createCriteria(ProjectInvitation.class);
        criteria.add(Restrictions.eq("inviteeEmail", userEmail).ignoreCase());
        criteria.add(Restrictions.in("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING }));
        criteria.addOrder(Order.desc("inviteTime"));
        @SuppressWarnings("unchecked")
        List<ProjectInvitation> invitationList = criteria.list();
        List<JsonProjectInvitation> pendingList = new ArrayList<JsonProjectInvitation>(invitationList.size());
        for (ProjectInvitation invitation : invitationList) {
            JsonProjectInvitation invitationJson = new JsonProjectInvitation();
            invitationJson.setInvitationId(invitation.getId());
            invitationJson.setInvitationMsg(invitation.getInvitationMsg());
            invitationJson.setInviteeEmail(invitation.getInviteeEmail());
            invitationJson.setInviterDisplay(invitation.getInviter().getDisplayName());
            invitationJson.setInviterEmail(invitation.getInviter().getEmail());
            invitationJson.setInviteTime(invitation.getInviteTime().getTime());
            invitationJson.setCode(getInvitationCode(String.valueOf(invitation.getId())));
            Project project = invitation.getProject();
            JsonProject jsonProject = toJsonProject(project, showDefaultInvitationMsg, fromListProjects);
            User owner = ProjectService.getOwner(session, project);
            JsonUser jsonOwner = toJson(owner);
            jsonProject.setOwner(jsonOwner);
            invitationJson.setProject(jsonProject);
            pendingList.add(invitationJson);
        }
        return pendingList;
    }

    public static long getTotalPendingInvitationsForUser(DbSession session, String userEmail) {
        Criteria criteria = session.createCriteria(ProjectInvitation.class);
        criteria.add(Restrictions.eq("inviteeEmail", userEmail).ignoreCase());
        criteria.add(Restrictions.in("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING }));
        criteria.setProjection(Projections.rowCount());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    @SuppressWarnings("unchecked")
    public static User getOwner(DbSession session, Project project) {
        String owner = project.getOwner();
        if (StringUtils.hasText(owner)) {
            Criteria criteria = session.createCriteria(User.class);
            criteria.add(Restrictions.eq("email", owner).ignoreCase());
            List<User> list = criteria.list();
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    public static Project getProject(DbSession session, UserSession us, int projectId) {
        Criteria criteria = getProjectCriteria(session, us, projectId);
        Project project = (Project)criteria.uniqueResult();
        if (project == null) {
            Set<Integer> abacProjects = getABACProjectIDs(session, us);
            if (abacProjects.contains(Integer.valueOf(projectId))) {
                project = session.get(Project.class, projectId);
            }
        }
        return project;
    }

    private static Criteria getProjectCriteria(DbSession session, UserSession us, int projectId) {
        DetachedCriteria dc = getProjectMembershipDetachedCriteria(us);
        dc.add(Restrictions.eq("project.id", projectId));

        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.eq("id", projectId));
        criteria.add(Restrictions.ne("name", UserMgmt.GROUP_NAME_PUBLIC));
        criteria.add(Subqueries.propertyIn("id", dc));
        return criteria;
    }

    public static Project getProjectWithParentTenant(DbSession session, UserSession us, int projectId) {
        Criteria criteria = getProjectCriteria(session, us, projectId);
        criteria.setFetchMode("parentTenant", FetchMode.JOIN);
        Project project = (Project)criteria.uniqueResult();
        if (project == null) {
            Set<Integer> abacProjects = getABACProjectIDs(session, us);
            if (abacProjects.contains(Integer.valueOf(projectId))) {
                criteria = session.createCriteria(Project.class);
                criteria.add(Restrictions.eq("id", projectId));
                criteria.setFetchMode("tenant", FetchMode.JOIN);
                project = session.get(Project.class, projectId);
            }
        }
        return project;
    }

    public static boolean hasProject(DbSession session, String projectName, String tenantId, String owner) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.eq("name", projectName).ignoreCase());
        criteria.add(Restrictions.eq("parentTenant.id", tenantId));
        criteria.add(Restrictions.eq("owner", owner).ignoreCase());
        return criteria.uniqueResult() != null;
    }

    public static Set<Integer> getABACProjectIDs(DbSession session, UserSession us) {
        com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(us.getUser().getId())).clientId(us.getClientId()).email(us.getUser().getEmail()).build();
        Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
        return MembershipPoliciesEvaluationHandler.evalABACMembershipPolicies(userEval, loginTenant.getName());
    }

    @SuppressWarnings("unchecked")
    public static List<Project> getProjects(DbSession session, UserSession us, List<Order> orders, Boolean ownedByMe,
        int size, int page) {
        Criteria mCriteria = session.createCriteria(Membership.class).createAlias("project", "project");
        mCriteria.add(Restrictions.conjunction(Restrictions.eq("user.id", us.getUser().getId()), Restrictions.eq("status", Membership.Status.ACTIVE), Restrictions.ne("project.name", UserMgmt.GROUP_NAME_PUBLIC)));

        Set<Integer> abacProjects = null;
        if (BooleanUtils.isNotTrue(ownedByMe)) {
            abacProjects = getABACProjectIDs(session, us);
        }

        if (ownedByMe != null) {
            User user = us.getUser();
            if (ownedByMe) {
                mCriteria.add(Restrictions.eq("project.owner", user.getEmail()).ignoreCase());
            } else {
                mCriteria.add(Restrictions.ne("project.owner", user.getEmail()).ignoreCase());
            }
        }

        if (orders != null && !orders.isEmpty()) {
            for (int i = 0; i < orders.size(); i++) {
                if ("projectActionTime".equals(orders.get(i).getPropertyName())) {
                    mCriteria.addOrder(orders.get(i));
                    orders.remove(i);
                    break;
                }
            }
        }

        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                mCriteria.addOrder(order);
            }
        } else {
            mCriteria.addOrder(Order.asc("project.name"));
        }

        List<Membership> memberships = mCriteria.list();
        List<Project> projects = new LinkedList<>();
        for (Membership membership : memberships) {
            projects.add(membership.getProject());
        }

        if (abacProjects != null && !abacProjects.isEmpty()) {
            Set<Integer> projectIds = new HashSet<>(projects.size());
            for (Project project : projects) {
                projectIds.add(project.getId());
            }
            Criteria criteria = session.createCriteria(Project.class);
            criteria.add(Restrictions.in("id", abacProjects.toArray()));
            if (ownedByMe != null) {
                User user = us.getUser();
                if (ownedByMe) {
                    criteria.add(Restrictions.eq("owner", user.getEmail()).ignoreCase());
                } else {
                    criteria.add(Restrictions.ne("owner", user.getEmail()).ignoreCase());
                }
            }
            List<Project> abacProjectList = criteria.list();
            Iterator<Project> iterator = abacProjectList.iterator();
            while (iterator.hasNext()) {
                Project project = iterator.next();
                if (projectIds.contains(project.getId())) {
                    iterator.remove();
                }
            }
            projects.addAll(abacProjectList);
        }

        if (size > 0 && !projects.isEmpty()) {
            int startIdx = (page - 1) * size;
            int numOfProjects = projects.size();
            if (startIdx >= numOfProjects) {
                return new LinkedList<>();
            }
            int endIdx = startIdx + size > numOfProjects ? numOfProjects : startIdx + size;
            return projects.subList(startIdx, endIdx);
        }

        return projects;
    }

    public static long getTotalProjects(DbSession session, UserSession us, Boolean ownedByMe) {
        Criteria criteria = getProjectMembershipCriteria(session, us);
        if (Nvl.nvl(ownedByMe)) {
            criteria.add(Restrictions.eq("owner", us.getUser().getEmail()).ignoreCase());
            criteria.setProjection(Projections.rowCount());
            Number total = (Number)criteria.uniqueResult();
            return total.longValue();
        } else {
            Set<Integer> abacProjects = getABACProjectIDs(session, us);
            int abacProjectNum = abacProjects.size();
            if (ownedByMe == null) {
                if (abacProjectNum > 0) {
                    criteria.add(Restrictions.not(Restrictions.in("id", abacProjects.toArray(new Integer[abacProjectNum]))));
                }
                criteria.setProjection(Projections.rowCount());
                Number total = (Number)criteria.uniqueResult();
                return total.longValue() + abacProjectNum;
            } else {
                criteria.setProjection(Projections.property("id"));
                criteria.add(Restrictions.eq("owner", us.getUser().getEmail()).ignoreCase());
                @SuppressWarnings("unchecked")
                List<Integer> ownerList = criteria.list();
                criteria = getProjectMembershipCriteria(session, us);
                criteria.setProjection(Projections.property("id"));
                criteria.add(Restrictions.ne("owner", us.getUser().getEmail()).ignoreCase());
                @SuppressWarnings("unchecked")
                List<Integer> invitedList = criteria.list();
                abacProjects.addAll(invitedList);
                abacProjects.removeAll(ownerList);
                return abacProjects.size();
            }
        }
    }

    private static Criteria getProjectMembershipCriteria(DbSession session, UserSession us) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Subqueries.propertyIn("id", getProjectMembershipDetachedCriteria(us)));
        criteria.add(Restrictions.ne("name", UserMgmt.GROUP_NAME_PUBLIC));
        return criteria;
    }

    private static DetachedCriteria getProjectMembershipDetachedCriteria(UserSession us) {
        DetachedCriteria dc = DetachedCriteria.forClass(Membership.class);
        dc.add(Restrictions.eq("user.id", us.getUser().getId()));
        dc.add(Restrictions.eq("status", Membership.Status.ACTIVE));
        dc.setProjection(Projections.property("project.id"));
        return dc;
    }

    public static JsonProject getProjectMetadata(DbSession session, UserSession us, Project project,
        boolean fromListProjects) {
        User user = us.getUser();
        User owner = ProjectService.getOwner(session, project);
        JsonUser jsonOwner = toJson(owner);
        boolean ownedByMe = owner != null && StringUtils.equalsIgnoreCase(owner.getEmail(), user.getEmail());
        boolean showDefaultInvitationMsg = true;
        String expiry = project.getExpiry();
        String watermark = project.getWatermark();
        Long totalMembers = getTotalActiveMembers(session, project, null, null);
        long totalFiles = getTotalFiles(session, project, null, null, null, null);
        JsonProject jsonProject = toJsonProject(project, showDefaultInvitationMsg, fromListProjects);
        jsonProject.setTotalMembers(totalMembers);
        jsonProject.setTotalFiles(totalFiles);
        jsonProject.setExpiry(expiry);
        jsonProject.setWatermark(watermark);
        jsonProject.setOwnedByMe(ownedByMe);
        jsonProject.setOwner(jsonOwner);
        if (project.getCustomerAccount() != null) {
            AccountType accountType = project.getCustomerAccount().getAccountType();
            jsonProject.setAccountType(accountType.toString());
            if (accountType == AccountType.PROJECT_TRIAL) {
                jsonProject.setTrialEndTime(project.getCreationTime().getTime() + PROJECT_TRIAL_DURATION);
            }
        }
        List<Order> orders = new ArrayList<>(1);
        Order order = Order.asc("u.displayName");
        orders.add(order);
        JsonProjectMemberList projectMembers = getJsonActiveMembers(session, us, project.getId(), 1, totalMembers == 6 ? 6 : 5, orders, false, null, null);
        jsonProject.setProjectMembers(projectMembers);
        return jsonProject;
    }

    public static long getTotalActiveMembers(DbSession session, Project project, List<String> searchFieldList,
        String searchString) {
        DetachedCriteria dc = DetachedCriteria.forClass(Membership.class, "m").createCriteria("user", "u");
        dc.add(Restrictions.eq("m.project.id", project.getId()));
        dc.add(Restrictions.eq("m.status", Membership.Status.ACTIVE));
        //        dc.add(Restrictions.eq("m.tenant.id", project.getTenant().getId()));
        if (searchFieldList != null && !searchFieldList.isEmpty() && StringUtils.hasText(searchString)) {
            dc.add(getUserSearchDisjunction(searchFieldList, searchString));
        }
        dc.setProjection(Projections.rowCount());
        Criteria criteria = dc.getExecutableCriteria(session.getSession());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    public static long getTotalPendingMembers(DbSession session, int projectId, String searchString) {
        Criteria criteria = session.createCriteria(ProjectInvitation.class);
        criteria.add(Restrictions.in("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING }));
        criteria.add(Restrictions.eq("project.id", projectId));
        if (StringUtils.hasText(searchString)) {
            criteria.add(EscapedLikeRestrictions.ilike("inviteeEmail", searchString, MatchMode.ANYWHERE));
        }
        criteria.setProjection(Projections.rowCount());
        Number total = (Number)criteria.uniqueResult();
        return total.longValue();
    }

    public static JsonProjectFileInfo getProjectFileInfo(DbSession session, RMSUserPrincipal principal,
        com.nextlabs.rms.eval.User user, int projectId,
        String filePath) {
        Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
        criteria.add(Restrictions.eq("project.id", projectId));
        criteria.add(Restrictions.eq("filePath", filePath));
        ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
        if (item == null) {
            return null;
        } else {
            JsonProjectFileInfo info = toJsonProjectFileInfo(item, session, principal, user);
            info.setOwner(false);
            info.setNxl(true);
            return info;
        }
    }

    public static Project createProject(DbSession session, String projectName, String projectDescription,
        String projectInvitationMessage, Tenant tenant, User user, AccountType accountType,
        KeyStoreEntry keyStoreEntry) {
        final Date now = new Date();

        Project project = new Project();
        project.setName(projectName);
        project.setDisplayName(projectName);
        project.setDescription(projectDescription);
        project.setParentTenant(tenant);
        project.setCreationTime(now);
        project.setLastModified(now);
        project.setConfigurationModified(now);
        if (user != null) {
            project.setOwner(user.getEmail());
            CustomerAccount customerAccount = createCustomerAccount(session, user, now, accountType);
            project.setCustomerAccount(customerAccount);
        }
        project.setStatus(Status.ACTIVE);

        project.setDefaultInvitationMsg(projectInvitationMessage);
        project.setExpiry(DEFAULT_EXPIRY);
        project.setWatermark(DEFAULT_WATERMARK);
        project.setKeystore(keyStoreEntry);
        session.save(project);
        return project;
    }

    private static CustomerAccount createCustomerAccount(DbSession session, User user,
        Date creationTime, AccountType accountType) {
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setCreationTime(creationTime);
        customerAccount.setLastUpdatedTime(creationTime);
        customerAccount.setUser(user);
        customerAccount.setAccountType(accountType);
        session.save(customerAccount);
        return customerAccount;
    }

    /***
     * This will delete the project without performing any check.
     * @param projectId
     */
    public static void deleteProject(int projectId) {
        DefaultRepositoryTemplate repository = null;
        try (DbSession session = DbSession.newSession()) {
            Project project = session.get(Project.class, projectId);
            Criteria criteria = session.createCriteria(User.class);
            criteria.add(Restrictions.eq("email", project.getOwner()));
            User projectOwner = (User)criteria.uniqueResult();
            RMSUserPrincipal principal = new RMSUserPrincipal(projectOwner.getId(), null, project.getParentTenant().getId(), null);
            repository = ProjectService.getProjectRepository(session, principal, projectId);

            session.beginTransaction();
            session.delete(project);
            session.createQuery("DELETE ActivityLog a WHERE a.repositoryId = :projectId").setParameter("projectId", String.valueOf(projectId)).executeUpdate();

            //TODO delete keys
            //TODO delete subscriptions and customer account?

            criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("project.id", projectId)).setProjection(Projections.rowCount());
            Number number = (Number)criteria.uniqueResult();
            if (number != null && number.intValue() > 0) {
                repository.deleteFile("/", "/");
            }
            session.commit();
        } catch (RepositoryException e) {
            LOGGER.error("Error occurred while deleting files in project {} : {}", projectId, e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred while deleting files in project {} : {}", projectId, e.getMessage(), e);
        } finally {
            if (repository instanceof Closeable) {
                IOUtils.closeQuietly(Closeable.class.cast(repository));
            }
        }

    }

    private static JsonUser toJson(User owner) {
        if (owner == null) {
            return null;
        }
        JsonUser user = new JsonUser();
        user.setUserId(owner.getId());
        user.setEmail(owner.getEmail());
        user.setName(owner.getDisplayName());
        return user;
    }

    private static JsonProjectFileInfo toJsonProjectFileInfo(ProjectSpaceItem file, DbSession session,
        RMSUserPrincipal principal, com.nextlabs.rms.eval.User user) {
        DefaultRepositoryTemplate repository = null;
        byte[] downloadedFileBytes = null;
        Map<String, String[]> tags = null;
        JsonProjectFileInfo fileInfo = new JsonProjectFileInfo();
        Rights[] rights = null;
        JsonExpiry expiry = new JsonExpiry();
        fileInfo.setSize(file.getSize());
        String filePathDisplay = file.getFilePathDisplay();
        String filePath = file.getFilePath();
        try {
            repository = new DefaultRepositoryProject(session, principal, file.getProject().getId());
        } catch (InvalidDefaultRepositoryException e) {
            LOGGER.error("Error occurred while getting the project repository", e.getMessage(), e);
        }
        try {
            downloadedFileBytes = RepositoryFileUtil.downloadPartialFileFromRepo(repository, filePath, filePathDisplay);
        } catch (RepositoryException e) {
            LOGGER.error("Error occurred while downloading from the project repository", e.getMessage(), e);
        }
        try (NxlFile metaData = NxlFile.parse(downloadedFileBytes)) {
            try {
                String membership = metaData.getOwner();
                tags = DecryptUtil.getTags(metaData, null);
                FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
                List<Policy> adhocPolicies = policy.getPolicies();
                String tokenGroupName = StringUtils.substringAfter(membership, "@");
                if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                    rights = Rights.fromInt(file.getPermissions());
                    expiry = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                    fileInfo.setProtectionType(ProtectionType.ADHOC.ordinal());
                } else if (EvaluationAdapterFactory.isInitialized()) {
                    fileInfo.setProtectionType(ProtectionType.CENTRAL.ordinal());
                    Tenant parentTenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(file.getFilePathSearchSpace(), membership, parentTenant.getName(), user, tags);
                    rights = PolicyEvalUtil.getUnionRights(responses);
                }
                String[] rightsList = SharedFileManager.toString(rights);
                fileInfo.setRights(rightsList);
                fileInfo.setExpiry(expiry);
            } catch (GeneralSecurityException | IOException | NxlException e) {
                LOGGER.error("Error occurred while getting the tags", e.getMessage(), e);
            }
            fileInfo.setClassification(tags);
        } catch (IOException | NxlException e1) {
            LOGGER.error("Error occurred while parsing the nxl file", e1.getMessage(), e1);
        }
        fileInfo.setName(getFileName(filePathDisplay));
        fileInfo.setPathId(file.getFilePath());
        fileInfo.setPathDisplay(filePathDisplay);
        String fileExt = "";
        if (filePathDisplay.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            String filePathNoNxl = filePathDisplay.substring(0, filePathDisplay.lastIndexOf('.'));
            fileExt = StringUtils.substringAfterLast(filePathNoNxl, ".");
        } else {
            fileExt = StringUtils.substringAfterLast(filePathDisplay, ".");
        }
        fileInfo.setFileType(fileExt);
        fileInfo.setLastModified(file.getLastModified().getTime());
        fileInfo.setCreationTime(file.getCreationTime().getTime());
        JsonProjectMember lastModifiedUser = null;
        User lastUser = file.getLastModifiedUser();
        if (lastUser != null) {
            lastModifiedUser = new JsonProjectMember();
            lastModifiedUser.setUserId(lastUser.getId());
            lastModifiedUser.setDisplayName(lastUser.getDisplayName());
            lastModifiedUser.setEmail(lastUser.getEmail());
        }
        JsonProjectMember createdBy = new JsonProjectMember();
        createdBy.setUserId(file.getUser().getId());
        createdBy.setDisplayName(file.getUser().getDisplayName());
        createdBy.setEmail(file.getUser().getEmail());
        fileInfo.setLastModifiedUser(lastModifiedUser);
        fileInfo.setCreatedBy(createdBy);
        boolean isShared = file.getStatus().ordinal() == 1 ? true : false;
        fileInfo.setShared(isShared);
        fileInfo.setRevoked(ProjectSpaceItem.Status.REVOKED.equals(file.getStatus()) ? true : false);
        if (isShared) {
            fileInfo.setShareWithProjects(getShareWithProjects(file.getDuid()));
        }
        return fileInfo;
    }

    private static Criteria getSharedWithProjectsListQuery(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(SharingRecipientProject.class, "srp").createAlias("srp.transaction", "st").createAlias("st.projectNxl", "ps").createAlias("st.user", "u");
        criteria.add(Restrictions.ne("srp.status", 1));
        criteria.add(Restrictions.ne("st.status", 1));
        criteria.add(Restrictions.eq("ps.status", ProjectSpaceItem.Status.SHARED));
        criteria.add(Restrictions.eq("ps.duid", duid));

        ProjectionList projectionList = Projections.projectionList();
        projectionList.add(Projections.property("u.email").as("sharedByUserEmail"));
        projectionList.add(Projections.property("srp.id.projectId").as("sharedToProjectId"));
        criteria.setProjection(projectionList);
        return criteria;
    }

    public static Map<Integer, Project> getProjectsMap(DbSession session, Set<Integer> projectIds) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.in("id", projectIds));
        List<Project> projectList = criteria.list();
        Map<Integer, Project> projectMap = new HashMap<Integer, Project>();
        for (Project project : projectList) {
            projectMap.put(project.getId(), project);
        }
        return projectMap;
    }

    private static Set<JsonSharedProject> getShareWithProjects(String duid) {
        try (DbSession session = DbSession.newSession()) {
            Set<JsonSharedProject> jsonSharedProjects = new HashSet<JsonSharedProject>();
            Criteria criteria = getSharedWithProjectsListQuery(session, duid);
            List<Object[]> list = criteria.list();

            if (list != null && !list.isEmpty()) {
                Set<Integer> projectIds = new HashSet<Integer>();
                for (Object[] item : list) {
                    JsonSharedProject jsonSharedProject = new JsonSharedProject();
                    jsonSharedProject.setSharedByUserEmail((String)item[0]);
                    jsonSharedProject.setId((Integer)item[1]);
                    jsonSharedProjects.add(jsonSharedProject);
                    projectIds.add(jsonSharedProject.getId());
                }

                Map<Integer, Project> projectMap = getProjectsMap(session, projectIds);
                for (JsonSharedProject jsonSharedProject : jsonSharedProjects) {
                    jsonSharedProject.setName(projectMap.get(jsonSharedProject.getId()).getName());
                }

            }

            return jsonSharedProjects;
        }
    }

    private static JsonProject toJsonProject(Project project, boolean showDefaultInvitationMsg,
        boolean fromListProjects) {
        JsonProject projectDetail = new JsonProject();
        projectDetail.setId(project.getId());
        projectDetail.setName(project.getName());
        projectDetail.setDisplayName(project.getDisplayName());
        projectDetail.setDescription(project.getDescription());
        if (showDefaultInvitationMsg && !fromListProjects) {
            projectDetail.setInvitationMsg(project.getDefaultInvitationMsg());
        }
        projectDetail.setCreationTime(project.getCreationTime().getTime());
        projectDetail.setConfigurationModified(project.getConfigurationModified().getTime());
        projectDetail.setParentTenantId(project.getParentTenant().getId());
        projectDetail.setParentTenantName(project.getParentTenant().getName());
        projectDetail.setTokenGroupName(project.getKeystore().getTokenGroupName());
        return projectDetail;
    }

    public static void persistAndSendInvitations(DbSession session, Project project, User user, Set<String> emails,
        List<String> nowInvited,
        List<String> alreadyMembers, List<String> alreadyInvited, String baseURL, String invitationMsg) {

        session.beginTransaction();

        String alreadyMembersQuery = "SELECT u.email FROM Membership m JOIN m.user u WHERE m.project.id=:projectId" + " AND m.status = :status AND u.email IN (:emails)";

        Query query = session.createQuery(alreadyMembersQuery);
        query.setParameter("projectId", project.getId());
        query.setParameter("status", Membership.Status.ACTIVE);
        query.setParameterList("emails", emails);

        @SuppressWarnings("unchecked")
        List<String> existingMembers = query.list();
        alreadyMembers.addAll(existingMembers);

        String alreadyInvitedQuery = "SELECT m.inviteeEmail FROM ProjectInvitation m WHERE m.project.id=:projectId" + " AND m.status IN (:status) AND m.inviteeEmail IN (:emails)";

        query = session.createQuery(alreadyInvitedQuery);
        query.setParameter("projectId", project.getId());
        query.setParameterList("status", new ProjectInvitation.Status[] { ProjectInvitation.Status.SENT,
            ProjectInvitation.Status.PENDING });
        query.setParameterList("emails", emails);

        @SuppressWarnings("unchecked")
        List<String> invitedMembers = query.list();
        alreadyInvited.addAll(invitedMembers);

        HashSet<String> notToInvite = new HashSet<String>();

        for (String str : alreadyMembers) {
            notToInvite.add(str);
        }

        for (String str : alreadyInvited) {
            notToInvite.add(str);
        }

        Date now = new Date();
        int count = 0;

        Date expire = DateUtils.addDays(DateUtils.midnight(), INVITATION_EXPIRY_DURATION_IN_DAYS);
        List<ProjectInvitation> currentInvitations = new ArrayList<ProjectInvitation>();
        for (String str : emails) {
            if (!notToInvite.contains(str)) {
                ProjectInvitation invitation = buildProjectInvitation(project, user, str, now, expire, invitationMsg);
                session.save(invitation);
                if (count % 20 == 19) {
                    session.flush();
                    session.clear();
                }
                ++count;
                nowInvited.add(str);
                currentInvitations.add(invitation);
            }
        }
        session.commit();
        /*
         * check if there is better way to do this
         */
        for (ProjectInvitation invitation : currentInvitations) {
            try {
                sendProjectInvitationLink(baseURL, invitation);
                session.beginTransaction();
                invitation.setStatus(ProjectInvitation.Status.SENT);
                session.commit();
            } catch (Throwable e) {
                LOGGER.error("Error sending project invitation " + invitation, e);
            }
        }

    }

    private static ProjectInvitation buildProjectInvitation(Project project, User inviter, String email, Date now,
        Date expire, String invitationMsg) {
        ProjectInvitation pi = new ProjectInvitation();
        pi.setExpireDate(expire);
        pi.setInviteTime(now);
        pi.setInviteeEmail(email);
        pi.setInviter(inviter);
        pi.setProject(project);
        pi.setStatus(ProjectInvitation.Status.PENDING);
        pi.setInvitationMsg(invitationMsg);
        return pi;
    }

    private static void sendProjectInvitationLink(String baseURL, ProjectInvitation invitation)
            throws GeneralSecurityException {
        String queryParams = getInvitationURLQueryString(String.valueOf(invitation.getId()));
        String inviteURL = new StringBuilder(baseURL).append("/invitation").append(queryParams).toString();

        Locale locale = Locale.getDefault();

        String userName = invitation.getInviter().getDisplayName();
        if (!StringUtils.hasText(userName)) {
            userName = invitation.getInviter().getEmail();
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        String expiryDate = formatter.format(invitation.getExpireDate());
        Properties prop = new Properties();
        prop.setProperty(Mail.KEY_RECIPIENT, invitation.getInviteeEmail());
        prop.setProperty("projectName", StringEscapeUtils.escapeHtml4(invitation.getProject().getDisplayName()));
        prop.setProperty("ownerFullName", userName);
        prop.setProperty("ownerEmail", invitation.getInviter().getEmail());
        prop.setProperty(Mail.BASE_URL, baseURL);
        prop.setProperty("inviteURL", inviteURL);
        prop.setProperty("expiryDate", expiryDate);
        if (StringUtils.hasText(invitation.getInvitationMsg())) {
            prop.setProperty(Mail.INVITATION_MSG_PREFIX, RMSMessageHandler.getClientString(Mail.INVITATION_MSG_PREFIX));
            prop.setProperty(Mail.INVITATION_MSG, "<p><b>\"</b>" + StringEscapeUtils.escapeHtml4(invitation.getInvitationMsg()) + "<b>\"</b></p>");
        } else {
            prop.setProperty(Mail.INVITATION_MSG, "");
            prop.setProperty(Mail.INVITATION_MSG_PREFIX, "");
        }
        Sender.send(prop, "projectInvitation", locale);
    }

    @SuppressWarnings("unchecked")
    public static void sendFileUploadedLink(String baseURL, String fileName, Project project, User user)
            throws GeneralSecurityException {

        List<String> emails;
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Membership.class);
            criteria.createAlias("user", "u");
            criteria.add(Restrictions.eq("project.id", project.getId()));
            criteria.add(Restrictions.eq("status", Membership.Status.ACTIVE));
            criteria.add(Restrictions.ne("u.id", user.getId()));
            criteria.setProjection(Projections.property("u.email"));
            emails = criteria.list();
        }

        String projectUrl = new StringBuilder(baseURL).append(String.format(PROJECT_FILE_LANDING_PAGE, project.getId())).toString();
        Locale locale = Locale.getDefault();

        String userName = user.getDisplayName();
        if (!StringUtils.hasText(userName)) {
            userName = user.getEmail();
        }

        Properties prop = new Properties();
        prop.setProperty(Mail.KEY_RECIPIENT, StringUtils.join(emails, ","));
        prop.setProperty("projectName", StringEscapeUtils.escapeHtml4(project.getName()));
        prop.setProperty("fileName", StringEscapeUtils.escapeHtml4(fileName));
        prop.setProperty("ownerFullName", userName);
        prop.setProperty("ownerEmail", user.getEmail());
        prop.setProperty(Mail.BASE_URL, baseURL);
        prop.setProperty("projectUrl", projectUrl);
        Sender.send(prop, "projectFileUpload", locale);
    }

    public static String getInvitationURLQueryString(String invitationId) throws GeneralSecurityException {
        String hash = getInvitationCode(invitationId);
        StringBuilder queryString = new StringBuilder("?id=").append(invitationId).append("&code=").append(hash);
        return queryString.toString();
    }

    private static String getInvitationCode(String invitationId) throws GeneralSecurityException {
        return Hex.toHexString(AuthUtils.hmac(invitationId.getBytes(StandardCharsets.UTF_8), KEY, null));
    }

    public static Map<String, String> getProjectInvitationDetails(String id, String code) {
        long invId = 0;
        try {
            if (!isValidInvitation(id, code)) {
                return null;
            }
            invId = Long.parseLong(id.trim());
        } catch (GeneralSecurityException e) {
            LOGGER.error("Error checking invitation validity", e);
            return null;
        }

        Map<String, String> projectDetails = new HashMap<String, String>();

        DbSession session = DbSession.newSession();
        try {
            ProjectInvitation invitation = session.get(ProjectInvitation.class, invId);
            if (invitation == null) {
                return null;
            }
            projectDetails.put(PROJECT_NAME, invitation.getProject().getDisplayName());
            projectDetails.put(PROJECT_DESCRIPTION, invitation.getProject().getDescription());
            projectDetails.put(INVITER, invitation.getInviter().getEmail());
            projectDetails.put(INVITER_DISPLAY, invitation.getInviter().getDisplayName());
            projectDetails.put(INVITEE, invitation.getInviteeEmail());
        } finally {
            session.close();
        }

        return projectDetails;
    }

    public static boolean isValidInvitation(String invitationId, String hmac) throws GeneralSecurityException {
        if (!StringUtils.hasText(invitationId) || !StringUtils.hasText(hmac)) {
            return false;
        }
        String hash = Hex.toHexString(AuthUtils.hmac(invitationId.getBytes(StandardCharsets.UTF_8), KEY, null));
        return StringUtils.equals(hash, hmac);
    }

    public static String getProjectFileDUID(int projectId, String filePath) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("project.id", projectId));
            criteria.add(Restrictions.eq("filePath", filePath));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item == null) {
                return null;
            }
            return item.getDuid();
        }
    }

    public static ProjectSpaceItem getProjectFileByDUID(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
        criteria.add(Restrictions.eq("duid", duid));
        criteria.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<ProjectSpaceItem> items = (List<ProjectSpaceItem>)criteria.list();
        if (!items.isEmpty()) {
            return items.get(0);
        }
        return null;
    }

    public static DefaultRepositoryTemplate getProjectRepository(DbSession session, RMSUserPrincipal userPrincipal,
        int projectId) throws UnauthorizedRepositoryException, InvalidDefaultRepositoryException {
        Criteria criteria = session.createCriteria(UserSession.class);
        criteria.add(Restrictions.eq("user.id", userPrincipal.getUserId()));
        criteria.add(Restrictions.eq("clientId", userPrincipal.getClientId()));
        criteria.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
        UserSession us = null;
        List<UserSession> list = criteria.list();
        if (!list.isEmpty()) {
            final Date now = new Date();
            for (UserSession userSession : list) {
                if (userSession.getExpirationTime().after(now)) {
                    us = userSession;
                    break;
                }
            }
        }
        if (us == null) {
            throw new UnauthorizedRepositoryException(RMSMessageHandler.getClientString("unauthorizedProjectMemberErr"));
        }
        if (checkUserProjectMembership(session, us, projectId, true)) {
            return new DefaultRepositoryProject(session, userPrincipal, projectId);
        } else {
            throw new UnauthorizedRepositoryException(RMSMessageHandler.getClientString("unauthorizedProjectMemberErr"));
        }
    }

    public static boolean checkUserProjectMembership(DbSession session, UserSession us, int projectId,
        boolean checkMembershipPolicies) {
        String alreadyMembersQuery = "SELECT count(*) FROM Membership m JOIN m.user u WHERE m.project.id=:projectId AND m.status = :status AND u.id = :userId";

        Query query = session.createQuery(alreadyMembersQuery);
        query.setParameter("projectId", projectId);
        query.setParameter("status", Membership.Status.ACTIVE);
        query.setParameter("userId", us.getUser().getId());

        return ((Long)query.uniqueResult() > 0 || (checkMembershipPolicies && MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, projectId)));
    }

    public static String getMembership(RMSUserPrincipal userPrincipal, Project project)
            throws IOException, TokenGroupException {
        Membership member = getActiveMembership(userPrincipal.getUserId(), project.getId());
        if (member == null) {
            try (DbSession session = DbSession.newSession()) {
                return UserMgmt.generateDynamicMemberName(userPrincipal.getUserId(), session.load(Project.class, project.getId()).getKeystore().getTokenGroupName());
            }
        } else {
            return member.getName();
        }
    }

    public static Membership getActiveMembership(int userId, int projectId) throws IOException {
        try (DbSession session = DbSession.newSession()) {
            return getActiveMembership(session, userId, projectId);
        }
    }

    public static Membership getActiveMembership(DbSession session, int userId, String tokenGroupName) {
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("user.id", userId));
        criteria.add(Restrictions.eq("status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
        criteria.add(Restrictions.eq("type", com.nextlabs.common.shared.Constants.TokenGroupType.TOKENGROUP_PROJECT));
        criteria.createCriteria("project", "p");
        criteria.createCriteria("p.keystore", "k");
        criteria.add(Restrictions.eq("k.tokenGroupName", tokenGroupName));
        return (Membership)criteria.uniqueResult();
    }

    public static Membership getActiveMembership(DbSession session, int userId, int projectId) {
        Criteria criteria = session.createCriteria(Membership.class);
        criteria.add(Restrictions.eq("user.id", userId));
        criteria.add(Restrictions.eq("project.id", projectId));
        criteria.add(Restrictions.eq("status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
        criteria.setFetchMode("project", FetchMode.JOIN);
        criteria.createAlias("project.keystore", "keystore", JoinType.LEFT_OUTER_JOIN);
        criteria.setFetchMode("keystore", FetchMode.JOIN);
        return (Membership)criteria.uniqueResult();
    }

    public static Membership getPublicMembership(RMSUserPrincipal userPrincipal) throws IOException {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Membership.class, "m").createCriteria("tenant", "t");
            criteria.add(Restrictions.eq("m.user.id", userPrincipal.getUserId()));
            criteria.add(Restrictions.eq("t.name", WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, Constants.DEFAULT_TENANT)));
            criteria.add(Restrictions.eq("m.status", com.nextlabs.rms.hibernate.model.Membership.Status.ACTIVE));
            return (Membership)criteria.uniqueResult();
        }
    }

    public static boolean checkRight(int projectId, String filePath, int userId, Rights right) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("project.id", projectId));
            criteria.add(Restrictions.eq("filePath", filePath));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item != null) {
                if (item.getUser().getId() == userId) {
                    return true;
                }
                Integer permissions = item.getPermissions();
                return ArrayUtils.contains(Rights.fromInt(permissions), right);
            }
            return false;
        }
    }

    public static Rights[] getFileRights(int projectId, String filePath) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("project.id", projectId));
            criteria.add(Restrictions.eq("filePath", filePath));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item == null) {
                return null;
            }
            Integer permissions = item.getPermissions();
            if (permissions == null) {
                return null;
            }
            return Rights.fromInt(permissions);
        }
    }

    public static Map<String, Long> getMyProjectStatus(DbSession session, int projectId) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.eq("id", projectId));
        Project project = (Project)criteria.uniqueResult();
        String myProjectPrefs = project.getPreferences();
        Long usage = 0L;
        Long projectQuota = MY_PROJECT_DEFAULT_QUOTA;
        JsonObject myProjectAttrs = null;
        if (StringUtils.hasText(myProjectPrefs)) {
            myProjectAttrs = GsonUtils.GSON.fromJson(myProjectPrefs, JsonObject.class);
            if (myProjectAttrs.has(RepoConstants.DB_STORAGE_USED)) {
                JsonElement elem = myProjectAttrs.get(RepoConstants.DB_STORAGE_USED);
                usage = elem.getAsLong();
            }
        }
        if (StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.PROJECT_SPACE_QUOTA))) {
            projectQuota = Long.parseLong(WebConfig.getInstance().getProperty(WebConfig.PROJECT_SPACE_QUOTA));
        }
        Map<String, Long> myProjectStatus = new HashMap<>(2);
        myProjectStatus.put(RepoConstants.STORAGE_USED, usage);
        myProjectStatus.put(PROJECT_QUOTA, projectQuota);
        return myProjectStatus;
    }

    public static void checkProjectStorageExceeded(DbSession session, int projectId)
            throws ProjectStorageExceededException {
        Map<String, Long> myProjectStatus = getMyProjectStatus(session, projectId);
        if (myProjectStatus.get(RepoConstants.STORAGE_USED) >= myProjectStatus.get(PROJECT_QUOTA)) {
            throw new ProjectStorageExceededException(myProjectStatus.get(RepoConstants.STORAGE_USED));
        }
    }

    public static Long getFolderLastUpdatedTime(DbSession session, int projectId, String folderPath) {
        Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
        criteria.add(Restrictions.and(Restrictions.eq("project.id", projectId), Restrictions.eq("filePath", folderPath)));
        ProjectSpaceItem folder = (ProjectSpaceItem)criteria.uniqueResult();
        return folder.getLastModified().getTime();
    }

    public static void addRootPath(int userId, int projectId) throws RMSRepositorySearchException {
        StoreItem data = new StoreItem();
        data.setDirectory(true);
        data.setRepoId(Integer.toString(projectId));
        data.setFilePath("/");
        data.setFilePathDisplay("/");
        data.setSize(0);
        data.setCreationTime(new Date());
        data.setLastModified(new Date());
        data.setProjectId(projectId);
        data.setUserId(userId);
        data.setLastModifiedUserId(userId);
        data.setFilePathSearchSpace("/");
        RMSProjectDBSearcherImpl searcher = new RMSProjectDBSearcherImpl();
        searcher.addRepoItem(data);
    }

    public static KeyStoreEntry createProjectKeyStore(String tenantName, String projectName, int userId)
            throws TokenGroupException, GeneralSecurityException, IOException {
        TokenGroupManager tgm = new TokenGroupManager(tenantName, projectName, userId);
        KeyStoreEntry keyStore = tgm.createKeyStore();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable worker = new BackupKeyStore(tgm.getTokenGroupName());
        executor.execute(worker);
        executor.shutdown();
        return keyStore;
    }

    public static Project getProjectByTokenGroupName(DbSession session, String tokenGroupName) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.createCriteria("keystore", "k");
        criteria.add(Restrictions.eq("k.tokenGroupName", tokenGroupName));
        return (Project)criteria.uniqueResult();
    }

    private ProjectService() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<Integer>> getRecipientMapProjects(DbSession session, List<JsonProjectFile> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> duidList = new ArrayList<String>();
        for (JsonProjectFile projectfile : list) {
            if (projectfile.isShared()) {
                duidList.add(projectfile.getDuid());
            }
        }
        if (duidList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<Integer>> recipientMap = new HashMap<String, Set<Integer>>();
        Criteria criteria = session.createCriteria(SharingRecipientProject.class);
        criteria.add(Restrictions.in("id.duid", duidList));
        criteria.add(Restrictions.ne("status", 1));
        List<SharingRecipientProject> recipientList = criteria.list();
        for (SharingRecipientProject recipient : recipientList) {
            Set<Integer> recipients = recipientMap.get(recipient.getId().getDuid());
            if (null == recipients) {
                recipients = new LinkedHashSet<Integer>();
                recipients.add(recipient.getId().getProjectId());
                recipientMap.put(recipient.getId().getDuid(), recipients);
            } else {
                recipients.add(recipient.getId().getProjectId());
            }
        }
        return recipientMap;
    }

    public static boolean checkRight(String duid, Rights right) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("duid", duid));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item != null) {
                Integer permissions = item.getPermissions();
                return ArrayUtils.contains(Rights.fromInt(permissions), right);
            }
            return false;
        }
    }

    public static List<JsonProject> getAllProjects(DbSession session, UserSession us, String tenantId) {
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.eq("parentTenant.id", tenantId));
        criteria.addOrder(Order.asc("name"));
        List<Project> projectList = criteria.list();
        List<JsonProject> jsonProjects = new ArrayList<>();

        for (Project project : projectList) {
            User owner = ProjectService.getOwner(session, project);
            boolean ownedByMe = owner != null && StringUtils.equalsIgnoreCase(owner.getEmail(), us.getUser().getEmail());
            long totalFiles = getTotalFiles(session, project, null, null, null, null);

            JsonProject jsonProject = new JsonProject();
            jsonProject.setId(project.getId());
            jsonProject.setName(project.getName());
            jsonProject.setOwnedByMe(ownedByMe);
            jsonProject.setTotalFiles(totalFiles);
            jsonProject.setCreationTime(project.getCreationTime().getTime());
            jsonProject.setOwner(toJson(owner));

            jsonProjects.add(jsonProject);
        }
        return jsonProjects;
    }

    public static boolean checkFileExists(UserSession us, String loginTenantId, int projectId, String filePath)
            throws InvalidDefaultRepositoryException, RepositoryException {
        try (DbSession session = DbSession.newSession()) {
            Tenant loginTenant = session.get(Tenant.class, loginTenantId);
            RMSUserPrincipal principal = new RMSUserPrincipal(us, loginTenant);
            DefaultRepositoryTemplate repository = ProjectService.getProjectRepository(session, principal, projectId);
            String id = repository.getExistingSpaceItemIdWithFilePath(filePath, String.valueOf(projectId));
            return id != null;
        }
    }

    public static boolean isValidMemberProjectIds(List<Integer> recipientProjIds, UserSession us) {
        try (DbSession session = DbSession.newSession()) {
            List<Project> userProjects = getProjects(session, us, null, null, 0, 0);
            Set<Integer> memberProjects = new HashSet<>();
            for (Project project : userProjects) {
                memberProjects.add(project.getId());
            }
            return memberProjects.containsAll(recipientProjIds);
        }
    }
}

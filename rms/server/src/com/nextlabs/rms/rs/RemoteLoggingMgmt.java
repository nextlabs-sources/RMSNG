package com.nextlabs.rms.rs;

import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.shared.Constants.AccessResult;
import com.nextlabs.common.shared.Constants.AccountType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonActivityLogContainer;
import com.nextlabs.common.shared.JsonActivityLogContainer.JsonActivityLogRecord;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.EscapedLikeRestrictions;
import com.nextlabs.rms.hibernate.model.ActivityLog;
import com.nextlabs.rms.hibernate.model.AllNxl;
import com.nextlabs.rms.hibernate.model.EnterpriseSpaceItem;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.service.EnterpriseWorkspaceService;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.service.UserService;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.CookieUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.JDBCException;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

@Path("/log")
public class RemoteLoggingMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int MAX_PAGE_SIZE = 50;
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withTrim();
    private static final String DEVICE_TYPE_DEFAULT = DeviceType.WEB.getDisplayName();
    private static final Map<Integer, String> OPERATION_MAPPING = new HashMap<>();
    private static final Map<String, Integer> OPERATION_REVERSE_MAPPING = new HashMap<>();
    private static final String OPERATION_DEFAULT = "View";
    private static final Map<String, String> SORT_FIELDS = new HashMap<>();

    private static final String EMAIL = "email";
    private static final String OPERATION = "operation";
    private static final String DEVICE_ID = "deviceId";
    private static final String ACCESS_RESULT = "accessResult";
    private static final String ACCESS_TIME = "accessTime";
    private static final String SORT_FIELD_DEFAULT = ACCESS_TIME;
    private static final String ACCESS_RESULT_ALLOW = "Allow";
    private static final String ACCESS_RESULT_DENY = "Deny";
    private static final String USER_ID = "userId";
    private static final String TICKET = "ticket";
    private static final String CLIENT_ID = "clientId";
    private static final String PLATFORM_ID = "platformId";
    private static final String NONE = "<none>";
    private static final String ACTIVITY_DATA = "activityData";

    static {
        OPERATION_MAPPING.put(Operations.PROTECT.getValue(), "Protect");
        OPERATION_MAPPING.put(Operations.SHARE.getValue(), "Share");
        OPERATION_MAPPING.put(Operations.REMOVE_USER.getValue(), "Remove User");
        OPERATION_MAPPING.put(Operations.VIEW.getValue(), "View");
        OPERATION_MAPPING.put(Operations.PRINT.getValue(), "Print");
        OPERATION_MAPPING.put(Operations.DOWNLOAD.getValue(), "Download");
        OPERATION_MAPPING.put(Operations.EDIT_SAVE.getValue(), "Edit/Save");
        OPERATION_MAPPING.put(Operations.REVOKE.getValue(), "Revoke");
        OPERATION_MAPPING.put(Operations.DECRYPT.getValue(), "Decrypt");
        OPERATION_MAPPING.put(Operations.COPY_CONTENT.getValue(), "Copy Content");
        OPERATION_MAPPING.put(Operations.CAPTURE_SCREEN.getValue(), "Capture Screen");
        OPERATION_MAPPING.put(Operations.CLASSIFY.getValue(), "Classify");
        OPERATION_MAPPING.put(Operations.RESHARE.getValue(), "Reshare");
        OPERATION_MAPPING.put(Operations.DELETE.getValue(), "Delete");
        OPERATION_MAPPING.put(Operations.OFFLINE.getValue(), "Download For Offline");
        OPERATION_MAPPING.put(Operations.REMOVE_PROJECT.getValue(), "Remove Project");
        OPERATION_MAPPING.put(Operations.SYSTEMBUCKET_DOWNLOAD.getValue(), "Download for system bucket");
        OPERATION_MAPPING.put(Operations.SYSTEMBUCKET_PARTIAL_DOWNLOAD.getValue(), "Partial Download for system bucket");
        OPERATION_MAPPING.put(Operations.UPLOAD_EDIT.getValue(), "Upload edited file");
        OPERATION_MAPPING.put(Operations.UPLOAD_NORMAL.getValue(), "Upload file");
        OPERATION_MAPPING.put(Operations.UPLOAD_PROJECT_SYSBUCKET.getValue(), "Upload file");
        OPERATION_MAPPING.put(Operations.UPLOAD_VIEW.getValue(), "Upload view file");
        OPERATION_MAPPING.put(Operations.UPLOAD_PROJECT.getValue(), "Upload file");
        OPERATION_MAPPING.put(Operations.UPLOAD_ASIS.getValue(), "Upload file");
        OPERATION_MAPPING.put(Operations.ADD_FILE_TO.getValue(), "Add file to");
        OPERATION_MAPPING.put(Operations.ADDED_FROM.getValue(), "Added From");
        OPERATION_MAPPING.put(Operations.SAVE_AS.getValue(), "Saved as");
        OPERATION_MAPPING.put(Operations.SAVED_FROM.getValue(), "Saved From");
        for (Map.Entry<Integer, String> entry : OPERATION_MAPPING.entrySet()) {
            OPERATION_REVERSE_MAPPING.put(entry.getValue(), entry.getKey());
        }

        SORT_FIELDS.put(ACCESS_TIME, ACCESS_TIME);
        SORT_FIELDS.put(ACCESS_RESULT, ACCESS_RESULT);
    }

    @PUT
    @Path("/activity/{user_id}/{ticket}")
    @Consumes("text/csv")
    @Produces(MediaType.APPLICATION_JSON)
    public String processActivityLog(@Context HttpServletRequest request, @PathParam("user_id") int userId,
        @PathParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, InputStream input) {

        if (userId < 0 || !StringUtils.hasText(ticket)) {
            String[] params = CookieUtil.getParamsFromCookies(request, USER_ID, TICKET, CLIENT_ID, PLATFORM_ID);
            if (params != null) {
                userId = Integer.parseInt(params[0]);
                ticket = params[1];
                clientId = params[2];
                platformId = Integer.parseInt(params[3]);
            }
        }
        if (userId < 0 || !StringUtils.hasText(ticket)) {
            return new JsonResponse(401, "Missing login parameters").toJson();
        }
        try (DbSession session = DbSession.newSession()) {
            UserSession us = UserMgmt.authenticate(session, userId, ticket, clientId, platformId);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed").toJson();
            }
        }
        return processLog(request, input, userId, platformId, 1);
    }

    @Secured
    @PUT
    @Path("/v2/activity")
    @Consumes("text/csv")
    @Produces(MediaType.APPLICATION_JSON)
    public String processActivityLogV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, InputStream input) {
        return processLog(request, input, userId, platformId, 2);
    }

    public String processLog(HttpServletRequest request, InputStream input, int userId, Integer platformId,
        int version) {
        int count = 0;
        boolean error = true;
        String pId = platformId != null ? String.valueOf(platformId) : NONE;

        try {
            InputStream is = null;
            try (DbSession session = DbSession.newSession()) {
                String contentEncoding = request.getHeader("Content-Encoding");
                if ("gzip".equalsIgnoreCase(contentEncoding)) {
                    is = new GZIPInputStream(input);
                } else if ("deflate".equalsIgnoreCase(contentEncoding)) {
                    is = new InflaterInputStream(input, new Inflater(true));
                } else {
                    is = input;
                }

                // TODO: limit line length to avoid attack with no line return,
                // which will exhaust memory.
                session.beginTransaction();
                try (CSVParser parser = new CSVParser(new InputStreamReader(is, StandardCharsets.UTF_8.name()), CSV_FORMAT)) {
                    List<CSVRecord> records = parser.getRecords();
                    if (!records.isEmpty()) {
                        for (CSVRecord record : records) {
                            ActivityLog log = createActivityLogRecord(record);
                            session.save(log);
                            if (count % 100 == 99) {
                                session.flush();
                                session.clear();
                            }
                            ++count;
                        }
                    }
                }
                session.commit();
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            error = false;
            return new JsonResponse("OK").toJson();
        } catch (IllegalArgumentException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred when processing activity log (user ID: {}, platform ID: {}): {}", userId, pId, e.getMessage());
            }
            return new JsonResponse(400, "Invalid log data").toJson();
        } catch (ZipException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Zip error occurred when processing activity log (user ID: {}, platform ID: {}): {}", userId, pId, e.getMessage(), e);
            }
            return new JsonResponse(400, "Invalid compression.").toJson();
        } catch (JDBCException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("DB error occurred when processing activity log (user ID: {}, platform ID: {}): {}", userId, pId, e.getMessage(), e);
            }
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred when processing activity log (user ID: {}, platform ID: {}): {}", userId, pId, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RemoteLoggingService", "processActivityLog" + version, !error ? 1 : 0, userId, count);
        }
    }

    public static void saveActivityLog(Activity activity) {
        try (DbSession session = DbSession.newSession()) {
            ActivityLog log = createActivityLog(activity.getDuid(), activity.getOwner(), activity.getUserId(), activity.getOperation(), activity.getDeviceId(), activity.getDeviceType(), activity.getRepositoryId(), activity.getFilePathId(), activity.getFileName(), activity.getFilePath(), activity.getAppName(), activity.getAppPath(), activity.getAppPublisher(), activity.getAccessResult(), activity.getAccessTime(), activity.getActivityData(), activity.getType());
            session.beginTransaction();
            session.save(log);
            session.commit();
        }
    }

    public static void saveActivityLog(Activity activity, DbSession session) {
        ActivityLog log = createActivityLog(activity.getDuid(), activity.getOwner(), activity.getUserId(), activity.getOperation(), activity.getDeviceId(), activity.getDeviceType(), activity.getRepositoryId(), activity.getFilePathId(), activity.getFileName(), activity.getFilePath(), activity.getAppName(), activity.getAppPath(), activity.getAppPublisher(), activity.getAccessResult(), activity.getAccessTime(), activity.getActivityData(), activity.getType());
        session.beginTransaction();
        session.save(log);
        session.commit();
    }

    public static ActivityLog createActivityLog(String duid, String owner, int userId, Operations operation,
        String deviceId, int deviceType, String repositoryId, String filePathId, String fileName,
        String filePath, String appName, String appPath, String appPublisher, AccessResult accessResult,
        Date accessTime, String activityData, AccountType type) {
        if (!StringUtils.hasText(duid)) {
            throw new IllegalArgumentException("DUID is required");
        } else if (duid.length() > 36) {
            throw new IllegalArgumentException("Invalid DUID '" + duid + "'");
        }
        if (DeviceType.getDeviceType(deviceType) == null) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid DeviceType: {} for DUID: {}", deviceType, duid);
            }
            throw new IllegalArgumentException("Unknown device type '" + deviceType + "'");
        }
        if (StringUtils.hasText(owner) && owner.length() > 150) {
            throw new IllegalArgumentException("Membership '" + owner + "' exceeds max. length");
        }
        if (StringUtils.hasText(deviceId) && deviceId.length() > 255) {
            throw new IllegalArgumentException("Device ID '" + deviceId + "' exceeds max. length");
        }
        if (StringUtils.hasText(repositoryId) && repositoryId.length() > 36) {
            throw new IllegalArgumentException("Repository ID '" + repositoryId + "' exceeds max. length");
        }
        if (StringUtils.hasText(filePathId) && filePathId.length() > 2000) {
            throw new IllegalArgumentException("File path ID '" + filePathId + "' exceeds max. length");
        }
        if (StringUtils.hasText(fileName) && fileName.length() > 255) {
            throw new IllegalArgumentException("Filename '" + fileName + "' exceeds max. length");
        }
        if (StringUtils.hasText(filePath) && filePath.length() > 2000) {
            throw new IllegalArgumentException("File path display '" + filePath + "' exceeds max. length");
        }
        if (StringUtils.hasText(appName) && appName.length() > 150) {
            throw new IllegalArgumentException("Application name '" + appName + "' exceeds max. length");
        }
        if (StringUtils.hasText(appPath) && appPath.length() > 2000) {
            throw new IllegalArgumentException("Application path '" + appPath + "' exceeds max. length");
        }
        if (StringUtils.hasText(appPublisher) && appPublisher.length() > 150) {
            throw new IllegalArgumentException("Application publisher '" + appPublisher + "' exceeds max. length");
        }
        ActivityLog log = new ActivityLog();
        log.setDuid(duid);
        log.setOwner(owner);
        log.setUserId(userId);
        log.setOperation(operation.getValue());
        log.setDeviceId(deviceId);
        log.setDeviceType(deviceType);
        if (StringUtils.hasText(repositoryId)) {
            log.setRepositoryId(repositoryId);
        }
        log.setFilePathId(filePathId);
        log.setFileName(fileName);
        log.setFilePath(filePath);
        log.setAppName(appName);
        log.setAppPath(appPath);
        log.setAppPublisher(appPublisher);
        log.setAccessResult(accessResult.ordinal());
        log.setAccessTime(accessTime);
        log.setActivityData(activityData);
        log.setAccountType(type);
        return log;
    }

    private ActivityLog createActivityLogRecord(CSVRecord record) {
        // duid,owner,userId,operation,deviceId,deviceType,repositoryId,filePathId,fileName,filePath,appName,appPath,appPublisher,accessResult,accessTime,activityData,logType
        int size = record.size();
        if (size != 16 && size != 17) {
            throw new IllegalArgumentException("Invalid total element for record '" + record.getRecordNumber() + "': " + size);
        }
        String duid = record.get(0);
        String owner = record.get(1);
        int userId = Integer.parseInt(record.get(2).trim());
        int operationValue = Integer.parseInt(record.get(3).trim());
        Operations operation = Operations.lookUpByValue(operationValue);
        if (operation == null) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid operation code: {}", operationValue);
            }
            throw new IllegalArgumentException("Invalid operation for record '" + record.getRecordNumber() + "': " + operationValue);
        }
        String deviceId = record.get(4);
        int deviceTypeValue = Integer.parseInt(record.get(5).trim());
        String repositoryId = StringUtils.hasText(record.get(6)) ? record.get(6) : null;
        String filePathId = record.get(7);
        String fileName = record.get(8);
        String filePath = record.get(9);
        String appName = record.get(10);
        String appPath = record.get(11);
        String appPublisher = record.get(12);
        int accessResultValue = Integer.parseInt(record.get(13).trim());
        if (AccessResult.DENY.ordinal() != accessResultValue && AccessResult.ALLOW.ordinal() != accessResultValue) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid AccessResult: {}", accessResultValue);
            }
            throw new IllegalArgumentException("Invalid access result for record '" + record.getRecordNumber() + "': " + accessResultValue);
        }
        AccessResult accessResult = AccessResult.values()[accessResultValue];
        Date accessTime = new Date(Long.parseLong(record.get(14)));
        String activityData = record.get(15).replace("\\", "");

        int logTypeValue = AccountType.PERSONAL.ordinal();
        if (size == 17) {
            logTypeValue = Integer.parseInt(record.get(16).trim());
            if (logTypeValue < 0 || logTypeValue >= AccountType.values().length) {
                if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Invalid LogTypeValue: {}", logTypeValue);
                }
                throw new IllegalArgumentException("Invalid log type for record '" + record.getRecordNumber() + "': " + logTypeValue);
            }
        }
        AccountType logType = AccountType.values()[logTypeValue];

        return createActivityLog(duid, owner, userId, operation, deviceId, deviceTypeValue, repositoryId, filePathId, fileName, filePath, appName, appPath, appPublisher, accessResult, accessTime, activityData, logType);
    }

    @GET
    @Path("/activity/{userId}/{ticket}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSharedFileActivity(@Context HttpServletRequest request, @PathParam("userId") int userId,
        @PathParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("duid") String duid, @QueryParam("start") int start,
        @QueryParam("count") int count, @QueryParam("searchField") String searchField,
        @QueryParam("searchText") String searchText, @QueryParam("orderBy") String orderBy,
        @QueryParam("orderByReverse") boolean orderByReverse) {

        if (userId < 0 || !StringUtils.hasText(ticket)) {
            String[] params = CookieUtil.getParamsFromCookies(request, USER_ID, TICKET, CLIENT_ID, PLATFORM_ID);
            if (params != null) {
                userId = Integer.parseInt(params[0]);
                ticket = params[1];
                clientId = params[2];
                platformId = Integer.parseInt(params[3]);
            }
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                return new JsonResponse(401, "Missing login parameters").toJson();
            }
        }
        UserSession us;
        try (DbSession session = DbSession.newSession()) {
            us = UserMgmt.authenticate(session, userId, ticket, clientId, platformId);
            if (us == null) {
                return new JsonResponse(401, "Authentication failed").toJson();
            }
        }
        return getActivity(request, us, duid, start, count, searchField, searchText, orderBy, orderByReverse, 1);
    }

    @Secured
    @GET
    @Path("/v2/activity/{duid}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSharedFileActivityV2(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("duid") String duid, @QueryParam("start") int start,
        @QueryParam("count") int count, @QueryParam("searchField") String searchField,
        @QueryParam("searchText") String searchText, @QueryParam("orderBy") String orderBy,
        @QueryParam("orderByReverse") boolean orderByReverse) {
        UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
        return getActivity(request, us, duid, start, count, searchField, searchText, orderBy, orderByReverse, 2);
    }

    private String getActivity(HttpServletRequest request, UserSession us, String duid, int start, int count,
        String searchField, String searchText, String orderBy, boolean orderByReverse, int version) {
        boolean error = true;
        try (DbSession session = DbSession.newSession()) {
            AllNxl sharedNxl = session.get(AllNxl.class, duid);
            String fileName;
            if (sharedNxl == null) {
                ProjectSpaceItem projectItem = ProjectService.getProjectFileByDUID(session, duid);
                if (projectItem == null) {
                    EnterpriseSpaceItem enterprisewsItem = EnterpriseWorkspaceService.getEnterpriseSpaceFileByDUID(session, duid);
                    if (enterprisewsItem == null) {

                        // If file not uploaded, get file info from NxlMetadata.
                        // Validate user to have access.

                        boolean isAccess = false;

                        NxlMetadata nxlMetadataDB = session.get(NxlMetadata.class, duid);
                        if (nxlMetadataDB == null) {
                            return new JsonResponse(404, "File not found in Project nor EnterpriseWS.").toJson();
                        }
                        String owner = nxlMetadataDB.getOwner();
                        String ownerTokenGroupName = StringUtils.substringAfter(owner, "@");
                        TokenGroupManager ownerTokenGroupManager = null;
                        try {
                            ownerTokenGroupManager = TokenGroupManager.newInstance(session, ownerTokenGroupName, us.getLoginTenant());
                        } catch (TokenGroupException e) {
                            return new JsonResponse(404, e.getMessage()).toJson();
                        }
                        switch (ownerTokenGroupManager.getGroupType()) {
                            case TOKENGROUP_TENANT:
                                if (isOwner(us.getUser().getId(), owner)) {
                                    isAccess = true;
                                }

                                break;
                            case TOKENGROUP_PROJECT:
                                Project project = ProjectService.getProjectByTokenGroupName(session, ownerTokenGroupName);
                                User projectOwner = ProjectService.getOwner(session, project);
                                if (projectOwner != null && us.getUser().getId() == projectOwner.getId()) {
                                    isAccess = true;
                                }

                                break;
                            case TOKENGROUP_SYSTEMBUCKET:
                                if (UserService.checkTenantAdmin(session, us.getLoginTenant(), us.getUser().getId())) {
                                    isAccess = true;
                                }

                                break;
                            default:
                                return new JsonResponse(404, "Invalid or unknown token group name").toJson();
                        }
                        if (!isAccess) {
                            return new JsonResponse(403, "Access denied").toJson();
                        }
                        fileName = "";

                    } else if (!us.getLoginTenant().equals(enterprisewsItem.getTenant().getId()) || !UserService.checkTenantAdmin(session, us.getLoginTenant(), us.getUser().getId())) {
                        return new JsonResponse(403, "Access denied").toJson();
                    } else {
                        fileName = enterprisewsItem.getFilePathDisplay().substring(enterprisewsItem.getFilePathDisplay().lastIndexOf("/") + 1);
                    }

                } else if (!ProjectService.checkUserProjectMembership(session, us, projectItem.getProject().getId(), true)) {
                    return new JsonResponse(403, "Access denied").toJson();
                } else {
                    User owner = ProjectService.getOwner(session, projectItem.getProject());
                    if (owner != null && us.getUser().getId() != owner.getId()) {
                        return new JsonResponse(403, "Access denied").toJson();
                    } else {
                        fileName = projectItem.getFilePathDisplay().substring(projectItem.getFilePathDisplay().lastIndexOf("/") + 1);
                    }
                }
            } else {
                if (us.getUser().getId() != sharedNxl.getUser().getId()) {
                    return new JsonResponse(403, "Access denied").toJson();
                }
                fileName = sharedNxl.getFileName();
            }

            JsonActivityLogContainer container = new JsonActivityLogContainer();
            container.setName(fileName);
            container.setDuid(duid);
            populateActivityLogRecords(session, start, count, duid, searchField, searchText, orderBy, orderByReverse, container);

            Criteria criteria = session.createCriteria(ActivityLog.class);
            setSearchCriteria(criteria, duid, searchField, searchText);
            criteria.setProjection(Projections.rowCount());
            Number totalCount = (Number)criteria.uniqueResult();
            JsonResponse response = new JsonResponse(200, "Success");
            response.putResult("totalCount", totalCount.longValue());
            response.putResult("data", container);
            error = false;
            return response.toJson();
        } catch (IllegalArgumentException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", request.getQueryString(), e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "RemoteLoggingService", "getSharedFileActivity" + version, error ? 0 : 1, us.getUser().getId());
        }
    }

    private boolean isOwner(int userId, String owner) {
        boolean isOwner = false;

        try (DbSession session = DbSession.newSession()) {
            Membership membership = session.get(Membership.class, owner);
            if (membership != null && membership.getUser() != null && userId == membership.getUser().getId()) {
                isOwner = true;
            }
        }
        return isOwner;
    }

    private void populateActivityLogRecords(DbSession session, int start, int count, String duid, String searchField,
        String searchText, String orderBy, boolean orderByReverse, JsonActivityLogContainer logContainer) {
        Criteria criteria = session.createCriteria(ActivityLog.class);

        criteria.setProjection(Projections.projectionList().add(Projections.property(USER_ID), USER_ID).add(Projections.property(OPERATION), OPERATION).add(Projections.property("deviceType"), "deviceType").add(Projections.property(DEVICE_ID), DEVICE_ID).add(Projections.property(ACCESS_TIME), ACCESS_TIME).add(Projections.property(ACCESS_RESULT), ACCESS_RESULT).add(Projections.property(ACTIVITY_DATA), ACTIVITY_DATA)).setResultTransformer(Transformers.aliasToBean(ActivityLog.class));

        String sortField = SORT_FIELD_DEFAULT;
        if (SORT_FIELDS.get(orderBy) != null) {
            sortField = SORT_FIELDS.get(orderBy);
            /*
             * because deny = 0 and allow = 1
             */
            if (ACCESS_RESULT.equals(sortField)) {
                orderByReverse = !orderByReverse;
            }
        } else {
            orderByReverse = true;
        }

        if (orderByReverse) {
            criteria.addOrder(Order.desc(sortField));
        } else {
            criteria.addOrder(Order.asc(sortField));
        }
        setSearchCriteria(criteria, duid, searchField, searchText);

        criteria.setFirstResult(start);

        if (count <= 0 || count > MAX_PAGE_SIZE) {
            count = MAX_PAGE_SIZE;
        }
        criteria.setMaxResults(count);

        @SuppressWarnings("unchecked")
        List<ActivityLog> activityLogList = criteria.list();

        if (activityLogList.isEmpty()) {
            return;
        }

        Set<Integer> userIdList = new HashSet<>();

        for (ActivityLog log : activityLogList) {
            userIdList.add(log.getUserId());
        }

        criteria = session.createCriteria(User.class);

        criteria.setProjection(Projections.projectionList().add(Projections.property("id"), "id").add(Projections.property(EMAIL), EMAIL)).setResultTransformer(Transformers.aliasToBean(User.class));

        criteria.add(Restrictions.in("id", userIdList));

        @SuppressWarnings("unchecked")
        List<User> userList = criteria.list();

        Map<Integer, String> idToEmailMapping = new HashMap<>();

        for (User user : userList) {
            idToEmailMapping.put(user.getId(), user.getEmail());
        }
        List<JsonActivityLogRecord> records = new ArrayList<>();
        for (ActivityLog log : activityLogList) {
            records.add(getJsonLogRecord(log, idToEmailMapping.get(log.getUserId())));
        }
        logContainer.setLogRecords(records);
    }

    private JsonActivityLogRecord getJsonLogRecord(ActivityLog log, String email) {
        JsonActivityLogRecord record = new JsonActivityLogRecord();
        record.setEmail(email);
        record.setAccessResult(log.getAccessResult() == 0 ? ACCESS_RESULT_DENY : ACCESS_RESULT_ALLOW);
        record.setAccessTime(log.getAccessTime().getTime());
        record.setApplicationPublisher(log.getAppPublisher());
        String deviceType = Objects.requireNonNull(DeviceType.getDeviceType(log.getDeviceType())).getDisplayName();
        if (deviceType == null) {
            deviceType = DEVICE_TYPE_DEFAULT;
        }
        record.setDeviceType(deviceType);
        record.setDeviceId(log.getDeviceId());
        String operation = OPERATION_MAPPING.get(log.getOperation());
        if (operation == null) {
            operation = OPERATION_DEFAULT;
        }
        record.setOperation(operation);
        record.setActivityData(log.getActivityData());
        return record;
    }

    private void setSearchCriteria(Criteria criteria, String duid, String searchField, String searchText) {
        criteria.add(Restrictions.eq("duid", duid));
        if (searchField == null || searchField.isEmpty() || searchText == null || searchText.isEmpty()) {
            return;
        }
        if (EMAIL.equals(searchField)) {
            appendUserCriteria(criteria, searchText);
        } else if (OPERATION.equals(searchField)) {
            appendOperationCriteria(criteria, searchText);
        } else if (DEVICE_ID.equals(searchField)) {
            appendDeviceIdCriteria(criteria, searchText);
        }
    }

    private List<String> getMatchingEntries(Set<String> data, String regex) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (String str : data) {
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                result.add(str);
            }
        }
        return result;
    }

    private void appendOperationCriteria(Criteria criteria, String searchText) {
        List<String> matchingOperations = getMatchingEntries(OPERATION_REVERSE_MAPPING.keySet(), ".*" + searchText + ".*");
        List<Integer> operationIds = new ArrayList<>();
        operationIds.add(-1);

        for (String operation : matchingOperations) {
            operationIds.add(OPERATION_REVERSE_MAPPING.get(operation));
        }

        criteria.add(Restrictions.in("operation", operationIds));
    }

    private void appendDeviceIdCriteria(Criteria criteria, String searchText) {
        criteria.add(EscapedLikeRestrictions.ilike("deviceId", searchText, MatchMode.ANYWHERE));
    }

    private void appendUserCriteria(Criteria criteria, String searchText) {
        DetachedCriteria subCriteria = DetachedCriteria.forClass(User.class);
        subCriteria.add(EscapedLikeRestrictions.like("email", searchText.toLowerCase(Locale.getDefault()), MatchMode.ANYWHERE));
        subCriteria.setProjection(Projections.property("id"));
        criteria.add(Property.forName("userId").in(subCriteria));
    }

    public static class Activity {

        String duid;
        String owner;
        int userId;
        Operations operation;
        String deviceId;
        int deviceType;
        String repositoryId;
        String filePathId;
        String fileName;
        String filePath;
        String appName;
        String appPath;
        String appPublisher;
        AccessResult accessResult;
        Date accessTime;
        String activityData;
        AccountType type;

        public Activity(String duid, String owner, int userId, Operations operation, String deviceId, int deviceType,
            String repositoryId, String filePathId, String fileName, String filePath, String appName, String appPath,
            String appPublisher, AccessResult accessResult, Date accessTime, String activityData, AccountType type) {
            this.duid = duid;
            this.owner = owner;
            this.userId = userId;
            this.operation = operation;
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.repositoryId = repositoryId;
            this.filePathId = filePathId;
            this.fileName = fileName;
            this.filePath = filePath;
            this.appName = appName;
            this.appPath = appPath;
            this.appPublisher = appPublisher;
            this.accessResult = accessResult;
            this.accessTime = accessTime;
            this.activityData = activityData;
            this.type = type;
        }

        public String getDuid() {
            return duid;
        }

        public void setDuid(String duid) {
            this.duid = duid;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public Operations getOperation() {
            return operation;
        }

        public void setOperation(Operations operation) {
            this.operation = operation;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public int getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(int deviceType) {
            this.deviceType = deviceType;
        }

        public String getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(String repositoryId) {
            this.repositoryId = repositoryId;
        }

        public String getFilePathId() {
            return filePathId;
        }

        public void setFilePathId(String filePathId) {
            this.filePathId = filePathId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getAppPath() {
            return appPath;
        }

        public void setAppPath(String appPath) {
            this.appPath = appPath;
        }

        public String getAppPublisher() {
            return appPublisher;
        }

        public void setAppPublisher(String appPublisher) {
            this.appPublisher = appPublisher;
        }

        public AccessResult getAccessResult() {
            return accessResult;
        }

        public void setAccessResult(AccessResult accessResult) {
            this.accessResult = accessResult;
        }

        public Date getAccessTime() {
            return accessTime;
        }

        public void setAccessTime(Date accessTime) {
            this.accessTime = accessTime;
        }

        public String getActivityData() {
            return activityData;
        }

        public void setActivityData(String activityData) {
            this.activityData = activityData;
        }

        public AccountType getType() {
            return type;
        }

        public void setType(AccountType type) {
            this.type = type;
        }
    }
}

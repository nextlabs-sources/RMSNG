package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.JsonRepoFile;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RepositoryFileManager.FileSyncResult;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Order;

@Path("/favorite")
public class FavoriteMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String MY_DRIVE_FILES = "mydrive";
    private static final String MY_VAULT_FILES = "myvault";
    private static final String ALL_FILES = "all";

    @Secured
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getFavoriteFilesFromAllRepos(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                String[] repoIds = RepositoryManager.getRepoIdsWithTenant(session, user);
                Map<String, FileSyncResult> resultMap = RepositoryFileManager.getAllFavoriteFiles(session, repoIds, null);
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("repos", new ArrayList<FileSyncResult>(resultMap.values()));
                error = false;
                return resp.toJson();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "FavoriteMgmt", "getFavoriteFilesFromAllRepos", error ? 0 : 1, userId);
        }
    }

    @Secured
    @POST
    @Path("/{repository_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String markFavorite(@Context HttpServletRequest request, @PathParam("repository_id") String repoId,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            if (!StringUtils.hasText(repoId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            List<JsonRepoFile> jsonFiles = new ArrayList<>();
            for (JsonWraper wraper : req.getParameterAsList("files")) {
                JsonRepoFile file = wraper.getAsObject(JsonRepoFile.class);
                if (file == null || !StringUtils.hasText(file.getPathId()) || !StringUtils.hasText(file.getPathDisplay()) || !StringUtils.hasText(file.getParentFileId())) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                }
                jsonFiles.add(file);
            }
            if (jsonFiles.isEmpty()) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                if (!RepositoryManager.isRepoOwner(session, user, repoId)) {
                    return new JsonResponse(403, "Unauthorized repository access").toJson();
                }
                RepositoryFileManager.markFilesAsFavorite(session, repoId, jsonFiles);
                JsonResponse resp = new JsonResponse("Files successfully marked as favorite.");
                error = false;
                return resp.toJson();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "FavoriteMgmt", "markFavorite", error ? 0 : 1, userId, repoId);
        }
    }

    @Secured
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String listFavoriteFiles(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @QueryParam("page") Integer page,
        @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy) {

        // Return all favorite files
        return listFavoriteFiles(request, new ArrayList<String>(), userId, ALL_FILES, page, size, orderBy);

    }

    @Secured
    @GET
    @Path("/list/{repository_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String listFavoriteFiles(@Context HttpServletRequest request,
        @PathParam("repository_id") String repoId, @HeaderParam("userId") int userId,
        @QueryParam("page") Integer page, @QueryParam("size") Integer size,
        @QueryParam("filter") String filter, @QueryParam("orderBy") String orderBy) {

        List<String> repoList = new ArrayList<String>();
        repoList.add(repoId);

        if (filter != null) {
            return listFavoriteFiles(request, repoList, userId, filter, page, size, orderBy);
        } else {
            // Default to all favorite files
            return listFavoriteFiles(request, repoList, userId, ALL_FILES, page, size, orderBy);
        }
    }

    private String listFavoriteFiles(@Context HttpServletRequest request, List<String> repoList,
        int userId, String filter, Integer page, Integer size, String orderBy) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            page = page != null && page.intValue() > 0 ? page : 1;
            size = size != null && size.intValue() > 0 ? size : -1;
            List<Order> orders = Collections.emptyList();
            if (StringUtils.hasText(orderBy)) {
                Map<String, String> supportedFields = new HashMap<>(4);
                supportedFields.put("name", "f.filePathSearchSpace");
                supportedFields.put("lastModifiedTime", "f.lastModified");
                supportedFields.put("fileSize", "f.fileSize");
                OrderBuilder builder = new OrderBuilder(supportedFields);
                List<String> list = StringUtils.tokenize(orderBy, ",");
                for (String s : list) {
                    builder.add(s);
                }
                orders = builder.build();
            }
            String searchString = request.getParameter("q.fileName");
            try (DbSession session = DbSession.newSession()) {

                // Default to all repo if none is given
                String[] repoIds = repoList.toArray(new String[repoList.size()]);
                if (repoIds.length == 0) {
                    User user = us.getUser();
                    repoIds = RepositoryManager.getRepoIdsWithTenant(session, user);
                }

                List<RepositoryContent> result = null;
                if (filter.equalsIgnoreCase(MY_DRIVE_FILES)) {
                    // All files in my drive are 
                    result = RepositoryFileManager.getNonProtectedFavoriteFileInList(session, repoIds, page, size, orders, searchString);
                } else if (filter.equalsIgnoreCase(MY_VAULT_FILES)) {
                    result = RepositoryFileManager.getProtectedFavoriteFileInList(session, repoIds, page, size, orders, searchString);
                } else {
                    result = RepositoryFileManager.getAllFavoriteFileInList(session, repoIds, page, size, orders, searchString);
                }

                JsonResponse resp = new JsonResponse("OK");
                if (size > 0 && result.size() > size) {
                    resp.putResult("loadMore", true);
                    resp.putResult("results", result.subList(0, size));
                } else {
                    resp.putResult("loadMore", false);
                    resp.putResult("results", result);
                }
                error = false;
                return resp.toJson();
            }
        } finally {
            Audit.audit(request, "API", "FavoriteMgmt", "listFavoriteFiles", error ? 0 : 1, userId);
        }
    }

    @Secured
    @GET
    @Path("/{repository_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getFavoriteFiles(@Context HttpServletRequest request, @PathParam("repository_id") String repoId,
        @QueryParam("lastModified") long lastModified, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            if (!StringUtils.hasText(repoId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                if (!RepositoryManager.isRepoOwner(session, user, repoId)) {
                    return new JsonResponse(403, "Unauthorized repository access").toJson();
                }
                Date since = lastModified != 0L ? new Date(lastModified) : null;

                FileSyncResult result = RepositoryFileManager.getFavoriteFilesSince(session, repoId, since);
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("isFullCopy", result.isFullCopy());
                resp.putResult("markedFavoriteFiles", result.getMarkedFavoriteFiles());
                resp.putResult("unmarkedFavoriteFiles", result.getUnmarkedFavoriteFiles());
                error = false;
                return resp.toJson();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "FavoriteMgmt", "getFavoriteFiles", error ? 0 : 1, userId, repoId);
        }
    }

    @Secured
    @DELETE
    @Path("/{repository_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String unmarkFavorite(@Context HttpServletRequest request, @PathParam("repository_id") String repoId,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, String json) {
        boolean error = true;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            if (!StringUtils.hasText(repoId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            List<JsonRepoFile> jsonFiles = new ArrayList<>();
            for (JsonWraper wraper : req.getParameterAsList("files")) {
                JsonRepoFile file = wraper.getAsObject(JsonRepoFile.class);
                if (file == null || !StringUtils.hasText(file.getPathId())) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                }
                jsonFiles.add(file);
            }

            if (jsonFiles.isEmpty()) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                if (!RepositoryManager.isRepoOwner(session, user, repoId)) {
                    return new JsonResponse(403, "Unauthorized repository access").toJson();
                }
                RepositoryFileManager.unmarkFilesAsFavorite(session, repoId, jsonFiles);
                JsonResponse resp = new JsonResponse("Files successfully unmarked as favorite.");
                error = false;
                return resp.toJson();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "FavoriteMgmt", "unmarkFavorite", error ? 0 : 1, userId, repoId);
        }
    }
}

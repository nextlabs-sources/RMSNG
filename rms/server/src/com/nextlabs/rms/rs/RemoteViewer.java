package com.nextlabs.rms.rs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Repository;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.repository.RepoConstants;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.UploadUtil;
import com.nextlabs.rms.util.Audit;
import com.nextlabs.rms.util.CookieUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/remoteView")
public class RemoteViewer {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String API_INPUT = "API-input";
    private static final String FILE = "file";

    @Secured
    @POST
    @Path("/local")
    @Produces(MediaType.APPLICATION_JSON)
    public String proxyUploadAndView(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId) {
        boolean error = true;
        HttpURLConnection urlConnection = null;
        OutputStream outputStreamToRequestBody = null;
        InputStream remoteResponseStream = null;
        InputStream fileStream = null;
        File uploadTmpDir = null;
        String fileName = null;
        String user = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) { //NOPMD
            }

            uploadTmpDir = RepositoryFileUtil.getTempOutputFolder();
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(UploadUtil.THRESHOLD_SIZE);
            factory.setRepository(uploadTmpDir);
            ServletFileUpload upload = new ServletFileUpload();
            upload.setHeaderEncoding("UTF-8");
            upload.setFileItemFactory(factory);
            upload.setSizeMax(UploadUtil.REQUEST_SIZE);
            Iterator<?> iter = upload.parseRequest(request).iterator();

            String json = null;
            while (iter.hasNext()) {
                FileItem item = (FileItem)iter.next();
                if (API_INPUT.equals(item.getFieldName())) {
                    json = item.getString("UTF-8");
                } else if (FILE.equals(item.getFieldName())) {
                    fileStream = item.getInputStream();
                }
            }

            JsonRequest req = JsonRequest.fromJson(json);

            if (req == null || fileStream == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            fileName = req.getParameter("fileName");
            user = req.getParameter("userName");
            String tenantName = req.getParameter("tenantName");
            String offset = req.getParameter("offset");
            int operations = req.getIntParameter("operations", 0);

            if (!StringUtils.hasText(offset)) {
                offset = "0";
            }

            if (!StringUtils.hasText(fileName) || !StringUtils.hasText(user) || !StringUtils.hasText(tenantName) || !StringUtils.hasText(clientId)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            WebConfig config = WebConfig.getInstance();
            StringBuilder sb = new StringBuilder(256);
            sb.append(config.getProperty(WebConfig.VIEWER_INTERNAL_URL, config.getProperty(WebConfig.VIEWER_URL)));
            sb.append("/RMSViewer/UploadAndView?operations=");
            sb.append(operations);

            URL url = new URL(sb.toString());
            urlConnection = (HttpURLConnection)url.openConnection();

            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(RepoConstants.READ_TIMEOUT);
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                urlConnection.setRequestProperty(headerName, request.getHeader(headerName));
            }

            urlConnection.setRequestMethod("POST");
            String boundaryString = "--------------RMSBoundary" + System.currentTimeMillis();
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);

            urlConnection.setDoOutput(true);

            outputStreamToRequestBody = urlConnection.getOutputStream();

            String boundary = "\r\n--" + boundaryString + "\r\n";
            String start = "\r\n" + boundary;
            String end = "\r\n--" + boundaryString + "--\r\n";
            byte[] boundaryBytes = boundary.getBytes("UTF-8");

            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            tempStream.write(start.getBytes("UTF-8"));
            addNormalFieldToBody(tempStream, "userName", user, boundaryBytes);
            addNormalFieldToBody(tempStream, "tenantName", tenantName, boundaryBytes);
            addNormalFieldToBody(tempStream, "uid", String.valueOf(userId), boundaryBytes);
            addNormalFieldToBody(tempStream, "offset", offset, boundaryBytes);
            addNormalFieldToBody(tempStream, "ticket", ticket, boundaryBytes);
            addNormalFieldToBody(tempStream, "clientId", clientId, boundaryBytes);
            addNormalFieldToBody(tempStream, "platformId", String.valueOf(platformId), boundaryBytes);

            tempStream.write(new StringBuilder("Content-Disposition: form-data;name=\"file\";filename=\"").append(fileName).append("\"").append("\r\nContent-Type: application/octet-stream\r\n\r\n").toString().getBytes("UTF-8"));

            IOUtils.copy(new ByteArrayInputStream(tempStream.toByteArray()), outputStreamToRequestBody);
            IOUtils.copy(fileStream, outputStreamToRequestBody);
            outputStreamToRequestBody.write(end.getBytes("UTF-8"));

            outputStreamToRequestBody.flush();
            outputStreamToRequestBody.close();

            int code = urlConnection.getResponseCode();

            if (code != 200) {
                LOGGER.error("Error code from viewer (user ID: {}, fileName: {}): {}", userId, fileName, code);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }

            remoteResponseStream = urlConnection.getInputStream();

            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String header = entry.getKey();
                if (!"Content-Length".equalsIgnoreCase(header)) {
                    response.addHeader(header, entry.getValue().get(0));
                }
            }

            List<String> cookiesResp = map.get("Set-Cookie");
            List<String> cookies;
            if (cookiesResp != null && !cookiesResp.isEmpty()) {
                cookies = new ArrayList<>(cookiesResp.size() + 7);
                cookies.addAll(cookiesResp);
            } else {
                cookies = new ArrayList<>(7);
            }
            long ttl = us.getTtl();
            final String ticketHex = Hex.toHexString(us.getTicket());
            final String domainName = CookieUtil.getCookieDomainName(request);
            int maxAge = (int)(ttl >= 0 ? TimeUnit.MILLISECONDS.toSeconds(ttl - System.currentTimeMillis()) : -1);
            NewCookie[] newCookies = new NewCookie[deviceId != null ? 6 : 5];
            DeviceType deviceType = DeviceType.getDeviceType(platformId);
            if (deviceType == DeviceType.RMX) {
                newCookies[0] = new NewCookie("rmxUserId", String.valueOf(userId), "/", domainName, "", maxAge, true, false);
                newCookies[1] = new NewCookie("rmxTicket", ticketHex, "/", domainName, "", maxAge, true, false);
                newCookies[2] = new NewCookie("rmxClientId", clientId, "/", domainName, "", maxAge, true, false);
                newCookies[3] = new NewCookie("rmxIdp", String.valueOf(us.getLoginType().ordinal()), "/", domainName, "", maxAge, true, false);
                newCookies[4] = new NewCookie("rmxPlatformId", String.valueOf(platformId), "/", domainName, "", maxAge, true, false);
                if (deviceId != null) {
                    newCookies[5] = new NewCookie("rmxDeviceId", URLEncoder.encode(deviceId, "UTF-8"), "/", domainName, "", maxAge, true, false);
                }
            } else {
                newCookies[0] = new NewCookie("userId", String.valueOf(userId), "/", domainName, "", maxAge, true, false);
                newCookies[1] = new NewCookie("ticket", ticketHex, "/", domainName, "", maxAge, true, false);
                newCookies[2] = new NewCookie("clientId", clientId, "/", domainName, "", maxAge, true, false);
                newCookies[3] = new NewCookie("idp", String.valueOf(us.getLoginType().ordinal()), "/", domainName, "", maxAge, true, false);
                newCookies[4] = new NewCookie("platformId", String.valueOf(platformId), "/", domainName, "", maxAge, true, false);
                if (deviceId != null) {
                    newCookies[5] = new NewCookie("deviceId", URLEncoder.encode(deviceId, "UTF-8"), "/", domainName, "", maxAge, true, false);
                }
            }
            for (NewCookie cookie : newCookies) {
                cookies.add(cookie.toString());
            }

            tempStream = new ByteArrayOutputStream();
            IOUtils.copy(remoteResponseStream, tempStream);

            JsonObject output = GsonUtils.GSON.fromJson(new String(tempStream.toByteArray(), "UTF-8"), JsonObject.class);
            JsonElement viewerElement = output.get("viewerUrl");
            JsonResponse jsonResponse = null;
            JsonElement statusCode = output.get("statusCode");
            JsonElement rightsElement = output.get("rights");
            JsonElement ownerElement = output.get("owner");
            JsonElement duidElement = output.get("duid");
            JsonElement membershipElement = output.get("membership");
            int status = statusCode.getAsInt();
            String message = "OK";
            if (status != 200) {
                JsonElement errorElement = output.get("error");
                message = "Error";
                if (errorElement != null && StringUtils.hasText(errorElement.getAsString())) {
                    message = errorElement.getAsString();
                }
            }
            jsonResponse = new JsonResponse(status, message);
            if (viewerElement != null && StringUtils.hasText(viewerElement.getAsString())) {
                jsonResponse.putResult("viewerURL", deviceType == DeviceType.RMX ? getViewerUrl(config, viewerElement.getAsString()) + "&source=rmx" : getViewerUrl(config, viewerElement.getAsString()));
                jsonResponse.putResult("permissions", (rightsElement != null) ? rightsElement.getAsInt() : 0);
                jsonResponse.putResult("owner", ownerElement != null && ownerElement.getAsBoolean());
                if (duidElement != null) {
                    jsonResponse.putResult("duid", duidElement.getAsString());
                }
                if (membershipElement != null) {
                    jsonResponse.putResult("membership", membershipElement.getAsString());
                }
            }
            jsonResponse.putResult("cookies", cookies);
            error = false;
            return jsonResponse.toJson();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Socket timeout when performing remote viewer (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return new JsonResponse(500, "Timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO error occurred when performing remote viewer (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (FileUploadException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File upload error occurred when performing remote viewer (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return new JsonResponse(500, "File Upload Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred when performing remote viewer (user ID: {}): {}", userId, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (urlConnection != null) {
                IOUtils.skipAll(urlConnection.getErrorStream());
                IOUtils.closeQuietly(urlConnection.getErrorStream());
            }
            IOUtils.close(urlConnection);
            IOUtils.closeQuietly(fileStream);
            IOUtils.closeQuietly(remoteResponseStream);
            IOUtils.closeQuietly(outputStreamToRequestBody);
            FileUtils.deleteQuietly(uploadTmpDir);
            Audit.audit(request, "API", "RemoteViewer", "local", error ? 0 : 1, userId, user, fileName);
        }
    }

    private void addNormalFieldToBody(OutputStream outputStream, String name, String value, byte[] boundaryBytes)
            throws IOException {
        StringBuilder strBuilder = new StringBuilder("Content-Disposition: form-data; name=\"").append(name).append("\"").append("\r\n\r\n").append(value);
        IOUtils.copy(new ByteArrayInputStream(strBuilder.toString().getBytes("UTF-8")), outputStream);
        outputStream.write(boundaryBytes);
    }

    @Secured
    @POST
    @Path("/repository")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String proxyShowFileCommand(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {
        boolean error = true;
        String email = null;
        HttpURLConnection urlConnection = null;
        InputStream remoteResponseStream = null;
        String repoId = null;
        String repoType = null;
        String filePath = null;
        String filePathDisplay = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) { //NOPMD
            }
            repoId = req.getParameter("repoId");
            filePath = req.getParameter("pathId");
            filePathDisplay = req.getParameter("pathDisplay");
            String offset = req.getParameter("offset");
            email = req.getParameter("email");
            String tenantName = req.getParameter("tenantName");
            String repoName = req.getParameter("repoName");
            repoType = req.getParameter("repoType");
            String lastModifiedDateStr = req.getParameter("lastModifiedDate");
            int operations = req.getIntParameter("operations", 0);

            if (!StringUtils.hasText(offset)) {
                offset = "0";
            }

            if (!StringUtils.hasText(lastModifiedDateStr)) {
                lastModifiedDateStr = "0";
            }

            if (!StringUtils.hasText(repoId) || !StringUtils.hasText(filePath) || !StringUtils.hasText(filePathDisplay) || !StringUtils.hasText(repoName) || !StringUtils.hasText(repoType)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                Repository repository = session.get(Repository.class, repoId);
                if (repository == null) {
                    return new JsonResponse(404, "Repository not found.").toJson();
                }
                if (repository.getUserId() != userId) {
                    return new JsonResponse(403, "Access denied.").toJson();
                }
            }

            WebConfig config = WebConfig.getInstance();
            StringBuilder sb = new StringBuilder(256);
            sb.append(config.getProperty(WebConfig.VIEWER_INTERNAL_URL, config.getProperty(WebConfig.VIEWER_URL)));
            sb.append("/RMSViewer/ShowFile?repoId=");
            sb.append(URLEncoder.encode(repoId, "UTF-8"));
            sb.append("&filePath=").append(URLEncoder.encode(filePath, "UTF-8"));
            sb.append("&filePathDisplay=").append(URLEncoder.encode(filePathDisplay, "UTF-8"));
            sb.append("&offset=").append(URLEncoder.encode(offset, "UTF-8"));
            sb.append("&repoName=").append(URLEncoder.encode(repoName, "UTF-8"));
            sb.append("&repoType=").append(URLEncoder.encode(repoType, "UTF-8"));
            sb.append("&lastModifiedDate=").append(URLEncoder.encode(lastModifiedDateStr, "UTF-8"));
            sb.append("&userName=").append(URLEncoder.encode(email, "UTF-8"));
            sb.append("&tenantName=").append(URLEncoder.encode(tenantName, "UTF-8"));
            sb.append("&operations=").append(operations);

            URL url = new URL(sb.toString());
            urlConnection = (HttpURLConnection)url.openConnection();

            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(RepoConstants.READ_TIMEOUT);
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                urlConnection.setRequestProperty(headerName, request.getHeader(headerName));
            }

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            int code = urlConnection.getResponseCode();

            if (code != 200) {
                LOGGER.error("Error code from viewer (user ID: {}, repoId: {}, filePath: {}): {}", userId, repoId, filePath, code);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }

            remoteResponseStream = urlConnection.getInputStream();

            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String header = entry.getKey();
                if (!"Content-Length".equalsIgnoreCase(header)) {
                    response.addHeader(header, entry.getValue().get(0));
                }
            }

            List<String> cookiesResp = map.get("Set-Cookie");
            List<String> cookies;
            if (cookiesResp != null && !cookiesResp.isEmpty()) {
                cookies = new ArrayList<>(cookiesResp.size() + 7);
                cookies.addAll(cookiesResp);
            } else {
                cookies = new ArrayList<>(7);
            }
            Tenant publicTenant = AbstractLogin.getDefaultTenant();
            long ttl = us.getTtl();
            final String ticketHex = Hex.toHexString(us.getTicket());
            final String domainName = CookieUtil.getCookieDomainName(request);
            int maxAge = (int)(ttl >= 0 ? TimeUnit.MILLISECONDS.toSeconds(ttl - System.currentTimeMillis()) : -1);
            NewCookie[] newCookies = new NewCookie[deviceId != null ? 7 : 6];
            newCookies[0] = new NewCookie("userId", String.valueOf(userId), "/", domainName, "", maxAge, true, false);
            newCookies[1] = new NewCookie("ticket", ticketHex, "/", domainName, "", maxAge, true, false);
            newCookies[2] = new NewCookie("clientId", clientId, "/", domainName, "", maxAge, true, false);
            newCookies[3] = new NewCookie("tenantId", publicTenant.getId(), "/", domainName, "", maxAge, true, false);
            newCookies[4] = new NewCookie("idp", String.valueOf(us.getLoginType().ordinal()), "/", domainName, "", maxAge, true, false);
            newCookies[5] = new NewCookie("platformId", String.valueOf(platformId), "/", domainName, "", maxAge, true, false);
            if (deviceId != null) {
                newCookies[6] = new NewCookie("deviceId", URLEncoder.encode(deviceId, "UTF-8"), "/", domainName, "", maxAge, true, false);
            }
            for (NewCookie cookie : newCookies) {
                cookies.add(cookie.toString());
            }

            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            IOUtils.copy(remoteResponseStream, tempStream);

            JsonObject output = GsonUtils.GSON.fromJson(new String(tempStream.toByteArray(), "UTF-8"), JsonObject.class);
            JsonElement viewerElement = output.get("viewerUrl");
            JsonElement statusCode = output.get("statusCode");
            JsonElement rightsElement = output.get("rights");
            JsonElement ownerElement = output.get("owner");
            JsonElement duidElement = output.get("duid");
            JsonElement membershipElement = output.get("membership");
            JsonResponse jsonResponse = null;
            int status = statusCode.getAsInt();
            if (viewerElement != null && StringUtils.hasText(viewerElement.getAsString())) {
                String message = (status == 200 ? "OK" : "Error");
                jsonResponse = new JsonResponse(status, message);
                jsonResponse.putResult("viewerURL", getViewerUrl(config, viewerElement.getAsString()));
                jsonResponse.putResult("cookies", cookies);
                jsonResponse.putResult("permissions", (rightsElement != null) ? rightsElement.getAsInt() : 0);
                jsonResponse.putResult("owner", ownerElement != null && ownerElement.getAsBoolean());
                if (duidElement != null) {
                    jsonResponse.putResult("duid", duidElement.getAsString());
                }
                if (membershipElement != null) {
                    jsonResponse.putResult("membership", membershipElement.getAsString());
                }
                error = false;
                return jsonResponse.toJson();
            }
            return new JsonResponse(status, "Internal Server Error").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unable to parse JSON (user ID: {}, platformId: {}, value: {}): {}", userId, platformId, json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Socket timeout when performing remote viewer from repository (user ID: {}, repository ID: {}): {}", userId, repoId, e.getMessage(), e);
            }
            return new JsonResponse(500, "Timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO error occurred when performing remote viewer from repository (user ID: {}, repository ID: {}): {}", userId, repoId, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred when performing remote viewer from repository (user ID: {}, repository ID: {}): {}", userId, repoId, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (urlConnection != null) {
                IOUtils.skipAll(urlConnection.getErrorStream());
                IOUtils.closeQuietly(urlConnection.getErrorStream());
            }
            IOUtils.close(urlConnection);
            IOUtils.closeQuietly(remoteResponseStream);
            Audit.audit(request, "API", "RemoteViewer", "repository", error ? 0 : 1, userId, email, repoId, repoType, filePath, filePathDisplay);

        }
    }

    @Secured
    @POST
    @Path("/project")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String proxyShowProjectFileCommand(@Context HttpServletRequest request,
        @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId, @HeaderParam("platformId") Integer platformId,
        @HeaderParam("deviceId") String deviceId, String json) {
        boolean error = true;
        String filePathDisplay = null;
        String email = null;
        HttpURLConnection urlConnection = null;
        InputStream remoteResponseStream = null;
        int projectId = -1;
        String filePath = null;
        try {
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            try {
                deviceId = StringUtils.hasText(deviceId) ? URLDecoder.decode(deviceId, "UTF-8") : null;
            } catch (UnsupportedEncodingException e) { //NOPMD
            }

            filePath = req.getParameter("pathId");
            filePathDisplay = req.getParameter("pathDisplay");
            String offset = req.getParameter("offset");
            String lastModifiedDateStr = req.getParameter("lastModifiedDate");
            projectId = req.getIntParameter("projectId", -1);
            String tenantName = req.getParameter("tenantName");
            email = req.getParameter("email");
            int operations = req.getIntParameter("operations", 0);

            if (!StringUtils.hasText(offset)) {
                offset = "0";
            }

            if (!StringUtils.hasText(lastModifiedDateStr)) {
                lastModifiedDateStr = "0";
            }

            if (!StringUtils.hasText(filePath) || !StringUtils.hasText(filePathDisplay) || projectId < 0 || !StringUtils.hasText(email) || !StringUtils.hasText(tenantName)) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }

            try (DbSession session = DbSession.newSession()) {
                if (!ProjectService.checkUserProjectMembership(session, us, projectId, true)) {
                    return new JsonResponse(403, "Access denied").toJson();
                }
            }

            WebConfig config = WebConfig.getInstance();
            StringBuilder sb = new StringBuilder(256);
            sb.append(config.getProperty(WebConfig.VIEWER_INTERNAL_URL, config.getProperty(WebConfig.VIEWER_URL)));
            sb.append("/RMSViewer/ShowProjectFile?projectId=");
            sb.append(projectId);
            sb.append("&pathId=").append(URLEncoder.encode(filePath, "UTF-8"));
            sb.append("&pathDisplay=").append(URLEncoder.encode(filePathDisplay, "UTF-8"));
            sb.append("&offset=").append(URLEncoder.encode(offset, "UTF-8"));
            sb.append("&lastModifiedDate=").append(URLEncoder.encode(lastModifiedDateStr, "UTF-8"));
            sb.append("&promptDownload=false&userName=");
            sb.append(URLEncoder.encode(email, "UTF-8"));
            sb.append("&tenantName=").append(URLEncoder.encode(tenantName, "UTF-8"));
            sb.append("&operations=").append(operations);

            URL url = new URL(sb.toString());
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(RepoConstants.READ_TIMEOUT);
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                urlConnection.setRequestProperty(headerName, request.getHeader(headerName));
            }
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            int code = urlConnection.getResponseCode();

            if (code != 200) {
                LOGGER.error("Error code from viewer (user ID: {}, repoId: {}, filePath: {}): {}", userId, projectId, filePath, code);
                return new JsonResponse(500, "Internal Server Error").toJson();
            }

            remoteResponseStream = urlConnection.getInputStream();

            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String header = entry.getKey();
                if (!"Content-Length".equalsIgnoreCase(header)) {
                    response.addHeader(header, entry.getValue().get(0));
                }
            }

            List<String> cookiesResp = map.get("Set-Cookie");
            List<String> cookies;
            if (cookiesResp != null && !cookiesResp.isEmpty()) {
                cookies = new ArrayList<>(cookiesResp.size() + 7);
                cookies.addAll(cookiesResp);
            } else {
                cookies = new ArrayList<>(7);
            }
            Tenant publicTenant = AbstractLogin.getDefaultTenant();
            long ttl = us.getTtl();
            final String ticketHex = Hex.toHexString(us.getTicket());
            final String domainName = CookieUtil.getCookieDomainName(request);
            int maxAge = (int)(ttl >= 0 ? TimeUnit.MILLISECONDS.toSeconds(ttl - System.currentTimeMillis()) : -1);
            NewCookie[] newCookies = new NewCookie[deviceId != null ? 7 : 6];
            newCookies[0] = new NewCookie("userId", String.valueOf(userId), "/", domainName, "", maxAge, true, false);
            newCookies[1] = new NewCookie("ticket", ticketHex, "/", domainName, "", maxAge, true, false);
            newCookies[2] = new NewCookie("clientId", clientId, "/", domainName, "", maxAge, true, false);
            newCookies[3] = new NewCookie("tenantId", publicTenant.getId(), "/", domainName, "", maxAge, true, false);
            newCookies[4] = new NewCookie("idp", String.valueOf(us.getLoginType().ordinal()), "/", domainName, "", maxAge, true, false);
            newCookies[5] = new NewCookie("platformId", String.valueOf(platformId), "/", domainName, "", maxAge, true, false);
            if (deviceId != null) {
                newCookies[6] = new NewCookie("deviceId", URLEncoder.encode(deviceId, "UTF-8"), "/", domainName, "", maxAge, true, false);
            }
            for (NewCookie cookie : newCookies) {
                cookies.add(cookie.toString());
            }

            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            IOUtils.copy(remoteResponseStream, tempStream);

            JsonObject output = GsonUtils.GSON.fromJson(new String(tempStream.toByteArray(), "UTF-8"), JsonObject.class);
            JsonElement viewerElement = output.get("viewerUrl");
            JsonElement statusCode = output.get("statusCode");
            JsonElement rightsElement = output.get("rights");
            JsonElement ownerElement = output.get("owner");
            JsonElement duidElement = output.get("duid");
            JsonElement membershipElement = output.get("membership");
            JsonResponse jsonResponse = null;
            int status = statusCode.getAsInt();
            if (viewerElement != null && StringUtils.hasText(viewerElement.getAsString())) {
                String message = (status == 200 ? "OK" : "Error");
                jsonResponse = new JsonResponse(status, message);
                jsonResponse.putResult("viewerURL", getViewerUrl(config, viewerElement.getAsString()));
                jsonResponse.putResult("cookies", cookies);
                jsonResponse.putResult("permissions", (rightsElement != null) ? rightsElement.getAsInt() : 0);
                jsonResponse.putResult("owner", ownerElement != null && ownerElement.getAsBoolean());
                if (duidElement != null) {
                    jsonResponse.putResult("duid", duidElement.getAsString());
                }
                if (membershipElement != null) {
                    jsonResponse.putResult("membership", membershipElement.getAsString());
                }
                error = false;
                return jsonResponse.toJson();
            }
            return new JsonResponse(status, "Internal Server Error").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unable to parse JSON (user ID: {}, platformId: {}, value: {}): {}", userId, platformId, json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (SocketTimeoutException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Socket timeout when performing remote viewer from Project (user ID: {}, project ID: {}, filePathId: {}): {}", userId, projectId, filePath, e.getMessage(), e);
            }
            return new JsonResponse(500, "Timeout.").toJson();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO error occurred when performing remote viewer from Project (user ID: {}, project ID: {}, filePathId: {}): {}", userId, projectId, filePath, e.getMessage(), e);
            }
            return new JsonResponse(500, "IO Error.").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred when performing remote viewer from Project (user ID: {}, project ID: {}, filePathId: {}): {}", userId, projectId, filePath, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            if (urlConnection != null) {
                IOUtils.skipAll(urlConnection.getErrorStream());
                IOUtils.closeQuietly(urlConnection.getErrorStream());
            }
            IOUtils.close(urlConnection);
            IOUtils.closeQuietly(remoteResponseStream);
            Audit.audit(request, "API", "RemoteViewer", "project", error ? 0 : 1, userId, email, projectId, filePath, filePathDisplay);
        }
    }

    private String getViewerUrl(WebConfig config, String urlFromViewer) {
        String viewerInternalURL = config.getProperty(WebConfig.VIEWER_INTERNAL_URL);
        if (StringUtils.hasText(viewerInternalURL)) {
            return config.getProperty(WebConfig.VIEWER_URL) + urlFromViewer.substring(urlFromViewer.indexOf("/viewer/") + 7);
        }
        return urlFromViewer;
    }

}

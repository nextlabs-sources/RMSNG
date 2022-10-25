package com.nextlabs.rms.rs;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/convert")
public class ConvertFile {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @POST
    @Path("/file/{userId}/{ticket}")
    public Response proxy(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @PathParam("userId") int userId, @PathParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {

        if (userId < 0 || !StringUtils.hasText(ticket)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Missing login parameters.").build();
        }

        try (DbSession session = DbSession.newSession()) {
            UserSession us = UserMgmt.authenticate(session, userId, ticket, clientId, platformId);
            if (us == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication failed").build();
            }
        }
        return convertFile(request, response, userId, 1);
    }

    @Secured
    @POST
    @Path("/v2/file")
    public Response proxyV2(@Context HttpServletRequest request, @Context HttpServletResponse response,
        @HeaderParam("userId") int userId, @HeaderParam("ticket") String ticket,
        @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId) {
        return convertFile(request, response, userId, 2);
    }

    private Response convertFile(HttpServletRequest request, HttpServletResponse response, int userId, int version) {
        boolean error = true;
        HttpURLConnection conn = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            WebConfig config = WebConfig.getInstance();
            StringBuilder sb = new StringBuilder(256);
            sb.append(config.getProperty(WebConfig.VIEWER_INTERNAL_URL, config.getProperty(WebConfig.VIEWER_URL)));
            sb.append("/RMSViewer/ConvertFile?");
            sb.append(request.getQueryString());

            URL url = new URL(sb.toString());
            HttpURLConnection.setFollowRedirects(false);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod(request.getMethod());
            conn.setConnectTimeout(20000);
            conn.setReadTimeout((int)TimeUnit.MINUTES.toMillis(5));
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                conn.setRequestProperty(headerName, request.getHeader(headerName));
            }

            conn.setDoOutput(true);
            os = conn.getOutputStream();
            IOUtils.copy(request.getInputStream(), os);
            os.close();
            os = null;

            int code = conn.getResponseCode();
            response.setStatus(code);

            Map<String, List<String>> map = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String header = entry.getKey();
                if (!"Content-Length".equalsIgnoreCase(header)) {
                    response.addHeader(header, entry.getValue().get(0));
                }
            }

            if (code == 200) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }
            if (is != null) {
                os = response.getOutputStream();
                IOUtils.copy(is, os);
                os.close();
                os = null;
            }
            error = false;
            return null;
        } catch (SocketTimeoutException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Socket timeout when converting file (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return Response.status(500).entity("Timeout.").build();
        } catch (IOException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("IO error occurred when converting file (user ID: {}): {}", userId, e.getMessage(), e);
            }
            return Response.status(500).entity("IO Error.").build();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(500).entity("File Conversion failed").build();
        } finally {
            if (conn != null) {
                IOUtils.skipAll(conn.getErrorStream());
                IOUtils.closeQuietly(conn.getErrorStream());
            }
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            IOUtils.close(conn);
            Audit.audit(request, "API", "ConvertFile" + version, "proxy", error ? 0 : 1, userId);
        }

    }
}

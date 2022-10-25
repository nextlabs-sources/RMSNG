package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadSharedWithMeFileCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {

        String transactionId = request.getParameter("transactionId");
        String transactionCode = request.getParameter("transactionCode");
        String spaceId = request.getParameter("spaceId");
        String decrypt = request.getParameter("decrypt");
        CloseableHttpResponse postResponse = null;
        InputStream is = null;
        OutputStream os = null;
        CloseableHttpClient client = null;

        try {
            if (!StringUtils.hasText(transactionId) || !StringUtils.hasText(transactionCode)) {
                redirectToErrorPage(request, response, "err.shared.with.me.file.download");
                return;
            }
            RMSUserPrincipal userPrincipal = null;
            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            client = HTTPUtil.getHTTPClient();
            postResponse = getSharedWithMeFileResponse(client, request, transactionId, transactionCode, spaceId, decrypt, userPrincipal);
            for (Header header : postResponse.getAllHeaders()) {
                if (!"Content-Length".equalsIgnoreCase(header.getName())) {
                    response.addHeader(header.getName(), header.getValue());
                }
            }
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                is = postResponse.getEntity().getContent();
                if (is != null) {
                    os = response.getOutputStream();
                    IOUtils.copy(is, os);
                    os.close();
                    os = null;
                } else {
                    redirectToErrorPage(request, response, "err.shared.with.me.file.download");
                    return;
                }
            } else if (statusCode == 403) {

                if ("true".equals(decrypt)) {
                    redirectToErrorPage(request, response, "error.extract.no.permission");
                } else {
                    redirectToErrorPage(request, response, "error.download.no.permission");
                }

                return;
            } else if (statusCode == 4001) {
                redirectToErrorPage(request, response, "err.unauthorized.view");
                return;
            } else if (statusCode == 4002) {
                redirectToErrorPage(request, response, "err.shared.with.me.file.deleted");
                return;
            } else {
                if ("true".equals(decrypt)) {
                    redirectToErrorPage(request, response, "err.project.file.extract");
                } else {
                    redirectToErrorPage(request, response, "err.shared.with.me.file.download");
                }
                return;
            }
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            redirectToErrorPage(request, response, "err.shared.with.me.file.download");
            return;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    public static CloseableHttpResponse getSharedWithMeFileResponse(CloseableHttpClient client,
        HttpServletRequest request, String transactionId, String transactionCode, String spaceId, String decrypt,
        RMSUserPrincipal userPrincipal)
            throws GeneralSecurityException, IOException {
        String urlString = ("true".equals(decrypt)) ? (HTTPUtil.getInternalURI(request) + "/rs/sharedWithMe/decrypt") : (HTTPUtil.getInternalURI(request) + "/rs/sharedWithMe/download");

        HttpPost postRequest = new HttpPost(urlString);
        JsonRequest jsonRequest = new JsonRequest();
        jsonRequest.addParameter("transactionId", transactionId);
        jsonRequest.addParameter("transactionCode", transactionCode);
        jsonRequest.addParameter("forViewer", "false");
        jsonRequest.addParameter("spaceId", spaceId);
        postRequest.setEntity(new StringEntity(jsonRequest.toJson(), "UTF-8"));
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = headers.nextElement();
            postRequest.setHeader(headerName, request.getHeader(headerName));
        }
        postRequest.setHeader("userId", userPrincipal.getUserId() + "");
        postRequest.setHeader("ticket", userPrincipal.getTicket());
        postRequest.setHeader("clientId", userPrincipal.getClientId());
        Integer platformId = userPrincipal.getPlatformId();
        if (platformId != null) {
            postRequest.setHeader("platformId", String.valueOf(platformId));
        }
        postRequest.setHeader("Content-Type", "application/json");
        postRequest.setHeader(HTTPUtil.HEADER_X_FORWARDED_FOR, HTTPUtil.getRemoteAddress(request));
        return client.execute(postRequest);
    }

    private void redirectToErrorPage(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        String redirectURL = request.getContextPath() + "/error?code=" + code;
        if (!response.isCommitted()) {
            response.sendRedirect(response.encodeURL(redirectURL));
        }
    }
}

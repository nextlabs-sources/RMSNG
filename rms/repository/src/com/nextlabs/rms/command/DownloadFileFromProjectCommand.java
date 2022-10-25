package com.nextlabs.rms.command;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.DownloadType;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class DownloadFileFromProjectCommand extends AbstractCommand {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String pathId = request.getParameter("pathId");
        int projectId = Integer.parseInt(request.getParameter("projectId"));
        String start = request.getParameter("start");
        String length = request.getParameter("length");
        String decrypt = request.getParameter("decrypt");

        String urlString = ("true".equals(decrypt)) ? HTTPUtil.getInternalURI(request) + "/rs/project/" + projectId + "/decrypt" : HTTPUtil.getInternalURI(request) + "/rs/project/" + projectId + "/v2/download";

        CloseableHttpResponse postResponse = null;
        InputStream is = null;
        OutputStream os = null;
        CloseableHttpClient client = null;

        try {
            RMSUserPrincipal userPrincipal = null;

            try (DbSession session = DbSession.newSession()) {
                userPrincipal = authenticate(session, request);
                if (userPrincipal == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            HttpPost postRequest = new HttpPost(urlString);

            JsonRequest jsonRequest = new JsonRequest();
            jsonRequest.addParameter("pathId", pathId);
            if ((!"true".equals(decrypt))) {
                jsonRequest.addParameter("type", DownloadType.NORMAL.ordinal());
            }
            if (start != null) {
                jsonRequest.addParameter("start", start);
            }
            if (length != null) {
                jsonRequest.addParameter("length", length);
            }
            if (decrypt != null) {
                jsonRequest.addParameter("decrypt", decrypt);
            }
            postRequest.setEntity(new StringEntity(jsonRequest.toJson(), "UTF-8"));

            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                postRequest.setHeader(headerName, request.getHeader(headerName));
            }
            postRequest.setHeader("userId", userPrincipal.getUserId() + "");
            postRequest.setHeader("ticket", userPrincipal.getTicket());
            postRequest.setHeader("clientId", userPrincipal.getClientId());
            if (userPrincipal.getPlatformId() != null) {
                postRequest.setHeader("platformId", String.valueOf(userPrincipal.getPlatformId()));
            }
            postRequest.setHeader("Content-Type", "application/json");
            postRequest.setHeader(HTTPUtil.HEADER_X_FORWARDED_FOR, HTTPUtil.getRemoteAddress(request));

            client = HTTPUtil.getHTTPClient();
            postResponse = client.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            switch (statusCode) {
                case 200:
                    is = postResponse.getEntity().getContent();
                    Header contentDisposition = postResponse.getFirstHeader("Content-Disposition");
                    if (contentDisposition != null) {
                        response.setHeader(contentDisposition.getName(), contentDisposition.getValue());
                    }
                    if (is != null) {
                        os = response.getOutputStream();
                        IOUtils.copy(is, os);
                        os.close();
                        os = null;
                    } else {
                        redirectToErrorPage(request, response, "err.project.file.download");
                    }
                    break;
                case 401:
                    redirectToTimeOutPage(request, response);
                    break;
                case 403:
                    if ("true".equals(decrypt)) {
                        redirectToErrorPage(request, response, "error.extract.no.permission");
                    } else {
                        redirectToErrorPage(request, response, "error.download.no.permission");
                    }
                    break;
                case 404:
                    redirectToErrorPage(request, response, "err.project.file.not.found");
                    break;
                default:
                    if ("true".equals(decrypt)) {
                        redirectToErrorPage(request, response, "err.project.file.extract");
                    } else {
                        redirectToErrorPage(request, response, "err.project.file.download");
                    }
                    break;
            }
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            redirectToErrorPage(request, response, "err.project.file.download");
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(postResponse);
            IOUtils.closeQuietly(client);
        }
    }

    private void redirectToErrorPage(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        String redirectURL = request.getContextPath() + "/error?code=" + code;
        if (!response.isCommitted()) {
            response.sendRedirect(response.encodeURL(redirectURL));
        }
    }

    private void redirectToTimeOutPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectURL = request.getContextPath() + "/timeout";
        if (!response.isCommitted()) {
            response.sendRedirect(response.encodeURL(redirectURL));
        }
    }
}

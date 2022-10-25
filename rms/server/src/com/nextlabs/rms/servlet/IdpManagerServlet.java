package com.nextlabs.rms.servlet;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.rms.idp.AzureAdOAuthHandler;
import com.nextlabs.rms.idp.FacebookOAuthHandler;
import com.nextlabs.rms.idp.GoogleOAuthHandler;
import com.nextlabs.rms.idp.LdapAuthHandler;
import com.nextlabs.rms.idp.LdapAuthHandler.LdapException;
import com.nextlabs.rms.idp.LdapAuthHandler.RMSLdapException;
import com.nextlabs.rms.idp.LdapIdpAttributes;
import com.nextlabs.rms.idp.SamlAuthHandler;
import com.nextlabs.rms.idp.SamlAuthHandler.SamlException;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IdpManagerServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final long serialVersionUID = 3092566310043076819L;

    public static final String GOOGLE_AUTH_START_URL = "/IdpManager/GoogleAuth/GoogleAuthStart";
    public static final String GOOGLE_AUTH_FINISH_URL = "/IdpManager/GoogleAuth/GoogleAuthFinish";
    public static final String FACEBOOK_AUTH_START_URL = "/IdpManager/FacebookAuth/FacebookAuthStart";
    public static final String FACEBOOK_AUTH_FINISH_URL = "/IdpManager/FacebookAuth/FacebookAuthFinish";
    public static final String AZURE_AUTH_START_URL = "/IdpManager/AzureAdAuth/AzureAdAuthStart";
    public static final String AZURE_AUTH_FINISH_URL = "/IdpManager/AzureAdAuth/AzureAdAuthFinish";
    public static final String SAML_AUTH_START_URL = "/IdpManager/SamlAuth/SamlAuthStart";
    public static final String SAML_METADATA_URL = "/IdpManager/SamlAuth/SamlMetadata";
    public static final String SAML_AUTH_FINISH_URL = "/IdpManager/SamlAuth/SamlAuthFinish";
    public static final String LDAP_AUTH_FINISH_URL = "/IdpManager/LdapAuth/LdapAuthFinish";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            String uri = request.getRequestURI();
            if (uri.endsWith(SAML_AUTH_FINISH_URL)) {
                handleSamlSignIn(request, response);
                return;
            }
            if (uri.endsWith(LDAP_AUTH_FINISH_URL)) {
                handleLdapSignIn(request, response);
                return;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        try {
            if (uri.endsWith(SAML_AUTH_START_URL) || uri.endsWith(SAML_METADATA_URL)) {
                handleSamlSignIn(request, response);
                return;
            }
            String redirectUri = "";
            if (uri.endsWith(GOOGLE_AUTH_START_URL) || uri.endsWith(GOOGLE_AUTH_FINISH_URL)) {
                redirectUri = handleGoogleSignIn(request, uri);
            } else if (uri.endsWith(FACEBOOK_AUTH_START_URL) || uri.endsWith(FACEBOOK_AUTH_FINISH_URL)) {
                redirectUri = handleFacebookSignIn(request, uri);
            } else if (uri.endsWith(AZURE_AUTH_START_URL)) {
                redirectUri = handleAzureAuthRequest(request);
            } else if (uri.endsWith(AZURE_AUTH_FINISH_URL)) {
                redirectUri = handleAzureSignIn(request);
            }
            if (!redirectUri.startsWith("http") && !redirectUri.startsWith(request.getServletContext().getServletContextName())) {
                redirectUri = new StringBuilder(request.getContextPath()).append(redirectUri).toString();
            }
            response.sendRedirect(redirectUri);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private String handleGoogleSignIn(HttpServletRequest request, String uri) {
        try {
            if (uri.endsWith(GOOGLE_AUTH_START_URL)) {
                return GoogleOAuthHandler.startAuth(request);
            } else if (uri.endsWith(GOOGLE_AUTH_FINISH_URL)) {
                return GoogleOAuthHandler.finishAuth(request);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return HeaderFilter.LOGIN_ENDPOINT + "?code=500g";
    }

    private String handleFacebookSignIn(HttpServletRequest request, String uri) {
        try {
            if (uri.endsWith(FACEBOOK_AUTH_START_URL)) {
                return FacebookOAuthHandler.startAuth(request);
            } else if (uri.endsWith(FACEBOOK_AUTH_FINISH_URL)) {
                return FacebookOAuthHandler.finishAuth(request);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return HeaderFilter.LOGIN_ENDPOINT + "?code=500f";
    }

    private String handleAzureAuthRequest(HttpServletRequest request) {
        try {
            return AzureAdOAuthHandler.startAuth(request);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return HeaderFilter.LOGIN_ENDPOINT + "?code=500g";
    }

    private String handleAzureSignIn(HttpServletRequest request) {
        try {
            return AzureAdOAuthHandler.finishAuth(request);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return HeaderFilter.LOGIN_ENDPOINT + "?code=500g";
    }

    private void handleSamlSignIn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        try {
            if (uri.endsWith(SAML_AUTH_START_URL)) {
                SamlAuthHandler auth = new SamlAuthHandler();
                auth.startAuth(request, response);
            } else if (uri.endsWith(SAML_AUTH_FINISH_URL)) {
                SamlAuthHandler auth = new SamlAuthHandler();
                auth.finishAuth(request, response);
            } else if (uri.endsWith(SAML_METADATA_URL)) {
                SamlAuthHandler auth = new SamlAuthHandler();
                auth.getMetadata(request, response);
            }
        } catch (IOException | SamlException e) {
            LOGGER.error(e.getMessage(), e);
            response.sendRedirect(new StringBuilder(request.getContextPath()).append(HeaderFilter.LOGIN_ENDPOINT).append("?code=500s").toString());
        }
    }

    private void handleLdapSignIn(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        try {
            String userName = request.getParameter("userName");
            String password = request.getParameter("password");
            String idpId = request.getParameter(AbstractLogin.IDP_ID);

            LdapIdpAttributes ldapIdpAttributes = LdapAuthHandler.getLdapAttributes(idpId);
            if (ldapIdpAttributes == null) {
                LOGGER.error("Unknown domain id: " + idpId);
                throw new LdapException(RMSMessageHandler.getClientString("authError"));
            }

            LdapAuthHandler ldapAuthHandler = new LdapAuthHandler(ldapIdpAttributes);
            boolean res = ldapAuthHandler.authenticate(idpId, request, response, userName, password);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Authentication result for user " + userName + " : " + res);
            }

            StringBuilder sb = new StringBuilder(request.getContextPath()).append(HeaderFilter.LOGIN_ENDPOINT).append("?i=").append(Constants.LoginType.LDAP.ordinal());
            response.sendRedirect(sb.toString());

        } catch (LdapException e) {
            if (e instanceof RMSLdapException) {
                redirectToLoginPage(response, request, "403l", e.getMessage());
                throw new IOException(e.getMessage(), e);
            }
            redirectToLoginPage(response, request, "500l", "Error occurred while logging in with LDAP.");
            throw new IOException("Error occurred while logging in with LDAP.", e);
        } catch (AuthenticationException e) {
            redirectToLoginPage(response, request, String.valueOf(HttpServletResponse.SC_UNAUTHORIZED), "The username or password you entered is incorrect.");
        }
    }

    private static void redirectToLoginPage(HttpServletResponse response,
        HttpServletRequest request, String code, String errorMsg)
            throws ServletException, IOException {
        if (isAjax(request)) {
            Map<String, String> result = new HashMap<>(2);
            result.put("error", errorMsg);
            result.put("code", code);
            com.nextlabs.rms.shared.JsonUtil.writeJsonToResponse(result, response);
            return;
        } else {
            StringBuilder redirectURL = new StringBuilder(request.getContextPath()).append(HeaderFilter.LOGIN_ENDPOINT).append("?code=").append(code).append("&msg=").append(URLEncoder.encode(errorMsg, "UTF-8"));
            response.sendRedirect(redirectURL.toString());
            return;
        }
    }

    private static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}

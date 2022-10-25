package com.nextlabs.rms.idp;

import com.google.gson.Gson;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.servlet.HeaderFilter;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SamlAuthHandler extends AbstractLogin {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final String EMAIL_SAML = "email";
    private static final String USERNAME_SAML = "name";
    private static final String UNIQUE_ID_KEY = "evalUserIdAttribute";
    private static final Map<String, Properties> IDP_MAP = new ConcurrentHashMap<String, Properties>();

    public SamlAuthHandler() {

    }

    public void getMetadata(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String idpId = request.getParameter(IDP_ID);
        Properties samlProps = getSamlProps(idpId);
        if (samlProps == null) {
            response.getWriter().write("SP metadata is not available for the specified IDP.");
            return;
        }
        Saml2Settings settings = new SettingsBuilder().fromProperties(samlProps).build();
        settings.setSPValidationOnly(true);
        try {
            String metadata = settings.getSPMetadata();
            List<String> errors = Saml2Settings.validateMetadata(metadata);
            if (errors.isEmpty()) {
                response.getWriter().write(metadata);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String error : errors) {
                    sb.append(error).append(System.lineSeparator());
                }
                LOGGER.error("Error occurred while validating metadata: " + sb.toString());
                response.getWriter().write("Error occurred while validating SP metadata.");
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while getting SP metadata for idp {}", idpId);
            LOGGER.error(e.getMessage(), e);
            response.getWriter().write("Error occurred while getting SP metadata.");
        }
    }

    public void startAuth(HttpServletRequest request, HttpServletResponse response)
            throws SamlException, IOException {
        try {
            State state = buildState(request);
            Properties samlProps = getSamlProps(state.getIdpId());
            if (samlProps == null) {
                throw new SamlException("SAML authentication is not configured for idp: " + state.getIdpId());
            }
            Saml2Settings settings = new SettingsBuilder().fromProperties(samlProps).build();
            Auth auth = new Auth(settings, request, response);
            auth.login(new Gson().toJson(state));
        } catch (SettingsException e) {
            SamlException ex = new SamlException("Invalid SAML settings");
            ex.initCause(e);
            throw ex;
        }
    }

    private static State buildState(HttpServletRequest request) {
        String idpId = request.getParameter(IDP_ID);
        String tenantId = request.getParameter(TENANT_NAME);
        String redirectUrl = request.getParameter("r");
        String loginApp = request.getParameter(LOGIN_APP);
        if (StringUtils.hasText(redirectUrl)) {
            try {
                redirectUrl = URLEncoder.encode(redirectUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
        }
        Cookie[] cookies = request.getCookies();
        String clientId = null;
        int platformId = DeviceType.WEB.getLow();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (StringUtils.equals(cookie.getName(), PLATFORM_ID)) {
                    String value = cookie.getValue();
                    if (!org.apache.commons.lang3.StringUtils.isNumeric(value)) {
                        throw new IllegalArgumentException("Invalid platform ID.");
                    }
                    platformId = Integer.parseInt(value);
                } else if (StringUtils.equals(cookie.getName(), CLIENT_ID)) {
                    String value = cookie.getValue();
                    if (value.length() > 32) {
                        throw new IllegalArgumentException("Invalid client ID.");
                    }
                    clientId = value;
                }
            }
        }
        clientId = StringUtils.hasText(clientId) ? clientId : generateClientId(); //NOPMD
        return new State(idpId, tenantId, clientId, platformId, loginApp, redirectUrl);
    }

    public void finishAuth(HttpServletRequest request, HttpServletResponse response)
            throws SamlException, IOException {
        try {
            String relayState = request.getParameter("RelayState");
            if (!StringUtils.hasText(relayState)) {
                throw new SamlException("Request is missing RelayState.");
            }
            State state = new Gson().fromJson(relayState, State.class);
            Properties samlProps = getSamlProps(state.getIdpId());
            if (samlProps == null) {
                throw new SamlException("SAML authentication is not configured for idp: " + state.getIdpId());
            }
            Saml2Settings settings = new SettingsBuilder().fromProperties(samlProps).build();
            Auth auth = new Auth(settings, request, response);
            try {
                auth.processResponse();
            } catch (Exception e) {
                SamlException ex = new SamlException("Error occurred while processing SAML Response");
                ex.initCause(e);
                throw ex;
            }

            if (!auth.isAuthenticated()) {
                String error = auth.getLastErrorReason();
                if (StringUtils.hasText(error)) {
                    LOGGER.error(error);
                }
                throw new SamlException("SAML authentication failed");
            }

            List<String> errors = auth.getErrors();
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String error : errors) {
                    sb.append(error).append('\n');
                }
                LOGGER.error(sb.toString());
            }

            Map<String, List<String>> userAttributes = auth.getAttributes();
            Map<String, String> attributesMap = IdpManager.getUserAttributeMap(state.getIdpId());
            if (attributesMap == null) {
                attributesMap = new HashMap<>();
            }
            if (userAttributes.get(EMAIL_SAML) == null || !StringUtils.hasText(userAttributes.get(EMAIL_SAML).get(0))) {
                throw new SamlException("Email not found in SAML user attributes.");
            }
            String email = userAttributes.get(EMAIL_SAML).get(0);
            String name;
            if (userAttributes.get(USERNAME_SAML) == null || !StringUtils.hasText(userAttributes.get(USERNAME_SAML).get(0))) {
                name = email;
            } else {
                name = userAttributes.get(USERNAME_SAML).get(0);
            }
            if (!userAttributes.containsKey(UserAttributeCacheItem.DISPLAYNAME)) {
                userAttributes.put(UserAttributeCacheItem.DISPLAYNAME, Arrays.asList(name));
            }
            if (!userAttributes.containsKey(UserAttributeCacheItem.EMAIL)) {
                userAttributes.put(UserAttributeCacheItem.EMAIL, Arrays.asList(email));
            }
            Set<String> samlAttributes = new HashSet<>(Arrays.asList(UserAttributeCacheItem.DISPLAYNAME, UserAttributeCacheItem.EMAIL));
            samlAttributes.addAll(attributesMap.values());
            userAttributes.keySet().retainAll(samlAttributes);

            for (Entry<String, String> mapEntry : attributesMap.entrySet()) {
                userAttributes.put(mapEntry.getKey(), userAttributes.remove(mapEntry.getValue()));
            }
            String uidIdentifier = samlProps.getProperty(UNIQUE_ID_KEY);
            if (!StringUtils.hasText(uidIdentifier)) {
                uidIdentifier = EMAIL_SAML;
            }
            userAttributes.put(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE, Collections.singletonList(uidIdentifier));
            userAttributes.putAll(IdpManager.getEAPAttributes(userAttributes.get(uidIdentifier)));
            try (DbSession session = DbSession.newSession()) {
                Response resp = loginSuccessed(request, session, "admin".equals(state.getLoginApp()), auth.getNameId(), email, name, userAttributes, state.getTenantName(), state.getClientId(), state.getPlatformId(), null);
                Map<String, NewCookie> cookies = resp.getCookies();
                for (Map.Entry<String, NewCookie> entry : cookies.entrySet()) {
                    response.addCookie(toServletCookie(entry.getValue()));
                }
                String redirectEndpoint = HeaderFilter.LOGIN_ENDPOINT;
                if ("admin".equals(state.getLoginApp())) {
                    redirectEndpoint = HeaderFilter.LOGIN_ADMIN_ENDPOINT;
                }
                StringBuilder sb = new StringBuilder(request.getContextPath()).append(redirectEndpoint).append("?i=").append(LoginType.SAML.ordinal());
                if (StringUtils.hasText(state.getRedirectUrl())) {
                    sb.append("&r=").append(state.getRedirectUrl());
                }
                response.sendRedirect(sb.toString());
            } catch (GeneralSecurityException e) {
                LOGGER.error(e.getMessage(), e);
                SamlException ex = new SamlException("Error occurred while logging in as SAML user");
                ex.initCause(e);
                throw ex;
            }

        } catch (SettingsException e) {
            SamlException ex = new SamlException("Invalid SAML settings");
            ex.initCause(e);
            throw ex;
        }
    }

    private static Properties getSamlProps(String id) {
        if (id == null) {
            return null;
        }

        Properties samlProps = IDP_MAP.get(id);
        if (samlProps != null) {
            return samlProps;
        }
        SamlIdpAttributes attrs = (SamlIdpAttributes)IdpManager.getIdpAttributes(id);
        samlProps = buildSamlProperties(attrs);
        if (samlProps != null) {
            IDP_MAP.put(id, samlProps);
        }
        return samlProps;
    }

    private static Properties buildSamlProperties(SamlIdpAttributes attrs) {
        if (attrs == null) {
            return null;
        }

        String idpId = attrs.getIdpEntityId();
        String idpSsoUrl = attrs.getIdpSsoUrl();
        String idpCert = attrs.getIdpX509Cert();
        String spId = attrs.getSpEntityId();
        String spAcsUrl = attrs.getSpAcsUrl();

        if (!StringUtils.hasText(idpId) || !StringUtils.hasText(idpSsoUrl) || !StringUtils.hasText(idpCert) || !StringUtils.hasText(spId) || !StringUtils.hasText(spAcsUrl)) {
            return null;
        }

        String spNameIdFormat = attrs.getSpNameIdFormat();
        if ("emailAddress".equalsIgnoreCase(spNameIdFormat)) {
            spNameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
        } else if ("persistent".equalsIgnoreCase(spNameIdFormat)) {
            spNameIdFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
        } else if ("transient".equalsIgnoreCase(spNameIdFormat)) {
            spNameIdFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
        } else {
            spNameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
        }

        String spLogoutUrl = "";
        String spCert = Nvl.nvl(attrs.getSpX509Cert());
        String spKey = Nvl.nvl(attrs.getSpPrivKey());
        String signRequests = String.valueOf(StringUtils.hasText(spCert) && StringUtils.hasText(spKey));

        String idpLogoutUrl = "";
        String idpLogoutSloUrl = "";

        String signatureAlgorithm = attrs.getSignAlgo();
        if ("sha1".equalsIgnoreCase(signatureAlgorithm)) {
            signatureAlgorithm = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        } else if ("sha384".equalsIgnoreCase(signatureAlgorithm)) {
            signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";
        } else if ("sha512".equalsIgnoreCase(signatureAlgorithm)) {
            signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";
        } else {
            signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
        }
        String authnContext = Nvl.nvl(attrs.getAuthNContext());

        Properties samlConfig = new Properties();
        samlConfig.setProperty("onelogin.saml2.strict", String.valueOf(attrs.isStrict()));
        samlConfig.setProperty("onelogin.saml2.debug", String.valueOf(attrs.isDebug()));

        samlConfig.setProperty("onelogin.saml2.sp.entityid", spId);
        samlConfig.setProperty("onelogin.saml2.sp.assertion_consumer_service.url", spAcsUrl);
        samlConfig.setProperty("onelogin.saml2.sp.assertion_consumer_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        samlConfig.setProperty("onelogin.saml2.sp.single_logout_service.url", spLogoutUrl);
        samlConfig.setProperty("onelogin.saml2.sp.single_logout_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlConfig.setProperty("onelogin.saml2.sp.nameidformat", spNameIdFormat);
        samlConfig.setProperty("onelogin.saml2.sp.x509cert", spCert);
        samlConfig.setProperty("onelogin.saml2.sp.privatekey", spKey);

        samlConfig.setProperty("onelogin.saml2.idp.entityid", idpId);
        samlConfig.setProperty("onelogin.saml2.idp.single_sign_on_service.url", idpSsoUrl);
        samlConfig.setProperty("onelogin.saml2.idp.single_sign_on_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlConfig.setProperty("onelogin.saml2.idp.single_logout_service.url", idpLogoutUrl);
        samlConfig.setProperty("onelogin.saml2.idp.single_logout_service.response.url", idpLogoutSloUrl);
        samlConfig.setProperty("onelogin.saml2.idp.single_logout_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlConfig.setProperty("onelogin.saml2.idp.x509cert", idpCert);

        samlConfig.setProperty("onelogin.saml2.security.nameid_encrypted", signRequests);
        samlConfig.setProperty("onelogin.saml2.security.authnrequest_signed", signRequests);
        samlConfig.setProperty("onelogin.saml2.security.logoutrequest_signed", signRequests);
        samlConfig.setProperty("onelogin.saml2.security.logoutresponse_signed", signRequests);
        samlConfig.setProperty("onelogin.saml2.security.sign_metadata", signRequests);

        samlConfig.setProperty("onelogin.saml2.security.want_messages_signed", "false");
        samlConfig.setProperty("onelogin.saml2.security.want_assertions_signed", "false");
        samlConfig.setProperty("onelogin.saml2.security.want_assertions_encrypted", "false");
        samlConfig.setProperty("onelogin.saml2.security.want_nameid_encrypted", "false");

        samlConfig.setProperty("onelogin.saml2.security.requested_authncontext", authnContext);
        samlConfig.setProperty("onelogin.saml2.security.onelogin.saml2.security.requested_authncontextcomparison", "exact");
        samlConfig.setProperty("onelogin.saml2.security.want_xml_validation", "true");
        samlConfig.setProperty("onelogin.saml2.security.signature_algorithm", signatureAlgorithm);
        samlConfig.setProperty(UNIQUE_ID_KEY, attrs.getEvalUserIdAttribute());
        return samlConfig;
    }

    @Override
    protected Constants.LoginType getLoginType() {
        return Constants.LoginType.SAML;
    }

    private static class State {

        private String idpId;
        private String tenantName;
        private String clientId;
        private int platformId;
        private String loginApp;
        private String redirectUrl;

        public State(String idpId, String tenantName, String clientId, int platformId, String loginApp,
            String redirectUrl) {
            this.idpId = idpId;
            this.tenantName = tenantName;
            this.clientId = clientId;
            this.platformId = platformId;
            this.loginApp = loginApp;
            this.redirectUrl = redirectUrl;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getIdpId() {
            return idpId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public String getClientId() {
            return clientId;
        }

        public int getPlatformId() {
            return platformId;
        }

        public String getLoginApp() {
            return loginApp;
        }
    }

    public static class SamlException extends Exception {

        private static final long serialVersionUID = -691510243981653801L;

        public SamlException(String msg) {
            super(msg);
        }

        public SamlException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }
}

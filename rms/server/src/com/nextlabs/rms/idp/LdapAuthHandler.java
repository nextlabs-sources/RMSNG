package com.nextlabs.rms.idp;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.UserAttributeCacheItem;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.shared.LogConstants;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LdapAuthHandler extends AbstractLogin {

    private String searchBase;
    private String domain;
    private String userSearchQuery;
    private String ldapType;
    private static Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private List<String> returnedAttributesList;
    private LdapIdpAttributes ldapIdpAttributes;

    public static final String DEFAULT_UNIQUE_IDENTIFIER = "objectSid";
    public static final String LDAP_AD = "AD";
    private static final Map<String, LdapIdpAttributes> IDP_MAP = new ConcurrentHashMap<String, LdapIdpAttributes>();
    private static final String MEMBEROF = "memberOf";
    private static final String EMAIL_LDAP = "mail";
    private static final String USERNAME_LDAP = "displayName";

    public LdapAuthHandler(LdapIdpAttributes ldapIdpAttributes) {
        this.returnedAttributesList = new ArrayList<>();
        this.ldapIdpAttributes = ldapIdpAttributes;
    }

    public boolean authenticate(String idpId, HttpServletRequest request, HttpServletResponse response,
        String userName, String password) throws LdapException, AuthenticationException {

        if (logger.isTraceEnabled()) {
            logger.trace("Attempting to authenticate user " + userName);
        }
        return authenticateUser(idpId, request, response, userName, password);
    }

    private boolean authenticateUser(String idpId, HttpServletRequest request, HttpServletResponse response,
        String userName, String password) throws LdapException, AuthenticationException {
        SearchControls searchCtls = new SearchControls();
        String uidIdentifier = ldapIdpAttributes.getEvalUserIdAttribute();
        if (!StringUtils.hasText(uidIdentifier)) {
            uidIdentifier = DEFAULT_UNIQUE_IDENTIFIER;
        }
        Map<String, String> attributesMap = IdpManager.getUserAttributeMap(idpId);
        if (attributesMap == null) {
            attributesMap = new HashMap<>();
        }
        if (!attributesMap.containsKey(uidIdentifier)) {
            attributesMap.put(uidIdentifier, uidIdentifier);
        }
        if (!attributesMap.containsKey(MEMBEROF)) {
            attributesMap.put(MEMBEROF, MEMBEROF);
        }
        if (!attributesMap.containsKey(UserAttributeCacheItem.EMAIL)) {
            attributesMap.put(UserAttributeCacheItem.EMAIL, EMAIL_LDAP);
        }
        if (!attributesMap.containsKey(UserAttributeCacheItem.DISPLAYNAME)) {
            attributesMap.put(UserAttributeCacheItem.DISPLAYNAME, USERNAME_LDAP);
        }
        returnedAttributesList.addAll(attributesMap.values());
        searchCtls.setReturningAttributes(returnedAttributesList.toArray(new String[returnedAttributesList.size()]));
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        LdapContext ldapCtxt = null;
        boolean error = false;
        try {
            ldapCtxt = getInitialContext(userName, password);
        } catch (CommunicationException e) {
            error = true;
            logger.error(e.getMessage(), e);
            throw new LdapException(RMSMessageHandler.getClientString("adHostNameErr"), e);
        } catch (AuthenticationException e) {
            error = true;
            throw (AuthenticationException)new AuthenticationException(RMSMessageHandler.getClientString("invalidCredentials")).initCause(e);
        } catch (NamingException e) {
            error = true;
            logger.error(e.getMessage(), e);
            throw new LdapException("Unable to resolve", e);
        } finally {
            if (error && ldapCtxt != null) {
                try {
                    ldapCtxt.close();
                } catch (NamingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        NamingEnumeration<?> answer = null;
        try {
            String searchFilter = userSearchQuery.replace("$USERID$", userName);
            answer = ldapCtxt.search(searchBase, searchFilter, searchCtls);
            String userGroup = null;
            if (answer == null || !answer.hasMoreElements()) {
                throw new LdapException(RMSMessageHandler.getClientString("invalidCredentials"));
            }
            SearchResult searchresult = (SearchResult)answer.next();
            Attributes attributes = searchresult.getAttributes();
            String uid = getUID(searchresult, uidIdentifier);
            logger.debug("Uid of user '" + userName + "' : " + uid);
            String rmsUserGrp = ldapIdpAttributes.getRmsGroup();
            if (rmsUserGrp != null && rmsUserGrp.length() > 0) {
                Attribute memberOf = (Attribute)searchresult.getAttributes().get(MEMBEROF);
                if (memberOf == null) {
                    //RMSUserGroup specified in config, but user not part of any group
                    throw new LdapException(RMSMessageHandler.getClientString("userNotInADGroup"));
                }
                String[] confUserGrpArr = rmsUserGrp.split(",");
                userGroup = memberOf.toString().split(":")[1];
                String[] groups = userGroup.split(",");
                boolean isMemberOfGroup = false;
                for (String groupName : groups) {
                    for (String confUserGrp : confUserGrpArr) {
                        if (groupName.trim().equalsIgnoreCase("CN=" + confUserGrp)) {
                            isMemberOfGroup = true;
                            break;
                        }
                    }
                }
                if (!isMemberOfGroup) {
                    //RMSUserGroup specified in config, but user not part of that group
                    throw new LdapException(RMSMessageHandler.getClientString("userNotInADGroup"));
                }
            }

            String email = null;
            if (attributes.get(EMAIL_LDAP) != null) {
                email = String.valueOf(attributes.get(EMAIL_LDAP)).replace(EMAIL_LDAP + ":", "").trim();
            }
            if (!StringUtils.hasText(email)) {
                throw new LdapException("Email not found in user attributes.");
            }
            String name;
            if (attributes.get(USERNAME_LDAP) == null) {
                name = email;
            } else {
                name = String.valueOf(attributes.get(USERNAME_LDAP)).replace(USERNAME_LDAP + ":", "").trim();
                if (!StringUtils.hasText(name)) {
                    name = email;
                }
            }
            Tenant tenant = AbstractLogin.getTenantFromUrl(request);
            if (tenant == null) {
                throw new LdapException("Tenant not found in request.");
            }

            Cookie[] cookies = request.getCookies();
            String clientId = null;
            int platformId = DeviceType.WEB.getLow();
            boolean adminApp = false;

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
                    } else if (StringUtils.equals(cookie.getName(), "adminApp")) {
                        adminApp = Boolean.parseBoolean(cookie.getValue());
                    }
                }
            }
            clientId = StringUtils.hasText(clientId) ? clientId : generateClientId(); //NOPMD
            Map<String, List<String>> userAttributes = new HashMap<>();
            for (Map.Entry<String, String> entry : attributesMap.entrySet()) {
                String attributeKey = entry.getKey();
                String mappedAttribute = entry.getValue();
                Attribute attributeValues = attributes.get(mappedAttribute);

                if (attributeValues != null) {
                    List<String> listVal = new ArrayList<String>();
                    if (ldapType.equalsIgnoreCase(LDAP_AD) && DEFAULT_UNIQUE_IDENTIFIER.equalsIgnoreCase(mappedAttribute)) {
                        byte[] sidByteArr = (byte[])attributeValues.get();
                        uid = getSIDAsString(sidByteArr);
                        listVal.add(uid);
                        userAttributes.put(attributeKey, listVal);
                        continue;
                    }
                    for (NamingEnumeration<?> attributeVal = attributeValues.getAll(); attributeVal.hasMore();) {
                        String attributeValue = String.valueOf(attributeVal.next()).replace(mappedAttribute + ":", "").trim();
                        listVal.add(attributeValue);
                    }
                    userAttributes.put(attributeKey, listVal);
                }
            }
            if (!StringUtils.hasText(userAttributes.get(UserAttributeCacheItem.DISPLAYNAME).get(0))) {
                userAttributes.put(UserAttributeCacheItem.DISPLAYNAME, userAttributes.get(UserAttributeCacheItem.EMAIL));
            }

            userAttributes.put(UserAttributeCacheItem.ADUSERNAME, Collections.singletonList(userName));
            userAttributes.put(UserAttributeCacheItem.ADPASS, Collections.singletonList(UserAttributeCacheItem.encrypt(password)));
            userAttributes.put(UserAttributeCacheItem.ADDOMAIN, Collections.singletonList(domain));
            String evalUserIdAttribute = ldapIdpAttributes.getEvalUserIdAttribute();
            if (!StringUtils.hasText(evalUserIdAttribute)) {
                evalUserIdAttribute = uidIdentifier;
            }
            userAttributes.put(UserAttributeCacheItem.UNIQUE_ID_ATTRIBUTE, Collections.singletonList(evalUserIdAttribute));
            userAttributes.putAll(IdpManager.getEAPAttributes(userAttributes.get(evalUserIdAttribute)));
            if (adminApp && !tenant.isAdmin(email)) {
                throw new RMSLdapException("Cannot login as admin.");
            }

            try (DbSession session = DbSession.newSession()) {
                Response resp = loginSuccessed(request, session, adminApp, uid, email, name, userAttributes, tenant.getName(), clientId, platformId, null);
                Map<String, NewCookie> cookiesMap = resp.getCookies();
                for (Map.Entry<String, NewCookie> entry : cookiesMap.entrySet()) {
                    response.addCookie(toServletCookie(entry.getValue()));
                }

            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
                LdapException ex = new LdapException("Error occurred while logging in as LDAP user", e);
                throw ex;
            }
            logger.info("Logged in successfully for user " + userName);
            return true;

        } catch (NamingException | GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
            LdapException ex = new LdapException("Invalid LDAP settings", e);
            throw ex;
        } finally {
            if (answer != null) {
                try {
                    answer.close();
                } catch (NamingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            try {
                ldapCtxt.close();
            } catch (NamingException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private String getUID(SearchResult searchresult, String uidIdentifier) throws NamingException {
        String uid = "";
        if (searchresult.getAttributes().get(uidIdentifier) != null) {
            if (ldapType.equalsIgnoreCase(LDAP_AD) && uidIdentifier.equalsIgnoreCase(DEFAULT_UNIQUE_IDENTIFIER)) {
                byte[] sidByteArr = (byte[])searchresult.getAttributes().get(DEFAULT_UNIQUE_IDENTIFIER).get();
                uid = getSIDAsString(sidByteArr);
            } else {
                uid = (String)searchresult.getAttributes().get(uidIdentifier).get();
            }
        }
        return uid;
    }

    public static String getSIDAsString(byte[] sid) {
        // Add the 'S' prefix
        StringBuilder strSID = new StringBuilder("S-");

        // bytes[0] : in the array is the version (must be 1 but might change in the future)
        strSID.append(sid[0]).append('-');

        // bytes[2..7] : the Authority
        StringBuilder tmpBuff = new StringBuilder();
        for (int t = 2; t <= 7; t++) {
            String hexString = Integer.toHexString(sid[t] & 0xFF);
            tmpBuff.append(hexString);
        }
        strSID.append(Long.parseLong(tmpBuff.toString(), 16));

        // bytes[1] : the sub authorities count
        int count = sid[1];

        // bytes[8..end] : the sub authorities (these are Integers - notice the endian)
        for (int i = 0; i < count; i++) {
            int currSubAuthOffset = i * 4;
            tmpBuff.setLength(0);
            tmpBuff.append(String.format("%02X%02X%02X%02X", (sid[11 + currSubAuthOffset] & 0xFF), (sid[10 + currSubAuthOffset] & 0xFF), (sid[9 + currSubAuthOffset] & 0xFF), (sid[8 + currSubAuthOffset] & 0xFF)));
            strSID.append('-').append(Long.parseLong(tmpBuff.toString(), 16));
        }
        return strSID.toString();
    }

    private LdapContext getInitialContext(String userName, String password) throws NamingException {
        String ldapHost = ldapIdpAttributes.getHostName();
        ldapType = ldapIdpAttributes.getLdapType();
        if (ldapType == null || org.apache.commons.lang3.StringUtils.isBlank(ldapType.trim())) {
            ldapType = LDAP_AD;
        }
        searchBase = ldapIdpAttributes.getSearchBase();
        domain = ldapIdpAttributes.getDomain();
        userSearchQuery = ldapIdpAttributes.getUserSearchQuery();
        boolean ldapSSL = ldapIdpAttributes.isLdapSSL();
        StringBuilder ldapHostSb = new StringBuilder();
        Hashtable<String, String> env = new Hashtable<String, String>(); //NOPMD
        if (ldapSSL) {
            ldapHostSb.append("ldaps://");
            env.put(DirContext.SECURITY_PROTOCOL, "ssl");
        } else {
            ldapHostSb.append("ldap://");
        }
        ldapHostSb.append(ldapHost);
        ldapHost = ldapHostSb.toString();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapHost);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, getSecurityPrincipal(userName));
        env.put(Context.SECURITY_CREDENTIALS, password);
        if (LDAP_AD.equalsIgnoreCase(ldapType)) {
            env.put("java.naming.ldap.attributes.binary", "objectSID");
        }
        return new InitialLdapContext(env, null);
    }

    private String getSecurityPrincipal(String userName) {
        String securityPrincipal = "";
        if (ldapType.equalsIgnoreCase(LDAP_AD)) {
            logger.debug("Using AD authentication for user : " + userName);
            if (ldapIdpAttributes.isSecurityPrincipalUseUserID()) {
                securityPrincipal = userName;
            } else {
                securityPrincipal = userName + "@" + domain;
            }
        } else {
            logger.debug("Using OPENLDAP authentication for user : " + userName);
            securityPrincipal = "cn=" + userName + "," + ldapIdpAttributes.getSearchBase();
        }
        return securityPrincipal;
    }

    public static LdapIdpAttributes getLdapAttributes(String idpId) {
        if (!StringUtils.hasText(idpId)) {
            return null;
        }

        LdapIdpAttributes ldapIdpAttributes = IDP_MAP.get(idpId);
        if (ldapIdpAttributes != null) {
            return ldapIdpAttributes;
        }

        ldapIdpAttributes = (LdapIdpAttributes)IdpManager.getIdpAttributes(idpId);
        if (ldapIdpAttributes != null) {
            IDP_MAP.put(idpId, ldapIdpAttributes);
        }
        return ldapIdpAttributes;
    }

    @Override
    protected LoginType getLoginType() {
        return Constants.LoginType.LDAP;
    }

    public static class LdapException extends Exception {

        /**
         * Login Exceptions with LDAP
         */
        private static final long serialVersionUID = 1L;

        public LdapException(String errMsg) {
            super(errMsg);
        }

        public LdapException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }

    public static class RMSLdapException extends LdapException {

        /**
         * Specific Exceptions with LDAP
         */
        private static final long serialVersionUID = 1L;

        public RMSLdapException(String errMsg) {
            super(errMsg);
        }

        public RMSLdapException(String msg, Throwable throwable) {
            super(msg, throwable);
        }
    }
}

package com.nextlabs.rms.idp;

import com.microsoft.aad.msal4j.AuthorizationCodeParameters;
import com.microsoft.aad.msal4j.AuthorizationRequestUrlParameters;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.Prompt;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.ResponseMode;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.LoginType;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.servlet.HeaderFilter;
import com.nextlabs.rms.servlet.IdpManagerServlet;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.SessionManagementUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AzureAdOAuthHandler {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    // Azure AD Auth Response
    private static final String CODE = "code";
    //private static final String STATE = "state";

    private AzureAdOAuthHandler() {
    }

    public static String startAuth(HttpServletRequest request)
            throws UnsupportedEncodingException, MalformedURLException {
        String port = request.getServerPort() == 443 || request.getServerPort() == 80 ? "" : ":" + request.getServerPort();
        String redirectURL = new StringBuilder(request.getScheme()).append("://").append(AbstractLogin.getDefaultTenant().getDnsName()).append(port).append(request.getContextPath()).append(IdpManagerServlet.AZURE_AUTH_FINISH_URL).toString();

        // state parameter to validate response from Authorization server and nonce parameter to validate idToken
        String state = UUID.randomUUID().toString(); //request.getParameter(AbstractLogin.TENANT_NAME);
        String nonce = UUID.randomUUID().toString();

        SessionManagementUtil.storeStateAndNonceInSession(state, nonce);
        String url = getAuthorizationCodeUrl(request.getParameter("claims"), redirectURL, state, nonce);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("Azure Auth URL: {}", url);
        }

        return url;
    }

    private static String getAuthorizationCodeUrl(String claims, String registeredRedirectURL, String state,
        String nonce)
            throws MalformedURLException {

        AzureAdIdpAttributes idp = (AzureAdIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.AZUREAD, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(">>>App Id: {}", idp.getAppId());
            LOGGER.debug(">>>Directory Id: {}", idp.getDirectoryId());
        }

        String authURL = AzureAdIdpAttributes.AUTH_ENDPOINT + idp.getDirectoryId() + "/";

        // Set the user profiles scope
        Set<String> scopes = new HashSet<String>();
        scopes.add("User.Read");
        scopes.add("User.ReadBasic.All");
        //scopes.add("User.Read.All");
        //scopes.add("Directory.Read.All");
        scopes.add("Directory.AccessAsUser.All");

        PublicClientApplication pca = PublicClientApplication.builder(idp.getAppId()).authority(authURL).build();

        AuthorizationRequestUrlParameters parameters = AuthorizationRequestUrlParameters.builder(registeredRedirectURL, scopes).responseMode(ResponseMode.QUERY).prompt(Prompt.SELECT_ACCOUNT).state(state).nonce(nonce).claimsChallenge(claims).build();

        return pca.getAuthorizationRequestUrl(parameters).toString();
    }

    public static String finishAuth(HttpServletRequest request) throws IOException {

        if (!containsAuthenticationCode(request)) {
            throw new IOException("Azure AD Authentication Failed");
        }

        String currentUri = request.getRequestURL().toString();
        try {
            processAuthenticationCodeRedirect(request, currentUri);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        Tenant tenant = AbstractLogin.getTenantFromUrl(request);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Tenant name: {}", tenant.getName());
        }
        boolean adminApp = false;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (StringUtils.equals(cookie.getName(), "adminApp")) {
                    adminApp = Boolean.parseBoolean(cookie.getValue());
                    break;
                }
            }
        }

        String redirectEndpoint = HeaderFilter.LOGIN_ENDPOINT;
        if (adminApp) {
            redirectEndpoint = HeaderFilter.LOGIN_ADMIN_ENDPOINT;
        }

        // Generate a random token
        String token = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder(redirectEndpoint).append("?i=").append(LoginType.AZUREAD.ordinal()).append("&t=").append(token).append("&tenant=").append(tenant.getName());
        String redirectUrl = sb.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RedirectUrl: {}", redirectUrl);
        }

        return redirectUrl;
    }

    private static void processAuthenticationCodeRedirect(HttpServletRequest httpRequest, String currentUri)
            throws ExecutionException, URISyntaxException, IOException {

        // code, state and session_state
        Map<String, List<String>> params = new HashMap<>();
        for (String key : httpRequest.getParameterMap().keySet()) {
            params.put(key, Collections.singletonList(httpRequest.getParameterMap().get(key)[0]));
        }

        // validate that state in response equals to state in request
        //StateData stateData = SessionManagementUtil.validateState(httpRequest.getSession(), params.get(STATE).get(0));
        String authCode = params.get(CODE).get(0);
        //String session_state = params.get(SESSION_STATE).get(0);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Azure Auth Code: {}", authCode);
        }

        if (authCode != null && !authCode.isEmpty()) {
            IAuthenticationResult result = getAuthResultByAuthCode(authCode, currentUri);

            /*
            if(LOGGER.isDebugEnabled()) {
                String idToken = result.idToken();
                String scopes = result.scopes();
                String accessToken = result.accessToken();
                Date tokenExpireDate = result.expiresOnDate();
            
            	LOGGER.debug("ID Token: {}", idToken);
            	LOGGER.debug("Scopes: {}", scopes);
            	LOGGER.debug("Access Token: {}", accessToken);
            	LOGGER.debug("Token Expire Date: {}", tokenExpireDate);
            }
            */
            SessionManagementUtil.setSessionAzureToken(result.accessToken());
        }

    }

    private static IAuthenticationResult getAuthResultByAuthCode(
        String authCode,
        String currentUri) throws ExecutionException, URISyntaxException, IOException {

        IAuthenticationResult result;
        ConfidentialClientApplication app;
        try {
            AzureAdIdpAttributes idp = (AzureAdIdpAttributes)IdpManager.getIdpAttributes(Constants.LoginType.AZUREAD, null);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("App Id: {}", idp.getAppId());
                LOGGER.debug("Directory Id: {}", idp.getDirectoryId());
            }

            app = createClientApplication(idp);

            AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(authCode, new URI(currentUri)).build();

            Future<IAuthenticationResult> future = app.acquireToken(parameters);

            result = future.get();
        } catch (InterruptedException e) {
            LOGGER.fatal(e);
            throw new IOException(e);
        }

        if (result == null) {
            throw new IOException("authentication result was null");
        }

        //SessionManagementUtil.storeTokenCacheInSession(httpServletRequest, app.tokenCache().serialize());

        return result;
    }

    private static ConfidentialClientApplication createClientApplication(AzureAdIdpAttributes idp)
            throws MalformedURLException {
        String authURL = AzureAdIdpAttributes.AUTH_ENDPOINT + idp.getDirectoryId() + "/";

        return ConfidentialClientApplication.builder(idp.getAppId(), ClientCredentialFactory.createFromSecret(idp.getAppSecret())).authority(authURL).build();
    }

    private static boolean containsAuthenticationCode(HttpServletRequest httpRequest) {
        Map<String, String[]> httpParameters = httpRequest.getParameterMap();

        //boolean isPostRequest = httpRequest.getMethod().equalsIgnoreCase("POST");
        boolean containsErrorData = httpParameters.containsKey("error");
        boolean containIdToken = httpParameters.containsKey("id_token");
        boolean containsCode = httpParameters.containsKey("code");

        return containsErrorData || containsCode || containIdToken;
    }

}

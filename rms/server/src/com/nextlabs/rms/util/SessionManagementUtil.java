package com.nextlabs.rms.util;

import com.nextlabs.rms.cache.RMSCacheManager;

import java.io.IOException;

public final class SessionManagementUtil {

    private static final String AZURE_NONCE = "azure_nonce";
    private static final String AZURE_STATE = "azure_state";
    private static final String AZURE_TOKEN = "azure_token";

    private SessionManagementUtil() {
    }

    @SuppressWarnings("unchecked")
    public static void storeStateAndNonceInSession(String state, String nonce) {

        try {
            // Add to cache
            RMSCacheManager.getInstance().getSessionCache().put(AZURE_STATE, state);
            RMSCacheManager.getInstance().getSessionCache().put(AZURE_NONCE, nonce);
        } catch (IllegalStateException e) {
            // Ignore
        }

    }

    public static boolean validateState(String state) {

        try {
            // Remove from cache
            String storedState = RMSCacheManager.getInstance().getSessionCache().get(AZURE_STATE);
            return (storedState != null && storedState.equals(state));
        } catch (IllegalStateException e) {
            // Ignore
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static void removeStateAndNonceFromSession(String state) {

        // Remove from cache
        try {
            RMSCacheManager.getInstance().getSessionCache().remove(AZURE_STATE);
            RMSCacheManager.getInstance().getSessionCache().remove(AZURE_NONCE);
        } catch (IllegalStateException e) {
            // Ignore
        }
    }

    /*
    private static void eliminateExpiredStates(Map<String, StateData> map) {
        Iterator<Map.Entry<String, StateData>> it = map.entrySet().iterator();
    
        Date currTime = new Date();
        while (it.hasNext()) {
            Map.Entry<String, StateData> entry = it.next();
            long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(currTime.getTime() - entry.getValue().getExpirationDate().getTime());
    
            if (diffInSeconds > STATE_TTL) {
                it.remove();
            }
        }
    }
     */

    public static void setSessionAzureToken(String token) {

        try {
            // Add to cache
            RMSCacheManager.getInstance().getSessionCache().put(AZURE_TOKEN, token);
        } catch (IllegalStateException e) {
            // Ignore
        }

    }

    public static void removeAzureTokenFromSession() {

        try {
            // Remove from cache
            RMSCacheManager.getInstance().getSessionCache().remove(AZURE_TOKEN);
        } catch (IllegalStateException e) {
            // Ignore
        }

    }

    public static String getSessionAzureToken() throws IOException {
        try {
            // Check cache
            String token = RMSCacheManager.getInstance().getSessionCache().get(AZURE_TOKEN);
            if (token != null) {
                return token;
            }
        } catch (IllegalStateException e) {
            // Ignore
        }

        throw new IOException("Could not find token for Azure AD");
    }

}

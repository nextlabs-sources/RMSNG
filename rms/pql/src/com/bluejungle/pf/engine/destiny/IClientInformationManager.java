package com.bluejungle.pf.engine.destiny;

/*
 * All sources, binaries and HTML pages (C) copyright 2008 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/IClientInformationManager.java#1 $
 */

/**
 * This interface defines the contract for accessing client identifiers.
 *
 * @author Sergey Kalinichenko
 */
public interface IClientInformationManager {

    /**
     * The default client resolver returns an empty multivalue for all domains.
     */
    IClientInformationManager DEFAULT = new IClientInformationManager() {

        private final String[] EMPTY = new String[0];

        public String[] getClientIdsForEmail(String domainName) {
            return EMPTY;
        }

        public ClientInformation getClient(String clientId) {
            return null;
        }

        public ClientInformation[] getClientsForUser(String uid) {
            return new ClientInformation[0];
        }
    };

    /**
     * Given an email, returns a multivalue
     *
     * @param email the email for which to look up the client id.
     *
     * @return zero or more Strings with the client IDs for the email.
     */
    String[] getClientIdsForEmail(String email);

    /**
     * Given a client id, return the client information
     *
     * @param clientId the client identifier
     * @return the corresponding ClientInformation or null if there is
     * no matching clientId
     */
    ClientInformation getClient(String clientId);

    /**
     * Given a uid, return all the clients associated with that user
     *
     * @param uid the user id
     * @return an array of ClientInformation associated with the uid.  This
     * array will be empty if the user is unknown or if the user has no clients
     */
    ClientInformation[] getClientsForUser(String uid);
}

package com.bluejungle.pf.engine.destiny;

/*
 * All sources, binaries and HTML pages (C) copyright 2008 by NextLabs, Inc.
 * San Mateo CA, Ownership remains with NextLabs, Inc.
 * All rights reserved worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/ClientInformation.java#1 $
 */

/**
 * This is a read-only bean-style class for accessing client identification and
 * other information associated with the specific client.
 *
 * @author Sergey Kalinichenko
 */
public class ClientInformation {

    /**
     * The identifier of this client.
     */
    private final String identifier;

    /**
     * The short name associated with this client.
     */
    private final String shortName;

    /**
     * The long name of this client.
     */
    private final String longName;

    /**
     * Constructs ClientInformation with the specified parameters.
     *
     * @param identifier the identifier of this client.
     * @param shortName the short name of this client.
     * @param longName the long name of this client.
     */
    public ClientInformation(
        String identifier
        , String shortName
        , String longName) {
        this.identifier = identifier;
        this.shortName = shortName;
        this.longName = longName;
    }

    /**
     * Gets the identifier of this client.
     *
     * @return the identifier of this client.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the short name of this client.
     *
     * @return the short name of this client.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Gets the long name of this client.
     *
     * @return the long name of this client.
     */
    public String getLongName() {
        return longName;
    }

    @Override
    public String toString() {
        return String.format(
                "{%s} - '%s' ('%s')"
                , identifier
                , longName
                , shortName
                );
    }

}

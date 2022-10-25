/*
 * Created on Jan 18, 2010
 *
 * All sources, binaries and HTML pages (C) copyright 2004-2010 by NextLabs, Inc.,
 * San Mateo CA, Ownership remains with NextLabs, Inc., All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.serviceprovider;

import com.bluejungle.framework.comp.PropertyKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author hchan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/serviceprovider/IServiceProviderManager.java#1 $
 */

public interface IServiceProviderManager extends INextlabsExternalSPResponseCode {

    public static final IServiceProviderManager DEFAULT = new IServiceProviderManager() {

        public void invoke(ArrayList inputArgs, ArrayList outputArgs) {
            outputArgs.add(CE_RESULT_GENERAL_FAILED);
            outputArgs.add(EMPTY_FORMAT_STRING);
        }

        public IServiceProvider getServiceProvider(String name) {
            return null;
        }

        public List<IServiceProvider> getAllServiceProviders() {
            return Collections.<IServiceProvider> emptyList();
        }

        public <T extends IServiceProvider> List<T> getAllServiceProvidersByType(Class<T> clazz) {
            return Collections.<T> emptyList();
        }
    };

    String EMPTY_FORMAT_STRING = "";
    PropertyKey<String[]> INIT_FOLDERS = new PropertyKey<String[]>("ServiceProviderFolders");
    PropertyKey<String> BASE_DIR_PROPERTY_NAME = new PropertyKey<String>("baseDirectory");

    /**
     * The manager will call the corresponding service with an array constructed
     *   from elements 1...n of <code>inputArgs</code>
     * If the service can't be found, it will return error <code>CE_FUNCTION_NOT_AVAILABLE</code>
     *   at the first element in <code>outputArgs</code>
     * @param inputArgs the first element is the service provider name.
     * @param outputArgs recommend to return <code>CE_RESULT_SUCCESS</code> if everything is ok.
     *                   The implementor is free to use the error codes defined in CESdk.h or
     *                   others as they see fit.
     */
    void invoke(ArrayList inputArgs, ArrayList outputArgs);

    /**
     * Return the named service provider or null if one does not exist
     * @param name the name of the service provider
     */
    IServiceProvider getServiceProvider(String name);

    /**
     * Return all service providers
     */
    List<IServiceProvider> getAllServiceProviders();

    /**
     * Get all service providers of the specified type
     */
    <T extends IServiceProvider> List<T> getAllServiceProvidersByType(Class<T> clazz);
}

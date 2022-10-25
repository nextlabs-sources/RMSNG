/*
 * Created on Mar 2, 2010
 *
 * All sources, binaries and HTML pages (C) copyright 2004-2010 by NextLabs, Inc.,
 * San Mateo CA, Ownership remains with NextLabs, Inc., All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.serviceprovider;

/**
 * copy from CEsdk.h
 *
 * @author hchan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/serviceprovider/INextlabsExternalSPResponseCode.java#1 $
 */

public interface INextlabsExternalSPResponseCode {

    int CE_RESULT_SUCCESS = 0;
    /**< Success */
    int CE_RESULT_GENERAL_FAILED = -1;
    /**< General failure */
    int CE_RESULT_CONN_FAILED = -2;
    /**< Connection failed */
    int CE_RESULT_INVALID_PARAMS = -3;
    /**< Invalid parameter(s) */
    int CE_RESULT_VERSION_MISMATCH = -4;
    /**< Version mismatch */
    int CE_RESULT_FILE_NOT_PROTECTED = -5;
    /**< File not protected */
    int CE_RESULT_INVALID_PROCESS = -6;
    /**< Invalid process */
    int CE_RESULT_INVALID_COMBINATION = -7;
    /**< Invalid combination */
    int CE_RESULT_PERMISSION_DENIED = -8;
    /**< Permission denied */
    int CE_RESULT_FILE_NOT_FOUND = -9;
    /**< File not found */
    int CE_RESULT_FUNCTION_NOT_AVAILBLE = -10;
    /**< Function not available */
    int CE_RESULT_TIMEDOUT = -11;
    /**< Timed out */
    int CE_RESULT_SHUTDOWN_FAILED = -12;
    /**< Shutdown failed */
    int CE_RESULT_INVALID_ACTION_ENUM = -13;
    /**< */
    int CE_RESULT_EMPTY_SOURCE = -14;
    /**< Empty source */
    int CE_RESULT_MISSING_MODIFIED_DATE = -15;
    /**< */
    int CE_RESULT_NULL_CEHANDLE = -16;
    /**< NULL or bad connection handle */
    int CE_RESULT_INVALID_EVAL_ACTION = -17;
    /**< */
    int CE_RESULT_EMPTY_SOURCE_ATTR = -18;
    /**< */
    int CE_RESULT_EMPTY_ATTR_KEY = -19;
    /**< */
    int CE_RESULT_EMPTY_ATTR_VALUE = -20;
    /**< */
    int CE_RESULT_EMPTY_PORTAL_USER = -21;
    /**< */
    int CE_RESULT_EMPTY_PORTAL_USERID = -22;
    /**< */
    int CE_RESULT_MISSING_TARGET = -23;
    /**< Missing target */
    int CE_RESULT_PROTECTION_OBJECT_NOT_FOUND = -24;
    /**< Object not found */
    int CE_RESULT_NOT_SUPPORTED = -25;
    /**< Not supported */
    int CE_RESULT_SERVICE_NOT_READY = -26;
    /**< Not ready */
    int CE_RESULT_SERVICE_NOT_FOUND = -27;
    /**< Not foudn */
    int CE_RESULT_INSUFFICIENT_BUFFER = -28;
    /**< I find your lack of space, disturbing */
    int CE_RESULT_ALREADY_EXISTS = -29;
    /**< Tried to create somethat that already exists */
    int CE_RESULT_APPLICATION_AUTH_FAILED = -30;
    /**< Consumer Authentication failed */
}

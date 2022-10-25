package com.nextlabs.rms.cc.exception;

/**
 * @author ssethy
 *
 */
public class ControlCentreNotSupportedException extends Exception {

    private static final long serialVersionUID = -102713458392165137L;

    private final String ccVersion;

    public ControlCentreNotSupportedException(String ccVersion) {
        this.ccVersion = ccVersion;
    }

    public String getCCVersion() {
        return ccVersion;
    }
}

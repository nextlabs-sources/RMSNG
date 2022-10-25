/**
 *
 */
package com.nextlabs.rms.viewer.exception;

/**
 * @author nnallagatla
 *
 */
public class NotAuthorizedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String owner;
    private final String duid;

    public NotAuthorizedException(String message, String duid, String owner) {
        super(message);
        this.duid = duid;
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public String getDuid() {
        return duid;
    }
}

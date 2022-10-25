/*
 * Created on Apr 7, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.domain.log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This is the implementation class for the resource information data object.
 * This data object stores information about resources found in the filesystem.
 *
 * @author ihanen
 * @version $Id:
 *          //depot/main/Destiny/main/src/server/container/dabs/src/java/main/com/bluejungle/destiny/container/dabs/components/log/hibernateimpl/ResourceInformation.java#2 $
 */

public abstract class ResourceInformation implements Externalizable {

    private String name;

    /**
     * Constructor
     *
     * @param name
     *            resource name
     */
    public ResourceInformation(String name) {
        this.name = name;
    }

    /**
     * Constructor
     */
    public ResourceInformation() {
    }

    /**
     * @see com.bluejungle.domain.log.IResourceInformation#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        // more than likely equal name => equal resource info
        return getName().hashCode();
    }

    /**
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
    }

    /**
     * Sets the resource name
     *
     * @param newName
     *            new name to set
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
    }
}

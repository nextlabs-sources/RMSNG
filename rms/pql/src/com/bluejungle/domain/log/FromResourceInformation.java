/*
 * Created on Jan 25, 2006
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
 * @author ihanen
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/domain/src/java/main/com/bluejungle/domain/log/FromResourceInformation.java#1 $
 */

public class FromResourceInformation extends ResourceInformation implements Externalizable {

    private long createdDate;
    private long modifiedDate;
    private long size;
    private String ownerId;

    /**
     * Constructor
     *
     * @param name
     *            from resource name
     * @param size
     *            from resource size
     * @param createdDate
     *            from resource creation date
     * @param modifiedDate
     *            from resource modification date
     * @param ownerId
     *            from resource owner id (SID)
     */
    public FromResourceInformation(String name, long size, long createdDate, long modifiedDate, String ownerId) {
        super(name);
        this.size = size;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.ownerId = ownerId;
    }

    /**
     * Constructor
     */
    public FromResourceInformation() {
        super();
    }

    /**
     * @see com.bluejungle.domain.log.IResourceInformation#getCreatedDate()
     */
    public long getCreatedDate() {
        return this.createdDate;
    }

    /**
     * @see com.bluejungle.domain.log.IResourceInformation#getModifiedDate()
     */
    public long getModifiedDate() {
        return this.modifiedDate;
    }

    /**
     * @see com.bluejungle.domain.log.IResourceInformation#getSize()
     */
    public long getSize() {
        return this.size;
    }

    /**
     * @see com.bluejungle.domain.log.IResourceInformation#getOwnerId()
     */
    public String getOwnerId() {
        return this.ownerId;
    }

    /**
     * Sets the created date
     *
     * @param newDate
     *            new date to set
     */
    public void setCreatedDate(long newDate) {
        this.createdDate = newDate;
    }

    /**
     * Sets the modified date
     *
     * @param newDate
     *            new date to set
     */
    public void setModifiedDate(long newDate) {
        this.modifiedDate = newDate;
    }

    /**
     * Sets the new size
     *
     * @param newSize
     *            new size to set
     */
    public void setSize(long newSize) {
        this.size = newSize;
    }

    /**
     * Sets the new owner id
     *
     * @param newId
     *            new owner id to set
     */
    public void setOwnerId(String newId) {
        this.ownerId = newId;
    }

    /**
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        createdDate = in.readLong();
        modifiedDate = in.readLong();
        size = in.readLong();
        ownerId = in.readUTF();
    }

    /**
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeLong(createdDate);
        out.writeLong(modifiedDate);
        out.writeLong(size);
        out.writeUTF(ownerId);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer rv = new StringBuffer("FromResourceInformation[");
        rv.append("name: " + getName());
        rv.append(", size: " + size);
        rv.append(", createdDate: " + createdDate);
        rv.append(", modifiedDate: " + modifiedDate);
        rv.append(", owner: " + ownerId);
        rv.append("]");
        return rv.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FromResourceInformation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        FromResourceInformation ri = (FromResourceInformation)obj;
        //There are two ways for equality
        //1) all fields equal
        //2) size is -1, sizes and names equal
        return ((getName().equals(ri.getName()) && size == ri.size && createdDate == ri.createdDate && modifiedDate == ri.modifiedDate && ownerId.equals(ri.ownerId)) || (getName().equals(ri.getName()) && ri.size < 0 && size < 0));
    }
}

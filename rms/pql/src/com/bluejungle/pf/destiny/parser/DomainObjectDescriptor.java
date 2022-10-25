package com.bluejungle.pf.destiny.parser;

/* All sources, binaries and HTML pages (C) Copyright 2007 by
 * NextLabs Inc, San Mateo, CA.
 * Ownership remains with NextLabs Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/DomainObjectDescriptor.java#1 $
 */

import com.bluejungle.framework.utils.ObjectHelper;
import com.bluejungle.framework.utils.UnmodifiableDate;
import com.bluejungle.pf.destiny.formatter.DomainObjectFormatter;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.destiny.lifecycle.EntityType;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

/**
 * Instances of this class serve as holders for parameters of IPQLVisitor.
 * This class lets us change the information the PQL parser reports back
 * without changing the signatures of IPQLVisitor's methods.
 * Objects of this class are immutable.
 */
public class DomainObjectDescriptor {

    public static final DomainObjectDescriptor EMPTY = new DomainObjectDescriptor();

    /** Represents the ID of the object. Can be null. */
    private final Long id;

    /** Represents the name of the object. Can be null. */
    private final String name;

    /** Represents unique id of the owner of the object. Can be null. */
    private final Long owner;

    /** Represents access policy on the object. Can be null. */
    private final IAccessPolicy accessPolicy;

    /** Represents the type of this entity. */
    private final EntityType type;

    /** Represents the description of the object. Can be null. */
    private final String description;

    /** Represents the development status of the object (draft, approved, etc.) */
    private final DevelopmentStatus status;

    /** Represents the version of the corresponding object. */
    private final int version;

    private final Date lastUpdated;

    /** The date the object was created. */
    private final Date whenCreated;

    /** The date the object was last updated. */
    private final Date lastModified;

    private final Long modifier;

    private final Date lastSubmitted;

    private final Long submitter;

    /** Indicates that the descriptor corresponds to a hidden object. */
    private final boolean hidden;

    /** Indicates that the descriptor should be accessible by the reader. */
    private boolean accessible;

    /** Caches the computed hashcode value.  -1 if not calculated yet */
    private int hashcode = -1;

    /** Used when the DOD is transmitted to the ControlCenter only.  Indicates that the descriptor has dependencies */
    private boolean hasDependencies;

    private DomainObjectDescriptor() {
        this(null // Long id
        , null // String name
        , null // Long owner
        , null // IAccessPolicy ap
        , EntityType.ILLEGAL // EntityType type
        , null // String description
        , DevelopmentStatus.EMPTY // DevelopmentStatus status
        );
    }

    /**
     * Creates a new <code>DomainObjectDescriptor</code> with the version of -1.
     * @param id the ID of the object.
     * @param name the name of the object.
     * @param type the type of the object.
     * @param description the description of the object.
     * @param version the version of the corresponding object.
     */
    public DomainObjectDescriptor(
        Long id
        , String name
        , Long owner
        , IAccessPolicy ap
        , EntityType type
        , String description
        , DevelopmentStatus status) {
        this(id // Long id
        , name // String name
        , owner // Long owner
        , ap // IAccessPolicy ap
        , type // EntityType type
        , description // String description
        , status // DevelopmentStatus status
        , -1 // int version
        , UnmodifiableDate.START_OF_TIME // Date lastUpdated
        , UnmodifiableDate.START_OF_TIME // Date whenCreated
        , UnmodifiableDate.START_OF_TIME // Date lastModified
        , null // Long modifier
        , UnmodifiableDate.START_OF_TIME // Date lastSubmitted
        , null // Long submitter
        , false // boolean hidden
        , true // boolean accessible
        , false // boolean dependencies
        );
    }

    /**
     * Creates a new <code>DomainObjectDescriptor</code>.
     * @param id the ID of the object.
     * @param name the name of the object.
     * @param type the type of the object.
     * @param description the description of the object.
     * @param status the development status of the object (draft, approved, etc.)
     * @param version the version of the corresponding object.
     * @param whenCreated the date the object was created.
     * @param lastUpdated the date the object was last updated.
     * @param hidden indicates that the entity corresponding to this descriptor is hidden.
     * @param accessible indicates whether the user has read access to the
     * object defined by this descriptor.
     * @param dependencies indicates that the entity corresponding to this descriptor has dependencies
     */
    public DomainObjectDescriptor(
        Long id
        , String name
        , Long owner
        , IAccessPolicy ap
        , EntityType type
        , String description
        , DevelopmentStatus status
        , int version
        , Date lastUpdated
        , Date whenCreated
        , Date lastModified
        , Long modifier
        , Date lastSubmitted
        , Long submitter
        , boolean hidden
        , boolean accessible
        , boolean dependencies) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.modifier = modifier;
        this.submitter = submitter;
        this.accessPolicy = ap;
        this.type = type;
        this.description = description;
        this.status = status;
        this.version = version;
        this.whenCreated = whenCreated;
        this.lastModified = lastModified;
        this.lastSubmitted = lastSubmitted;
        this.lastUpdated = lastUpdated;
        this.hidden = hidden;
        this.accessible = accessible;
        this.hasDependencies = dependencies;
    }

    /**
     * Gets the ID of the object.
     * @return Returns the ID.
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the description of the object.
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the name of this object.
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the owner of this object.
     * @return Returns the owner.
     */
    public Long getOwner() {
        return owner;
    }

    /**
     * Gets access policy on this object.
     * @return Returns the access policy
     */
    public IAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    /**
     * Gets access policy on this object.
     * @return Returns the access policy
     */
    public String getAccessPolicyString() {
        if (accessPolicy == null) {
            return null;
        }
        DomainObjectFormatter dof = new DomainObjectFormatter();
        dof.formatAccessPolicy(accessPolicy);
        return dof.getPQL();
    }

    /**
     * Gets the type of this entity.
     * @return the type of this entity.
     */
    public EntityType getType() {
        return type;
    }

    /**
     * Gets the development status of this object.
     * @return Returns the status.
     */
    public DevelopmentStatus getStatus() {
        return status;
    }

    /**
     * Gets the current version.
     * @return the current version
     */
    public int getVersion() {
        return version;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Gets the <code>Date</code> this object was created.
     * @return the <code>Date</code> this object was created.
     */
    public Date getWhenCreated() {
        return whenCreated;
    }

    /**
     * Gets the <code>Date</code> this object was last updated.
     * @return the <code>Date</code> this object was last updated.
     */
    public Date getLastModified() {
        return lastModified;
    }

    public Long getModifier() {
        return modifier;
    }

    public Date getLastSubmitted() {
        return lastSubmitted;
    }

    public Long getSubmitter() {
        return submitter;
    }

    /**
     * Determines if the entity is hidden.
     * @return Returns true if the entity is hidden.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Determines if the entity is accessible.
     * @return Returns true if the entity is accessible.
     */
    public boolean isAccessible() {
        return accessible;
    }

    /**
     * Determines if the entity has dependencies
     * @return Returns true if the entity has dependencies
     */
    public boolean hasDependencies() {
        return hasDependencies;
    }

    /**
     * Given a <code>Collection</code> of <code>DomainObjectDescriptor</code>
     * objects, returns a <code>Collection</code> of IDs extracted from them.
     * @param descriptors a <code>Collection</code> of <code>DomainObjectDescriptor</code>
     * objects.
     * @return a <code>Collection</code> of IDs extracted from the descriptors.
     */
    public static Collection<Long> extractIds(Collection<DomainObjectDescriptor> descriptors) {
        if (descriptors == null) {
            throw new NullPointerException("descriptors");
        }
        Long[] res = new Long[descriptors.size()];
        int i = 0;
        for (DomainObjectDescriptor descriptor : descriptors) {
            res[i++] = descriptor.getId();
        }
        return Arrays.asList(res);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof DomainObjectDescriptor)) {
            return false;
        }
        DomainObjectDescriptor desc = (DomainObjectDescriptor)arg0;

        Long thisId = this.getId();
        Long compareId = desc.getId();

        if ((thisId != null) && (compareId != null)) {
            return thisId.equals(compareId);
        }

        return ObjectHelper.nullSafeEquals(name, desc.name)
                && ObjectHelper.nullSafeEquals(owner, desc.owner)
                && ObjectHelper.nullSafeEquals(type, desc.type)
                && ObjectHelper.nullSafeEquals(description, desc.description)
                && ObjectHelper.nullSafeEquals(status, desc.status)
                && (version == desc.version)
                && ObjectHelper.nullSafeEquals(lastUpdated, desc.lastUpdated)
                && ObjectHelper.nullSafeEquals(lastModified, desc.lastModified)
                && ObjectHelper.nullSafeEquals(modifier, desc.modifier)
                && ObjectHelper.nullSafeEquals(lastSubmitted, desc.lastSubmitted)
                && ObjectHelper.nullSafeEquals(submitter, desc.submitter);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (hashcode != -1) {
            return hashcode;
        }

        if (this.getId() != null) {
            hashcode = this.getId().hashCode();
        } else {
            hashcode = ObjectHelper.nullSafeHashCode(
                    name
                    , owner
                    , type
                    , description
                    , status
                    , version
                    , lastUpdated
                    , lastModified
                    , modifier
                    , lastSubmitted
                    , submitter
                    );
        }
        return hashcode;
    }

    /**
     * Provides a <code>Comparator</code> for sorting descriptors.
     * Descriptors are sorted on their ordinal first (higher ordinals
     * are sorted ahead of lower ones to ensure that folders are sorted
     * higher than their content, i.e. policies). When the ordinals are
     * identical, names are compared case-insensitively.
     */
    public static Comparator<DomainObjectDescriptor> CASE_INSENSITIVE_COMPARATOR = new Comparator<DomainObjectDescriptor>() {

        public int compare(DomainObjectDescriptor lhs, DomainObjectDescriptor rhs) {
            EntityType lhst = lhs.getType();
            EntityType rhst = rhs.getType();
            if (lhst == null || rhst == null) {
                throw new NullPointerException("DomainObjectDescriptor.getType()");
            }
            if (lhst.getType() != rhst.getType()) {
                return rhst.getType() - lhst.getType();
            } else {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        }
    };

}

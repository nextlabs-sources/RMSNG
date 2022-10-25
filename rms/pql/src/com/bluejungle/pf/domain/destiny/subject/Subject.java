package com.bluejungle.pf.domain.destiny.subject;

/*
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/Subject.java#1 $
 */

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.pf.domain.epicenter.subject.ISubject;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The default representation of a subject.
 */

public class Subject implements IDSubject, Serializable {

    private static final long serialVersionUID = 1L;

    /** Some opaque unique id */
    private String uid;
    /** Some opaque unique name (principalName) */
    private String uniqueName;
    /** Destiny id. */
    private Long id;
    /** Name of this subject. */
    private final String name;
    /** Type of this subject. */
    private final ISubjectType subjectType;

    /**
     * Constructor
     * @param uid
     * @param type
     */
    public Subject(String name, ISubjectType subjectType) {
        this.name = name;
        this.subjectType = subjectType;
    }

    /**
     * Constructor
     * @param uid
     * @param name
     * @param id
     * @param type
     */
    public Subject(String uid, String uniqueName, String name, Long id, ISubjectType subjectType) {
        this(name, subjectType);
        this.uid = uid;
        this.uniqueName = uniqueName;
        this.id = id;
    }

    /**
     * Returns the uid.
     * @return the uid.
     */
    public String getUid() {
        return this.uid;
    }

    /**
     * Returns the uniqueName.
     * @return the uniqueName.
     */
    public String getUniqueName() {
        return this.uniqueName;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer();
        if (name != null) {
            rv.append("Name: ").append(name).append(" ");
        }
        if (uid != null) {
            rv.append("UID: ").append(uid).append(" ");
        }
        if (uniqueName != null) {
            rv.append("PrincipalName: ").append(uniqueName).append(" ");
        }
        if (id != null) {
            rv.append("Id: ").append(id).append(" ");
        }
        if (subjectType != null) {
            rv.append("Type: ").append(subjectType);
        }

        return rv.toString();
    }

    /**
     * @see ISubject#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @see ISubject#getSubjectType()
     */
    public ISubjectType getSubjectType() {
        return subjectType;
    }

    /**
     * @see IHasId#getId()
     */
    public final Long getId() {
        return id;
    }

    /**
     * @see IDSubject#getGroups()
     */
    public IEvalValue getGroups() {
        return EvalValue.EMPTY;
    }

    /**
     * @see IDSubject#getAttribute(String)
     */
    public IEvalValue getAttribute(String name) {
        // FIXME This is done for compatibility with the old name attribute.
        // Remove the "if" when getName is removed from the subject interface.
        if ("name".equals(name)) {
            return EvalValue.build(getName());
        } else {
            return EvalValue.NULL;
        }
    }

    /**
     * @see IDSubject#getEntrySet()
     */

    public synchronized Set<Map.Entry<String, IEvalValue>> getEntrySet() {
        Map<String, IEvalValue> name = new HashMap<String, IEvalValue>();
        name.put("name", EvalValue.build(getName()));
        return name.entrySet();
    }

    public boolean isCacheable() {
        return true;
    }

}

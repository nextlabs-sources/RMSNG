/*
 * Created on Jan 5, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.subject;

import com.bluejungle.framework.patterns.EnumBase;
import com.bluejungle.pf.domain.epicenter.common.SpecType;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

import java.util.Set;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/SubjectType.java#2 $:
 */

public class SubjectType extends EnumBase implements ISubjectType {

    /**
     * <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1559857490432347965L;

    public static final SubjectType USER = new SubjectType("user", SpecType.USER);
    public static final SubjectType APPUSER = new SubjectType("appuser", SpecType.APPUSER);
    public static final SubjectType HOST = new SubjectType("host", SpecType.HOST);
    public static final SubjectType APP = new SubjectType("application", SpecType.APPLICATION);
    public static final SubjectType AGGREGATE = new SubjectType("principal", SpecType.ILLEGAL);
    public static final SubjectType RECIPIENT = new SubjectType("recipient", SpecType.RECIPIENT);

    private final SpecType specType;

    public SpecType getSpecType() {
        return specType;
    }

    private SubjectType(String name, SpecType specType) {
        super(name, SubjectType.class);
        this.specType = specType;
    }

    public static SubjectType getElement(String name) {
        return getElement(name, SubjectType.class);
    }

    public static int numElements() {
        return numElements(SubjectType.class);
    }

    public static Set<SubjectType> elements() {
        return elements(SubjectType.class);
    }

    public static SubjectType forName(String name) {
        return getElement(name, SubjectType.class);
    }
}

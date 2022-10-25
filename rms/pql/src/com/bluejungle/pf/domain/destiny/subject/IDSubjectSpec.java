package com.bluejungle.pf.domain.destiny.subject;

/*
 * Created on Jan 5, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.PredicateConstants;
import com.bluejungle.pf.domain.destiny.common.BuiltInSpec;
import com.bluejungle.pf.domain.destiny.common.IDSpec;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/IDSubjectSpec.java#1 $:
 */

public interface IDSubjectSpec extends IDSpec {

    public static final IDSubjectSpec EMPTY = new SubjectSpecBuiltin(
            "EMPTY"
            , "Built-in <EMPTY> subject"
            , SubjectType.AGGREGATE
            , PredicateConstants.FALSE) {
    };

    public static final IDSubjectSpec ALL_SUBJECTS = new SubjectSpecBuiltin(
            "*"
            , "Built-in <*> subject"
            , SubjectType.AGGREGATE
            , PredicateConstants.TRUE) {
    };

    public static final IDSubjectSpec ALL_USERS = new SubjectSpecBuiltin(
            "ALL_USERS"
            , "Built-in <ALL_USERS> subject"
            , SubjectType.USER
            , PredicateConstants.TRUE) {
    };

    public static final IDSubjectSpec ALL_HOSTS = new SubjectSpecBuiltin(
            "ALL_HOSTS"
            , "Built-in <ALL_HOSTS> subject"
            , SubjectType.HOST
            , PredicateConstants.TRUE) {
    };

    public static final IDSubjectSpec ALL_APPS = new SubjectSpecBuiltin(
            "ALL_APPLICATIONS"
            , "Built-in <ALL_APPLICATIONS> subject"
            , SubjectType.APP
            , PredicateConstants.TRUE) {
    };

    public class SubjectSpecBuiltin extends BuiltInSpec implements IDSubjectSpec {

        private final SubjectType subjectType;

        SubjectSpecBuiltin(String name, String description, SubjectType subjectType, IPredicate pred) {
            super(null, subjectType.getSpecType(), null, name, description, pred, true);
            this.subjectType = subjectType;
        }

        /**
         * @see com.bluejungle.pf.domain.destiny.subject.IDSubjectSpec#getSubjectType()
         */
        public ISubjectType getSubjectType() {
            return subjectType;
        }
    }

    /**
     * @return type of this subject spec
     */
    public ISubjectType getSubjectType();

}

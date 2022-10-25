/*
 * Created on Mar 16, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.destiny.parser;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.subject.Location;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/DefaultPQLVisitor.java#1 $:
 */

public class DefaultPQLVisitor implements IPQLVisitor {

    /**
     * @see IPQLVisitor#visitPolicy(DomainObjectDescriptor, IDPolicy)
     */
    public void visitPolicy(DomainObjectDescriptor descriptor, IDPolicy policy) {
    }

    /**
     * @see IPQLVisitor#visitAccessPolicy(DomainObjectDescriptor, IAccessPolicy)
     */
    public void visitAccessPolicy(DomainObjectDescriptor descriptor, IAccessPolicy accessPolicy) {
    }

    /**
     * @see IPQLVisitor#visitFolder(DomainObjectDescriptor)
     */
    public void visitFolder(DomainObjectDescriptor descriptor) {
    }

    /**
     * @see IPQLVisitor#visitComponent(DomainObjectDescriptor, IPredicate)
     */
    public void visitComponent(DomainObjectDescriptor descriptor, IPredicate spec) {
    }

    /**
     * @see IPQLVisitor#visitLocation(DomainObjectDescriptor, Location)
     */
    public void visitLocation(DomainObjectDescriptor descriptor, Location location) {
    }

}

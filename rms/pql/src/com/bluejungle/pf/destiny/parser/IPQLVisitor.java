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
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/IPQLVisitor.java#1 $:
 */

public interface IPQLVisitor {

    void visitPolicy(DomainObjectDescriptor descriptor, IDPolicy policy);

    void visitFolder(DomainObjectDescriptor descriptor);

    void visitComponent(DomainObjectDescriptor descriptor, IPredicate spec);

    void visitAccessPolicy(DomainObjectDescriptor descriptor, IAccessPolicy accessPolicy);

    void visitLocation(DomainObjectDescriptor descriptor, Location location);

}

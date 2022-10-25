/*
 * Created on Jun 17, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.resource;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;

/**
 * Policy-related resources, such as policies, subject specs, action specs, etc...
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/resource/IPResource.java#1 $:
 */

public interface IPResource extends IArguments {

    IDSubject getOwner();
}

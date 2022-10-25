package com.bluejungle.pf.domain.epicenter.policy;

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.epicenter.misc.IAccessControlled;

// Copyright Blue Jungle, Inc.

/*
 * IPolicyObject is either a policy or a policy set.
 *
 * @author Sasha Vladimirov
 *
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/epicenter/IPolicyObject.java#4 $
 */

public interface IPolicyObject extends IHasId, IAccessControlled {

    /**
     * @return name of this policy object
     */
    public String getName();

    /**
     *
     * @return description of this policy object
     */
    public String getDescription();

    /**
     * sets the description of this policy object
     * @param description to set
     */
    public void setDescription(String description);

    /**
     * @return a owner of this policy object.
     */
    IDSubject getOwner();

}

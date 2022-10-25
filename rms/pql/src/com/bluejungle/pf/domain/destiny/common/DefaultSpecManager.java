package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.comp.ComponentInfo;
import com.bluejungle.framework.comp.IHasComponentInfo;
import com.bluejungle.framework.comp.LifestyleType;
import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/DefaultSpecManager.java#1 $
 */

/**
 * The default implementation of the IDSpecManager (does nothing).
 */
public class DefaultSpecManager implements IDSpecManager, IHasComponentInfo<DefaultSpecManager> {

    public static final ComponentInfo<DefaultSpecManager> COMP_INFO =
            new ComponentInfo<DefaultSpecManager>(
                    DefaultSpecManager.class.getName(),
                    DefaultSpecManager.class,
                    DefaultSpecManager.class,
                    LifestyleType.SINGLETON_TYPE);

    protected static final String COMP_NAME = "DefaultSpecManager";

    Map<String, IDSpec> specsByName = Collections.synchronizedMap(new HashMap<String, IDSpec>());

    Map<Long, IDSpec> specsById = Collections.synchronizedMap(new HashMap<Long, IDSpec>());

    /**
     * @see IDSpecManager#resolveSpec(String,SpecType)
     */
    public void saveSpec(IDSpec spec) {
        if (spec.getName() == null && spec.getId() == null) {
            return;
        }
        if (spec.getName() != null) {
            specsByName.put(spec.getName(), spec);
        }
        if (spec.getId() != null) {
            specsById.put(spec.getId(), spec);
        }
    }

    /**
     * @see IDSpecManager#resolveSpec(String,SpecType)
     */
    public IDSpec resolveSpec(String specName) {
        if (specsByName.containsKey(specName)) {
            return specsByName.get(specName);
        }
        // TODO: look in deployed entities.
        return null;
    }

    /**
     * @see IDSpecManager#getSpecReference(String)
     */
    public IPredicate getSpecReference(String specName) {
        return new SpecReference(specName);
    }

    /**
     * @see IHasComponentInfo#getComponentInfo()
     */
    public ComponentInfo<DefaultSpecManager> getComponentInfo() {
        return COMP_INFO;
    }

    /**
     * @see IDSpecManager#resolveSpec(Long)
     */
    public IDSpec resolveSpec(Long id) {
        if (specsById.get(id) != null) {
            return (IDSpec)specsById.get(id);
        }
        // TODO: look in deployed entities.
        return null;
    }

    public IPredicate getSpecReference(Long id) {
        return new SpecReference(id);
    }

}

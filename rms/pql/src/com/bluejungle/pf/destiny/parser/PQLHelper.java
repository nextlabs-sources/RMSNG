/*
 * Created on Jul 28, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.destiny.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class with static utilities for handling PQL
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/PQLHelper.java#1 $:
 */

public class PQLHelper {

    /**
     * Extracts PQL from a <code>Collection</code> of <code>IHasPQL</code> objects.
     * @param entities a <code>Collection</code> of <code>IHasPQL</code> objects.
     * @return a <code>Collection</code> of PQL strings.
     */
    public static Collection<String> extractPQL(Collection<? extends IHasPQL> entities) {
        List<String> res = new ArrayList<String>();
        for (IHasPQL pqlEnt : entities) {
            res.add(pqlEnt.getPql());
        }
        return res;
    }

}

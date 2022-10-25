/*
 * Created on Dec 28, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.obligation;

import java.util.List;

/**
 * Obligation Manager Interface
 *
 * @author sasha
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/etc/eclipse/destiny-code-templates.xml#3 $:
 */

public interface IDObligationManager {

    String CLASSNAME = IDObligationManager.class.getName();

    LogObligation createLogObligation();

    NotifyObligation createNotifyObligation(String emailAddresses, String body);

    DisplayObligation createDisplayObligation(String message);

    CustomObligation createCustomObligation(String oblName, List args);

}

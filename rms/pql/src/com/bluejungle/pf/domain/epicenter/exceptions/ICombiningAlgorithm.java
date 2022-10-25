/*
 * Created on Mar 13, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/exceptions/ICombiningAlgorithm.java#1 $:
 */
package com.bluejungle.pf.domain.epicenter.exceptions;

import com.bluejungle.framework.patterns.IEnum;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;

public interface ICombiningAlgorithm extends IEnum {

    public IDEffectType getEffect(int numberAllow, int numberDeny);
}

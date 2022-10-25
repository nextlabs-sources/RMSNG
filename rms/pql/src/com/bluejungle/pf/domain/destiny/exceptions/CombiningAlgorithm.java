/*
 * Created on Mar 13, 2013
 *
 * All sources, binaries and HTML pages (C) copyright 2013 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author amorgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/exceptions/CombiningAlgorithm.java#1 $:
 */

package com.bluejungle.pf.domain.destiny.exceptions;

import com.bluejungle.framework.patterns.EnumBase;
import com.bluejungle.pf.domain.destiny.misc.EffectType;
import com.bluejungle.pf.domain.destiny.misc.IDEffectType;
import com.bluejungle.pf.domain.epicenter.exceptions.ICombiningAlgorithm;

public abstract class CombiningAlgorithm extends EnumBase implements ICombiningAlgorithm {

    private CombiningAlgorithm(String name) {
        super(name);
    };

    public static final CombiningAlgorithm ALLOW_OVERRIDES = new CombiningAlgorithm("allow_overrides") {

        @Override
        public IDEffectType getEffect(int numberAllow, int numberDeny) {
            if (numberAllow > 0) {
                return EffectType.ALLOW;
            } else if (numberDeny > 0) {
                return EffectType.DENY;
            } else {
                return EffectType.DONT_CARE;
            }
        }
    };

    public static final CombiningAlgorithm DENY_OVERRIDES = new CombiningAlgorithm("deny_overrides") {

        @Override
        public IDEffectType getEffect(int numberAllow, int numberDeny) {
            if (numberDeny > 0) {
                return EffectType.DENY;
            } else if (numberAllow > 0) {
                return EffectType.ALLOW;
            } else {
                return EffectType.DONT_CARE;
            }
        }
    };

    public IDEffectType getEffect(int numberAllow, int numberDeny) {
        return EffectType.DONT_CARE;
    }
}

package com.bluejungle.pf.engine.destiny;

/*
 * Created on Nov 10, 2008
 *
 * All sources, binaries and HTML pages (C) copyright 2008 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/IContentAnalysisManager.java#1 $
 */

import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

import java.util.BitSet;

public interface IContentAnalysisManager {

    public static final IContentAnalysisManager DEFAULT = new IContentAnalysisManager() {

        public BitSet performContentAnalysis(IEvaluationRequest req, IDPolicy[] policies, BitSet applicables) {
            return null;
        }
    };

    /**
     * @param req the evaluation request
     * @param policies the deployed policies
     * @param applicables the policies determined to be applicable by use of the maps
     * @return a bitset of the policies for which content analysis was not done (because it was determined to be unnecessary)
     */
    BitSet performContentAnalysis(IEvaluationRequest req, IDPolicy[] policies, BitSet applicables);
}

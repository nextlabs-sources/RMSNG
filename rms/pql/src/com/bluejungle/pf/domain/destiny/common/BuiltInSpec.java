package com.bluejungle.pf.domain.destiny.common;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.pf.destiny.lifecycle.DevelopmentStatus;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

/* All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/common/BuiltInSpec.java#1 $
 */

/**
 * @author sergey
 */
public class BuiltInSpec extends SpecBase {

    /**
     * Constructor
     * @param id
     * @param name
     * @param description
     */
    public BuiltInSpec(IDSpecManager manager, SpecType specType, Long id, String name, String description,
        IPredicate pred, boolean hidden) {
        super(manager, specType, id, name, description, DevelopmentStatus.APPROVED, pred, hidden);
    }

    /**
     * Makes it illegal to change the <code>IPredicate</code> of this spec.
     * @param pred the new <code>IPredicate</code> object.
     */
    public final void setPredicate(IPredicate ignored) {
        throw new UnsupportedOperationException("Changing the predicate of a built-in spec is illegal.");
    }

    /**
     * Makes it illegal to change the name of this spec.
     * @param name the new name.
     */
    public final void setName(String ignored) {
        throw new UnsupportedOperationException("Changing the name of a built-in spec is illegal.");
    }

    /**
     * Makes it illegal to change the description of this spec.
     * @param description The description to set.
     */
    public final void setDescription(String ignored) {
        throw new UnsupportedOperationException("Changing the description of a built-in spec is illegal.");
    }

    /**
     * Makes it illegal to change the status of this spec.
     * @param description The status to set.
     */
    public final void setStatus(DevelopmentStatus ignored) {
        throw new UnsupportedOperationException("Changing the status of a built-in spec is illegal.");
    }

    /**
     * @see com.bluejungle.pf.domain.destiny.common.IDSpec#accept(com.bluejungle.pf.domain.destiny.common.IPredicateVisitor)
     * Makes it illegal to change the status of this spec.
     * @param description The status to set.
     */
    public void accept(IPredicateVisitor v, IPredicateVisitor.Order order) {
        v.visit(this);
    }
}

package com.bluejungle.pf.domain.destiny.action;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IPredicateVisitor;
import com.bluejungle.framework.patterns.EnumBase;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;

import java.util.Collection;

// Copyright Blue Jungle, Inc.

/**
 * This class implements leaf-level (non-group) actions in the policy framework.
 *
 * @author Sasha Vladimirov
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/common/pf/com/bluejungle/pf/domain/destiny/action/Action.java#1 $
 */

public class DAction extends EnumBase implements IDAction {

    public static IDAction CREATE_NEW = new DAction(CREATE_NEW_NAME);
    public static IDAction OPEN = new DAction(OPEN_NAME);
    public static IDAction DELETE = new DAction(DELETE_NAME);
    public static IDAction CHANGE_PROPERTIES = new DAction(CHANGE_PROPERTIES_NAME);
    public static IDAction CHANGE_SECURITY = new DAction(CHANGE_SECURITY_NAME);
    public static IDAction EDIT = new DAction(EDIT_NAME);
    public static IDAction EMBED = new DAction(EMBED_NAME);
    public static IDAction EDIT_COPY = new DAction(EDIT_COPY_NAME);
    public static IDAction SENDTO = new DAction(SENDTO_NAME);
    public static IDAction CUT_PASTE = new DAction(CUT_PASTE_NAME);
    public static IDAction COPY_PASTE = new DAction(COPY_PASTE_NAME);
    public static IDAction BATCH = new DAction(BATCH_NAME);
    public static IDAction BURN = new DAction(BURN_NAME);
    public static IDAction PRINT = new DAction(PRINT_NAME);
    public static IDAction COPY = new DAction(COPY_NAME);
    public static IDAction RENAME = new DAction(RENAME_NAME);
    public static IDAction MOVE = new DAction(MOVE_NAME);
    public static IDAction SHARE = new DAction(SHARE_NAME);
    public static IDAction EMAIL = new DAction(EMAIL_NAME);
    public static IDAction IM = new DAction(IM_NAME);
    public static IDAction WEBMAIL = new DAction(WEBMAIL_NAME);
    public static IDAction ADMIN = new DAction(ADMIN_NAME);
    public static IDAction READ = new DAction(READ_NAME);
    public static IDAction WRITE = new DAction(WRITE_NAME);
    public static IDAction DEPLOY = new DAction(DEPLOY_NAME);
    public static IDAction APPROVE = new DAction(APPROVE_NAME);
    public static IDAction NOP = new DAction(NOP_NAME);
    public static IDAction EXPORT = new DAction(EXPORT_NAME);
    public static IDAction ATTACH = new DAction(ATTACH_NAME);
    public static IDAction RUN = new DAction(RUN_NAME);
    public static IDAction AVD = new DAction(AVD_NAME);
    public static IDAction MEETING = new DAction(MEETING_NAME);
    public static IDAction PRESENCE = new DAction(PRESENCE_NAME);
    public static IDAction RECORD = new DAction(RECORD_NAME);
    public static IDAction QUESTION = new DAction(QUESTION_NAME);
    public static IDAction VOICE = new DAction(VOICE_NAME);
    public static IDAction VIDEO = new DAction(VIDEO_NAME);
    public static IDAction JOIN = new DAction(JOIN_NAME);

    synchronized public static DAction getAction(String name) {
        if (!existsAction(name)) {
            IDAction dummy = new DAction(name);
        }
        return getElement(name, DAction.class);
    }

    public static DAction getAction(int type) {
        return getElement(type, DAction.class);
    }

    public static boolean existsAction(String name) {
        return existsElement(name, DAction.class);
    }

    public static boolean existsAction(int type) {
        return existsElement(type, DAction.class);
    }

    public static Collection<DAction> allActions() {
        return elements(DAction.class);
    }

    /**
     * Returns if action is equal to this object.
     *
     * @param action
     */
    public boolean match(IArguments request) {
        if (!(request instanceof IEvaluationRequest)) {
            return false;
        }
        return this.equals(((IEvaluationRequest)request).getAction());
    }

    private DAction(String name) {
        super(name, DAction.class);
    }

    /**
     * @see com.bluejungle.pf.domain.destiny.common.IDSpec#accept(com.bluejungle.pf.domain.destiny.common.ISpecVisitor)
     */
    public void accept(IPredicateVisitor v, IPredicateVisitor.Order order) {
        v.visit(this);
    }

    public Long getId() {
        return new Long(getType());
    }
}

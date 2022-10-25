/*
 * Created on Dec 30, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.destiny.action;

import com.bluejungle.pf.domain.epicenter.action.IAction;

/**
 * Destiny-specific actions
 *
 * @author sasha
 * @version $Id:
 *          //depot/personal/sasha/main/Destiny/src/etc/eclipse/destiny-code-templates.xml#3 $:
 */

public interface IDAction extends IAction {

    String CREATE_NEW_NAME = "CREATE_NEW";
    String OPEN_NAME = "OPEN";
    String DELETE_NAME = "DELETE";
    String CHANGE_PROPERTIES_NAME = "CHANGE_ATTRIBUTES";
    String CHANGE_SECURITY_NAME = "CHANGE_SECURITY";
    String EDIT_NAME = "EDIT";
    String EMBED_NAME = "EMBED";
    String EDIT_COPY_NAME = "EDIT_COPY";
    String SENDTO_NAME = "SENDTO";
    String CUT_PASTE_NAME = "CUT_PASTE";
    String COPY_PASTE_NAME = "PASTE";
    String BATCH_NAME = "BATCH";
    String BURN_NAME = "BURN";
    String PRINT_NAME = "PRINT";
    String COPY_NAME = "COPY";
    String RENAME_NAME = "RENAME";
    String MOVE_NAME = "MOVE";
    String SHARE_NAME = "SHARE";
    String EMAIL_NAME = "EMAIL";
    String IM_NAME = "IM";
    String WEBMAIL_NAME = "WEBMAIL";
    String ADMIN_NAME = "ADMIN";
    String READ_NAME = "READ";
    String WRITE_NAME = "WRITE";
    String DEPLOY_NAME = "DEPLOY";
    String APPROVE_NAME = "APPROVE";
    String NOP_NAME = "NOP";
    String EXPORT_NAME = "EXPORT";
    String ATTACH_NAME = "ATTACH";
    String RUN_NAME = "RUN";
    String AVD_NAME = "AVDCALL";
    String MEETING_NAME = "MEETING";
    String PRESENCE_NAME = "PRESENCE";
    String RECORD_NAME = "RECORD";
    String QUESTION_NAME = "QUESTION";
    String VOICE_NAME = "VOICE";
    String VIDEO_NAME = "VIDEO";
    String JOIN_NAME = "JOIN";
}

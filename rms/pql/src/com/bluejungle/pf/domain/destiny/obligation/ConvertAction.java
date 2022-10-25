package com.bluejungle.pf.domain.destiny.obligation;

/*
 * Create on Apr 9, 2007
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Next Labs Inc.,
 * San Mateo CA, Ownership remains with Next Labs Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.domain.action.ActionEnumType;

import java.util.HashMap;
import java.util.Map;

public class ConvertAction {

    private static final Map<String, String> actionToGUIString;

    public static String convert(String action) {
        String value = actionToGUIString.get(action);

        if (value == null) {
            value = action;
        }

        return value;
    }

    static {
        actionToGUIString = new HashMap<String, String>();

        actionToGUIString.put(ActionEnumType.ACTION_CHANGE_ATTRIBUTES.getName(), "Change Attributes");
        actionToGUIString.put(ActionEnumType.ACTION_CHANGE_SECURITY.getName(), "Change File Permissions");
        actionToGUIString.put(ActionEnumType.ACTION_COPY.getName(), "Copy / Embed File");
        actionToGUIString.put(ActionEnumType.ACTION_PASTE.getName(), "Paste");
        actionToGUIString.put(ActionEnumType.ACTION_DELETE.getName(), "Delete");
        actionToGUIString.put(ActionEnumType.ACTION_MOVE.getName(), "Move");
        actionToGUIString.put(ActionEnumType.ACTION_EMBED.getName(), "XXX");
        actionToGUIString.put(ActionEnumType.ACTION_PRINT.getName(), "Print");
        actionToGUIString.put(ActionEnumType.ACTION_OPEN.getName(), "Open");
        actionToGUIString.put(ActionEnumType.ACTION_EDIT.getName(), "Create/Edit");
        actionToGUIString.put(ActionEnumType.ACTION_RENAME.getName(), "Rename"); // XXX
        actionToGUIString.put(ActionEnumType.ACTION_SEND_EMAIL.getName(), "Email");
        actionToGUIString.put(ActionEnumType.ACTION_SEND_IM.getName(), "Instant Message");
        actionToGUIString.put(ActionEnumType.ACTION_EXPORT.getName(), "Export");
        actionToGUIString.put(ActionEnumType.ACTION_ATTACH.getName(), "Attach to Item");
    }
}

package com.bluejungle.pf.domain.destiny.obligation;

/*
 * Created on Apr 14, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Next Labs Inc.,
 * San Mateo CA, Ownership remains with Next Labs Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.domain.log.PolicyActivityInfoV5;
import com.bluejungle.domain.log.ResourceInformation;
import com.bluejungle.framework.utils.StringUtils;

import java.text.DateFormat;
import java.util.Date;

/**
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/obligation/NotifyObligation.java#2 $:
 */

public class NotifyObligation extends DObligation {

    private static final long serialVersionUID = 1L;

    protected String emailAddresses;
    protected String body;

    public static final String OBLIGATION_NAME = "notify";

    private static final DateFormat DF = DateFormat.getDateTimeInstance();

    /**
     * Constructor
     * @param emailAddresses
     * @param body
     */
    NotifyObligation(String emailAddresses, String body) {
        super();
        this.emailAddresses = emailAddresses;
        this.body = body;
    }

    /**
     * @see com.bluejungle.pf.domain.epicenter.misc.IObligation#getType()
     */
    public String getType() {
        return OBLIGATION_NAME;
    }

    /**
     * Creates the message body
     *
     * @param request evaluation request
     * @param effect policie's effect
     * @param userResponse user's response
     * @return notification message body
     */
    public String createBody(PolicyActivityInfoV5 args) {
        StringBuffer rv = new StringBuffer(body);

        String decision = args.getPolicyDecision().toString();

        // Capitalize first letter.  Gross.
        if (decision == null || decision.equals("allow")) {
            decision = "Allow";
        } else {
            decision = "Deny";
        }
        rv.append("\nEnforcement: ").append(decision);
        rv.append("\nUser: ").append(args.getUserName());
        rv.append("\nHost: ").append(args.getHostName());
        rv.append("\nApplication: ").append(args.getApplicationName());
        rv.append("\nAction: ").append(ConvertAction.convert(args.getAction()));
        rv.append("\nFrom: ").append(args.getFromResourceInfo().getName());

        String emailRecipients = StringUtils.join(args.getAttributesMap().get(PolicyActivityInfoV5.FROM_RESOURCE_ATTRIBUTES_TAG).getStrings("Sent to"), ";");

        if (!emailRecipients.equals("")) {
            rv.append("\nSent to: ").append(emailRecipients);
        }

        ResourceInformation info = args.getToResourceInfo();
        String toName = null;
        if (info != null && (toName = info.getName()) != null && toName.length() > 0) {
            rv.append("\nTo: ").append(info.getName());
        }
        rv.append("\nPolicy: ").append(po.getName());

        String desc = po.getDescription();
        if (desc == null) {
            desc = "";
        }
        rv.append("\nDescription: ").append(desc);
        long ts = args.getTs();
        Date date = new Date(ts);
        rv.append("\nEvent Time: ").append(DF.format(date));
        return rv.toString();
    }

    /**
     * @see com.bluejungle.pf.domain.destiny.obligation.IDObligation#toPQL()
     */
    public String toPQL() {
        return getType() + " \"" + StringUtils.escape(emailAddresses) + "\" \"" + StringUtils.escape(body) + "\"";
    }

    /**
     * Returns the body.
     * @return the body.
     */
    public String getBody() {
        return this.body;
    }

    /**
     * Sets the body
     * @param body The body to set.
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Returns the emailAddresses.
     * @return the emailAddresses.
     */
    public String getEmailAddresses() {
        return this.emailAddresses;
    }

    /**
     * Sets the emailAddresses
     * @param emailAddresses The emailAddresses to set.
     */
    public void setEmailAddresses(String emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

}

/*
 * Created Feb 21, 2007
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Next Labs Inc.,
 * Redwood City CA, Ownership remains with Next Labs Inc, All rights reserved
 * worldwide.
 */

package com.bluejungle.pf.domain.destiny.obligation;

import com.bluejungle.framework.utils.StringUtils;
import com.bluejungle.pf.destiny.parser.PQLParser;

import java.util.List;

/*
 * Implementation of CustomObligation for the server side
 * @author Alan Morgan
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/obligation/CustomObligation.java#1 $
 */

public class CustomObligation extends DObligation {

    public static final long serialVersionUID = 1L;
    public static final String OBLIGATION_NAME = "custom";

    private String oblName;
    private List<? extends Object> args;

    public CustomObligation(String oblName) {
        this.oblName = oblName;
        args = null;
    }

    public CustomObligation(String oblName, List<? extends Object> args) {
        this.oblName = oblName;
        this.args = args;
    }

    public String toPQL() {
        StringBuffer res = new StringBuffer();
        res.append(PQLParser.quoteName(oblName));
        if (args != null && !args.isEmpty()) {
            res.append("(");
            boolean isFirst = true;
            for (Object arg : args) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    res.append(", ");
                }
                if (arg instanceof String) {
                    String s = (String)arg;
                    res.append("\"" + StringUtils.escape(s) + "\"");
                } else {
                    res.append(arg);
                }
            }
            res.append(")");
        }
        return res.toString();
    }

    public String getType() {
        return OBLIGATION_NAME;
    }

    public String getCustomObligationName() {
        return oblName;
    }

    public void setCustomObligationName(String oblName) {
        this.oblName = oblName;
    }

    public List<? extends Object> getCustomObligationArgs() {
        return args;
    }

    public void setCustomArgs(List<? extends Object> args) {
        this.args = args;
    }
}

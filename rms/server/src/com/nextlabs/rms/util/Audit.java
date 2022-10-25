package com.nextlabs.rms.util;

import com.nextlabs.common.util.Hex;
import com.nextlabs.rms.shared.LogConstants;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Audit {

    private static final Logger LOG = LogManager.getLogger(LogConstants.RMS_AUDIT_LOG_NAME);

    private Audit() {
    }

    public static void audit(HttpServletRequest req, String category, String svc, String cmd, int result,
        Object... params) {
        Object[] objs = new Object[params.length + 7];
        objs[0] = category;
        objs[1] = svc;
        objs[2] = cmd;
        objs[3] = result;
        objs[4] = req.getRemoteAddr();
        objs[5] = req.getHeader("clientId");
        objs[6] = req.getHeader("platformId");
        System.arraycopy(params, 0, objs, 7, params.length);
        LOG.info(getMessage(objs));
    }

    private static String getMessage(Object[] objs) {
        StringBuilder builder = new StringBuilder();
        for (Object obj : objs) {
            if (obj instanceof byte[]) {
                builder.append(Hex.toHexString((byte[])obj));
            } else if (obj instanceof Object[]) {
                builder.append(getMessage((Object[])obj));
            } else if (obj != null) {
                builder.append(' ').append(obj.toString());
            }
        }
        return builder.toString();
    }
}

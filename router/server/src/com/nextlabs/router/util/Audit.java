package com.nextlabs.router.util;

import com.nextlabs.common.util.Hex;
import com.nextlabs.router.servlet.LogConstants;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Audit {

    private static final Logger LOG = LogManager.getLogger(LogConstants.ROUTER_AUDIT_LOG_NAME);

    private Audit() {
    }

    public static void audit(HttpServletRequest req, String category, String svc, String cmd, int result,
        Object... params) {
        Object[] objs = new Object[params.length + 6];
        objs[0] = category;
        objs[1] = svc;
        objs[2] = cmd;
        objs[3] = result;
        objs[4] = req.getRemoteAddr();
        objs[5] = req.getHeader("client_id");
        System.arraycopy(params, 0, objs, 6, params.length);
        LOG.info(getMessage(objs));
    }

    private static String getMessage(Object[] objs) {
        StringBuilder builder = new StringBuilder();
        int i = objs.length;
        for (int j = 0; j < i; j++) {
            Object obj = objs[j];
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

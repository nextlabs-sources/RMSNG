package com.nextlabs.rms.viewer.util;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Audit {

    private static final Logger LOG = LogManager.getLogger(LogConstants.VIEWER_AUDIT_LOG_NAME);

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
        StringBuilder strBuilder = new StringBuilder();
        int i = objs.length;
        for (int j = 0; j < i; j++) {
            Object localObject = objs[j];
            if (localObject instanceof Object[]) {
                strBuilder.append(getMessage((Object[])localObject)).append(' ');
            } else if (localObject != null) {
                strBuilder.append(localObject.toString()).append(' ');
            }
        }
        return strBuilder.toString();
    }
}

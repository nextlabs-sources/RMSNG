package com.nextlabs.rms.viewer.command;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public static AbstractCommand createCommand(HttpServletRequest request)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String relativeUri = request.getRequestURI();
        String ctxt = request.getContextPath() + "/RMSViewer";
        StringBuffer clsNameBuf = null;
        if (relativeUri.startsWith(ctxt)) {
            relativeUri = relativeUri.substring(ctxt.length() + 1);
            clsNameBuf = new StringBuffer("com.nextlabs.rms.viewer.command");
        } else {
            throw new IllegalArgumentException("Unrecognized command");
        }
        String[] paths = relativeUri.split("/");

        for (String str : paths) {
            clsNameBuf.append('.');
            clsNameBuf.append(str);
        }
        clsNameBuf.append("Command");
        String clsName = clsNameBuf.toString();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Command class to be instantiated: " + clsName);
        }
        Class<?> cls = Class.forName(clsName);
        return (AbstractCommand)cls.newInstance();
    }

    public abstract void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException;

}

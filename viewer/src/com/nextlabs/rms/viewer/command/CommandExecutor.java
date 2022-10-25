package com.nextlabs.rms.viewer.command;

import com.nextlabs.rms.viewer.servlets.LogConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CommandExecutor {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private CommandExecutor() {
    }

    public static void executeCommand(HttpServletRequest request, HttpServletResponse response)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        AbstractCommand cmd = null;
        try {
            cmd = AbstractCommand.createCommand(request);
            cmd.doAction(request, response);
        } catch (Exception e) {
            LOGGER.error("Error occurred while executing command - " + (cmd != null ? cmd.getClass().getName() : "<unknown>"), e);
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Command: {} executed, returning..", cmd.getClass().getName());
        }
    }
}

package com.nextlabs.rms.viewer.servlets;

import com.nextlabs.rms.viewer.command.CommandExecutor;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewerServlet extends HttpServlet {

    private static final long serialVersionUID = -5553255427500833904L;

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        processReq(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        processReq(request, response);
    }

    public void processReq(HttpServletRequest request, HttpServletResponse response) {
        try {
            CommandExecutor.executeCommand(request, response);
        } catch (Exception e) {
            LOGGER.error("Error occured while processing request", e);
        }
    }
}

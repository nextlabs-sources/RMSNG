package com.nextlabs.rms.servlets;

import com.nextlabs.rms.command.AbstractCommand;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RMSServlet extends HttpServlet {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final long serialVersionUID = -5553255427500833904L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processReq(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processReq(request, response);
    }

    public void processReq(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            AbstractCommand cmd = AbstractCommand.createCommand(request);
            cmd.doAction(request, response);
        } catch (IllegalAccessException e) {
            if (!response.isCommitted()) {
                response.sendError(400, "Invalid command.");
            }
        } catch (InstantiationException e) {
            if (!response.isCommitted()) {
                response.sendError(400, "Invalid command.");
            }
        } catch (ClassNotFoundException e) {
            if (!response.isCommitted()) {
                response.sendError(404, "Invalid command.");
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(500, "Internal Server Error.");
            }
        }
    }
}

package com.nextlabs.rms.command;

import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.hibernate.DbSession;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractCommand {

    public static AbstractCommand createCommand(HttpServletRequest request) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        URI uri = URI.create(request.getRequestURI());
        String path = uri.getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int pos = path.lastIndexOf('/');
        String cmd = path.substring(pos + 1);
        String clsName = "com.nextlabs.rms.command." + cmd + "Command";
        Class<?> cls = Class.forName(clsName);
        return (AbstractCommand)cls.newInstance();
    }

    protected static RMSUserPrincipal authenticate(DbSession session, HttpServletRequest request) {
        return AuthManager.authenticate(session, request);
    }

    protected static RMSUserPrincipal authenticate(HttpServletRequest request) {
        try (DbSession session = DbSession.newSession()) {
            return authenticate(session, request);
        }
    }

    public abstract void doAction(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException;
}

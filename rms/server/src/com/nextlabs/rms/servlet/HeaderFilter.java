package com.nextlabs.rms.servlet;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.rs.AbstractLogin;

import java.io.IOException;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeaderFilter implements Filter {

    private boolean debugMode;
    public static final String LOGIN_PAGE = "/Login.jsp";
    public static final String LOGIN_ADMIN_PAGE = "/LoginAdmin.jsp";
    public static final String LOGIN_ENDPOINT = "/login";
    public static final String LOGIN_ADMIN_ENDPOINT = "/loginAdmin";
    public static final String RESET_PASS_PAGE = "/ResetPassword.jsp";
    public static final String RESET_PASS_ENDPOINT = "/resetPassword";
    public static final String FORGOT_PASS_PAGE = "/ForgotPassword.jsp";
    public static final String FORGOT_PASS_ENDPOINT = "/forgotPassword";
    public static final String REGISTER_PAGE = "/Register.jsp";
    public static final String REGISTER_ENDPOINT = "/register";
    public static final String INTRO_PAGE = "/Intro.jsp";
    public static final String INTRO_ENDPOINT = "/intro";
    private static final String VALID_METHODS = "GET, POST, PATCH, PUT, DELETE, OPTIONS";
    private static final Logger LOGGER = LogManager.getLogger(HeaderFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        URI uri = URI.create(req.getRequestURL().toString());
        resp.addHeader("Access-Control-Allow-Origin", "*");
        if (!debugMode && "http".equalsIgnoreCase(uri.getScheme())) {
            resp.sendRedirect("https://" + uri.getHost() + req.getContextPath());
            return;
        }

        if (uri.getPath().endsWith(LOGIN_PAGE) || uri.getPath().endsWith(LOGIN_ENDPOINT) || uri.getPath().endsWith(LOGIN_ADMIN_PAGE) || uri.getPath().endsWith(LOGIN_ADMIN_ENDPOINT) || uri.getPath().endsWith(FORGOT_PASS_PAGE) || uri.getPath().endsWith(FORGOT_PASS_ENDPOINT) || uri.getPath().endsWith(RESET_PASS_PAGE) || uri.getPath().endsWith(RESET_PASS_ENDPOINT) || uri.getPath().endsWith(REGISTER_PAGE) || uri.getPath().endsWith(REGISTER_ENDPOINT) || uri.getPath().endsWith(INTRO_PAGE) || uri.getPath().endsWith(INTRO_ENDPOINT)) {
            lookupTenant(req);
        }

        String origin = req.getHeader("Origin");
        if (origin == null) {
            // Return standard response if OPTIONS request w/o Origin header
            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                resp.setHeader("Allow", VALID_METHODS);
                resp.setStatus(200);
                return;
            }
        } else {
            // This is a cross-domain request, add headers allowing access
            try {
                resp.setHeader("Access-Control-Allow-Origin", encodeWord(origin));
                resp.setHeader("Access-Control-Allow-Methods", VALID_METHODS);

                String headers = req.getHeader("Access-Control-Request-Headers");
                if (headers != null) {
                    resp.setHeader("Access-Control-Allow-Headers", encodeWord(headers));
                }
                resp.setHeader("Access-Control-Max-Age", "3600");
                resp.addHeader("Access-Control-Allow-Credentials", "true");
            } catch (IllegalArgumentException e) {
                LOGGER.warn(e.getMessage(), e);
                resp.sendError(200, "Illegal header");
                return;
            }
        }
        // Pass request down the chain, except for OPTIONS
        if (!"OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        WebConfig config = WebConfig.getInstance();
        debugMode = Boolean.parseBoolean(config.getProperty(WebConfig.DEBUG, "false"));
    }

    @Override
    public void destroy() {
    }

    private static String encodeWord(String word) {
        if (word.contains("\r\n")) {
            throw new IllegalArgumentException("HTTP response splitting attack: " + word);
        }
        return word;
    }

    private void lookupTenant(HttpServletRequest req) {
        Tenant tenant = AbstractLogin.getTenantFromUrl(req);
        if (tenant != null) {
            req.setAttribute("tenant", tenant);
        }
    }
}

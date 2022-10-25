package com.nextlabs.rms.viewer.servlets;

import java.io.IOException;

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

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);
    private static final String VALID_METHODS = "GET, POST, PATCH, PUT, DELETE, OPTIONS";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletResponse resp = (HttpServletResponse)response;
        HttpServletRequest req = (HttpServletRequest)request;

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
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
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
}

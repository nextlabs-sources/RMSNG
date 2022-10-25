package com.nextlabs.router.servlet;

import com.nextlabs.common.shared.WebConfig;

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

public class HeaderFilter implements Filter {

    private boolean debugMode;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletResponse resp = (HttpServletResponse)response;
        if (debugMode) {
            resp.addHeader("Access-Control-Allow-Origin", "*");
        } else {
            HttpServletRequest req = (HttpServletRequest)request;
            URI uri = URI.create(req.getRequestURL().toString());
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                resp.sendRedirect("https://" + uri.getHost() + req.getContextPath());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        WebConfig config = WebConfig.getInstance();
        debugMode = Boolean.parseBoolean(config.getProperty(WebConfig.DEBUG, "false"));
    }

    @Override
    public void destroy() {
    }
}

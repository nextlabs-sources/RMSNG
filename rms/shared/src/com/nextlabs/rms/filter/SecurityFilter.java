package com.nextlabs.rms.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class SecurityFilter implements Filter {

    private boolean xssProtectionEnabled;

    @Override
    public void init(FilterConfig config) throws ServletException {
        xssProtectionEnabled = Boolean.parseBoolean(config.getInitParameter("xss-protection"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse resp = HttpServletResponse.class.cast(response);
            if (xssProtectionEnabled) {
                resp.setHeader("X-XSS-Protection", "1; mode=block");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@page import="java.io.File" %>
<%
    String tenantDomain = (String)request.getAttribute("resolvedTenantUI");
    String webDir = request.getServletContext().getRealPath("/");
    String tenantDir = webDir + File.separator + "tenants" + File.separator + tenantDomain + File.separator + "images";
    File tenantLogoFile = new File(tenantDir, "logo.png");
    boolean tenantLogoPresent = tenantLogoFile.exists();
%>
<div id="smaller-browser-control-partner-logos">
    <div id="browser-control-nxl-logo">
        <img id="nxl-logo-mobile" src="tenants/skydrm.com/images/logo-black.svg"/>
    </div>
    <% if(tenantLogoPresent) { %>
        <div id="browser-control-solution-partner-logo">
            <img src="tenants/<%=tenantDomain%>/images/logo.png"/>
        </div>
    <% } %>
</div>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@page import="java.io.File" %>
<%@page import="com.nextlabs.rms.hibernate.model.Tenant" %>
<%@page import="com.nextlabs.common.shared.WebConfig" %>
<%@page import="javax.servlet.http.Cookie" %>
<%@page import="java.net.URLDecoder" %>
<%
    String defaultTenant = "skydrm.com";
    String configuredTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
    String tenantDomain = configuredTenant;

	// This is used for IDPLogins
	String tenantFromCookie = null;
	Cookie[] cookies = request.getCookies();
	if (cookies != null) {
		for (Cookie cookie : cookies) {
			if ("lt".equals(cookie.getName())) {
				tenantFromCookie = cookie.getValue();
				if(tenantFromCookie!=null)	{
					tenantFromCookie = URLDecoder.decode(tenantFromCookie,"UTF-8");
                }                
				break;
			}
		}
	}

	if(tenantFromCookie != null) {
		tenantDomain = tenantFromCookie;
	} else {
		Tenant t = (Tenant)request.getAttribute("tenant");
		if(t != null && t.getName()!=null && !"".equals(t.getName())){
			tenantDomain = URLDecoder.decode(t.getName(),"UTF-8");
		}
	}

    String webDir = request.getServletContext().getRealPath("/");
    String tenantDir = webDir + File.separator + "tenants" + File.separator + tenantDomain;
    File tenantPageFile = new File(tenantDir, "index.jsp");
    String tenantUI = tenantPageFile.exists() ? tenantDomain : defaultTenant;
    
    request.setAttribute("resolvedTenant",tenantDomain);
    request.setAttribute("resolvedTenantUI",tenantUI);
    request.setAttribute("defaultTenant",defaultTenant);
    pageContext.setAttribute("defaultTenant", defaultTenant);
%>

<% if(!defaultTenant.equals(tenantUI)){ %>
<link href="${pageContext.request.contextPath }/tenants/${requestScope.resolvedTenantUI}/css/style.css?v=${applicationScope['version']}" rel="stylesheet" type="text/css" >
<% } %>
<link href="${pageContext.request.contextPath }/tenants/${defaultTenant}/css/style.css?v=${applicationScope['version']}" rel="stylesheet" type="text/css" >
<script type="text/javascript">
    function redirectToTenantPage(page){
        var query = window.location.search;
        <% if(!configuredTenant.equals(tenantDomain)){ %>
            if(!query || query === "") {
                query = "?tenant=${requestScope.resolvedTenant}";
            } else if (query.indexOf("tenant=") === -1){
                query += "&tenant=${requestScope.resolvedTenant}";
            }
        <% } %>
        window.location.assign("${pageContext.request.contextPath}" + page + query);
    }
	function goToIntro(){
		redirectToTenantPage("/intro");
	}
	function goToLogin(){
		if(readCookie("adminApp") == "true") {
			redirectToTenantPage("/loginAdmin")
		} else {
			redirectToTenantPage("/login");
		}
	}
	function goToForgotPassword(){
		redirectToTenantPage("/forgotPassword");
	}
	function goToRegister(){
		redirectToTenantPage("/register");
	}
</script>
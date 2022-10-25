<%@page import="com.nextlabs.rms.util.CookieUtil"%>
<%@page import="com.nextlabs.rms.config.Constants"%>
<%@page import="com.nextlabs.common.shared.WebConfig"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" />

    <title>SkyDRM</title>

    <!-- Bootstrap core CSS -->
    <link href="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/ui/css/seccoll.css?v=${applicationScope['version']}" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}" rel="stylesheet" />
    <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script> 
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <jsp:include page = 'TenantResolver.jsp' />
    <%
        String publicTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, Constants.DEFAULT_TENANT);
        String tenant = (String) request.getAttribute("resolvedTenant");
        String adminAppVal = "false";
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                if("adminApp".equals(cookie.getName())) {
                    adminAppVal = cookie.getValue();
                    break;
                }
            }
        }
        String redirectLoginPage = adminAppVal.equals("true") ? "/loginAdmin" : "/login";
        String url = request.getContextPath() + redirectLoginPage  + (tenant == null || publicTenantName.equals(tenant) ? "" : "?tenant=" + tenant);
        pageContext.setAttribute("redirectUrl", url);
        
        CookieUtil.clearCookies(request, response);        
    %>
    
    <script type="text/javascript">
        function checkErrorMsg(){
            var errMsg = "Your session has expired. You will be redirected to the Login page. Click <a href='${redirectUrl}'>here</a> if you are not redirected in 5 seconds.";
            setTimeout(function(){
                window.location = '${redirectUrl}';
            }, 5000);
            $('div.container-error').block({message: '',overlayCSS: { backgroundColor: '#fff' }});
            var error=document.getElementById("display-error");
            error.innerHTML=errMsg;
            error.style.visibility='visible';
            $('div.container-error').unblock();
        }
        window.onload = checkErrorMsg;
    </script>
  </head>
  <body>
    <br>
    <div class="container-error">
        <div id="display-error" style="text-align: center; vertical-align: middle;margin-bottom:1px;margin-left:auto;margin-right:auto; visibility:hidden;" >
            <img src="${pageContext.request.contextPath}/ui/img/Alert_P.svg" alt="Error" /> 
        </div>
    </div>
    <div id="footer">
        <div class="container">
            <p class="text-muted credit text-center">Copyright © 2021 <a href="http://www.nextlabs.com">NextLabs, Inc.</a> All rights reserved.</p>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/jquery.blockUI.js"></script>
  </body>
</html>

<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isELIgnored="false" session="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<script src="${pageContext.request.contextPath }/ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
<script src="${pageContext.request.contextPath }/ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
<script src="${pageContext.request.contextPath }/ui/lib/3rdParty/jquery.blockUI.js"></script>
<head>
<link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
<link href="${pageContext.request.contextPath }/ui/css/style.css" rel="stylesheet">
<link href="${pageContext.request.contextPath }/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">
<link href="${pageContext.request.contextPath }/ui/css/seccoll.css" rel="stylesheet">
<link href="${pageContext.request.contextPath }/ui/css/font/fira.css" rel="stylesheet" />
<meta charset="UTF-8">
<title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
</head>
<body>
    <div class="container-error">
        <br/>
        <div id="display-error" style="text-align: center; vertical-align: middle;margin-bottom:1px;margin-left:auto;margin-right:auto; visibility:hidden;" >   
            <img src="${pageContext.request.contextPath }/ui/img/Alert_P.svg" alt="Error" />    
        </div>
    </div>
    <div id="footer">
        <div class="container">
            <br/>
            <p class="text-muted credit text-center">Copyright © 2021 <a style="color:#337ab7" href="http://www.nextlabs.com">NextLabs, Inc.</a> All rights reserved.</p>
        </div>
    </div>
    <script type="text/javascript">
        <%  String requestURL = (String) request.getAttribute("javax.servlet.error.request_uri");
            String pattern = "^/viewer/temp/.*/.*\\.xlsx?(\\.nxl)?.html(\\?.*)?$";
            if (java.util.regex.Pattern.matches(pattern, requestURL)==true ) { %> 
            var errorMsg = "<%= LocalizationUtil.getMessage(request, "err.cache.not.found", null, null) %>";
            $("#display-error").text(errorMsg).css("visibility", "visible");
        <% } else { %>
            var errorMsg = "<%= LocalizationUtil.getMessage(request, "err.http.404", null, null) %>";
            $("#display-error").text(errorMsg).css("visibility", "visible");
        <% } %>
    </script>
</body>
</html>
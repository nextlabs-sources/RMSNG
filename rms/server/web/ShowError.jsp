<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" isELIgnored="false"%>
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

    <%-- Bootstrap core CSS --%>
    <link href="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css?v=${applicationScope['version']}" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/ui/css/seccoll.css?v=${applicationScope['version']}" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}" rel="stylesheet" />

    <script type="text/javascript">
    
        function checkErrorMsg(){
            $('div.container-error').block({message: '',overlayCSS: { backgroundColor: '#fff' }});
            <%
            String error = request.getParameter("errMsg");
            String code = request.getParameter("code");
            String msg = code != null ? LocalizationUtil.getMessage(getServletContext(), code, null, null) : error;
            pageContext.setAttribute("errorMsg", StringEscapeUtils.ESCAPE_ECMASCRIPT.translate(msg));
            if (msg != null) {
            %>
                var errMsg=decodeURIComponent('${errorMsg}');
                var error=document.getElementById("display-error");
                error.innerHTML=errMsg;
                error.style.visibility='visible';
            <%
            }
            %>
            $('div.container-error').unblock();
         }
         window.onload = checkErrorMsg;
    </script>
  </head>
  <body>
    <br>
    <div class="container-error">
        <div id="display-error" style="text-align: center; vertical-align: middle;margin-bottom:1px;margin-left:auto;margin-right:auto; visibility:hidden;" >
            <img src="ui/img/Alert_P.svg" alt="Error" /> 
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

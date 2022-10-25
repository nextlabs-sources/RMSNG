 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Unregister</title>
    <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">

    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/jquery-1.10.2.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/bootstrap.min.js?v=${applicationScope['version']}"></script>

    <jsp:include page = 'TenantResolver.jsp' />
    
    <script>
    $(document).ready(function() {
        $("#footer").show();
    });

    function post() {
        var form = $('<form></form>');
        form.attr("method", "post");
        form.attr("action", 'rs/usr/unregister');
        var parameters = {};
        parameters["account_id"] = '<%=request.getParameter("account_id")%>';
        parameters["otp"] = '<%=request.getParameter("otp")%>';
        parameters["suc_url"] = '<%=request.getParameter("suc_url")%>';

        $.each(parameters, function(key, value) {
            var field = $('<input></input>');
            field.attr("type", "hidden");
            field.attr("name", key);
            field.attr("value", value);
            form.append(field);
        });
        $(document.body).append(form);
        form.submit();
    }
    </script>

 </head>
 <body>
 <div id="cont">
    <jsp:include page ="tenants/${requestScope.resolvedTenantUI}/index.jsp"/>
    <div class="right-column">
        <div class="login-right-column register">
            <jsp:include page = 'LogoWrapper.jsp' />
            <div class="wrapper text-align-center">
                <img class="rms-logo pointer-click" src="${pageContext.request.contextPath}/ui/img/rms-logo-with-text.svg" onclick="goToIntro()"/>
                <div id="unregister_text" class="rms-activate-unregister-text subtitle">Please click the button below to unregister your account.</div>
                <div>
                    <button id="unregister_button" type="button" class="btn btn-default rms-unregister-button btn-block" 
                    onclick="post()">Unregister</button>
                </div>
            </div>
        </div>
        <jsp:include page = 'Footer.jsp' />
    </div>
</div>
</body>
</html>
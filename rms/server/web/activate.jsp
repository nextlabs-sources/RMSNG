 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
 <%@page import="com.nextlabs.rms.rs.UserMgmt" %>
 <%@page import="com.nextlabs.common.shared.JsonResponse" %>
 <%@page import="com.nextlabs.rms.util.CookieUtil" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <% 
        int accountId = Integer.parseInt(request.getParameter("account_id"));
        JsonResponse validationResp = UserMgmt.validateLoggedInUserForActivation(request, accountId);
    %>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Activate Account</title>
    <link rel="icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath }/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath }/ui/css/font/fira.css">

    <script src="${pageContext.request.contextPath }/ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath }/ui/lib/3rdParty/bootstrap.min.js"></script>

    <jsp:include page = 'TenantResolver.jsp' />
    
    <script>
    $(document).ready(function() {
        $("#footer").show();
        checkAccountLoggedIn();
    });

    function checkAccountLoggedIn() {
        <% if(validationResp.getStatusCode() == 4001) { %>
            $('#activate_text').text("<%= validationResp.getMessage()%>");
            $('#activate_button').text('Log out');
            $('#activate_button').attr('onclick', 'logout()');
        <% } %>
        displayMessage();
    }

    function displayMessage() {
        $('#message-pane').css("display", "block");
        $('#loading-gif').css("display", "none");
    }

    function logout() {
        $.ajax({
            url: '${pageContext.request.contextPath}/rs/usr/logout/',
            async: true,
            cache: false,
            headers: {
                'Content-Type': 'application/json',
                'userId': readCookie('userId'),
                'ticket': readCookie('ticket'),
                'clientId': readCookie('clientId'),
                'platformId': readCookie('platformId')
            },
            type: 'GET'
        }).done(function (data) {
            if (data && data.statusCode === 200) {
                location.reload();
            }
        });
    }
    
    function post() {
        var form = $('<form></form>');
        form.attr("method", "post");
        form.attr("action", 'rs/usr/activate');
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


        <%
            String id = request.getParameter("invitationId");
            String code = request.getParameter("invitationCode");
            if (id != null && !id.isEmpty() && code != null && !code.isEmpty())
            {
        %>
                storeInvitationParams(id, code);
        <%
            }
        %>
    }
    function storeInvitationParams(id, code){
        var date = new Date();
        date.setTime(date.getTime() + (30 * 60 * 1000)); // 30 minutes
        setCookie("id", id, date);
        setCookie("code", code, date);
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
                <img class="rms-logo pointer-click" src="${pageContext.request.contextPath }/ui/img/rms-logo-with-text.svg" onclick="goToIntro()"/>
                <img id="loading-gif" src="${pageContext.request.contextPath }/ui/img/loading-icon.gif"/>
                <div id="message-pane" style="display: none">
                    <div id="activate_text" class="rms-activate-unregister-text subtitle text-align-left" style="font-weight: normal">You are just one step away from creating your account. Please click the button below to activate your account.</div>
                    <div>
                        <button id="activate_button" type="button" class="btn btn-default rms-activate-button btn-block" onclick="post()">Activate</button>
                    </div>
                </div>
            </div>
        </div>
        <jsp:include page = 'Footer.jsp' />
    </div>
</div>
</body>
</html>

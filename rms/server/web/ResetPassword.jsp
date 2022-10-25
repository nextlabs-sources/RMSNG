 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@page import="com.nextlabs.rms.util.CookieUtil" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <% 
        final String cookieDomain = CookieUtil.getCookieDomainName(request);
    %>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Reset Password</title>
    <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">
    <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/core-min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/sha256.js"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <jsp:include page = 'TenantResolver.jsp' />
    
    <script type="text/javascript">
    
    var userId;
    var otp;

    $(document).ready(function() {
        $("#footer").show();
        $("#password").focus();
        checkState();
    });
     
    function checkState() {
        var code = getParameterByName("code");
        if(code && code != "200"){
            window.location.href = "${pageContext.request.contextPath}/forgotPassword?code="+code ;
            return;
        }
        var domainName = "<%=cookieDomain%>";
        userId = readCookie('userId');
        otp = readCookie('otp');
        deleteCookie('userId', domainName);
        deleteCookie('ticket', domainName);
        deleteCookie('tenantId', domainName);
        deleteCookie('idp', domainName);
        deleteCookie('otp');
    }

     function validateInputs(){
        var password=document.getElementById('password').value;
        if(password==""){
            displayError("Please enter the password.");
            $("#password").focus();
            return false;
        }else if(!validPassword()){
            return false;
        }
        var passwordConfirm=document.getElementById('password-confirm').value;
        if(passwordConfirm==""){
            displayError("Please confirm the password.");
            $("#password-confirm").focus();
            return false;
        }else if(password != passwordConfirm){
            displayError("Passwords do not match.");
            $("#password").focus();
            return false;
        }
        return true;
     }
     
     function resetPassword(){
         var element;
         element=document.getElementById("display-error");
         element.style.display='none';
         if(validateInputs()){
            $("#submit-btn").attr("disabled", true);
            document.getElementById("loading").style.display = "block";
            
            $.post('${pageContext.request.contextPath}/rs/usr/resetPassword', {
                "userId" :  userId,
                "otp" : otp,
                "newPassword" : CryptoJS.SHA256(($("#password").val())).toString()
            }).done(function(data){
                if(data.statusCode != 200){
                    displayError(data.message);
                }else {
                    document.getElementById("display-error").style.display='none';
                    document.getElementById("display-success").style.display='block';
                    element=document.getElementById("display-success-msg");
                    element.innerHTML="Your password has been successfully updated. Please click <a class='underline' href='${pageContext.request.contextPath}/" +  (readCookie("adminApp") == "true" ? "loginAdmin" : "login" ) +"'>here</a> to log in.";
                    element.style.display='block';
                    document.getElementById("register-form").style.display = 'none';
                }
            }).fail(function() {
                displayError("Reset password failed. Please register again or contact support.");
            }).always(function() {
                $("#submit-btn").attr("disabled", false);
                document.getElementById("loading").style.display = "none";
            });
        }
    }

    $(document).keydown(function(e) {
        if(e.which == 13) {
            resetPassword();
        }
    });
    </script>
 </head>
 <body>
 <div id="cont">
    <jsp:include page ="tenants/${requestScope.resolvedTenantUI}/index.jsp"/>
    <div class="right-column">
        <div class="login-right-column register">
            <jsp:include page = 'LogoWrapper.jsp' />
            <div class="wrapper">
                <img class="rms-logo pointer-click" src="ui/img/rms-logo-with-text.svg" onclick="goToIntro()"/>
                <div class="subtitle"><center>Reset Password</center></div>
                <div id="display-error" class="alert alert-danger alert-dismissable message">
                    <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
                </div>
                <div id="display-success" class="message reg-success"> 
                    <p id="display-success-msg"></p>
                    <br>
                </div>          
                <div class="login-box">
					<div id="register-form">
						<div class="form-group">
							<div class="input-layout">
								<input id="password" name="password" type="password" tabindex="1" autocomplete="off" required/>
								<label for="password">Password</label>
							</div>    
						</div>
						<div class="form-group">
							<div class="input-layout">
								<input id="password-confirm" name="password-confirm" type="password" tabindex="2" autocomplete="off" required/>
								<label for="password-confirm">Retype Password</label>
							</div>
						</div>
						<div class="form-group" style="margin-top: 20px;">
							<button id="submit-btn" class="btn btn-default rms-login-button " onclick="resetPassword()">Reset</button>
							<img class="loading" id="loading" src="ui/img/loading-icon.gif"/>
						</div>
					</div>
                </div>
            </div>
        </div>
        <jsp:include page = 'Footer.jsp' />
    </div>
</div>
</body>
</html>
 <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@page import="com.nextlabs.rms.util.CookieUtil" %>

<!DOCTYPE html>
<html lang="en">
  <head>
    <%
        String userEmail = request.getParameter("userEmail");
        if(!com.nextlabs.common.util.StringUtils.hasText(userEmail)){
            Cookie[] cookies = request.getCookies();
            if( cookies != null ){
                for (int i = 0; i < cookies.length; i++){
                    Cookie cookie = cookies[i];
                    if (cookie.getName().equals("userEmail")) {
                        userEmail = cookie.getValue();
                    }
                }
            }
        }
        if(!com.nextlabs.common.util.StringUtils.hasText(userEmail)){
            userEmail = "";
        } else {
            userEmail = java.net.URLDecoder.decode(userEmail, "UTF-8");
        }
    %>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Forgot Password</title>
    <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">
    <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <jsp:include page = 'TenantResolver.jsp' />
    
    <script>
    
    
    $(document).ready(function() {
        $("#footer").show();
        refreshCaptcha();
        var code = getParameterByName("code");
        translateCode(code);
        var email = "<%=userEmail%>";
        if(email && email != ""){
            $("#username").val(email);
            $("#captcha").focus();
        } else {
            $("#username").focus();
        }
    });

     var email;
     var emailSent = false;
     var accLocked = false;
     
     function validateInputs(){
      
        var username=document.getElementById('username').value;
        if(username==""){
            displayError("Please enter the Email Address.");
            return false;
        }
        var captcha=document.getElementById('captcha').value;
        if(captcha==""){
            displayError("Please enter the Captcha text.");
            return false;
        }
        return validEmail();
     }

    function refreshCaptcha(){
         $.get('${pageContext.request.contextPath}/rs/usr/captcha?t=' + new Date().getTime()).done(function(data) {
            if(data.statusCode!= 200){
                displayError(data.message);
            }else{
                nonce = data.results.nonce;
                $("#captcha_image").attr("src",'data:image/png;base64,'+data.results.captcha);
                $("#captcha_image").removeClass("captcha_loading");
                var email = readCookie("userEmail");
                var userName = $("#username").val();
                if(userName && userName != ""){
                	$("#captcha").focus();
                } else if(email && email != ""){
                	$("#username").val(email);
                	$("#captcha").focus();
                } else {
                	$("#username").focus();
                }
            }
        }).fail(function() {
            displayError("Unable to generate captcha");
            return false;
        })
     }
     
     function resetPassword(){
         var element;
         element=document.getElementById("display-error");
         element.style.display='none';
         if(validateInputs()){
            closeDialog();
            $("#submit-btn").attr("disabled", true);
            document.getElementById("loading").style.display = "block";
            $.post('${pageContext.request.contextPath}/rs/usr/forgotPassword', {    
                "email" :  $("#username").val(),
                "nonce" : nonce,
                "captcha" : $("#captcha").val()
            }).done(function(data){
                if(data.statusCode == 406){
                    displayError(data.message);
                    refreshCaptcha();
                    $('#captcha').val("");
                }else if(data.statusCode == 303 || data.statusCode == 200){
                    email = $("#username").val();
                    if(data.statusCode == 303){
                        displayMsg = "An account has been registered with this email but it has not been activated yet.";
                        document.getElementById("check-span-msg").style.display='none';
                        accLocked = true;
                    }else{
                        displayMsg = "We have sent an email to <a>"+email+"</a>. Please follow the instructions in the email to reset your password.";
                    }
                    document.getElementById("display-error").style.display='none';
                    document.getElementById("display-success").style.display='block';
                    element=document.getElementById("display-success-msg");
                    element.innerHTML=displayMsg;
                    element.style.display='block';
                    document.getElementById("register-form").style.display = 'none';
                    document.getElementById("mail-btns").style.display='block';
                    emailSent = true;
                    document.getElementById('mail-btn').disabled = true;
                    setTimeout(function(){
                        document.getElementById('mail-btn').disabled = false;
                    }, 5000);
                }else{
                    displayError(data.message);
                }
            }).fail(function() {
                displayError("Reset password failed. Please register again or contact support.");
            }).always(function() {
                $("#submit-btn").attr("disabled", false);
                document.getElementById("loading").style.display = "none";
            });
        }
    }
    
    function resendNotification(){
        document.getElementById("loading-mail").style.display = "block";
        document.getElementById("mail-btn").disabled = true;
        document.getElementById("mail-btn").style.backgroundColor = "#399649";
        
        $.post('${pageContext.request.contextPath}/rs/usr/resendEmail', {  
            "email" :  email,
            "type" : accLocked ? "register" : "resetPwd"
        }).done(function(data){
            if(data.statusCode!= 200){
                displayError(data.message);
            }else if(accLocked){
                document.getElementById("display-success-msg").innerHTML="We have sent a verification email to <a>"+email+"</a>. Please follow the instructions in the email to activate your account.";
                document.getElementById("check-span-msg").style.display='block';
            }
        }).fail(function() {
            displayError("Error occurred while sending email. Please contact support.");
        }).always(function() {
            document.getElementById('mail-btn').disabled = true;
            setTimeout(function(){
                document.getElementById('mail-btn').disabled = false;
            }, 5000);
            document.getElementById("loading-mail").style.display = "none";
        });
    }
    
    $(document).keydown(function(e) {
        if(e.which == 13) {
            if(emailSent){
                resendNotification();
            }else{
                resetPassword();
            }
        }
    });
    </script>
 </head>
 <body>
 <div id="cont">
    <jsp:include page ="tenants/${requestScope.resolvedTenantUI}/index.jsp"/>
    <div class="right-column">
        <div class="login-right-column register">
            <div class="wrapper">
                <jsp:include page = 'LogoWrapper.jsp' />
                <img class="rms-logo pointer-click" src="ui/img/rms-logo-with-text.svg" onclick="goToIntro()"/>
                <div class="subtitle">Reset Password</div>
                <div id="display-error" class="alert alert-danger alert-dismissable message">
                    <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
                </div>
                <div id="display-success" class="message reg-success"> 
                    <p id="display-success-msg"></p>
                    <p id="check-span-msg">Didn't get the email? Check your spam folder.</p>
                    <br>
                </div>  
                <div id="mail-btns" class="form-group" class="message reg-success" style="display:none;">
                    <button id= "mail-btn" class="btn btn-default rms-login-button" onclick="resendNotification()" disabled="true">Resend Email</button>
                    <button id="cancel-btn" class="btn btn-default" onclick="goToLogin()">Cancel</button>
                    <img class="loading" id="loading-mail" src="ui/img/loading-icon.gif"/>
                    <br/>
                </div>          
                <div class="login-box">
                    <div id="register-form">
                        <div class="form-group">
                            <div class="input-layout">
                                <input id="username" name="username" type="text" tabindex="1" autocomplete="off" required/>
                                <label for="username">Email Address</label>
                            </div>    
                        </div>
                        <div class="form-group "></div>
                        <div class="form-group ">
                            <label>Prove you are not a robot</label><br>
                            <img id="captcha_image" class="captcha_loading" src="ui/img/loading-icon.gif"/>
                            <input id="refresh-image" tabindex="-1" type="image" src="ui/img/Refresh_P.svg"  title = "Refresh Captcha" onclick="refreshCaptcha()"></input>
                        </div>
                        <div class="form-group">
                            <div class="input-layout">
                                <input id="captcha" name="captcha" type="text" tabindex="2" autocomplete="off" required/>
                                <label for="captcha">Enter the text seen above</label>
                            </div>    
                        </div>
                        <div class="form-group" style="margin-top: 20px;">
                            <button id="submit-btn" class="btn btn-default rms-login-button " onclick="resetPassword()">Reset</button>
                            <button id="cancel-btn" class="btn btn-default" onclick="goToLogin()">Cancel</button>
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
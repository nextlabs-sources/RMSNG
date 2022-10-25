<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@page import="com.nextlabs.common.shared.WebConfig" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <%  String webContext=request.getContextPath();
        WebConfig webConfig = WebConfig.getInstance();
        String termsUrl = webConfig.getProperty(WebConfig.TERMS_URL, webContext + "/TermsAndConditions.html");
        String loginUrl = webConfig.getProperty(WebConfig.TERMS_URL, webContext + "/login");
        String privacyUrl = webConfig.getProperty(WebConfig.PRIVACY_URL, webContext + "/PrivacyPolicy.html");

        Cookie[] cookies = request.getCookies();
        String id = null;
        String code = null;
        String projectName = null;
        String invitee = null;
        if( cookies != null ){
            for (int i = 0; i < cookies.length; i++){
                Cookie cookie = cookies[i];
                if (cookie.getName().equals("id")) {
                    id = cookie.getValue();
                } else if (cookie.getName().equals("code")) {
                    code = cookie.getValue();
                }
           }
        }
        if (id != null && code != null) {
            java.util.Map<String,String> details = com.nextlabs.rms.service.ProjectService.getProjectInvitationDetails(id, code);
            if (details != null) {
                projectName = details.get(com.nextlabs.rms.service.ProjectService.PROJECT_NAME);
                invitee = details.get(com.nextlabs.rms.service.ProjectService.INVITEE);
            }
        }
    %>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <title>SkyDRM - Request an account</title>
    <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/login.min.css?v=${applicationScope['version']}">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">
    <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/core-min.js"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/sha256.js"></script>
    <script src="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <jsp:include page = 'TenantResolver.jsp' />

    <script>
    var nonce;
    var email;

    var registrationEmailSent = false;
    $( document ).ready(function() {
        $("#footer").show();
        refreshCaptcha();
        var email = readCookie("userEmail");
        if(email && email != ""){
            $("#username").val(email);
            $("#password").focus();
        } else {
            $("#username").focus();
        }
    });

    window.onload = checkMsg;
    var popupWindow = null;

    function openCenteredPopup(url,winName,w,h,scroll){
        LeftPosition = (screen.width) ? (screen.width-w)/2 : 0;
        TopPosition = (screen.height) ? (screen.height-h)/2 : 0;
        settings = 'height='+h+',width='+w+',top='+TopPosition+',left='+LeftPosition+',scrollbars='+scroll+',resizable';
        popupWindow = window.open(url,winName,settings);
        popupWindow.focus();
    }

     function checkMsg()
     {
        var element;
        var err="<%=request.getParameter("error")%>";
        var suc="<%=request.getParameter("success")%>";
        if(suc!=null && suc!="null" || err!=null && err!="null"){
            element=document.getElementById("register-form");
            element.style.visibility = 'hidden';
        }

        if(err!=null && err!="null"){
            displayError(err);
        }
        else if(suc!=null && suc!="null"){
            element=document.getElementById("display-success");
            element.innerHTML=suc;
        }
        if(element!=null){
            element.style.visibility='visible';
            var content = document.getElementById("contentDiv");
            if(content!=null){
                content.style.visibility = 'hidden';
                content.parentNode.removeChild(content);
            }
        }
     }

     function validateInputs(){
        var emailId=document.getElementById('username').value;
        if(emailId==""){
            displayError("Please enter the Email Address.");
            $("#username").focus();
            return false;
        }
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
        var captcha=document.getElementById('captcha').value;
        if(captcha==""){
            displayError("Please enter the Captcha text.");
            $("#captcha").focus();
            return false;
        }
        return validEmail() && validName();
     }


     function refreshCaptcha(){
         $.get('${pageContext.request.contextPath}/rs/usr/captcha?t=' + new Date().getTime()).done(function(data){
            if(data.statusCode!= 200){
                displayError(data.message);
            }else{
                nonce = data.results.nonce;
                $("#captcha_image").attr("src",'data:image/png;base64,'+data.results.captcha);
                $("#captcha_image").removeClass("captcha_loading");
                var email = readCookie("userEmail");
                var userName = $("#username").val();
                if(userName && userName != ""){
                    var password = $("#password").val();
                    if(password && password != ""){
                        $("#captcha").focus();	
                    } else {
                        $("#password").focus();
                    }
                } else if(email && email != ""){
		            $("#username").val(email);
		            $("#password").focus();
                } else {
		            $("#username").focus();
                }
            }
        }).fail(function() {
            displayError("Unable to generate captcha");
            return false;
        })
     }

    function resendNotification(){
        document.getElementById("loading-mail").style.display = "block";
        document.getElementById("mail-btn").disabled = true;
        document.getElementById("mail-btn").style.backgroundColor = "#399649";

        var id = readCookie("id");
        var code = readCookie("code");

        $.post('${pageContext.request.contextPath}/rs/usr/resendEmail', {
            "email" :  email,
            "type" : "register",
            "id" : id,
            "code" : code
        }).done(function(data){
            if(data.statusCode!= 200){
                displayError(data.message);
            }else{
                document.getElementById("display-accountLock").style.display='none';
                document.getElementById("display-success").style.display='none';
                document.getElementById("display-error").style.display='none';
                document.getElementById("display-mail-success").style.display='block';
                element=document.getElementById("display-mail-success-msg");
                element.innerHTML="We have sent a verification email to <a>"+email+"</a>. Please follow the instructions in the email to activate your account."
                element.style.display='block';
                document.getElementById("register-form").style.display = 'none';
            }
        }).fail(function() {
            displayError("Error occurred while sending email. Please contact support.");
        }).always(function() {
            document.getElementById("mail-btn").disabled = false;
            document.getElementById("loading-mail").style.display = "none";
        });
    }

     function registerUser(){
         var element;
         element=document.getElementById("display-error");
         element.style.display='none';
         if(validateInputs()){
            closeDialog();
            $("#submit-btn").attr("disabled", true);
            document.getElementById("loading").style.display = "block";

            var id = readCookie("id");
            var code = readCookie("code");

            $.post('${pageContext.request.contextPath}/rs/usr/register', {
                "email" :  $("#username").val(),
                "password" : CryptoJS.SHA256(($("#password").val())).toString(),
                "displayName" : $("#displayName").val(),
                "nonce" : nonce,
                "captcha" : $("#captcha").val(),
                "id" : id,
                "code" : code,
                "tenant":"${requestScope.resolvedTenant}"
            }).done(function(data){
                if(data.statusCode === 303 || data.statusCode === 304){
                    if(data.statusCode === 303){
                        email = $("#username").val();
                        displayMsg = "An account has been registered with this email but it has not been activated yet.<br><br>Please click the 'Resend Email' button below to resend the activation email.";
                        document.getElementById("reset-btn").style.display = "none";
                        document.getElementById("mail-btn").style.display = "block";
                    }else if(data.statusCode === 304) {
                        displayMsg = "An account that is associated with this email already exists.";
                        document.getElementById("reset-btn").style.display = "block";
                        document.getElementById("mail-btn").style.display = "none";
                    }
                    document.getElementById("register-form").style.display = 'none';
                    document.getElementById("mail-btns").style.display='block';
                    document.getElementById("display-accountLock").style.display='block';
                    element=document.getElementById("display-accountLock-msg");
                    element.innerHTML=displayMsg;
                    element.style.display='block';
                }else if(data.statusCode == 406){
                    displayError(data.message);
                    refreshCaptcha();
                    $('#captcha').val("");
                }else if(data.statusCode!= 200){
                    displayError(data.message);
                }else{
                    email = $("#username").val();
                    document.getElementById("display-error").style.display='none';
                    document.getElementById("display-success").style.display='block';
                    element=document.getElementById("display-success-msg");
                    element.innerHTML="We have sent a verification email to <a class='underline'>"+email+"</a>. Please follow the instructions in the email to activate your account."
                    element.style.display='block';
                    document.getElementById("register-form").style.display = 'none';
                    document.getElementById("reset-btn").style.display = "none";
                    document.getElementById("mail-btns").style.display='block';
                    registrationEmailSent = true;
                }
            }).fail(function() {
                displayError("Registration failed. Please register again or contact support.");
            }).always(function() {
                $("#submit-btn").attr("disabled", false);
                document.getElementById("loading").style.display = "none";
            });
        }
    }
    $(document).keydown(function(e) {
        if(e.which == 13) {
            if(!registrationEmailSent)
                registerUser();
            else
                resendNotification();
        }
    });
    function fillInviteeEmail() {
        var id = readCookie("id");
        var code = readCookie("code");
        if (id && code) {
            document.getElementById('username').value = "<%=invitee%>";
        }
    }
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
                    <div class="subtitle">Create an account</div>
                    <div id="display-error" class="alert alert-danger alert-dismissable message">
                        <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
                    </div>
                    <div id="display-success" class="message reg-success">
                        <div class="subtitle" style="font-size: 14px;"><center>Almost there ...</center></div>
                        <p>Thank you for registering with NextLabs.</p>
                        <p id="display-success-msg"></p>
                        <p>Didn't get the email? Check your spam folder.</p>
                        <br>
                    </div>
                    <div id="display-accountLock" class="message reg-success">
                        <p id="display-accountLock-msg"></p>
                        <br>
                   </div>
                   <div id="display-mail-success" class="message reg-success">
                        <p id="display-mail-success-msg"></p>
                        <p>Didn't get the email? Check your spam folder.</p>
                        <br>
                   </div>
                    <div id="mail-btns" class="form-group" class="message reg-success" style="display:none;">
                        <button id= "reset-btn" class="btn btn-default rms-login-button" onclick="goToForgotPassword()">Reset Password</button>
                        <button id= "mail-btn" class="btn btn-default rms-login-button" onclick="resendNotification()">Resend Email</button>
                        <button id="cancel-btn" class="btn btn-default" onclick="goToLogin()">Back to Home</button>
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
                            <div class="form-group">
                                <div class="input-layout">
                                    <input id="password" name="password" type="password" tabindex="2" autocomplete="off" required/>
                                    <label for="password">Password</label>
                                </div>    
                            </div>
                            <div class="form-group">
                                <div class="input-layout">
                                    <input id="password-confirm" name="password-confirm" type="password" tabindex="3" autocomplete="off" required/>
                                    <label for="password-confirm">Retype Password</label>
                                </div>    
                            </div>
                            <div class="form-group">
                                <div class="input-layout">
                                    <input id="displayName" name="displayName" type="text" tabindex="4" autocomplete="off" required/>
                                    <label for="displayName">Full Name (Optional)</label>
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
                                    <input id="captcha" name="captcha" type="text" tabindex="6" autocomplete="off" required/>
                                    <label for="captcha">Enter the text seen above</label>
                                </div>    
                            </div>
                            <div class="form-group" id="agreeTC-div">
                                <label class="noselect" style="font-weight:normal">By signing up, you agree to our <a href="<%=termsUrl%>" style="color:#4990ef !important;" target="_blank">Terms</a> and <a href="<%=privacyUrl%>" style="color:#4990ef !important;" target="_blank">privacy policy.</a></input></label><br>
                            </div>
                            <div class="form-group">
                                <button id="submit-btn" class="btn btn-default rms-login-button" onclick="registerUser()" tabindex="8">Create</button>
                                <button id="cancel-btn" class="btn btn-default" onclick="goToIntro()" tabindex="9">Cancel</button>
                                <img class="loading" id="loading" src="ui/img/loading-icon.gif"/>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <jsp:include page = 'Footer.jsp' />
        </div>
    </div>
    <script type="text/javascript">
        fillInviteeEmail();
    </script>
</body>
</html>

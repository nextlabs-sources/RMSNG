<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false" %>
<%@page import="com.nextlabs.common.shared.WebConfig" %>
<%@page import="com.nextlabs.rms.util.CookieUtil" %>
<%@page import="java.net.URLDecoder" %>
<!DOCTYPE html>
    <html>
    <head>
        <%
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
                    pageContext.setAttribute("invitee", invitee);
                }
            }

            String t = request.getParameter("t");
            int idp = com.nextlabs.common.shared.Constants.LoginType.DB.ordinal();
            try {
                idp = Integer.parseInt(request.getParameter("i"));
            } catch (NumberFormatException e) {
                idp = com.nextlabs.common.shared.Constants.LoginType.DB.ordinal();
            }
        %>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <title>SkyDRM</title>
        <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/login.min.css?v=${applicationScope['version']}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">
        <link rel="preload" href="${pageContext.request.contextPath}/ui/css/font/woff2/FiraSans-Regular.woff2" as="font" type="font/woff2" crossorigin >
        <link rel="preload" href="${pageContext.request.contextPath}/ui/css/font/woff2/FiraSans-Medium.woff2" as="font" type="font/woff2" crossorigin >
        <script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
        <script src="${pageContext.request.contextPath}/ui/app/login.min.js?v=${applicationScope['version']}"></script>
        <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/core-min.js"></script>
        <script src="${pageContext.request.contextPath}/ui/lib/3rdParty/sha256.js"></script>
        <script src="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/js/bootstrap.min.js"></script>
        <jsp:include page = 'TenantResolver.jsp' />
    </head>
    <body>
        <div id="loading" style="top: 50%; left:50%; position: absolute;">
            <img src="ui/img/loading-icon.gif" />
        </div>
        <div id="cont" style="display:none;">
            <jsp:include page ="tenants/${requestScope.resolvedTenantUI}/index.jsp"/>
            <!-- page content -->
            <div class="right-column">
                <div class="login-right-column">
                    <div class="wrapper">
                        <jsp:include page = 'LogoWrapper.jsp' />
                        <img class="rms-logo pointer-click" src="${pageContext.request.contextPath}/ui/img/rms-logo-with-text.svg" onclick="goToIntro()"/>
                        <div class="subtitle">Log in to your account</div>
                            <!-- content -->
                        <div id="display-error" class="alert alert-danger alert-dismissable message">
                            <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
                        </div>
                        <div class="login-box">
                            <% 
                                boolean idpRMSEnabled = false;
                                boolean idpFacebookEnabled = false;
                                boolean idpGoogleEnabled = false;
                                boolean socialLoginEnabled = false;
                                boolean idpAzureEnabled = false;
                                java.util.Map<Integer, String> ldapIdps = new java.util.HashMap<Integer, String>();
                                java.util.Map<Integer, String> samlIdps = new java.util.HashMap<Integer, String>();
                                java.util.List<com.nextlabs.common.shared.JsonIdentityProvider> idps = com.nextlabs.rms.rs.IdpMgmt.getConfiguredIDPs((String)request.getAttribute("resolvedTenant"), null);
                                com.google.gson.Gson gson = new com.google.gson.Gson(); 
                                for(com.nextlabs.common.shared.JsonIdentityProvider jsonIdp: idps) {
                                    if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.FACEBOOK.ordinal()) {
                                        idpFacebookEnabled = true;
                                    } else if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.GOOGLE.ordinal()) {
                                        idpGoogleEnabled = true;
                                    } else if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.DB.ordinal()) {
                                        idpRMSEnabled = true;
                                    } else if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.AZUREAD.ordinal()) {
                                        idpAzureEnabled = true;
                                    } else if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.LDAP.ordinal()) {
                                        java.util.Map<String, String> map = gson.fromJson(jsonIdp.getAttributes(), java.util.Map.class);
                                        ldapIdps.put(jsonIdp.getId(), map.get("domain"));
                                    } else if(jsonIdp.getType() == com.nextlabs.common.shared.Constants.LoginType.SAML.ordinal()) {
                                        java.util.Map<String, String> map = gson.fromJson(jsonIdp.getAttributes(), java.util.Map.class);
                                        samlIdps.put(jsonIdp.getId(), map.get("buttonText"));
                                    }
                                }
                                socialLoginEnabled = idpGoogleEnabled || idpFacebookEnabled || idpAzureEnabled;
                                if(idpRMSEnabled) { 
                            %>
                        
                            <div class="input-layout">
                                <input id="username" name="username" type="text" tabindex="1" autocomplete="off" required/>
                                <label for="username">Email Address</label>
                            </div>
                            <div class="input-layout">
                                <input id="password" name="password" type="password" tabindex="2" autocomplete="off" required/>
                                <label for="password">Password</label>
                            </div>
                            <span id="rememberMeSpan" class="hide-control"><label class="noselect" style="font-weight:normal;margin-top:10px">
                            	<input id="rememberMe" type="checkbox" tabindex="3"> Remember me</label><br>
                            </span>
                            <div style="margin-top: 20px;">
                            	<button class="btn btn-default rms-login-button" id="submit-btn" disabled="disabled" tabindex="4">Log In</button>
                            	<img class="loading" id="loading-login" src="ui/img/loading-icon.gif"/>
                            </div>
                            <div class="clearfix"></div>

                            <div class="login-user-actions">
                                <div id="forgot-password-div">
                                    <a class="pointer-click" onclick="forgotPassword()" tabindex="6">Forgot password?</a>
                                </div>
                                <div class="clearfix"></div>
                                <div id="create-account-button-div">
                                    <a class="pointer-click" onclick="register()" tabindex="5">Create a new account</a>
                                </div>
                            </div>
                            <div class="or-divider-div">
                                <hr class="left-divider">
                                <% if(socialLoginEnabled) { %>
                                <span>Or</span>
                                <% } %>
                            </div>
                            
                            <% } if(!ldapIdps.isEmpty()) { %>
                            
                            <div class="input-layout">
                                <input id="ldap-username" name="ldap-username" type="text" tabindex="7" autocomplete="off" required/>
                                <label for="ldap-username">Username</label>
                            </div>
                            <div class="input-layout">
                                <input id="ldap-password" name="ldap-password" type="password" tabindex="8" autocomplete="off" required/>
                                <label for="ldap-password">Password</label>
                            </div>
                            <div class="input-layout">
                                <select id="ldap-domain">
                                    <% for(java.util.Map.Entry<Integer, String> ldapIdp : ldapIdps.entrySet()) { %>
                                        <option value="<%=ldapIdp.getKey()%>"><%=ldapIdp.getValue()%></option>
                                    <% } %>
                                </select>
                            </div>
                            <div style="margin-top: 20px;">
                                <button class="btn btn-default rms-login-button" id="ldap-login-btn" tabindex="10">Log In</button>
                                <img class="loading" id="loading-login" src="ui/img/loading-icon.gif"/>
                            </div>
                            
                            <div class="or-divider-div">
                                <hr class="left-divider">
                                <% if(socialLoginEnabled) { %>
                                <span>Or</span>
                                <% } %>
                            </div>
                            
                            <% } if(!samlIdps.isEmpty()) { %>
                            
                            <div style="font-size: 15px; font-weight: 500; margin-bottom: 20px;">Log in using an identity provider</div>
                            
                                <% for(java.util.Map.Entry<Integer, String> samlIdp : samlIdps.entrySet()) { %>
                            
                            <div style="margin-top: 20px;">
                                <button style="width:auto; min-width: 140px; max-width: 275px;" class="btn btn-default rms-login-button" onclick='samlLogin("<%=samlIdp.getKey()%>")' tabindex="11">
                                    <span style="overflow: hidden; white-space: nowrap; display: block; text-overflow: ellipsis;"><%=samlIdp.getValue()%></span>
                                </button>
                                <img class="loading" id="loading-login" src="ui/img/loading-icon.gif"/>
                            </div>
                            
                                <% } %>
                            
                            <div class="or-divider-div">
                                <hr class="left-divider">
                                <% if(socialLoginEnabled) { %>
                                <span>Or</span>
                                <% } %>
                            </div>
                            
                            <% } if(socialLoginEnabled) { %>

                            <div class="form-group social-login">
                                <div class="subheader">Sign up / log in using your social account</div>
                                <% if(idpGoogleEnabled) { %>
                                <button class="google" id="googleLogin" tabindex="12">
                                    <img src="${pageContext.request.contextPath}/ui/img/Google_logo.svg" alt="Google"/> <span>Continue with Google</span>
                                </button>
                                <% } %>
                                <% if(idpFacebookEnabled) { %>
                                <button class="facebook" id="facebookLogin" tabindex="13">
                                    <img src="${pageContext.request.contextPath}/ui/img/Facebook_logo.svg" alt="Facebook"/> <span>Continue with Facebook</span>
                                </button>
                                <% } %>
                                <% if(idpAzureEnabled) { %>
                                <button class="azure" id="azureLogin" tabindex="13">
                                    <img src="${pageContext.request.contextPath}/ui/img/Azuread_dark_logo.svg" alt="Azure AD"/> 
                                </button>
                                <% } %>
                            </div>
                            
                            <% } %>
                            
                            <div class="clearfix"></div>
                        </div>
                    </div>
                    <jsp:include page = 'Footer.jsp' />
                </div>
            </div>

        <script type="text/javascript">
            setCookie("adminApp", "false");

            function register() {
                if($("#username").val() != "") {
                    var date = new Date();
                    date.setTime(date.getTime() + (5 * 60 * 1000)); // 5 minutes
                    setCookie("userEmail", $("#username").val(), date);
                }
                goToRegister();
            }

            function forgotPassword() {
                if($("#username").val() != "") {
                    var date = new Date();
                    date.setTime(date.getTime() + (5 * 60 * 1000)); // 5 minutes
                    setCookie("userEmail", $("#username").val(), date);
                }
                goToForgotPassword();
            }

            function loginRMS(data){
                if (isProjectInvitation(data) === false) {
                    goToMain(data.defaultTenantUrl);
                }
            }

            function isProjectInvitation(data) {
                var id = readCookie("id");
                var code = readCookie("code");

                var statusCode = 200;
                var jsonObj = {};
                if (id && code) {
                    /*
                    make call to accept API. if success, direct to project home
                    else take to the screen that show email address mismatch!

                    Delete cookies after accepting invite

                    */
                    $.ajax({
                        url: '${pageContext.request.contextPath}/rs/project/accept',
                        cache: false,
                        type: 'GET',
                        headers: {
                            'userId': data.userId,
                            'ticket': data.ticket,
                            'clientId': readCookie('clientId'),
                            'platformId':  readCookie('platformId')
                        },
                        data: {id: id, code: code},
                        async : false,
                        success: function (result) {
                           statusCode = result['statusCode'];
                           jsonObj = result['results'];
                        },
                        error: function (error) {
                            statusCode = 500;
                        }
                    });

                    deleteCookie("id");
                    deleteCookie("code");
                    var date = new Date();
                    date.setTime(date.getTime() + (5 * 60 * 1000)); // 5 minutes
                   if (statusCode == 200) {
                        setCookie("projectName", "<%=org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(projectName)%>", date);
                        window.location.href = "main#/app/projects/" + jsonObj.projectId;
                        return true;
                   } else if (statusCode == 4003) {
                        setCookie("projectName", "<%=org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript(projectName)%>", date);
                        window.location.href = "main#/public/emailMismatch";
                        return true;
                   }
                   // for other cases, and goto main page, set status code and show error message.
                   setCookie("statusCode", statusCode, date);
                }
                return false;
            }

            function goToMain(redirectUrl){
                deleteCookie("userEmail");
                <%  String r = request.getParameter("r");
                    if (r != null) {
                        r = URLDecoder.decode(r, "UTF-8");
                %>
                    window.location.href = "<%=r%>";
                    return;
                <% } else { %>
                	var url = redirectUrl ? redirectUrl + "/" : "";
                    window.location.href = url + "main";
                <% } %>
            }

            function rmsLogin(){
                if(validateInputs()){
                    closeDialog();
                    setCookie("adminApp", "false");
                    var tenant = decodeURIComponent("${resolvedTenant}");
                    $("#loading-login").show();
                    $("#submit-btn").attr("disabled", true);
                    $.post('${pageContext.request.contextPath}/rs/usr',
                        {
                            "email" :  $("#username").val(),
                            "password" : CryptoJS.SHA256(($("#password").val())).toString(),
                            "rememberMe": $("#rememberMe").is(":checked"),
                            "tenant": tenant
                        }
                    ).done(function(data){
                        if(data.statusCode!= 200 || data.extra == null){
                        	if(data.statusCode == 307) {
                            	let redirectUrl = data.message;
                            	window.location = redirectUrl;
								                        		
                        	} else {
	                        	var date = new Date();
	                        	date.setTime(date.getTime() + (5 * 60 * 1000)); // 5 minutes
	                        	setCookie("userEmail", $("#username").val(), date);
	                        	
	                            displayError(data.message);
	                            $("#loading-login").hide();
	                        	$("#submit-btn").attr("disabled", false);
                        	}
                        }else{
                            loginRMS(data.extra);
                        }
                    }).fail(function() {
                        displayError("Could not connect to server.");
                        $("#loading-login").hide();
                        $("#submit-btn").attr("disabled", false);
                        return false;
                    });
                }
            }
            
            function ldapLogin(){
                var username = document.getElementById('ldap-username').value.trim();
                if(username == "") {
                    displayError("Please enter the username.");
                    $("#ldap-username").focus();
                    return false;
                }
                var password = document.getElementById('ldap-password').value.trim();
                if(password==""){
                    displayError("Please enter the password.");
                    $("#ldap-password").focus();
                    return false;
                }
                var idpId = document.getElementById('ldap-domain').value.trim();
                $.post('${pageContext.request.contextPath}/IdpManager/LdapAuth/LdapAuthFinish',
                        {
                            "userName" :  username,
                            "password" : password,
                            "id": idpId,
                            "tenant": decodeURIComponent("${resolvedTenant}"),
                        }
                ).done(function(data){
                    $("#ldap-login-btn").attr("disabled", false);
                    if(data.code && data.code == '401') {
                        displayError("The username or password you entered is incorrect.");
                    } else if(data.code && data.code == '500l') {
                    	displayError("Error occurred while logging in with LDAP.");
                    } 
                    checkQueryParameters();
                }).fail(function(data) {
                    displayError("Error occurred while logging in with LDAP.");
                    $("#ldap-login-btn").attr("disabled", false);
                    return false;
                });
            }

            function validateInputs(){
                document.getElementById('username').value = document.getElementById('username').value.trim();
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
                }
                return validEmail();
            }

            var googleUser = {};

            function googleLogin() {
                showLoading();
                var search = window.location.search + ( window.location.search.match( /[\?]/g ) ? '&' : '?' ) + "tenant="+decodeURIComponent("${resolvedTenant}");
                window.location = '${pageContext.request.contextPath}/IdpManager/GoogleAuth/GoogleAuthStart' + search;
            }

            function facebookLogin() {
                showLoading();
                var search = window.location.search + ( window.location.search.match( /[\?]/g ) ? '&' : '?' ) + "tenant="+decodeURIComponent("${resolvedTenant}");
                window.location = '${pageContext.request.contextPath}/IdpManager/FacebookAuth/FacebookAuthStart' + search;
            }
            
            function azureLogin() {
                showLoading();
                var search = window.location.search + ( window.location.search.match( /[\?]/g ) ? '&' : '?' ) + "tenant="+decodeURIComponent("${resolvedTenant}");
                window.location = '${pageContext.request.contextPath}/IdpManager/AzureAdAuth/AzureAdAuthStart' + search;
            }
            
            function samlLogin(idpId) {
                showLoading();
                var search = window.location.search + ( window.location.search.match( /[\?]/g ) ? '&' : '?' ) + 'id=' + idpId + "&tenant="+decodeURIComponent("${resolvedTenant}");
                window.location = '${pageContext.request.contextPath}/IdpManager/SamlAuth/SamlAuthStart' + search;
            }

            function onLoginSuccess(path, token){
                $.post(path, {"token" :  token, "tenant": decodeURIComponent("${resolvedTenant}")}
                ).done(function(data){
                	
                    if(data.statusCode == 307){
                    	let redirectUrl = data.message;
                    	window.location = redirectUrl;

                    } else if(data.statusCode!= 200 || data.extra == null){
                        displayError(data.message);
                        hideLoading();
                    }else{
                        loginRMS(data.extra);
                    }
                }).fail(function() {
                    displayError("Could not connect to server.");
                    hideLoading();
                    return false;
                })
            }

            function checkQueryParameters(){
                var userId = readCookie('userId');
                var ticket = readCookie('ticket');
                if(userId && ticket && userId != "" && ticket != ""){
                    if (isProjectInvitation({"userId":userId, "ticket":ticket}) === false) {
                        goToMain();
                    }
                    return;
                }

                var code = getParameterByName('code');
                if(code){
                    hideLoading();
                    /* Disabled auto fill and auto focus
                    $("#username").focus();
                    translateCode(code);
                    if(code == "200"){
                        var email = readCookie("userEmail");
                        if(email && email != ""){
                            $("#username").val(email);
                            $("#password").focus();
                        }
                    }
                    */
                    return;
                }
				
                <%
                  if(t != null && idp == com.nextlabs.common.shared.Constants.LoginType.FACEBOOK.ordinal()) { %>
                    onLoginSuccess('${pageContext.request.contextPath}/rs/login/fb', "<%=t%>");
                <% } else if(t != null && idp == com.nextlabs.common.shared.Constants.LoginType.GOOGLE.ordinal()) { %>
                    onLoginSuccess('${pageContext.request.contextPath}/rs/login/google', "<%=t%>");
                <% } else if(t != null && idp == com.nextlabs.common.shared.Constants.LoginType.AZUREAD.ordinal()) { %>
                    onLoginSuccess('${pageContext.request.contextPath}/rs/login/azuread', "<%=t%>");
                <% } else { %>
                    hideLoading();
                    var redirectUrl = decodeURIComponent(getParameterByName("r"));
                    if(redirectUrl.indexOf("/app/viewSharedFile") !== -1) {
                        displaySuccess("Please log in to SkyDRM to access the shared file.");
                    } else if (redirectUrl.indexOf("/projects/") !== -1) {
                        displaySuccess("Please log in to SkyDRM to access the project space.");
                    }
                    var email = readCookie("userEmail");
                    if(email && email != "") {
                        $("#username").val(email);
                    }
//                        if (!jscd.mobile) {
//                            $("#password").focus();
//                        }
//                    } else {
//                        if (!jscd.mobile) {
//                            $("#username").focus();
//                        }
//                    }
                <% } %>
            }

            function hideLoading(){
                $("#cont").show();
                $("#loading").hide();
                adjustRightColumnHeight();
            }

            function showLoading(){
                $("#cont").hide();
                $("#loading").show();
            }

            $(document).ready(function () {
                if (jscd.mobile) {
                    $("span#rememberMeSpan").remove();
                }
                if (!jscd.cookies) {
                    displayError("Cookies are disabled in your browser. Enable cookies in your browser and try again.");
                    return;
                } else if(isIE9OrBelow()){
                    displayError("We recommend using Internet Explorer version 10 or above.");
                }
                if (jscd.os == "iOS") {
                    $("input").addClass("ios-only");
                }
                $('#facebookLogin').click(facebookLogin);
                $('#googleLogin').click(googleLogin);
                $('#azureLogin').click(azureLogin);
                $('#submit-btn').click(rmsLogin);
                $('#submit-btn').prop("disabled", false);
                $('#ldap-login-btn').click(ldapLogin);
                $('#ldap-login-btn').prop("disabled", false);
	            
	            <% if(idpRMSEnabled) { %> 
	            $('#username, #password').keydown(function(e) {
	                if(e.which == 13) {
	                    rmsLogin();
	                }
	            });
	            <% } %>
                <% if(!ldapIdps.isEmpty()) { %>
	            $('#ldap-username, #ldap-password').keydown(function(e) {
                    if(e.which == 13) {
                        ldapLogin();
                    }
                });
                <% } %> 
                
                <%
                	if (invitee != null) {
                %>
                $('#username').val('${invitee}');
                <%
                	}
                %>
                <%
                    String email = request.getParameter("e");
                    if(email != null) {
                %>
                $('#username').val("<%=email%>");
                <%
                    }
                %>
            })

            $(window).on("load", function() {
                checkQueryParameters();
            });
        <%--
            //Preventing browsers from caching this page in history since jquery is not invoked on clicking back button
            // http://stackoverflow.com/questions/2638292/after-travelling-back-in-firefox-history-javascript-wont-run
            // http://stackoverflow.com/questions/8788802/prevent-safari-loading-from-cache-when-back-button-is-clicked
        --%>
            $(window).bind("pageshow", function(event) {
                if (event.originalEvent.persisted) {
                    window.location.reload()
                }
            });
        </script>
    </body>
</html>

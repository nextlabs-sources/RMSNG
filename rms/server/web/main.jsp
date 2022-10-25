<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<!doctype html>
<html>
<head>
  <%@page import="com.nextlabs.common.shared.WebConfig" %>
  <%
    WebConfig webConfig = WebConfig.getInstance();
    String webContext= request.getContextPath();
    String publicTenantName = webConfig.getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
    String projectTermsUrl = webConfig.getProperty(WebConfig.PROJECT_TERMS_URL, webContext + "/ProjectTermsAndConditions.html");
    String projectPrivacyUrl = webConfig.getProperty(WebConfig.PROJECT_PRIVACY_URL, webContext + "/ProjectPrivacyPolicy.html");
    boolean saasMode = Boolean.parseBoolean(webConfig.getProperty(WebConfig.SAAS, "false"));
   %>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>SkyDRM</title>
  <link rel="icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" />
  <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css?v=${applicationScope['version']}">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/lib/font-awesome/4.4.0/css/font-awesome.min.css?v=${applicationScope['version']}">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/lib/jstree/3.2.1/css/style.min.css?v=${applicationScope['version']}">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/main.min.css?v=${applicationScope['version']}">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/font/fira.css?v=${applicationScope['version']}">
 
 <%
  com.nextlabs.rms.command.GetInitSettingsCommand command = new com.nextlabs.rms.command.GetInitSettingsCommand();
  com.google.gson.JsonObject jsonObj = command.getInitSettingsJSON(request, response);
  String viewerURL = "";
  if (jsonObj == null) {
    viewerURL = "/viewer";
  } else {    
  String rmsVersion = jsonObj.get("rmsVersion").isJsonNull() ? "" : jsonObj.get("rmsVersion").getAsString();
  viewerURL = jsonObj.get("viewerURL").isJsonNull() ? "/viewer" : jsonObj.get("viewerURL").getAsString();
  }
 %> 
  <script type="text/javascript">
    var APPNAME;
    if (!String.prototype.endsWith) {
        String.prototype.endsWith = function(search, this_len) {
            if (this_len === undefined || this_len > this.length) {
                this_len = this.length;
            }
            return this.substring(this_len - search.length, this_len) === search;
        };
    }
    if (window.location.pathname.endsWith('admin')) {
      APPNAME = "admin";
    } else {
      APPNAME = "user";
    }
  </script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/app/main.min.js?v=${applicationScope['version']}"></script>
  <script type="text/javascript">
    var CONTEXT_PATH = "<%=request.getContextPath()%>";
    var VIEWER_URL = "<%=viewerURL%>";
    var VERSION = "${applicationScope['version']}";
    var PUBLIC_TENANT = "<%=publicTenantName%>";
    var PROJECT_TERMS_URL = "<%=projectTermsUrl%>";
    var PROJECT_PRIVACY_URL = "<%=projectPrivacyUrl%>";
    <% if (jsonObj!=null ) { %> 
       var initSettingsData = <%=jsonObj.toString()%>;
    <% } else { %>
        var url = window.location.pathname + window.location.hash;
        if(url.indexOf("/personal/viewSharedFile") !== -1 || url.indexOf("/projects/") !== -1){
            var hrefStr = 'login?r=' + encodeURIComponent(url);
            if (url.indexOf("/personal/viewSharedFile") !== -1) {
                var email = getParameterByName('e');
                if (email) {
                    hrefStr = hrefStr + '&e=' + email;
                }
            }
            window.location.href = hrefStr;
        } else {
            // window.location.href = '<%= ( saasMode ? "intro" : "login" ) %>';
            window.location.href = "login";
        }
    <% } %>
    var i18n_data;
    $(document).ready(function(){
      $("div#index-loading-background").remove();
      $("div#rms-header-help-container").show();
    });
    //prevent drag and drop outside of drag and drop area
    window.addEventListener("dragover",function(e){
	  e = e || event;
	  e.preventDefault();
    },false);
    window.addEventListener("drop",function(e){
	  e = e || event;
	  e.preventDefault();
    },false);
    //to hide the status bar when the keyboard appears in mobile
    var MAX_TABLET_WIDTH = 1024;
    var initialScreenHeight = window.innerHeight;
    var initialScreenWidth = window.innerWidth;
    window.addEventListener("resize", function() {
      isKeyboard = (window.innerHeight < initialScreenHeight);
      if(initialScreenWidth > MAX_TABLET_WIDTH) {
        isKeyboard = false;
      }
      if(isKeyboard){
        $("div.rms-error-messages.alert.alert-upload").css("visibility","hidden");
      } else {
        $("div.rms-error-messages.alert.alert-upload").css("position","fixed");
        $("div.rms-error-messages.alert.alert-upload").css("visibility","visible");
      }
  }, false);
  </script>
</head>

<%
	if (request.getRequestURL().toString().endsWith("main") || request.getRequestURL().toString().endsWith("rms/")) {
%>
<body data-ng-app='userApp' data-ng-controller="loginController">
<%	} else 
{ %>
<body data-ng-app='adminApp' data-ng-controller="loginAdminController">
<%	}
%>
  <div class="cc-layout">
    <div id="nextlabs-header" class="cc-header">
      <%
        if (request.getRequestURL().toString().endsWith("main")) {
      %>
      <a href="main#/home"><div class="cc-header-logo"></div></a>   
      <%  } else 
      { %>   
      <a href="admin#/home"><div class="cc-header-logo"></div></a> 
      <%  }
      %> 
      <div id="rms-header-help-container" class="btn-group rms-login-wrapper " style="display:none">
        <div uib-dropdown class="rms-help-dropdown">
          <button id="rms-header-help" type="button" class="btn btn-sm " uib-dropdown-toggle>
            <i data-ng-include="'ui/img/Help_P.svg'"></i>
          </button>
          <ul class="uib-dropdown-menu rms-login-dropdown-menu-override" role="menu" aria-labelledby="split-button">
          <li role="menuitem"><a href="javascript:void(0)" data-ng-click="help()">{{'help' | translate}}</a></li>
              <li role="menuitem"><a href="javascript:void(0)" data-ng-click="submitFeedBack()">{{'feedback' | translate}}</a></li>
          </ul>
        </div>  
        <div uib-dropdown class="rms-profile-dropdown">
          <button type="button" class="btn" uib-dropdown-toggle style="padding: 0; border: none; background: none;">
            <div id="profileImageHeader" class="inline-block">
              <div class="inline-block">
                <b>{{ loggedInUserName | initials}}</b>
              </div>
            </div>
          </button>
          <ul class="uib-dropdown-menu rms-login-dropdown-menu-override" role="menu" aria-labelledby="split-button"> 
            <li role="menuitem"><a href="javascript:void(0)" data-ng-click="manageProfile()">{{'profile' | translate}}</a></li>
            <%
              if (request.getRequestURL().toString().endsWith("main")) {
            %>
            <li role="menuitem"><a href="javascript:void(0)" data-ng-click="managePreference()">{{'preferences' | translate}}</a></li>
            <li ng-show="isRMDDownloadable" role="menuitem"><a ng-href="#/personal/downloadRMD">{{'download.rmd.tab' | translate}}</a></li>
            <%	}
            %>
            <li role="menuitem"><a href="javascript:void(0)" data-ng-click="doLogout()">{{'logout' | translate}}</a></li>
          </ul>
        </div> 
      </div>          
    </div>
    <div id="index-loading-background">
      <div id="pgLoading-image">
              <img  src="${pageContext.request.contextPath}/ui/img/loading-icon.gif" alt="Loading..." />
       </div>
    </div>
    <div id="rms-main-container" class="container-fluid rms-container-fluid">      
      <div data-ng-controller="appController">
       <div ui-view></div>
      </div>
    </div>
  </div>
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<!doctype html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>SkyDRM</title>
  <link rel="icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" type="image/x-icon"/>
  <link rel="shortcut icon" href="${pageContext.request.contextPath}/ui/img/favicon.ico" />
  <link href="${pageContext.request.contextPath}/ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/font-awesome/4.4.0/css/font-awesome.min.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/angular-ui-switch/angular-ui-switch.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/jstree/3.2.1/css/style.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/ng-resizable/angular-resizable.min.css" rel="stylesheet">

  <link href="${pageContext.request.contextPath}/ui/lib/hopscotch/css/hopscotch.min.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/css/style.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/css/jstreestyle.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/css/font/fira.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/tag-it/css/jquery.tagit.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/ui/lib/tag-it/css/tagit.ui-zendesk.css" rel="stylesheet">
 <%@page import="com.nextlabs.common.shared.WebConfig" %>
 <%
  WebConfig webConfig = WebConfig.getInstance();
  String webContext= request.getContextPath();
  String publicTenantName = webConfig.getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
  String projectTermsUrl = webConfig.getProperty(WebConfig.PROJECT_TERMS_URL, webContext + "/ProjectTermsAndConditions.html");
  String projectPrivacyUrl = webConfig.getProperty(WebConfig.PROJECT_PRIVACY_URL, webContext + "/ProjectPrivacyPolicy.html");
  boolean saasMode = Boolean.parseBoolean(webConfig.getProperty(WebConfig.SAAS, "false"));
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
      <div id="rms-header-help-container" class="btn-group rms-login-wrapper" style="display:none">
        <div uib-dropdown class="rms-help-dropdown">
          <button id="rms-header-help" type="button" class="btn btn-sm" uib-dropdown-toggle>
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
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/jquery/1.8.2/jquery.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular/1.4.7/angular.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular/1.4.7/angular-sanitize.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular/1.4.7/angular-animate.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular/1.4.7/angular-messages.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular/1.4.7/angular-cookies.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular-ui-router/0.2.15/angular-ui-router.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular-ui/bootstrap/ui-bootstrap-tpls-0.14.3.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular-ui-switch/angular-ui-switch.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular-translate/2.8.1/angular-translate.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/angular-dnd/2.1.0/angular-drag-and-drop-lists.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/jstree/3.2.1/js/jstree.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/tag-it/tag-it.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/ng-jstree/0.0.10/ngJsTree-custom.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/ng-file-upload/ng-file-upload.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/ng-resizable/angular-resizable.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/ngclipboard/clipboard.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/ngclipboard/ngclipboard.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/trNgGrid-3.1.5/trNgGrid.min.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/rms/rmsUtil.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/rms/clientDetector.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/lib/hopscotch/js/hopscotch.min.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/js-joda.min.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/dateformat.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/moment.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/sha256.js"></script>
  <script type="text/javascript">
    var CONTEXT_PATH = "<%=request.getContextPath()%>";
    var VIEWER_URL = "<%=viewerURL%>";
    var VERSION = "${applicationScope['version']}";
    var PUBLIC_TENANT = "<%=publicTenantName%>";
    var PROJECT_TERMS_URL = "<%=projectTermsUrl%>";
    var PROJECT_PRIVACY_URL = "<%=projectPrivacyUrl%>";
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
            window.location.href = '<%= ( saasMode ? "intro" : "login" ) %>';
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
    <script type="text/javascript"
    src="${pageContext.request.contextPath}/ui/app/templates.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/app/userApp.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/ui/app/adminApp.js"></script>
  <script type="text/javascript" src="ui/app/initSettingsService.js"></script>
  <script type="text/javascript" src="ui/app/appController.js"></script>
  <script type="text/javascript" src="ui/app/directive/friendlyDate.js"></script>
  <script type="text/javascript" src="ui/app/directive/tagIt.js"></script>
  <script type="text/javascript" src="ui/app/directive/goAsYouType.js"></script>
  <script type="text/javascript" src="ui/app/directive/scrollTop.js"></script>
  <script type="text/javascript" src="ui/app/directive/focusElement.js"></script>
  <script type="text/javascript" src="ui/app/directive/backImg.js"></script>
  <script type="text/javascript" src="ui/app/directive/angular-md5.js"></script>
  <script type="text/javascript" src="ui/app/filter/trusted.js"></script>
  <script type="text/javascript" src="ui/app/directive/passwordMatch.js"></script>
  <script type="text/javascript" src="ui/app/directive/reservedNameValidator.js"></script>
  <script type="text/javascript" src="ui/app/directive/angular.dcb-clear-input.js"></script>
  <script type="text/javascript" src="ui/app/filter/friendlyFileSize.js"></script>
  <script type="text/javascript" src="ui/app/services/digitalRightsExpiryService.js"></script>
  <script type="text/javascript" src="ui/app/services/navService.js"></script>
  <script type="text/javascript" src="ui/app/services/dialogService.js"></script>
  <script type="text/javascript" src="ui/app/services/uploadDialogService.js"></script>
  <script type="text/javascript" src="ui/app/services/shareDialogService.js"></script>
  <script type="text/javascript" src="ui/app/services/protectWidgetService.js"></script>
  <script type="text/javascript" src="ui/app/services/networkService.js"></script>
  <script type="text/javascript" src="ui/app/Home/homeController.js"></script>
  <script type="text/javascript" src="ui/app/Home/mySpaceController.js"></script>
  <script type="text/javascript" src="ui/app/Home/userPreferenceController.js"></script>
  <script type="text/javascript" src="ui/app/Home/watermarkController.js"></script>
  <script type="text/javascript" src="ui/app/Home/digitalRightsExpiryDateController.js"></script>
  <script type="text/javascript" src="ui/app/Home/userPreferenceService.js"></script>
  <script type="text/javascript" src="ui/app/services/uploadFileService.js"></script>
  <script type="text/javascript" src="ui/app/Login/loginController.js"></script>
  <script type="text/javascript" src="ui/app/Login/loginAdminController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/repositoryService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/repoListController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/repositoryController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/personal/personalRepoController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/application/applicationRepoController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/sharedWorkspaceService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/favoritesController.js"></script>
  <script type="text/javascript"
    src="ui/app/Home/SharedFiles/sharedFileListController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Common/userAttributeMappingController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/assignProjAdminsController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/manageProjectTagsController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/IdentityProvidersController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/RightsProtectionMethodController.js"></script>
  <script type="text/javascript" src="ui/app/Home/settings/serviceProvidersController.js"></script>
  <script type="text/javascript" src="ui/app/Home/settings/serviceProviderService.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/RightsProtectionMethodService.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/IdentityProvidersService.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/manageTagsService.js"></script>
  <script type="text/javascript" src="ui/app/Home/settingsController.js"></script>
  <script type="text/javascript" src="ui/app/Home/systemSettingsController.js"></script>
  <script type="text/javascript" src="ui/app/Home/settingsService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/manageRepoController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Profile/manageProfileController.js"></script>
  <script type="text/javascript" src="ui/app/Home/welcomeController.js"></script>
  <script type="text/javascript" src="ui/app/Home/downloadRMDController.js"></script>
  <script type="text/javascript" src="ui/app/Home/uploadProgressController.js"></script>
  <script type="text/javascript" src="ui/app/Home/SharedFiles/shareFileService.js"></script>
  <script type="text/javascript" src="ui/app/Home/LocalFiles/manageLocalFileController.js"></script>
  <script type="text/javascript" src="ui/app/Home/PolicyStudio/policyStudioController.js"></script>
  <script type="text/javascript" src="ui/app/Home/PolicyStudio/policyStudioComponentsController.js"></script>
  <script type="text/javascript" src="ui/app/Home/PolicyStudio/policyService.js"></script>
  <script type="text/javascript"
    src="ui/app/Home/SharedFiles/viewSharedFileController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/projectController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/projectService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/projectStateService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/landingPageController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/emailMismatchController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/upgradeProjectController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/partials/classificationController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Repositories/partials/classificationSelectionController.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/core-min.js"></script>
  <script type="text/javascript" src="ui/lib/3rdParty/sha256.js"></script>
  <script type="text/javascript" src="ui/app/Home/Projects/landingPageService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Configurations/configurationService.js"></script>
  <script type="text/javascript" src="ui/app/Admin/adminLandingPageController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/adminHomeController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Tenant/tenantController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Tenant/tenantService.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Tenant/manageProjectPoliciesController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Super/userController.js"></script>
  <script type="text/javascript" src="ui/app/Admin/Super/userDetailsController.js"></script>  
  <script type="text/javascript" src="ui/app/Admin/Super/userService.js"></script>
  <script type="text/javascript" src="ui/app/Home/Workspace/workSpaceController.js"></script>
  <script type="text/javascript" src="ui/app/Home/Workspace/workSpaceService.js"></script>
</body>
</html>
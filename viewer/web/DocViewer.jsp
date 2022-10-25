<%@page import="com.nextlabs.rms.viewer.config.Operations"%>
<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	isELIgnored="false" session="false" %>
<%
    String op = request.getParameter("operations");
    int operations = -1;
    if (op != null) {
        try {
            operations = Integer.parseInt(op);
        } catch (NumberFormatException e) {
        }
    }
    boolean showFileInfo = true;
    boolean showPrint = true;
    boolean showProtect = false;
    boolean showShare = false;
    boolean showDownload = true;
    boolean showDecrypt = true;
    if (operations >= 0) {
        showFileInfo = Operations.isFlagSet(operations, Operations.VIEW_FILE_INFO.getValue());    
        showPrint = Operations.isFlagSet(operations, Operations.PRINT.getValue());    
        showProtect = Operations.isFlagSet(operations, Operations.PROTECT.getValue());    
        showShare = Operations.isFlagSet(operations, Operations.SHARE.getValue());    
        showDownload = Operations.isFlagSet(operations, Operations.DOWNLOAD.getValue());    
        showDecrypt = Operations.isFlagSet(operations, Operations.DECRYPT.getValue());
    }
    boolean showAnyButton = showFileInfo | showPrint | showProtect | showShare | showDownload | showDecrypt;
%>
<!doctype html>

<script src="ui/lib/rms/clientDetector.js?v=${applicationScope['version']}"></script>
<script src="ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
<script src="ui/lib/3rdParty/js-joda.min.js"></script>
<script src="ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
<script src="ui/lib/3rdParty/jqueryRotate.js"></script>
<script src="ui/lib/3rdParty/jquery.scrollTo.min.js"></script>
<script src="ui/lib/3rdParty/bootstrap.min.js"></script>
<script src="ui/lib/3rdParty/shortcut.js"></script>
<script src="ui/lib/3rdParty/jquery.blockUI.js"></script>
<script src="ui/lib/3rdParty/dateformat.js"></script>
<script type="text/javascript">
	var VERSION = "${applicationScope['version']}";
</script>
<script type="text/javascript" src="ui/app/viewers/viewer.min.js"></script>
<script type="text/javascript" src="ui/app/viewers/angular/shareFileController.js"></script>
<script type="text/javascript" src="ui/app/viewers/angular/watermarkController.js"></script>
<script type="text/javascript" src="ui/app/viewers/angular/digitalRightsExpiryDateController.js"></script>
<html>
    <head>
        <meta charset="utf-8"/>
        <meta http-equiv="Content-type" content="text/html; charset=utf-8" />
        <meta http-equiv="pragma" content="no-cache" />
        <meta http-equiv="X-UA-Compatible" content="chrome=1, IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=0.5, maximum-scale=0.5, user-scalable=yes" />
        <title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
        <link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
        <link rel="stylesheet" href="ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" />     
		<link rel="stylesheet" href="ui/css/style.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/viewer.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/font/fira.css" />
        <link href="ui/lib/font-awesome/4.4.0/css/font-awesome.min.css" rel="stylesheet">
		<link href="ui/lib/tag-it/css/jquery.tagit.css" rel="stylesheet">
  		<link href="ui/lib/tag-it/css/tagit.ui-zendesk.css" rel="stylesheet">
    </head>
    <body style="overflow: hidden;" ng-app="mainApp" ng-controller="shareFileController">
        <div id="error" class="alert alert-danger alert-dismissable" style="display:none" >
             <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
        </div>
       
		<div class="pageWrapper" id="pageWrapper">
			<div class="cc-header">
				<div class="cc-header-logo"></div>
				<button id="rms-help-button" title="Help" onclick="showHelp('/viewer/help_users/index.html')" class="toolbar-button btn btn-default desktop-only spaced "> </button>
			</div>
			
			<div class = "toolbarContainerPlaceholder"> </div>
			
			<div class = "toolbarContainer fade-div">
			
				<div id="titleContainer" class="titleContainer">
					<h5 class="titleText"><b><span id="titleDesktop" data-toggle="tooltip" data-placement="top" class="hide-viewer-title-desktop"></span></b><b><span id="titleMobile" data-toggle="tooltip" data-placement="top" class="hide-viewer-title-mobile show-viewer-title-mobile"></span></b></h5>
				</div>
				
				<div id="toolBar">
					<div class="toolbar-tools">
                        <button id="rms-rotate-left-button" onclick="rotate(-90)" class="toolbar-button btn btn-default spaced dont-show-toolbar-doc"  title="Rotate Anticlockwise"></button>
													
                        <button id="rms-rotate-right-button" onclick="rotate(90)" class="toolbar-button btn btn-default spaced dont-show-toolbar-doc" title="Rotate Clockwise"></button>
						
                        <% if (showAnyButton) { %>
                        <div class="tool-seperator dont-show-toolbar-doc"></div>
                        <% }
                        if (showPrint) { %>
                        <button id="rms-print-button" title="Print" onclick="printAllPages(metaData)" class="toolbar-button btn btn-default spaced print-enabled dont-show-toolbar-doc" disabled="disabled"></button>
                        <% }
                        if (showFileInfo) { %>
                        <button id="rms-info-button" type="button" data-ng-click="onClickInfo(metaData)" class="toolbar-button info btn btn-default spaced" title="View File Info" disabled="disabled"> </button>
                        <% }
                        if (showProtect) { %>
                        <button id="rms-protect-button" type="button" data-ng-click="onClickProtect(metaData)" class="toolbar-button protect btn btn-default spaced" title="Protect File" disabled="disabled"></button>
                        <% }
                        if (showShare) { %>
                        <button id="rms-share-button" type="button" data-ng-click="onClickShare(metaData)" class="toolbar-button info btn btn-default spaced" title="Share File" disabled="disabled"> </button>
                        <% }
                        if (showDownload) { %>
                        <button id="rms-download-button" type="button" onclick="downloadFile(metaData)" class="toolbar-button download btn btn-default spaced" title="Download File" disabled="disabled"></button>
                        <% } 
                        if (showDecrypt) { %>
                        <button id="rms-extract-button" type="button" onclick="decryptFile(metaData)" class="toolbar-button extract btn btn-default spaced" title="Extract Content" disabled="disabled"></button>
                        <% } %>                   
					</div>
				</div>		

				<div id="pageNumContainer" style="display:none;">
					<b><input type="text" id="pageNumberDisplayTextBox" maxlength="1" value="" style="width:1em"/></b>
					<span>/</span>
					<span id="totalPageNum"></span>
				</div>
				
            </div>
			
			<div id="loading">
				<img id="loading-image" src="${pageContext.request.contextPath }/ui/img/loading_48.gif" alt="Loading..." />
			</div>

			<div id="rms-viewer-content" style="position: relative; top: 80px;">
				<div id="imageWrapper" class="imageWrapper noselect">
					<div id="imageContainer" style="margin: auto;">
						<img id="mainImg" src="" >
					</div>
				</div>
				
				<div id="prev-btn" class="fade-div disabled" style="position: absolute; top: 50%; left:20px;">
					<button id="rms-prev-button" onclick='navigate("previous")' class="btn btn-default spaced" data-toggle="tooltip" data-placement="bottom" title="Previous Page" style="height:60px;"></button>       		
				</div>
				
				<div id="next-btn" class="fade-div" style="position: absolute; top: 50%; right:20px;">
					<button id="rms-next-button" onclick='navigate("next")' class="btn btn-default spaced" data-toggle="tooltip" data-placement="bottom" title="Next Page" style="height:60px;"></button>
				</div>
				
				<div id="rendering" style = "display: none; position: absolute; bottom: 100px; right:20px;" >
				    Improving resolution ...
				</div>
				
				<div class="fade-div" style = "position: absolute; bottom: 50px; right:20px;">
					<button id="rms-fit-height-button" onclick="fitToHeight()" class="toolbar-button btn btn-default spaced" title="Fit to Height"></button>
					<button id="rms-fit-width-button" onclick="fitToWidth()" class="toolbar-button btn btn-default spaced" title="Fit to Width"></button>
					<button id="rms-zoom-in-button" onclick="zoomIn()" class="toolbar-button btn btn-default spaced" title="Zoom in"></button>
					<button id="rms-zoom-out-button" onclick="zoomOut()" class="toolbar-button btn btn-default spaced" title="Zoom out"></button>
				</div>
			</div>
        </div>

        <div id="printDiv"></div>
		<div id="viewer-dialog"></div>
      
      	
		<script src="ui/lib/rms/protect.js?v=${applicationScope['version']}"></script>
		<script>var version="${applicationScope['version']}"</script>
		<script src="ui/app/viewers/Viewer.js?v=${applicationScope['version']}"></script>
		<script src="ui/app/viewers/DocViewer.js?v=${applicationScope['version']}"></script>
    </body>
</html>
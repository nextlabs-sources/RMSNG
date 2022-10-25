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
<script src="ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
<script src="ui/lib/3rdParty/js-joda.min.js"></script>
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
        <title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
        <link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
        <link rel="stylesheet" href="ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" />
        
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/TreeControl.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/Toolbar.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/Common.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/Desktop.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/Mobile.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/NoteText.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/jquery-ui.min.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/PropertyWindow.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/ViewerSettings.css" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer/css/jquery.minicolors.css" type="text/css">
		
		<link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/Toolbar.css?v=${applicationScope['version']}" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/PropertyWindow.css?v=${applicationScope['version']}" type="text/css" />
		<link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/Common.css?v=${applicationScope['version']}" type="text/css" />
        <link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/Desktop.css?v=${applicationScope['version']}" type="text/css" />
        <link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/ModelBrowser.css?v=${applicationScope['version']}" type="text/css" />
        <link rel="stylesheet" href="ui/app/viewers/cadviewer-modified/css/TreeControl.css?v=${applicationScope['version']}" type="text/css" />

		<link rel="stylesheet" href="ui/css/style.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/viewer.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/font/fira.css" />
        <link href="ui/lib/font-awesome/4.4.0/css/font-awesome.min.css" rel="stylesheet">
		<link href="ui/lib/tag-it/css/jquery.tagit.css" rel="stylesheet">
  		<link href="ui/lib/tag-it/css/tagit.ui-zendesk.css" rel="stylesheet">

    </head>
    <body style="overflow: hidden;" ng-app="mainApp" ng-controller="shareFileController">
	    <div id="content">

        <div id="error" class="alert alert-danger alert-dismissable" style="display:none; top:55%;" >
             <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
        </div>

		<div class="pageWrapper" id="pageWrapper">
			<div class="cc-header">
				<div class="cc-header-logo"></div>
				<button id="rms-help-button" title="Help" onclick="showHelp('/viewer/help_users/index.html')" class="toolbar-button btn btn-default desktop-only spaced "> </button>
			</div>

			<div class = "toolbarContainer fade-div">

				<div id="titleContainer" class="titleContainer">
					<h5 class="titleText"><b><span id="titleDesktop" data-toggle="tooltip" data-placement="top" class="hide-viewer-title-desktop"></span></b><b><span id="titleMobile" data-toggle="tooltip" data-placement="top" class="hide-viewer-title-mobile show-viewer-title-mobile"></span></b></h5>
				</div>

				<div id="toolBar">
					<div class="toolbar-tools" style="display:flex;">
						<div class="tool-seperator "></div>
						<div id="home-button" title="Reset Camera" data-operatorclass="toolbar-home" class="hoops-tool">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div class="tool-seperator "></div>
						<div id="view-button" title="Camera Menu" data-operatorclass="toolbar-camera" data-submenu="view-submenu" class="hoops-tool toolbar-menu">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div id="edgeface-button" title="Shaded With Lines" data-operatorclass="toolbar-wireframeshaded" data-submenu="edgeface-submenu" class="hoops-tool toolbar-menu dont-show-toolbar-cad">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div class="tool-seperator "></div>
                        <div id="redline-button" title="Redline Note" data-operatorclass="toolbar-redline-note" data-submenu="redline-submenu" class="hoops-tool toolbar-radio">
                             <div class="tool-icon"></div>
                        </div>
						<div id ="click-button" title="Select" data-operatorclass="toolbar-select" data-submenu="click-submenu" class="hoops-tool toolbar-radio">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div id="camera-button" title="Orbit Camera" data-operatorclass="toolbar-orbit" data-submenu="camera-submenu" class="hoops-tool toolbar-menu">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div class="tool-seperator "></div>
						<div id="explode-button" title="Explode Menu" data-operatorclass="toolbar-explode" data-submenu="explode-slider" class="hoops-tool toolbar-menu-toggle dont-show-toolbar-cad">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div id="cuttingplane-button" title="Cutting Plane Menu" data-operatorclass="toolbar-cuttingplane" data-submenu="cuttingplane-submenu" class="hoops-tool toolbar-menu-toggle dont-show-toolbar-cad">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
						<div class="tool-seperator dont-show-toolbar-cad"></div>
						<div id="settings-button" title="Settings" data-operatorclass="toolbar-settings" class="hoops-tool dont-show-toolbar-cad">
							<div class="tool-icon btn btn-default" style="padding: 0px;"></div>
						</div>
                        <% if (showAnyButton) { %>
                        <div class="tool-seperator dont-show-toolbar-cad"></div>
                        <% }
                        if (showPrint) { %>
                        <button id="rms-print-button" title="Print" data-operatorclass="toolbar-snapshot" class="hoops-tool tool-icon btn btn-default spaced print-enabled dont-show-toolbar-cad" disabled="disabled"> </button>
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
					<div id="submenus">
                        <div id="redline-submenu" data-button="redline-button" class="toolbar-submenu submenus">
                            <table>
                              <tr>
                                <td>
                                  <div
                                    id="redline-circle-button"
                                    title="Redline Circle"
                                    data-operatorclass="toolbar-redline-circle"
                                    class="submenu-icon"
                                  ></div>
                                </td>
                                <td>
                                  <div
                                    id="redline-rectangle-button"
                                    title="Redline Rectangle"
                                    data-operatorclass="toolbar-redline-rectangle"
                                    class="submenu-icon"
                                  ></div>
                                </td>
                              </tr>
                              <tr>
                                <td>
                                  <div
                                    id="redline-note"
                                    title="Redline Note"
                                    data-operatorclass="toolbar-redline-note"
                                    class="submenu-icon"
                                  ></div>
                                </td>
                                <td>
                                  <div
                                    id="redline-freehand"
                                    title="Redline Freehand"
                                    data-operatorclass="toolbar-redline-freehand"
                                    class="submenu-icon"
                                  ></div>
                                </td>
                              </tr>
                            </table>
                        </div>
						<div id="camera-submenu" data-button="camera-button" class = "toolbar-submenu submenus">
							<table>
								<tr><td><div id="operator-camera-walk" title="Walk" data-operatorclass="toolbar-walk" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr><td><div id="operator-camera-turntable" title="Turntable" data-operatorclass="toolbar-turntable" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr><td><div id="operator-camera-orbit" title="Orbit Camera" data-operatorclass="toolbar-orbit" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
							</table>
						</div>

						<div id="edgeface-submenu" data-button="edgeface-button" class = "toolbar-submenu submenus">
							<table>
								<tr><td><div id="viewport-wireframe-on-shaded" title="Shaded With Lines" data-operatorclass="toolbar-wireframeshaded" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr><td><div id="viewport-shaded" title="Shaded" data-operatorclass="toolbar-shaded" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr><td><div id="viewport-hidden-line"  title="Hidden Line" data-operatorclass="toolbar-hidden-line" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr><td><div id="viewport-wireframe" title="Wireframe" data-operatorclass="toolbar-wireframe" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
							</table>
						</div>
						<div id="view-submenu" class ="toolbar-submenu submenus">
							<table>
								<tr><td><div id="view-face" title="Orient Camera To Selected Face" data-operatorclass="toolbar-face" class="submenu-icon btn btn-default" style="padding:0px;"></div></td></tr>
								<tr>
									<td><div id="view-iso" title="Iso View" data-operatorclass="toolbar-iso" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-ortho" title="Orthographic Projection" data-operatorclass="toolbar-ortho" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-persp" title="Perspective Projection" data-operatorclass="toolbar-persp" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
								<tr>
									<td><div id="view-left" title="Left View" data-operatorclass="toolbar-left" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-right" title="Right View" data-operatorclass="toolbar-right" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-bottom" title="Bottom View" data-operatorclass="toolbar-bottom" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
								<tr>
									<td><div id="view-front" title="Front View" data-operatorclass="toolbar-front" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-back" title="Back View" data-operatorclass="toolbar-back" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="view-top" title="Top View" data-operatorclass="toolbar-top" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
							</table>
						</div>
						<div id="click-submenu" data-button="click-button"  class ="toolbar-submenu submenus">
							<table>
								<tr>
									<td><div id="measure-angle-between-faces" title="Measure Angle Between Faces" data-operatorclass="toolbar-measure-angle" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="measure-distance-between-faces" title="Measure Distance Between Faces" data-operatorclass="toolbar-measure-distance" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
								<tr>
									<td><div id="measure-edges" title="Measure Edges" data-operatorclass="toolbar-measure-edge" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
									<td><div id="measure-point-to-point" title="Measure Point to Point" data-operatorclass="toolbar-measure-point" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
								<tr>
									<td><div title="Select Parts" data-operatorclass="toolbar-select" class="submenu-icon btn btn-default" style="padding:0px;"></div></td>
								</tr>
							</table>
						</div>

						<div id="explode-slider" class = "toolbar-submenu slider-menu submenus">
							<div id="explosion-slider" style="height:90px;" class="toolbar-slider"></div>
						</div>

						<div id="cuttingplane-submenu" class="toolbar-submenu cutting-plane submenus no-modal">
							<table>
								<tr><td><div id="cuttingplane-face" title="Create Cutting Plane On Selected Face" data-plane="face" data-operatorclass="toolbar-cuttingplane-face" class="toolbar-cp-plane submenu-icon"></div></td></tr>
								<tr>
									<td><div id="cuttingplane-x" title="Cutting Plane X" data-plane="x" data-operatorclass="toolbar-cuttingplane-x" class="toolbar-cp-plane submenu-icon"></div></td>
									<td><div id="cuttingplane-section" title="Cutting Plane Visibility Toggle" data-plane="section" data-operatorclass="toolbar-cuttingplane-section" class="toolbar-cp-plane submenu-icon disabled"></div></td>
								</tr>
								<tr>
									<td><div id="cuttingplane-y" title="Cutting Plane Y" data-plane="y" data-operatorclass="toolbar-cuttingplane-y" class="toolbar-cp-plane submenu-icon"></div></td>
									<td><div id="cuttingplane-toggle" title="Cutting Plane Section Toggle" data-plane="toggle" data-operatorclass="toolbar-cuttingplane-toggle" class="toolbar-cp-plane submenu-icon disabled"></div></td>
								</tr>
								<tr>
									<td><div id="cuttingplane-z" title="Cutting Plane Z" data-plane="z" data-operatorclass="toolbar-cuttingplane-z" class="toolbar-cp-plane submenu-icon"></div></td>
									<td><div id="cuttingplane-reset" title="Cutting Plane Reset" data-plane="reset" data-operatorclass="toolbar-cuttingplane-reset" class="toolbar-cp-plane submenu-icon disabled"></div></td>
								</tr>
							</table>
						</div>
					</div>
				</div>
            </div>
            <div id ="snapshot-dialog" class="hoops-ui-window">
                <div class="hoops-ui-window-body"><img id="snapshot-dialog-image" class="snapshot-image" alt="Snapshot"/></div>
            </div>
			<div id="viewer-dialog"  style="top:80px !important"></div>
			<div id="rms-viewer-content">
				<div id="viewerContainer">
					<div id="Wrapper" class="Wrapper">
					</div>
				</div>
			</div>
        </div>

		<!-- Settings -->

		<!-- Viewer Settings Window -->
		<div id="viewer-settings-dialog" class="hoops-ui-window">
				<!-- Header -->
				<div class="hoops-ui-window-header">
					<span class="tab selected" id="settings-tab-label-general">General</span>
					<span class="tab" id="settings-tab-label-walk">Walk</span>
					<span class="tab" id="settings-tab-label-drawing">Drawing</span>
					<span class="tab" id="settings-tab-label-floorplan">Floorplan</span>
				</div>


				<!-- Floorplan tab-->
				<div class="hoops-ui-window-body" id="settings-tab-floorplan">
					<div class="settings-group-header">Floorplan</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<span id="settings-floorplan-active-text">Active:</span>
						<input type="checkbox" id="settings-floorplan-active" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-track-camera-text">Track Camera:</span>
						<input type="checkbox" id="settings-floorplan-track-camera" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-orientation-text">Orientation:</span>
						<select id="settings-floorplan-orientation" class="right-align">
						  <option value="0">North Up</option>
						  <option value="1">Avatar Up</option>
						</select>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-auto-activate-text">Auto Activation:</span>
						<select id="settings-floorplan-auto-activate" class="right-align">
						  <option value="0">Bim</option>
						  <option value="1">Bim + Walk</option>
						  <option value="2">Never</option>
						</select>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-feet-per-pixel-style">
						  <span id="settings-floorplan-feet-per-pixel-text">Overlay Feet per Pixel:</span>
						  <input
							type="number"
							min="0"
							step=".1"
							id="settings-floorplan-feet-per-pixel"
							class="right-align"
						  />
						</span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-zoom-style">
						  <span id="settings-floorplan-zoom-text">Overlay Zoom Level:</span>
						  <input
							type="number"
							min="0"
							max="10"
							step=".1"
							id="settings-floorplan-zoom"
							class="right-align"
						  />
						</span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-background-opacity-text"
						  >Overlay Background Opacity:</span
						>
						<input
						  type="number"
						  min="0"
						  max="1"
						  step=".01"
						  id="settings-floorplan-background-opacity"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-border-opacity-text">Overlay Border Opacity:</span>
						<input
						  type="number"
						  min="0"
						  max="1"
						  step=".01"
						  id="settings-floorplan-border-opacity"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="settings-floorplan-avatar-opacity-text">Avatar Opacity:</span>
						<input
						  type="number"
						  min="0"
						  max="1"
						  step=".01"
						  id="settings-floorplan-avatar-opacity"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<div>Floorplan Colors:</div>
						<div>
						  <input
							type="text"
							id="settings-floorplan-background-color"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Background</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-floorplan-border-color"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Border</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-floorplan-avatar-color"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Avatar</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-floorplan-avatar-outline-color"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Avatar Outline</label>
						</div>
					  </div>
					</div>
				</div>

				<!-- Drawing tab -->
				<div class="hoops-ui-window-body" id="settings-tab-drawing">
					<div class="settings-group-header">Drawing</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<div>Drawing Colors:</div>
						<div>
						  <input
							type="text"
							id="settings-drawing-background"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Background</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-drawing-sheet"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Sheet</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-drawing-sheet-shadow"
							class="color-picker"
							data-position="bottom left"
						  />
						  <label>Sheet Shadow</label>
						</div>
						<div>
						  <input type="checkbox" id="settings-drawing-background-enabled" />
						  <label>Show Sheet Background</label>
						</div>
					  </div>
					</div>
				</div>

				<!-- Walk tab -->
				<div class="hoops-ui-window-body" id="settings-tab-walk">
					<div class="settings-group-header">Walk Mode</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<label>Walk Mode:</label>
						<select id="settings-walk-mode" class="right-align">
						  <option value="0">Mouse</option>
						  <option value="1">Keyboard</option>
						</select>
						<span class="clear-both"></span>
					  </div>

					  <div class="settings-block" id="walk-navigation-keys">
						<div id="walk-navigation-text" class="center-align bold">Navigation Keys</div>
						<div>
						  <span id="walk-key-forward">W</span> / <span id="walk-key-left">A</span> /
						  <span id="walk-key-backward">S</span> /
						  <span id="walk-key-right">D</span>
						  <span class="right-align">Move</span>
						</div>
						<div>
						  <span id="walk-key-rotate-left">Q</span> /
						  <span id="walk-key-rotate-right">E</span>
						  <span class="right-align">rotate</span>
						</div>
						<div>
						  <span id="walk-key-up">X</span> /
						  <span id="walk-key-down">C</span>
						  <span class="right-align">Up / Down</span>
						</div>
						<div>
						  <span id="walk-key-tilt-up">R</span> /
						  <span id="walk-key-tilt-down">F</span>
						  <span id="move-keys" class="right-align">Tilt</span>
						</div>
						<div>
						  <span id="walk-key-toggle-collision">V</span>
						  <span class="right-align">Toggle Collision Detection</span>
						</div>
					  </div>

					  <div class="settings-block">
						<span id="walk-rotation-text">Rotation (Deg/s):</span>
						<input type="number" min="0" id="settings-walk-rotation" class="right-align" />
						<span class="clear-both"></span>
					  </div>

					  <div class="settings-block">
						<span id="walk-speed-text"
						  >Walk Speed (<span id="walk-speed-units">m</span>/s):</span
						>
						<input
						  type="number"
						  min="0"
						  id="settings-walk-speed"
						  step=".1"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="walk-elevation-text"
						  >Elevation Speed (<span id="elevation-speed-units">m</span>/s):</span
						>
						<input
						  type="number"
						  min="0"
						  id="settings-walk-elevation"
						  step=".1"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="walk-view-angle-text">Field of View (Deg):</span>
						<input
						  type="number"
						  min="30"
						  max="150"
						  id="settings-walk-view-angle"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span id="walk-mouse-look-text">Enable Mouse Look:</span>
						<input type="checkbox" id="settings-mouse-look-enabled" />

						<span id="settings-mouse-look-style" class="grayed-out right-align">
						  <label>Speed: </label>
						  <input id="settings-mouse-look-speed" type="number" />
						</span>
					  </div>
					  <div class="settings-block">
						<span id="walk-bim-mode-text">Enable Collision Detection:</span>
						<input type="checkbox" id="settings-bim-mode-enabled" />
						<span class="clear-both"></span>
					  </div>
					</div>
				</div>

				<!--General tab-->
				<div class="hoops-ui-window-body selected" id="settings-tab-general">
					<!-- general -->
					<div class="settings-group-header">General</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<label>Projection Mode:</label>
						<select id="settings-projection-mode" class="right-align">
						  <option value="0">Orthographic</option>
						  <option value="1">Perspective</option>
						</select>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span class="framerate-text">Framerate (fps):</span>
						<input type="number" min="0" id="settings-framerate" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span>Hidden Line Opacity (0-1): </span>
						<input
						  id="settings-hidden-line-opacity"
						  class="right-align"
						  type="number"
						  min="0"
						  max="1"
						  step=".1"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<label for="settings-show-backfaces">Show Backfaces:</label>
						<input type="checkbox" id="settings-show-backfaces" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<label for="settings-show-capping-geometry">Show Capping Geometry:</label>
						<input type="checkbox" id="settings-show-capping-geometry" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span>Enable Face / Line Selection: </span>
						<input
						  type="checkbox"
						  id="settings-enable-face-line-selection"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span>Selection Honors Scene Visibility:</span>
						<input type="checkbox" id="settings-select-scene-invisible" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span>Rotate Around Camera Center:</span>
						<input type="checkbox" id="settings-orbit-mode" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					</div>

					<!-- effects -->
					<div class="settings-group-header">Effects</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<span>Enable Ambient Occlusion:</span>
						<input type="checkbox" id="settings-ambient-occlusion" />
						<input
						  id="settings-ambient-occlusion-radius"
						  type="number"
						  min="0"
						  max="10"
						  step=".01"
						  class="right-align"
						/>
						<div class="right-align">Radius:</div>
						<span class="clear-both"></span>
					  </div>

					  <div class="settings-block">
						<span>Enable Anti-Aliasing:</span>
						<input type="checkbox" id="settings-anti-aliasing" class="right-align" />
						<span class="clear-both"></span>
					  </div>

					  <div class="settings-block">
						<div>
						  <span>Enable Bloom:</span>
						  <input type="checkbox" id="settings-bloom-enabled" class="right-align" />
						</div>
						<span id="settings-bloom-style" class="grayed-out">
						  <label>Intensity Scale:</label>
						  <input
							id="settings-bloom-intensity"
							type="number"
							min="0"
							step=".1"
							class="right-align"
							disabled
						  />
						  <span class="clear-both"></span>
						  <label>Threshold:</label>
						  <input
							type="range"
							id="settings-bloom-threshold"
							min="0"
							max="1"
							step="0.1"
							class="right-align"
							disabled
						  />
						</span>
					  </div>

					  <div class="settings-block">
						<div>
						  <span>Silhouette Edges:</span>
						  <input type="checkbox" id="settings-silhouette-enabled" class="right-align" />
						</div>
					  </div>

					  <div class="settings-block">
						<div>
						  <span>Reflection Planes:</span>
						  <input type="checkbox" id="settings-reflection-enabled" class="right-align" />
						</div>
					  </div>

					  <div class="settings-block">
						<div>
						  <span>Enable Shadows:</span>
						  <input type="checkbox" id="settings-shadow-enabled" class="right-align" />
						</div>
						<span id="settings-shadow-style" class="grayed-out">
						  <span>Interactive:</span>
						  <input
							type="checkbox"
							id="settings-shadow-interactive"
							class="right-align"
							disabled
						  />
						  <span class="clear-both"></span>

						  <label>Blur Samples:</label>
						  <input
							type="range"
							id="settings-shadow-blur-samples"
							min="0"
							max="20"
							step="1"
							class="right-align"
							disabled
						  />
						</span>
					  </div>
					</div>

					<!-- axis -->
					<div class="settings-group-header">Axis</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<span>Show Axis Triad:</span>
						<input type="checkbox" id="settings-axis-triad" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					  <div class="settings-block">
						<span>Show Nav Cube:</span>
						<input type="checkbox" id="settings-nav-cube" class="right-align" />
						<span class="clear-both"></span>
					  </div>
					</div>

					<!-- point cloud -->
					<div class="settings-group-header">Point Cloud</div>
					<div class="settings-group settings-group-general">
					  <div class="settings-block">
						<span>Enable Splats:</span>
						<input type="checkbox" id="settings-splat-rendering-enabled" />

						<span id="settings-splat-enabled-style" class="grayed-out right-align">
						  <label>Size: </label>
						  <input id="settings-splat-rendering-size" step=".01" type="number" />

						  <label>Mode:</label>
						  <select id="settings-splat-rendering-point-size-unit">
							<option value="0">ScreenPixels</option>
							<option value="1">CSSPixels</option>
							<option value="2">World</option>
							<option value="3">ScreenWidth</option>
							<option value="4">ScreenHeight</option>
							<option value="5">BoundingDiagonal</option>
						  </select>
						</span>
					  </div>
					  <div class="settings-block">
						<span>Enable Eye-Dome Lighting:</span>
						<input
						  type="checkbox"
						  id="settings-eye-dome-lighting-enabled"
						  class="right-align"
						/>
						<span class="clear-both"></span>
					  </div>
					</div>

					<!--color-->
					<div class="settings-group-header">Color</div>
					<div class="settings-group settings-group-colors">
					  <div class="settings-block">
						<div>Background Color:</div>
						<div>
						  <input
							type="text"
							id="settings-background-top"
							class="color-picker"
							data-position="top left"
						  />
						  <label>Top</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-background-bottom"
							class="color-picker"
							data-position="top left"
						  />
						  <label>Bottom</label>
						</div>
					  </div>
					  <div class="settings-block">
						<div>Capping Geometry:</div>
						<div>
						  <input
							type="text"
							id="settings-capping-face-color"
							class="color-picker"
							data-position="top left"
						  />
						  <label>Face</label>
						</div>
						<div>
						  <input
							type="text"
							id="settings-capping-line-color"
							class="color-picker"
							data-position="top left"
						  />
						  <label>Line</label>
						</div>
					  </div>
					  <div class="settings-block">
						<div>Selection Color:</div>
						<input
						  type="text"
						  id="settings-selection-color-body"
						  class="color-picker"
						  data-position="top left"
						/>
						<label>Body</label>
						<div>
						  <span id="settings-selection-color-face-line-style">
							<input
							  type="text"
							  id="settings-selection-color-face-line"
							  class="color-picker"
							  data-position="top left"
							/>
							<label>Faces and Lines</label>
						  </span>
						</div>
					  </div>
					  <div class="settings-block">
						<input
						  type="text"
						  id="settings-measurement-color"
						  class="color-picker"
						  data-position="top left"
						/>
						<label>Measurement Color</label>
					  </div>
					  <div class="settings-block">
						<span id="settings-pmi-color-style" class="grayed-out">
						  <input
							type="text"
							id="settings-pmi-color"
							class="color-picker"
							data-position="top left"
							disabled
						  />
						  <label>PMI Override Color</label>
						</span>
						<input type="checkbox" id="settings-pmi-enabled" />
						<label>Enable</label>
					  </div>
					</div>
				</div>

				<!-- Footer -->
				<div>
					<div class="hoops-ui-window-footer">
					  <div class="version">
						Viewer Version:
						<span id="settings-viewer-version"></span>, Format Version:
						<span id="settings-format-version"></span>
					  </div>
					  <div class="hoops-ui-footer-buttons">
						<button id="viewer-settings-ok-button">Ok</button>
						<button id="viewer-settings-cancel-button">Cancel</button>
						<button id="viewer-settings-apply-button">Apply</button>
					  </div>
					</div>
				</div>
			</div>

		<!-- End of Settings -->

		</div>

		<script src="ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
		<script type="text/javascript" src="ui/app/viewers/cadviewer/js/jquery.ui.touch-punch.min.js"></script>
        <script type="text/javascript" src="ui/app/viewers/cadviewer/js/keymaster.min.js"></script>
        <script type="text/javascript" src="ui/app/viewers/cadviewer/js/iscroll.min.js"></script>
        <script type="text/javascript" src="ui/app/viewers/cadviewer/js/mobile-detect.min.js"></script>
		<script type="text/javascript" src="ui/app/viewers/cadviewer/js/hoops_web_viewer.js?v=${applicationScope['version']}"></script>
		<script type="text/javascript" src="ui/app/viewers/cadviewer/js/jquery.minicolors.min.js"></script>
		<script type="text/javascript" src="ui/app/viewers/cadviewer/js/web_viewer_ui.js"></script>

		<script type="text/javascript" src="ui/app/viewers/cadviewer-modified/js/web_viewer_ui.js?v=${applicationScope['version']}"></script>
		<script src="ui/lib/3rdParty/bootstrap.min.js"></script>
        <script src="ui/lib/3rdParty/shortcut.js"></script>
        <script src="ui/lib/3rdParty/fontChecker.js"></script>
		<script src="ui/lib/3rdParty/dateformat.js"></script>
		<script src="ui/lib/rms/protect.js?v=${applicationScope['version']}"></script>
		<script src="ui/lib/rms/clientDetector.js?v=${applicationScope['version']}"></script>
		<script>var contextPath = "${pageContext.request.contextPath}"; var version="${applicationScope['version']}"</script>
		<script src="ui/app/viewers/Viewer.js?v=${applicationScope['version']}"></script>
		<script src="ui/app/viewers/CADViewer.js?v=${applicationScope['version']}"></script>
    </body>
</html>

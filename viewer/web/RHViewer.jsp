<%@page import="com.nextlabs.rms.viewer.config.Operations"%>
<%@page import="com.nextlabs.rms.shared.LocalizationUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	isELIgnored="false" session="false"%>
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
<html>
    <head>
	<meta charset="utf-8"/>
	<meta http-equiv="Content-type" content="text/html; charset=utf-8" />
	<meta http-equiv="pragma" content="no-cache" />
	<meta http-equiv="X-UA-Compatible" content="chrome=1, IE=edge">
	<title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
	<link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
	<link rel="stylesheet" href="ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" />     
	<link rel="stylesheet" href="ui/css/style.css?v=${applicationScope['version']}" />
	<link rel="stylesheet" href="ui/css/viewer.css?v=${applicationScope['version']}" />
	<link rel="stylesheet" href="ui/css/rhViewer.css?v=${applicationScope['version']}" />
	<link rel="stylesheet" href="ui/css/font/fira.css" />
	<link href="ui/lib/font-awesome/4.4.0/css/font-awesome.min.css" rel="stylesheet">
	<link href="ui/lib/tag-it/css/jquery.tagit.css" rel="stylesheet">
  	<link href="ui/lib/tag-it/css/tagit.ui-zendesk.css" rel="stylesheet">
	
	<script src="ui/lib/rms/clientDetector.js?v=${applicationScope['version']}"></script>
	<script src="ui/lib/3rdParty/jquery-1.10.2.min.js"></script>
	<script src="ui/lib/3rdParty/js-joda.min.js"></script>
	<script src="ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
	<script src="ui/lib/3rdParty/bootstrap.min.js"></script>
	<script src="ui/lib/3rdParty/shortcut.js"></script>
	<script src="ui/lib/3rdParty/jquery.blockUI.js"></script>
	<script src="ui/lib/3rdParty/dateformat.js"></script>
	<script src="ui/lib/rms/protect.js?v=${applicationScope['version']}"></script>
	<script>var version="${applicationScope['version']}"</script>
	<script src="ui/app/viewers/Viewer.js?v=${applicationScope['version']}"></script>
	<script src="ui/app/viewers/RHViewer.js?v=${applicationScope['version']}"></script>
	<script type="text/javascript">
		var VERSION = "${applicationScope['version']}";
	</script>
	<script type="text/javascript" src="ui/app/viewers/viewer.min.js"></script>
	<script type="text/javascript" src="ui/app/viewers/angular/shareFileController.js"></script>
	<script type="text/javascript" src="ui/app/viewers/angular/watermarkController.js"></script>
	<script type="text/javascript" src="ui/app/viewers/angular/digitalRightsExpiryDateController.js"></script>

<script type="text/javascript">

	var g_MovementsCount = 0;
	
	var g_Action, g_Behavior, metaData;

	function element(id) { return document.getElementById(id); }
	function rh() { return element("DeepView"); }

	function playFirstStep()
	{
		var step = rh().Scene.Steps.GetByIndex(0);
		step.Play();
	}

	

	function handleStepEvent(e) 
	{
		var eventReport = element("Results");
		if (eventReport == null) return;
		
	}

	function handleMouseEvent(e)
	{
		if (e.IsDoubleClick)
		
		if (e.IsMouseDown)
			
		if (e.IsMouseUp)
			
		if (e.IsMouseHit)
			
		if (e.IsMouseOut)
			
		if (e.IsMouseOver)
			
 		
		if (e.IsMouseMove)
		{
			g_MovementsCount++;
			var mm = element("MouseMove");
			if (mm != null) mm.innerHTML = g_MovementsCount;
		}
	}

	function handleNodesSelected(selection)
	{
		var output = 'NodesSelected: ';
		for (var i=0; i < selection.count; i++)
		{
			var node = selection.item(i);
			
			output += node.name + ', ';
			
			if (node.HotspotActionCount > 0)
			{
				var hid = rh().Scene.SelectedHotspotActionIndex;

				if (hid >= 0)
				{
					g_Action = node.HotspotAction(hid);
					g_Behavior = node.HotspotBehavior(hid)
					document.getElementById("hotspotAction").innerHTML = "Action: " + g_Action + "<br>Behavior: " + g_Behavior;
					document.getElementById("hotspotResult").innerHTML = "Not Executed";
				}
				else
				{
					g_Action = "";
					g_Behavior = "";
					document.getElementById("hotspotAction").innerHTML = "Action: -<br>Behavior: -";
					document.getElementById("hotspotResult").innerHTML = "Dialog cancelled";
				}
			}
		}
		
		
		
		
		return false;
	}

	function onSceneLoaded()
	{
	 

		var creator = rh().Creator;
		var runtime = rh().Runtime;

		// Create StepEventHandler and attach it to DeepView runtime
		if (creator.StepEventHandler)
		{
			var stepEventHandler = creator.StepEventHandler.Create();
			stepEventHandler.OnEvent = handleStepEvent;
			runtime.AddEventHandler(stepEventHandler);
		}
		
		// Create MouseEventHandler and attach it to DeepView runtime
		var mouseEventHandler = creator.MouseEventHandler.Create();
		mouseEventHandler.OnEvent = handleMouseEvent;
		mouseEventHandler.OnMouseDoubleClick = true;
		mouseEventHandler.OnMouseDown = true;
		mouseEventHandler.OnMouseUp = true;
		mouseEventHandler.OnMouseHit = true;
		mouseEventHandler.OnMouseMove = true;
		mouseEventHandler.OnMouseOut = true;
		mouseEventHandler.OnMouseOver = true;
		runtime.AddEventHandler(mouseEventHandler);

		// Attach nodesSelected event
		rh().NodesSelected = handleNodesSelected;
		
		rh().ShowGUIElement("right_toolbar",true);
		rh().ShowGUIElement("left_toolbar",true);
		rh().ShowGUIElement("right_click_menu",true);
		rh().ShowGUIElement("bottom_toolbar",false);
		rh().ShowMenuItem  ( "M3011" ,false);  
		rh().ShowMenuItem  ( "M3018" ,false);
		rh().ShowMenuItem  ( "M3105" ,false); 
		rh().ShowMenuItem  ( "M1171" ,false); 
		rh().ShowMenuItem  ( "M1214" ,false);
		rh().ShowMenuItem  ( "M1216" ,false);
		rh().ShowMenuItem  ( "M1218" ,false);
		rh().ShowMenuItem  ( "M1220" ,false);
		rh().ShowMenuItem  ( "M1350" ,false);
		rh().ShowMenuItem  ( "M4050" ,false);
		rh().ShowMenuItem  ( "M1297" ,false);
		rh().ShowMenuItem  ( "M3104" ,false); 	

		
		$('#DeepViewDiv').bind('contextmenu', function(e) {
		return true;
		}); 
		$('body').bind('contextmenu', function(e) {
		return false;
		}); 
		var menuEventHandler = creator.MenuEventHandler.Create();
		menuEventHandler.onEvent = handleMenuEvent;
		runtime.addEventHandler(menuEventHandler);
	}
	
	function handleMenuEvent(e)
	{
		var name = e.MenuItemName;
		var checked = e.MenuItemChecked;
	}

	function onProgress(int1, int2, message) {
	    
	}

	function initialisePage()
	{
		if( AXOrNull("SAP.rh") != null) {
			checkAuth();
		}
		//document.getElementById("DeepView").LoadFile(filePath);
	}

	$(window).resize(function() {
		closeViewerDialog();
	});
	
	var checkAuth = function(){
        var docId = getDocId();
	    var s = getSessionId(); 
	    var source = getViewSource();
	    $.post('/viewer/RMSViewer/GetDocMetaData', {documentId: docId,s: s, source: source}, function(data, status){
        	metaData=data;
            if(data.errMsg && data.errMsg.length>0){
                window.location.href = 'ShowError.jsp?errMsg='+data.errMsg;
                return;
            }
			<% 
            if (showPrint) { %>
			checkPrintEnabled(metaData);
			<%}
			if (showDownload) { %>
			checkDownloadEnabled(metaData);
			<%}
            if (showFileInfo) { %>
            checkFileInfoEnabled(metaData);
            <%}
            if (showDecrypt) { %>
            checkDecryptEnabled(metaData);
            <%}
			if (showShare) { %>
			checkShareEnabled(metaData);
			<%}
			if (showProtect) { %>
			checkProtectEnabled(metaData);
			<%} %>
			if (!isPrintEnabled(metaData)){
				rh().ShowMenuItem  ( "M1279" ,true);
			}
			
			isPMIAllowed = true;
			rh().ShowMenuItem ("M1181",isPMIAllowed);
			rh().ShowMenuItem ("M3210",isPMIAllowed);
			rh().ShowMenuItem ("M3211",isPMIAllowed);
			
            var filePath="/RMSViewer/GetFileContent?d=" + getParameterByName('d') + "&s=" + getParameterByName('s');            
			var fileName=metaData.originalFileName;
			addTitle(fileName);
			if (rh() != null)
				rh().SceneLoaded = onSceneLoaded;
			rh().Progress = onProgress;
			rh().FileName=filePath;
            translateIfRequired(function callback(){
                showShareIntro();
			});
		});
	}

</script>
  
</head>

<body onload="initialisePage()" style="background-color: #F3F3F0" ng-app="mainApp" ng-controller="shareFileController">
	<div id="error" class="alert alert-danger alert-dismissable" style="display:none;" >
         <button type="button" class="close" onclick="closeDialog()" aria-hidden="true">x</button><span id="errmsg"></span>
    </div> 
	<div class="pageWrapper" id="pageWrapper" style="position: absolute; overflow: auto;">
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
                 <%
                    if (showPrint) { %>
					<button id="rms-print-button" title="Print" onclick="print()" class="toolbar-button btn btn-default desktop-only spaced print-enabled" disabled="disabled"> </button>
				 <% }
                    if (showFileInfo) { %>	
					<button id="rms-info-button" type="button" data-ng-click="onClickRHInfo()" class="toolbar-button info btn btn-default spaced" title="View File Info" disabled="disabled"> </button>
				 <% }
                    if (showProtect) { %>	
					<button id="rms-protect-button" type="button" data-ng-click="onClickRHProtect()" class="toolbar-button protect btn btn-default spaced" title="Protect File" disabled="disabled"></button>
				 <% }
                    if (showShare) { %>	
					<button id="rms-share-button" type="button" data-ng-click="onClickRHShare()" class="toolbar-button protect btn btn-default spaced" title="Share File" disabled="disabled" data-toggle="popover"></button>
				 <% }
                    if (showDownload) { %>	
					<button id="rms-download-button" type="button" onclick="showDownloadFile()" class="toolbar-button download btn btn-default spaced" title="Download File" disabled="disabled"></button>
				<% } 
                    if (showDecrypt) { %>
                    <button id="rms-extract-button" type="button" onclick="decryptFile(metaData)" class="toolbar-button extract btn btn-default spaced" title="Extract Content" disabled="disabled"></button>
				 <% } %>
				</div>
			</div>
		</div>
		
		<div class="rh-viewer-dialog" id="viewer-dialog"></div>
		
		<iframe id="overlay-iframe" style="display: none; left: 25%; position: absolute; top: 45%; width: 50%; z-index:1; height:100%;" src="" frameBorder="0" scrolling="no"></iframe>
		<iframe id="overlay-iframe-2" style="display: none; left: 25%; position: absolute; top: 45%; width: 50%; z-index:1; height:100%;" src="" frameBorder="0" scrolling="no"></iframe>
		<iframe id="overlay-iframe-share-intro" style="display: none; left: 25%; position: absolute; top: 45%; width: 50%; z-index:1; height:100%;" src="" frameBorder="0" scrolling="no"></iframe>
		
		<div id="rms-viewer-content">
			<div id="all">
				<div id="DeepViewDiv">
					<object id="DeepView" type="application/rh">
					</object>
				</div>
			</div>
		</div>
	</div>
</body>
</html>

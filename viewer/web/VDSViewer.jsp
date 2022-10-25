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
<!DOCTYPE html>
<html>
	<head>
		<meta charset="UTF-8">		
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="keywords" content="Control Shell">
		<meta name="apple-mobile-web-app-capable" content="yes" />
	    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
		<%--<meta content="width=device-width, initial-scale= 1.0, user-scalable=no" name="viewport"> --%>
		<meta name="viewport" content="width=device-width; initial-scale=0.5; maximum-scale=0.5; user-scalable=yes" />
	    <meta name="HandheldFriendly" content="True" />
	    <title><%= LocalizationUtil.getMessage(request, "app.title", null, null) %></title>
	    <link rel="shortcut icon" href="${pageContext.request.contextPath }/ui/img/favicon.ico" />
		<link rel="stylesheet" href="ui/lib/bootstrap/3.3.5/css/bootstrap.min.css" />     
		<link rel="stylesheet" href="ui/css/style.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/viewer.css?v=${applicationScope['version']}" />
		<link rel="stylesheet" href="ui/css/font/fira.css" />
	    <link href="ui/lib/font-awesome/4.4.0/css/font-awesome.min.css" rel="stylesheet">
		<link href="ui/lib/tag-it/css/jquery.tagit.css" rel="stylesheet">
	  	<link href="ui/lib/tag-it/css/tagit.ui-zendesk.css" rel="stylesheet">
		
		<script src="${pageContext.request.contextPath}/ui/lib/jquery/jquery-1.10.2.min.js"></script>
		<script src="ui/app/viewers/SAPViewer/resources/Loco.js"></script>
		<script src="ui/app/viewers/SAPViewer/lib/thirdparty/html2canvas.js"></script>
		<script src="ui/app/viewers/SAPViewer/lib/dvl.js"></script>

		<script src="ui/lib/rms/clientDetector.js?v=${applicationScope['version']}"></script>
		<script src="ui/lib/jquery-ui/1.11.4/jquery-ui.min.js"></script>
	    <script src="ui/lib/3rdParty/js-joda.min.js"></script>
		<script src="ui/lib/3rdParty/bootstrap.min.js"></script>
		<script src="ui/lib/3rdParty/shortcut.js"></script>
		<script src="ui/lib/3rdParty/jquery.blockUI.js"></script>
		<script src="ui/lib/rms/protect.js?v=${applicationScope['version']}"></script> 
	    <script src="ui/lib/3rdParty/fontChecker.js"></script>
		<script src="ui/lib/3rdParty/dateformat.js"></script>
		<script>var version="${applicationScope['version']}"</script>
		<script src="ui/app/viewers/Viewer.js?v=${applicationScope['version']}"></script>
		<script src="ui/app/viewers/VDSViewer.js?v=${applicationScope['version']}"></script>
		<script type="text/javascript">
			var VERSION = "${applicationScope['version']}";
		</script>
		<script type="text/javascript" src="ui/app/viewers/viewer.min.js"></script>
		<script type="text/javascript" src="ui/app/viewers/angular/shareFileController.js"></script>
		<script type="text/javascript" src="ui/app/viewers/angular/watermarkController.js"></script>
		<script type="text/javascript" src="ui/app/viewers/angular/digitalRightsExpiryDateController.js"></script>
</head>
<body id="body" style="overflow: hidden;" ng-app="mainApp" ng-controller="shareFileController">	
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
					<% if (showAnyButton) { %>
					<div class="tool-seperator "></div>
					<% } %>

					<div style="display: inline-block; margin-top: 10px;">					
						<span style="padding: 10px 20px 0px 20px;">Procedure: <select id="proceduresList" onchange="updateSteps()"></select> &nbsp; Step: <select id="stepsList"></select></span>		
					</div>

					<% if (showAnyButton) { %>
					<div class="tool-seperator "></div>
					<% } %>

					<!--<button id="vds-toggleStepInFo" title="Toggle Step Info" class="toolbar-button btn btn btn-default spaced menu-button" onclick="toggleStepInFo()"></button>-->

					<!--<button id="vds-resetStep" title="Reset" class="toolbar-button btn btn btn-default spaced menu-button" onclick="resetStep()"></button>-->
					
					<button id="vds-play" title="Play Current Scene" class="toolbar-button btn btn btn-default spaced menu-button" onclick="playStep()"></button>

					<button id="vds-pauseStep" title="Pause Current Scene" class="toolbar-button btn btn btn-default spaced menu-button" onclick="pauseStep()"></button>
					
					<button id="vds-stopStep" title="Stop" class="toolbar-button btn btn btn-default spaced menu-button" onclick="stopStep()"></button>
					
					<!--<button id="vds-playAll" title="Play All" class="toolbar-button btn btn btn-default spaced menu-button" onclick="playAll()"></button>-->
					<% if (showAnyButton) { %>					
					<div class="tool-seperator "></div>
					<% }
					if (showPrint) { %>					
					<button id="rms-print-button" title="Print" onclick="printModel(metaData)" class="toolbar-button btn btn-default spaced print-enabled dont-show-toolbar-vds" disabled="disabled"> </button>
					<% }
					if (showFileInfo) { %>
					<button id="rms-info-button" type="button" data-ng-click="onClickInfo(metaData)" class="toolbar-button info btn btn-default spaced" title="View File Info" disabled="disabled"> </button>
					<% }
					if (showProtect) { %>
					<button id="rms-protect-button" type="button" data-ng-click="onClickProtect()" class="toolbar-button protect btn btn-default spaced" title="Protect File" disabled="disabled"></button>
					<% }
					if (showShare) { %>
					<button id="rms-share-button" type="button" data-ng-click="onClickShare()" class="toolbar-button info btn btn-default spaced" title="Share File" disabled="disabled"> </button>
					<% }
					if (showDownload) { %>
					<button id="rms-download-button" type="button" onclick="downloadFile(metaData)" class="toolbar-button download btn btn-default spaced" title="Download File" disabled="disabled"></button>
					<% } 
					if (showDecrypt) { %>
					<button id="rms-extract-button" type="button" onclick="decryptFile(metaData)" class="toolbar-button extract btn btn-default spaced" title="Extract Content" disabled="disabled"></button> 
					<% } %>
				</div>				

			</div>
		</div>
		
		<div id="printTemp" style="visibility: hidden"></div>
		<div id="content" style="text-align: center;">
				<canvas id="canvas" style="border: 1px solid #000; touch-action: none; transform: scale(1) translate(0px, 0px); user-select: none; visibility: visible;" oncontextmenu="event.preventDefault()">
    			</canvas>
    			<p>The rendering can be moved by clicking both left and right mouse-buttons and dragging the object</p>
		</div>				
	</div>
	<script>
			// HELPER: Get parameter from URL
	        function getUrlParams() {
	            var params = [], paramPair;
	            var parameters = decodeURIComponent(window.location.href.slice(window.location.href.indexOf('?') + 1));
	            var paramStrings=parameters.split('&');
	            for (var i = 0; i < paramStrings.length; i++) {
	                paramPair = paramStrings[i].split('=');
	                params.push(paramPair[0]);
	                params[paramPair[0]] = paramPair[1];
	            }
	            return params;
	        }
	    </script>
    	<script type="text/javascript" defer>			
			var params = getUrlParams();
			var sidePanelWidth = 250;
        	var verticalSpacerWidth = 10;
        	var approxMargin = 8;
        	var bLoggingActive = false;
        	var advanced = params["advanced"] ? params["advanced"] == "true" : false;   
        	var totalMemory=(64 * 1024 * 1024);
			var mem = params["totalMemory"] ? totalMemory = params["totalMemory"] * 2097152 : null; // increase now for iPAD2, before memory number was 1048576     	
			var url;
        	var metaData;
        	var fromTheBeginning=false;
				        
	        $(window).resize(function() {	            
	            addWaterMark();
	        });
	        $(window).bind( 'orientationchange', function(e){
	            addWaterMark();
	            closeViewerDialog();
	        });
	        $(document).ready(function(){
	            checkWebGL();
	            checkAuth();
	        });

	        var checkAuth = function(){
				$.blockUI({ message: '<img src="${pageContext.request.contextPath}/ui/img/loading_48.gif" />' ,
							css: { width: '4%', textAlign:  'center',left:'50%',border:'0px solid #FFFFFF',cursor:'wait',backgroundColor:'#FFFFFF'},
							overlayCSS:  { backgroundColor: '#FFFFFF',opacity:1.0,cursor:'wait'} 
	            });  
	            $("#pageWrapper").hide();
				
	            var docId = getDocId();
	    	    var s = getSessionId();
	    	    var source = getViewSource();
	    	    $.post('/viewer/RMSViewer/GetDocMetaData', {documentId: docId,s: s, source: source}, function(data, status){
	                if(data.errMsg && data.errMsg.length>0){
	                    window.location.href = 'ShowError.jsp?errMsg='+data.errMsg;
	                    return;
	                }
	                metaData=data;
					checkPrintEnabled(metaData);
					checkFileInfoEnabled(metaData);
					checkDownloadEnabled(metaData);
					checkDecryptEnabled(metaData);
					checkShareEnabled(metaData);
					checkProtectEnabled(metaData); 
	                initialize();
	            });
				
	        }

		    // Init
			var oDvl = sap.ve.dvl.createRuntime({ totalMemory: 128 * 1024 * 1024 }); // 128 MB
			oDvl.CreateCoreInstance("SAPViewer");
			oDvl.Core.Init(oDvl.Core.GetMajorVersion(), oDvl.Core.GetMinorVersion());

			//Setup canvas and activate DVL automatically
			var devicePixelRatio = window.devicePixelRatio || 1;			

			var canvas = document.getElementById("canvas");
			canvas.width = 1400 * devicePixelRatio;
			canvas.height = 700 * devicePixelRatio;

			var webGLContext = oDvl.Core.CreateWebGLContext(canvas, { antialias: true, alpha: true, preserveDrawingBuffer: true });
			// preserveDrawingBuffer required as canvas needs to have the data while printing

			// Renderer
			oDvl.Core.InitRenderer();
			oDvl.Renderer.SetDimensions(canvas.width, canvas.height);
			oDvl.Renderer.SetBackgroundColor(0.2, 0.2, 0.2, 1, 0.8, 0.8, 0.8, 1);

			oDvl.Renderer.SetOptionF(sap.ve.dvl.DVLRENDEROPTIONF.DVLRENDEROPTIONF_DPI, 96 * devicePixelRatio);
			oDvl.Core.StartRenderLoop();

			// Basic Gesture Handling using the Loco library
			var track = new sap.ve.Loco(oDvl, canvas, true);
			track.activeScaleRatio = devicePixelRatio;

			collectProcedures = function(sceneId) {
				while (proceduresList.options.length) {
					proceduresList.remove(0);
				}
				var ps = oDvl.Scene.RetrieveProcedures(sceneId);
				for(n = 0; n < ps.procedures.length; n++) {
					var proc = document.createElement("option");
					proc.text = ps.procedures[n].name;
					proc.id = ps.procedures[n].id;
					proc.steps = ps.procedures[n].steps;
					proceduresList.options.add(proc);
				}
				updateSteps();
			}

			updateSteps = function() {
				while (stepsList.options.length) {
					stepsList.remove(0);
				}
				if(proceduresList.options.length > 0) {
					var proc = proceduresList.options[proceduresList.options.selectedIndex];
					for(n = 0; n < proc.steps.length; n++) {
						var step = document.createElement("option");
						step.text = proc.steps[n].name;
						step.id = proc.steps[n].id;
						stepsList.options.add(step);
					}
				}
			}

			playStep = function() {
				if(stepsList.options.length > 0) {
					var step = stepsList.options[stepsList.options.selectedIndex];
					oDvl.Scene.ActivateStep(oDvl.Settings.LastLoadedSceneId, step.id, fromTheBeginning, false);
				}
			}

			pauseStep = function() {                
                if(stepsList.options.length > 0) {
                    var step = stepsList.options[stepsList.options.selectedIndex];
                    oDvl.Scene.PauseCurrentStep(oDvl.Settings.LastLoadedSceneId, step.id, true, false);
                    fromTheBeginning=false;
                }
            }

            stopStep = function() {                
                if(stepsList.options.length > 0) {
                    var step = stepsList.options[stepsList.options.selectedIndex];
                    oDvl.Scene.PauseCurrentStep(oDvl.Settings.LastLoadedSceneId, step.id, true, false);
                    fromTheBeginning=true;
                }
            }

            playAll = function() {       
                // not functional yet
            }

            resetStep = function() {                
                collectProcedures();
            }

			function LoadFileUrl(url) {
				if (url) {
					var xhr = new XMLHttpRequest();

					xhr.onerror = function(event) {
						//TODO: report error
						window.location.href = 'ShowError.jsp?code=err.file.processing';
						console.log("xhr error " + event);
						return;
					};

					xhr.onload = function(event) {

						if (xhr.status === 200) {
							addWaterMark();
							addViewerDialog();
							addErrorDiv();
							$("#pageWrapper").show();
							$.unblockUI();
							//setTimeout(checkSteps, 10);
							translateIfRequired(function callback(){
							   showShareIntro();
		                    });
							oDvl.Core.CreateFileFromArrayBuffer(xhr.response, url, "remote");
							var sceneId = oDvl.Core.LoadSceneByUrl(url, null, "remote");
							oDvl.Renderer.AttachScene(sceneId);
							collectProcedures(sceneId);
							oDvl.Scene.Release(sceneId);

						} else {
							// TODO: report error
							// xhr.status,
							// xhr.statusText
							window.location.href = 'ShowError.jsp?code=err.file.processing';
							console.log("xhr load error " + xhr.status);
							return;
						}
					};

					xhr.onprogress = function(event) {
						//	loaded: event.loaded,
						//	total: event.total
					};

					xhr.open("GET", url, true);
					xhr.responseType = "arraybuffer";
					xhr.send(null);
				}
			};

			function initialize(){
	        	var params=getUrlParams();			        			        		
	            url="/viewer/RMSViewer/GetFileContent?d=" + params.d + "&s=" + params.s;
	            addTitle();
	            var loadFile=setInterval(function () {
					LoadFileUrl(url);					
					clearInterval(loadFile);
				}, 3000);	            
	        }
	        
			function addErrorDiv() {
	            var errorDiv="<div id=\"error\" class=\"alert alert-danger alert-dismissable\" style=\"display:none\" ><button type=\"button\" class=\"close\" onclick=\"closeDialog()\" aria-hidden=\"true\">x</button><span id=\"errmsg\"></span></div>";
	            $("body").prepend(errorDiv);
	        }
			
	        function addStepsDiv(){
				var step=$("#__navigation0-stepscroller").clone();
				$(".toolbarContainer").prepend(step);
	        }    
		</script>
</body>
</html>
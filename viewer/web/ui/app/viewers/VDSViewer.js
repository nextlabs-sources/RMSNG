/*
    Code from previous SAP Viewer has not been removed yet - please find SAP Viewer code from July 2019 for prior fully working SAP vds viewer for older vds file formats.
    The previous SAP Viewer had a few more visual elements that were not migrated fully.
    In addition some of the buttons like Toggle, Play All, Pause have been commented out at present.
*/

function playall() {
    oStepNavigation.mProperties.settings.currentStepPaused = false;
    oStepNavigation.mProperties.settings.playAllActive = true;
    oViewer.playAllSteps();
}

function pauseStep() {
    oStepNavigation.mProperties.settings.currentStepPaused = true;
    oViewer.pauseStep();
}

function resetStep() {
    if (oStepNavigation.oModel.oData.procedures.length == 0) {
        oViewer.resetView();
    } else {
        oStepNavigation.mProperties.settings.playAllActive = false;
        oStepNavigation.mProperties.settings.currentStepPaused = false;
        var firstStepId = oStepNavigation.oModel.oData.procedures[0].steps[0].id;
        oViewer.playStep(firstStepId, !oStepNavigation.mProperties.settings.currentStepPaused, oStepNavigation.mProperties.settings.playAllActive);
    }
}

function play() {
    oViewer.playStep(oStepNavigation.mProperties.settings.currentStepId, !oStepNavigation.mProperties.settings.currentStepPaused, oStepNavigation.mProperties.settings.playAllActive);
    oStepNavigation.mProperties.settings.currentStepPaused = false;
}

function toggleStepInFo() {
    if (oStepNavigation.mProperties.showStepInfo) {
        oStepNavigation.mProperties.showStepInfo = false;
        $("#vds-toggleStepInFo").addClass("infoHide");
        $(".sapUiShd").css("visibility", "hidden");
    } else {
        $("#vds-toggleStepInFo").removeClass("infoHide");
        oStepNavigation.setShowStepInfo();
    }
}

function checkSteps() {
    if (oStepNavigation.oModel.oData.procedures.length == 0) {
        $("#vds-toggleStepInFo").addClass("disabled");
        $("#vds-play").addClass("disabled");
        $("#vds-pauseStep").addClass("disabled");
        $("#vds-playAll").addClass("disabled");
    }
}

/*
	Created webGL context with parameter preserveDrawingBuffer otherwise canvas's toDataURL() returns blank image
	https://forum.unity.com/threads/webgl-print-canvas.377045/#post-2445444
*/
function printModel(metaData) {
    var userId = getUserId();
    var ticket = getTicket();
    $("#printTemp").empty();
    var overlay = $(".canvas").clone();
    $("#printTemp").prepend(overlay);
    var dataUrl = document.getElementById("canvas").toDataURL();
    var windowContent = '<div id="image">' + document.getElementById("printTemp").innerHTML + '<img src="' + dataUrl + '"></div>';
    handleError('Please close the print window to proceed.');
    sendActivityLog(metaData, ticket, userId);
    $('#pageWrapper').block({
        message: '',
        overlayCSS: {
            backgroundColor: '#fff'
        }
    });
    var printWin = window.open('', 'Print-Window', 'width=1000,height=1000');
    printWin.document.open();
    printWin.document.write('<html><head><meta name="format-detection" content="date=no"><meta name="format-detection" content="address=no"><meta name="format-detection" content="telephone=no"></head><body onload="window.print()">' + windowContent + '</body></html>');
    printWin.document.close();
    setTimeout(function() {
        printWin.close();
        $("#printTemp").empty();
        closeDialog();
        $('#pageWrapper').unblock();
    }, 100);
}

function addViewerDialog() {
    $("body").prepend('<div id="viewer-dialog"></div>');
    viewer_dialog = $('#viewer-dialog').dialog({
        autoOpen: false,
        dialogClass: 'no-close ui-dialog-titlebar-close custom-viewer-dialog',
        width: 600,
        height: $(window).height() - 80,
        position: {
            my: 'right top',
            at: 'right top+80px',
            of: $(window)
        }
    });

    // fix for VDS files without animations (Bug 36384: OK button not working on share modal)
    $("#ViewerSampleLayout").resize();
}

function addWaterMark() {
    var body = document.body,
        html = document.documentElement;
    var height = $("#pageWrapper").height();
    var width = $("#pageWrapper").width();
    console.log("height=" + height + ", width=" + width);
    var diagonal = Math.sqrt(width * width + height * height);
    var watermark = metaData.watermark;
    if (watermark == null)
        return;
    var overlay = watermark.waterMarkStr;
    var split = watermark.waterMarkSplit;
    overlay = htmlEntityEncode(overlay);
    overlay = overlay.replace(/\n/gi, "<br/>");
    var lines = overlay.split("<br/>");
    var i, j, maxLineLength = 0;
    for (i = 0; i < lines.length; i++) {
        maxLineLength = Math.max(maxLineLength, lines[i].length);
    }
    var fontsize = watermark.waterMarkFontSize;
    var fontcolor = watermark.waterMarkFontColor;
    var transparency = watermark.waterMarkTransparency / 100;
    var fontname = watermark.waterMarkFontName;
    var density = watermark.waterMarkDensity;
    var rotation = watermark.waterMarkRotation;
    var watermarkWidth = 0;
    var watermarkHeight = 0;
    var watermarkHeightOffset = 0;
    var watermarkWidthOffset = 0;

    if (!new Detector().detect(fontname)) {
        fontname = "Arial";
    }
    if (density == "Dense") {
        watermarkHeightOffset = 150;
        watermarkWidthOffset = 150;
    } else {
        watermarkHeightOffset = 200;
        watermarkWidthOffset = 200;
    }
    watermarkWidthOffset = watermarkWidthOffset * fontsize / 9; //this was changed from 36 to 9 as it seemed to fix the issue
    watermarkHeightOffset = watermarkHeightOffset * fontsize / 36;
    var tempDiv = "<div  class=\"watermark\" style=\"font-size:" + fontsize + "px;color:" + fontcolor + ";position:absolute;pointer-events: none;z-index:1000;white-space:nowrap;opacity:" + transparency + "; font-family:" + fontname + ";\"><p><span style=\"display:inline-block; text-align:center\" id=\"Test\">" + overlay + "</span></p></div>";
    var rotation_css = "-ms-transform-origin:0px 0px; -webkit-transform-origin:0px 0px; -moz-webkit-transform-origin:0px 0px; transform-origin:0px 0px;";
    if (rotation == "Clockwise") {
        var translateX = watermarkHeight + 15;
        rotation_css = rotation_css + "-ms-transform: translate(" + translateX + "px,0px) rotate(45deg); -webkit-transform: translate(" + translateX + "px,0px) rotate(45deg); -moz-webkit-transform: translate(" + translateX + "px,0px) rotate(45deg); transform: translate(" + translateX + "px,0px) rotate(45deg);";
    } else {
        var translateX = -watermarkHeight + 15;
        var translateY = height;
        rotation_css = rotation_css + "-ms-transform: translate(" + translateX + "px," + translateY + "px) rotate(-45deg); -webkit-transform: translate(" + translateX + "px," + translateY + "px) rotate(-45deg); -moz-webkit-transform: translate(" + translateX + "px," + translateY + "px) rotate(-45deg); transform: translate(" + translateX + "px," + translateY + "px) rotate(-45deg);";
    }

    $("#waterMarkContainer").remove();
    var waterMarkContainer = "<div id=\"waterMarkContainer\" style=\"display:inline-block;\"> </div>";
    var waterMarkRow = "<div id=\"waterMarkRow\" style=\"white-space:nowrap;  z-index: 1000; position:absolute; display:inline-block;" + rotation_css + "; font-family:" + fontname + "\"> </div>";
    waterMarkContainer = $(waterMarkContainer).clone();
    $(".pageWrapper").prepend(waterMarkContainer);
    $(".pageWrapper").prepend($(waterMarkRow).clone());

    for (var j = 0; j < diagonal + 500; j = j + watermarkWidth + watermarkWidthOffset) {
        $("#waterMarkRow").append($(tempDiv).clone().css({
            'margin-top': '0px',
            'margin-left': j + 'px'
        }));
        if (watermarkHeight == 0) {
            watermarkHeight = $("#waterMarkRow")[0].children[0].clientHeight;
            watermarkWidth = $("#waterMarkRow")[0].children[0].clientWidth;
        }
    }
    for (var j = 0; j < diagonal + 500; j = j + watermarkWidth + watermarkWidthOffset) {
        $("#waterMarkRow").append($(tempDiv).clone().css({
            'margin-left': j + 'px'
        }));
    }
    for (var i = -diagonal; i < diagonal + 500; i = i + watermarkHeight + watermarkHeightOffset) {
        waterMarkRow = $("#waterMarkRow").clone().css('margin-top', i + 'px');
        $(waterMarkContainer).append(waterMarkRow);
    }

    waterMarkContainer.css({
        'height': height + "px",
        'position': 'fixed',
        'z-index': '1000'
    });
    $("#waterMarkRow").remove();
}

function addTitle() {
    $(".sapVeViewerStepNavigationToolbar").hide();
    var TITLE_MAX_LENGTH_DESKTOP = 50;
    var TITLE_MAX_LENGTH_MOBILE = 15;
    var fileName = metaData.originalFileName;
    var titleDesktopSpan = document.getElementById('titleDesktop');
    var titleMobileSpan = document.getElementById('titleMobile');
    titleDesktopSpan.innerHTML += getShortName(fileName, TITLE_MAX_LENGTH_DESKTOP);
    titleMobileSpan.innerHTML += getShortName(fileName, TITLE_MAX_LENGTH_MOBILE);
    $("#titleDesktop").attr("title", fileName);
    $("#titleMobile").attr("title", fileName);
    $('[data-toggle="tooltip"]').tooltip();
}

function getFileName(name) {
    var pathArray = name.split("/");
    var result = pathArray[pathArray.length - 1]
    if (result == null)
        return null;
    else
        return decodeURIComponent(result.replace(/\+/g, " "));
}

function checkWebGL() {
    if (!window.WebGLRenderingContext) {
        // the browser doesn't even know what WebGL is
        window.location.href = '/viewer/ShowError.jsp?errMsg=Your browser does not support webGL. This is required for visualizing CAD files in the web browser.';
    } else {
        var canvas = document.createElement('canvas');
        var context = canvas.getContext("webgl") || canvas.getContext('experimental-webgl');
        if (!context) {
            // browser supports WebGL but initialization failed.
            window.location.href = '/viewer/ShowError.jsp?errMsg=Please enable <a href="https://get.webgl.org/"> WebGL</a> in your web browser.';
        }
    }
}

function closeDialog() {
    document.getElementById("error").style.display = "none";
    document.getElementById("viewer-dialog").style.color = "";
    document.getElementById("error").style.zIndex = '1';
    //document.getElementById("ViewerSampleControls").style.opacity = 1.0;
}

function handleError(message) {
    if (document.getElementById("ViewerSampleControls") != undefined) {
        document.getElementById("ViewerSampleControls").style.opacity = 0.4;
        document.getElementById("errmsg").innerHTML = message;
        document.getElementById("error").style.display = 'block';
        $("#error").addClass("alert-danger");
        $("#error").removeClass("alert-success");
        if (document.getElementById("viewer-dialog") != undefined) {
            document.getElementById("viewer-dialog").style.color = "rgba(0,0,0,0.4)";
            document.getElementById("error").style.zIndex = '40002';
        } else {
            closeViewerDialog();
        }
    }
}

function displayMessage(message) {
    if (document.getElementById("ViewerSampleControls") != undefined) {
        document.getElementById("ViewerSampleControls").style.opacity = 0.4;
        document.getElementById("errmsg").innerHTML = message;
        document.getElementById("error").style.display = 'block';
        $("#error").removeClass("alert-danger");
        $("#error").addClass("alert-success");
        closeViewerDialog();
    }
}

function translateCallback(data) {
    $("#rms-print-button").attr('title', data['viewer.toolbar.print']);
    $("#rms-help-button").attr('title', data['viewer.toolbar.help']);
    $("#rms-download-button").attr('title', data['viewer.toolbar.download']);
    $("#rms-protect-button").attr('title', data['viewer.toolbar.protect']);
    $("#rms-share-button").attr('title', data['viewer.toolbar.share']);
    $("#rms-info-button").attr('title', data['viewer.toolbar.info']);
    $("#vds-toggleStepInFo").attr('title', data['viewer.toolbar.vds.showStep']);
    $("#vds-resetStep").attr('title', data['viewer.toolbar.vds.resetStep']);
    $("#vds-play").attr('title', data['viewer.toolbar.vds.play']);
    $("#vds-pauseStep").attr('title', data['viewer.toolbar.vds.pauseStep']);
    $("#vds-playAll").attr('title', data['viewer.toolbar.vds.playAll']);
}
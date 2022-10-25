var metaData;
var headerToolbarHeight;

$(document).ready(function() {
    addErrorDiv();
    modifyTOCLinks();
    getMetaData();
    $("ul.ISYS_TOC").addClass("fade-div");
    $(".toolbarContainerPlaceholder").css({
        "background-color": "white",
        "border-bottom": "3px solid white !important"
    });
    $("ul.ISYS_TOC").css({
        "height": "auto"
    });
    headerToolbarHeight = $(".cc-header").height() + $(".toolbarContainer").height() + $("ul.ISYS_TOC").height();
    
    $(".ISYS_BODY_DIV").css({
        'top': headerToolbarHeight + "px",
        'position': 'relative',
        'overflow': 'visible',
        'display': 'inline-block'
    });

    if ($(".ISYS_BODY_DIV")[0]) {
        var height = $(".ISYS_BODY_DIV")[0].clientHeight;
        var width = $(".ISYS_BODY_DIV")[0].clientWidth;
    } else {
        var height = $("body").clientHeight;
        var width = $("body").clientWidth;
    }
    $("#watermark").css("min-width", width);
    $("#watermark").css("min-height", height);
    $("#watermark").css({'top': headerToolbarHeight + "px", 'display': 'block'});
    
});


$(window).bind('orientationchange', function(e) {
    closeViewerDialog();
});

$(window).on("hashchange", function(e) {
    var height = $("#pageWrapper").scrollTop() - headerToolbarHeight - 5;
    $("#pageWrapper").scrollTop(height);
});

function translateCallback(data) {
    $("#rms-print-button").attr('title', data['viewer.toolbar.print']);
    $("#rms-help-button").attr('title', data['viewer.toolbar.help']);
    $("#rms-download-button").attr('title', data['viewer.toolbar.download']);
    $("#rms-extract-button").attr('title', data['viewer.toolbar.extract']);
    $("#rms-protect-button").attr('title', data['viewer.toolbar.protect']);
    $("#rms-share-button").attr('title', data['viewer.toolbar.share']);
    $("#rms-info-button").attr('title', data['viewer.toolbar.info']);
}

function addErrorDiv() {
    $('body').prepend("<div id=\"error\" class=\"alert alert-danger alert-dismissable\" style=\"display:none; top:55%;\"><button type=\"button\" class=\"close\" onclick=\"closeDialog()\" aria-hidden=\"true\">x</button><span id=\"errmsg\"></span></div>");
}

function modifyTOCLinks(){
    $("div#rms-viewer-content > ul.ISYS_TOC").first().children().each(function(idx, sheet){
        var anchor = $(sheet).find('a:first');
        if($(anchor).attr('href') && !$(anchor).attr('target')) {
            $(anchor).attr('target', '_self');  //https://stackoverflow.com/a/31505668
        }
    });
}


function getMetaData() {
    var docId = getDocId();
    var user = $("div#user").attr("value");
    var viewingSession = getSessionId();
    var source = getViewSource();
    $.get('/viewer/RMSViewer/GetDocMetaData?documentId=' + docId + "&s=" + viewingSession + "&source=" + source, function(data, status) {
        if (data.errMsg && data.errMsg.length > 0) {
            window.location.href = '/viewer/ShowError.jsp?errMsg=' + data.errMsg;
            return;
        }
        metaData = data;
        $("#titleDesktop").text(getShortName(metaData.originalFileName, TITLE_MAX_LENGTH_DESKTOP));
        $("#titleDesktop").attr("title", metaData.originalFileName);
        $("#titleMobile").text(getShortName(metaData.originalFileName, TITLE_MAX_LENGTH_MOBILE));
        $("#titleMobile").attr("title", metaData.originalFileName);
        $('[data-toggle="tooltip"]').tooltip();
        checkOperations();
        checkPrintEnabled(metaData);
        checkShareEnabled(metaData);
        checkFileInfoEnabled(metaData);
        checkDownloadEnabled(metaData);
        checkDecryptEnabled(metaData);
        checkProtectEnabled(metaData);
        $('#loading').hide();
        translateIfRequired(function callback(){
            showShareIntro();
        });
    });
}

function printFile() {
    sendActivityLog(metaData, getTicket(), getUserId());
    window.scrollTo(0, 0); //move the viewport to origin
    window.print();
}

function checkOperations() {
    var operations = getParameterByName('operations');
    if (operations) {
    	(operations & 1) == 0 ? $('#rms-info-button').remove() : $('#rms-info-button').show();
    	(operations & 2) == 0 ? $('#rms-print-button').remove() : $('#rms-print-button').show();
    	(operations & 4) == 0 ? $('#rms-protect-button').remove() : $('#rms-protect-button').show();
    	(operations & 8) == 0 ? $('#rms-share-button').remove() : $('#rms-share-button').show();
    	(operations & 16) == 0 ? $('#rms-download-button').remove() : $('#rms-download-button').show();
        (operations & 32) == 0 ? $('#rms-extract-button').remove() : $('#rms-extract-button').show();
    } else {
    	$('#toolBar button').show();
    }
}

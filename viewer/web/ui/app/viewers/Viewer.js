var i18n_language = "en";
var i = null;
var prevPos = "";
var mouseOnToolbar = false;
var i18n_data;
var viewer_dialog;
var modalCompleted = false;
var TITLE_MAX_LENGTH_DESKTOP = 50;
var TITLE_MAX_LENGTH_MOBILE = 15;

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(decodeURIComponent(window.location.href));
    if (results == null)
        return "";
    else
        return decodeURIComponent(results[1].replace(/\+/g, " "));
}

function generateTagListFromTagMap(tags) {
    var tagList = {};
    if (tags != null) {
        Object.keys(tags).forEach(function(key, index) {
            var value = "";
            var values = tags[key];
            for (var i = 0; i < values.length; i++) {
                value += values[i];
                if (i != values.length - 1) {
                    value += ",";
                }
            }
            tagList[key] = value;
        });
    }
    return tagList;
}

/*
taken from https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
*/
function htmlEntityEncode(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#x27;').replace(/\//g, '&#x2F;');
}


function downloadRepoFile(rmsUrl, repoType, repoId, filePathId, filePath, isNXL, fileName) {
    if (isNXL) {
        if (isMyVault(repoType, isNXL)) {
            downloadMyVaultToLocal(rmsUrl, filePathId, fileName);
        } else if (isPersonalRepo(repoType)) {
            downloadRepoFileToLocal(rmsUrl, filePathId, repoType, repoId, fileName);
        } else if (isSharedWorkspace(repoType)) {
            downloadRepoFileToLocal(rmsUrl, filePath, repoType, repoId, fileName);
        }
        return;
    }

    window.open(rmsUrl + "/RMSViewer/DownloadFile?filePath=" + encodeURIComponent(filePathId) +
        "&filePathDisplay=" + encodeURIComponent(filePath) +
        "&repoType=" + encodeURIComponent(repoType) +
        "&repoId=" + encodeURIComponent(repoId));
}

function downloadMyVaultToLocal(rmsUrl, filePathId, fileName) {
    var params = {
        parameters: {
            src: {
                filePathId: filePathId,
                spaceType: "MY_VAULT",
            },
            dest: {
                spaceType: "LOCAL_DRIVE"
            },
            overwrite: true
        }
    };
    transferNXLToLocalDrive(rmsUrl, params, fileName);
}

function downloadRepoFileToLocal(rmsUrl, filePathId, repoType, repoId, fileName) {
    var params = {
        parameters: {
            src: {
                filePathId: filePathId,
                spaceType: repoType,
                spaceId: repoId,
            },
            dest: {
                spaceType: "LOCAL_DRIVE"
            },
            overwrite: true
        }
    };
    transferNXLToLocalDrive(rmsUrl, params, fileName);
}

function downloadSharedWithMeToLocal(rmsUrl, transactionId, fileName) {
    var params = {
        parameters: {
            src: {
                transactionId: transactionId,
                spaceType: "SHARED_WITH_ME"
            },
            dest: {
                spaceType: "LOCAL_DRIVE"
            },
            overwrite: true
        }
    };
    transferNXLToLocalDrive(rmsUrl, params, fileName);
}

// private function, download from source to local drive
function transferNXLToLocalDrive(rmsUrl, params, fileName) {
    $("#loading").show();
    var xhttp = new XMLHttpRequest();
    xhttp.open('POST', rmsUrl + '/rs/transform/transfer', true);
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            $("#loading").hide();
            if (this.status == 200) {
                if (arrayBufferIsJsonResp(this.response)) {
                    var jsonResponse = arrayBufferToObj(this.response);
        
                    showSnackbar({
                        isSuccess: false,
                        messages: i18n_data['repo.file.download.error'] + ' ' + jsonResponse.message
                    });
                    return;
                }
        
                downloadDataAsFile(this.response, fileName);
                return;
            }
            showSnackbar({
                isSuccess: false,
                messages: i18n_data['repo.file.download.error'] + ' Error ' + this.status.toString()
            });        
        }
    };
    xhttp.responseType = 'arraybuffer';
    xhttp.setRequestHeader('Content-Type', 'application/json');
    xhttp.setRequestHeader('Accept', 'application/json, application/octet-stream');
    xhttp.setRequestHeader('userId', readCookie('userId'));
    xhttp.setRequestHeader('clientId', readCookie('clientId'));
    xhttp.setRequestHeader('ticket', readCookie('ticket'));
    xhttp.setRequestHeader('platformId', readCookie('platformId'));
    xhttp.send(JSON.stringify(params));
}
// private function ends

function downloadProjectFile(rmsUrl, projectId, filePathId) {
    window.open(rmsUrl + "/RMSViewer/DownloadFileFromProject?pathId=" + encodeURIComponent(filePathId) +
        "&projectId=" + encodeURIComponent(projectId));
}

function downloadWorkspaceFile(rmsUrl, filePathId) {
    window.open(rmsUrl + "/RMSViewer/DownloadFileFromWorkspace?pathId=" + encodeURIComponent(filePathId));
}


function decryptProjectFile(rmsUrl, projectId, filePathId) {
    window.open(rmsUrl + "/RMSViewer/DownloadFileFromProject?pathId=" + encodeURIComponent(filePathId) +
                "&projectId=" + encodeURIComponent(projectId) + "&decrypt=true");
}

function decryptWorkspaceFile(rmsUrl, filePathId) {
    window.open(rmsUrl + "/RMSViewer/DownloadFileFromWorkspace?pathId=" + encodeURIComponent(filePathId) + "&decrypt=true");
}

function downloadFileSharedWithProject(rmsUrl, transactionId, transactionCode, spaceId) {
    window.open(rmsUrl + "/RMSViewer/DownloadSharedWithMeFile?transactionId=" + encodeURIComponent(transactionId) +
        "&transactionCode=" + encodeURIComponent(transactionCode) +
        "&spaceId=" + encodeURIComponent(spaceId));
}

function decryptFileSharedWithProject(rmsUrl, transactionId, transactionCode, spaceId) {
    window.open(rmsUrl + "/RMSViewer/DownloadSharedWithMeFile?transactionId=" + encodeURIComponent(transactionId) +
        "&transactionCode=" + encodeURIComponent(transactionCode)+
        "&spaceId=" + encodeURIComponent(spaceId) + "&decrypt=true");
}

// for use with tranform NXL API
function downloadDataAsFile(data, fileName) {
    var newFileName = fileName;
    if (!newFileName) {
        newFileName = 'new-file';
    }
    var blob = new Blob([data], { type: 'application/octet-stream' });
    
    // IE11 fallback
    if (navigator.msSaveOrOpenBlob) {
        navigator.msSaveOrOpenBlob(blob, newFileName);
        return;
    }

    var link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(link.href);
}

function arrayBufferIsJsonResp(buffer) {
    return arrayBufferToStr(buffer).indexOf('statusCode') > -1;
}

function arrayBufferToStr(buffer) {
    // IE11 fallback
    if (typeof(TextEncoder) == 'undefined') {
        var newString = '';
        var bytes = new Uint8Array(buffer);
        var len = bytes.byteLength;
        for (var i = 0; i < len; i++) {
            newString += String.fromCharCode(bytes[i]);
        }
        return newString;
    }

    return new TextDecoder().decode(buffer);
}

function arrayBufferToObj(buffer) {
    return JSON.parse(arrayBufferToStr(buffer));
}
// transform NXL API helpers end

// file space checks
function isMyVault(repoType, isNXL) {
    return isNXL && (repoType == undefined || repoType == 'S3' || repoType == 'LOCAL_DRIVE');
}

function isPersonalRepo(repoType) {
    var personalRepos = [
        'GOOGLE_DRIVE',
        'ONE_DRIVE',
        'BOX',
        'DROPBOX'
    ];

    return repoType && personalRepos.indexOf(repoType) > -1;
}

function isSharedWorkspace(repoType) {
    var sharedWorkspaces = [
        'SHAREPOINT_ONLINE'
    ];

    return repoType && sharedWorkspaces.indexOf(repoType) > -1;
}
// file space checks end

function handleError(message) {
    if (document.getElementById("rms-main-view") != undefined) {
        document.getElementById("rms-main-view").style.opacity = 0.4;
        document.getElementById("errmsg").innerHTML = message;
        document.getElementById("error").style.display = 'block'
        document.getElementById("error").style.position = 'fixed';
    }
}

function closeDialog() {
    document.getElementById("error").style.display = "none";
    document.getElementById("rms-main-view").style.opacity = 1.0;
}

function openCenteredPopup(url, winName, w, h, scroll) {
    LeftPosition = (screen.width) ? (screen.width - w) / 2 : 0;
    TopPosition = (screen.height) ? (screen.height - h) / 2 : 0;
    settings = 'height=' + h + ',width=' + w + ',top=' + TopPosition + ',left=' + LeftPosition + ',scrollbars=' + scroll + ',resizable';
    popupWindow = window.open(url, winName, settings);
    popupWindow.focus();
}

/*
taken from http://stackoverflow.com/q/5639346/404165 and modified
*/
(function() {

    function readCookie(name, c, C, i) {

        c = document.cookie.split('; ');
        var cookies = {};

        for (i = c.length - 1; i >= 0; i--) {
            C = c[i].split('=');
            cookies[C[0]] = C[1];
        }

        return cookies[name] ? decodeURIComponent(cookies[name]) : cookies[name];
    }

    window.readCookie = readCookie; // or expose it however you want
})();

/**
 *  isSuccess: true for success message & false for error message
 *  messages: a list of messages
 *  duration: how long to dismiss the snackbar
 *  linkCallback: click callback for the link message
 */
function showSnackbar(params) {
    if (params) {
        if ($('#rms-snackbar')) {
            $('#rms-snackbar').remove();
        }
        var defaultHTML = '<div id="rms-snackbar" class="%CLASS%"><div>%MESSAGE%</div><button type="button" class="close" onclick="dismissSnackbar()">x</button></div>';
        var message = params.messages instanceof Array ? params.messages.join('<br>') : params.messages;
        var snackbarHTML = defaultHTML.replace('%CLASS%', params.isSuccess ? 'success' : 'error').replace('%MESSAGE%', message);
        $('body').append(snackbarHTML);
        var left = ($("body").width() - $('#rms-snackbar').width() - 16) / 2;
        $('#rms-snackbar').css('left', left);
        if (params.linkCallback) {
            $('#link').click(params.linkCallback);
        }
        var snackbar = $('#rms-snackbar');
        snackbar.addClass('display');
        setTimeout(function () {
            snackbar.removeClass('display');
        }, params.duration ? params.duration : 10000);
    }
}

function dismissSnackbar() {
    $('#rms-snackbar').remove();
}

// fix for viewport scaling bug on iOS
// http://webdesignerwall.com/tutorials/iphone-safari-viewport-scaling-bug
(function(doc) {
    var isiPad = navigator.userAgent.match(/iPad/i) != null;
    if (isiPad) {
        var addEvent = 'addEventListener',
            type = 'gesturestart',
            qsa = 'querySelectorAll',
            scales = [1, 1],
            meta = qsa in doc ? doc[qsa]('meta[name=viewport]') : [];

        function fix() {
            meta.content = 'width=device-width,minimum-scale=' + scales[0] + ',maximum-scale=' + scales[1];
            doc.removeEventListener(type, fix, true);
        }

        if ((meta = meta[meta.length - 1]) && addEvent in doc) {
            fix();
            scales = [.25, 1.6];
            doc[addEvent](type, fix, true);
        }
    }

}(document));

var translateIfRequired = function(callback){
    if(i18n_data) {
        return callback();
    }
    $.getJSON("/viewer/ui/app/i18n/" + i18n_language + ".json?v=" + version).done(function(data) {
        i18n_data = data;
        if (typeof translateCallback === 'function'){
            translateCallback(data);
        }
        if (typeof callback === 'function'){
            callback();
        }
    });
} 

$(document).ready(function() {
    translateIfRequired();
    window.onbeforeunload = function() {
        var sessionId = getSessionId();
        var docId = getDocId();
        $.ajax({
            type: 'GET',
            url: '/viewer/RMSViewer/RemoveFromCache?documentId=' + docId + '&s=' + sessionId,
            async: false
        });
    }

    $(".pageWrapper").mousemove(function(e) {
        var currPos = e.pageX + '-' + e.pageY;
        if (prevPos != currPos && !mouseOnToolbar) {
            clearTimeout(i);
            $(".fade-div").fadeIn(500);
            i = setTimeout(function() {
                $(".fade-div").fadeOut(500);
            }, 3000);
            prevPos = currPos;
        }
    });

    $(".fade-div").mouseover(function(e) {
        mouseOnToolbar = true;
        clearTimeout(i);
    }).mouseleave(function(e) {
        mouseOnToolbar = false;
        clearTimeout(i);
    });

    $(document).mousedown(function(event) {
        var target = event.target ? event.target : window.event.srcElement;
        if ($(target).is("#viewer-dialog *")) {
            return;
        }
    });

    $(document).keyup(function(e) {
        if (e.keyCode == 27) { // escape key maps to keycode `27`
            closeViewerDialog();
        }
    });
    initViewerDialog();
});

$(window).resize(function() {
    if (!$('#rms-viewer-confirm-dialog').is(':visible')) {
        initViewerDialog();
    }
});

function showShareIntro() {
    if ($("#rms-share-button").length && !$("#rms-share-button:disabled").length) {
        showSnackbar({
            isSuccess: true,
            messages: i18n_data['tour.content.share']
        });
    }
}

function initViewerDialog() {
    viewer_dialog = $('#viewer-dialog').dialog({
        autoOpen: false,
        dialogClass: 'no-close ui-dialog-titlebar-close custom-viewer-dialog',
        width: 600,
        height: $(window).height() - 80,
        position: {
            my: 'right top',
            at: 'right top',
            of: $(window)
        }
    });
}

function showConfirmDialog(title, msg, showCheckbox, successCallback) {
    var titleDiv = title ? ("<div class='modal-header'><h3 class='modal-title'>" + title + "</h3></div>") : "";
    var msgDiv = msg ? ("<div class='modal-body'>" + msg + "</div>") : "";
    var checkBox = showCheckbox ? ("<label class='noselect' style='float: left; font-size: small; font-style: italic'><input id='confirm-dialog-checkbox' type='checkbox'>" + i18n_data['dont.show.again'] + "</label>") : "";
    var footerDiv = "<div class='modal-footer' style='border-top:none;'>" + checkBox +
        "<a id='confirm-dialog-cancel-btn' class='btn btn-default rms-settings-button' type='button' style=margin-bottom:0px;>" + i18n_data['cancel'] + "</a>" +
        "<a id='confirm-dialog-ok-btn' class='btn btn-default rms-settings-button rms-settings-button-color' type='button'>" + i18n_data['confirm'] + "</a><div>";

    var html = "<div id='rms-viewer-confirm-dialog' class='modal-dialog modal-content' style='margin: 30px auto; top:30px; border:0px;'>" + titleDiv + msgDiv + footerDiv + "</div>";

    var confirm_dialog = $('#viewer-dialog').dialog({
        autoOpen: false,
        dialogClass: 'no-close ui-dialog-titlebar-close custom-viewer-dialog',
        width: 600,
        height: "auto",
        resizable: false,
        position: {
            my: 'top',
            at: 'top',
            of: $(window)
        }
    });
    confirm_dialog.html(html);
    $('#confirm-dialog-ok-btn').click(function() {
        successCallback($('#confirm-dialog-checkbox').is(":checked"));
        initViewerDialog();
        closeViewerDialog();
    });
    $('#confirm-dialog-cancel-btn').click(function() {
        initViewerDialog();
        closeViewerDialog();
    });
    confirm_dialog.dialog('open');
}

function closeDialog() {
    document.getElementById("error").style.display = "none";
    document.getElementById("error").style.zIndex = '1';
    document.getElementById("rms-viewer-content").style.opacity = 1.0;
    document.getElementById("viewer-dialog").style.color = "";
}

function handleError(message) {
    if (document.getElementById("rms-viewer-content") != undefined) {
        document.getElementById("rms-viewer-content").style.opacity = 0.4;
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
    if (document.getElementById("rms-viewer-content") != undefined) {
        document.getElementById("rms-viewer-content").style.opacity = 0.4;
        document.getElementById("errmsg").innerHTML = message;
        document.getElementById("error").style.display = 'block';
        $("#error").removeClass("alert-danger");
        $("#error").addClass("alert-success");
        closeViewerDialog();
    }
}

function showHelp(url) {
    helpWindow = window.open(window.location.origin + url, "NextlabsViewerHelp");
    helpWindow.focus();
}

function checkFileInfoEnabled(metadata) {
    var el = $("#rms-info-button");
    if(metadata.isNXL) {
        el.removeClass("disabled");
        el.attr("disabled", false);
    } else {
        el.hide();
    }
}

function checkDownloadEnabled(metadata) {
    var el = $("#rms-download-button");
    if(metadata.isRepoReadOnly) {
        el.hide();
        return;
    }
    if (!isDownloadEnabled(metadata)) {
        el.addClass("disabled");
        el.attr("disabled", true);
        el.attr('onclick', null).off('click');
    } else {
        el.removeClass("disabled");
        el.attr("disabled", false);
    }
}

function checkShareEnabled(metadata) {
    var el = $("#rms-share-button");

    if (metadata.projectFile) {
        el.hide();
        return;
    }
    if (metadata.workspaceFile) {
        el.hide();
        return;
    }

    if (!isShareEnabled(metadata)) {
        el.addClass("disabled");
        el.attr("disabled", true);
        el.attr('onclick', null).off('click');
    } else {
        el.removeClass("disabled");
        el.attr("disabled", false);
    }
}

function checkDecryptEnabled(metadata) {
    var el = $("#rms-extract-button");
    if (!metadata.projectFile && !metadata.workspaceFile) {
        el.hide();
        return;
    }
    if (!isDecryptEnabled(metadata)) {
        el.addClass("disabled");
        el.attr("disabled", true);
        el.attr('onclick', null).off('click');
    } else {
        el.removeClass("disabled");
        el.attr("disabled", false);
    }
}

function checkProtectEnabled(metaData) {
    var el = $("#rms-protect-button");
    if (metaData.projectFile) {
        el.hide();
        return;
    }
    if (metaData.workspaceFile) {
        el.hide();
        return;
    }
    if (!isProtectEnabled(metaData)) {
        el.addClass("disabled");
        el.attr("disabled", true);
        el.attr('onclick', null).off('click');
    } else {
        el.removeClass("disabled");
        el.attr("disabled", false);
    }
}

function isLocalFile(metadata) {
    return ($.trim(metadata.repoId).length == 0 && $.trim(metadata.projectId).length == 0 && !metadata.workspaceFile) ||
        $.trim(metadata.filePath).length == 0 ||
        $.trim(metadata.filePathDisplay).length == 0;
}

function isProtectEnabled(metadata) {
    return !metadata.isNXL;
}

function isDownloadEnabled(metadata) {
    var rights = metadata.rights;
    var o = metadata.owner;
    if (metadata.transactionId == null && metadata.transactionCode == null) {
        return ((rights && $.isArray(rights) && rights.indexOf('DOWNLOAD') >= 0) || (o === true && !metadata.projectId && !metadata.workspaceFile) || (metadata.protectionType == 0 && o === true && metadata.projectId)) && !isLocalFile(metadata);
    } else {
        return ((rights && $.isArray(rights) && rights.indexOf('DOWNLOAD') >= 0) || (o === true && !metadata.projectId && !metadata.workspaceFile));
    }
}

function isShareEnabled(metadata) {
    var rights = metadata.rights;
    var o = metadata.owner;
    return ((o === true && !metadata.projectId && !metadata.workspaceFile) || (rights && $.isArray(rights) && rights.indexOf('SHARE') >= 0));
}

function isDecryptEnabled(metadata) {
    var rights = metadata.rights;
    var o = metadata.owner;

    if (metadata.transactionId == null && metadata.transactionCode == null) {
        return ((rights && $.isArray(rights) && rights.indexOf('DECRYPT') >= 0) || (o === true && !metadata.projectId && !metadata.workspaceFile) || (metadata.protectionType == 0 && o === true && metadata.projectId)) && !isLocalFile(metadata);
    } else {
        return (rights && $.isArray(rights) && rights.indexOf('DECRYPT') >= 0);
    }
}

function isPrintEnabled(metadata) {
    var rights = metadata.rights;
    var o = metadata.owner;
    return (o === true && !metadata.projectId && !metadata.workspaceFile) || (rights && $.isArray(rights) && rights.indexOf('PRINT') >= 0) || (metadata.protectionType == 0 && o === true && metadata.projectId);
}

function isWatermarkDisabled(metadata) {
    var rights = metadata.rights;
    var o = metadata.owner;
    return (rights && $.isArray(rights) && rights.indexOf('WATERMARK') < 0) || (o === true && !metadata.projectId && !metadata.workspaceFile) || $.trim(metadata.watermark.waterMarkStr).length == 0;
}

function checkPrintEnabled(metadata) {
    var el = $("#rms-print-button");
    if (!isPrintEnabled(metadata)) {
        el.addClass("disabled");
        el.attr("disabled", true);
        el.attr('onclick', null).off('click');
    } else {
        el.removeClass("disabled");
        el.attr("disabled", false);
    }
}

var promptProjDownload = readCookie("promptProjDownload") === "true";
var promptEWSDownload = readCookie("promptEWSDownload") === "true";

function decryptFile(metaData) {
    var filePath = metaData.filePath;
    var projectId = metaData.projectId;
    var rmsUrl = metaData.rmsURL;
    var transactionId = metaData.transactionId;
    var transactionCode = metaData.transactionCode;
    var isProjectFile = metaData.projectFile;

    if (transactionId != null && transactionCode != null) {
        if(isProjectFile){
            decryptFileSharedWithProject(rmsUrl, transactionId, transactionCode, projectId);
            return;
        }
    }
    if(metaData.workspaceFile){
        decryptWorkspaceFile(rmsUrl, filePath);
        return;
    }
    decryptProjectFile(rmsUrl, projectId, filePath);
}

function downloadFile(metaData) {
    var fileName = metaData.originalFileName;
    var filePath = metaData.filePath;
    var projectId = metaData.projectId;
    var isProjectFile = metaData.projectFile;
    var isWorkspaceFile = metaData.workspaceFile;
    var rmsUrl = metaData.rmsURL;
    var repoId = metaData.repoId;
    var repoType = metaData.repoType;
    var filePathDisplay = metaData.filePathDisplay;
    var transactionId = metaData.transactionId;
    var transactionCode = metaData.transactionCode;
    var isNXL = metaData.isNXL;
    if (transactionId != null && transactionCode != null) {
        if(isProjectFile){
            downloadFileSharedWithProject(rmsUrl, transactionId, transactionCode, projectId);
            return;
        } else {
            downloadSharedWithMeToLocal(rmsUrl, transactionId, fileName);
            return;
        }
    }
    if (isProjectFile) {
        if (promptProjDownload) {
            var msg = i18n_data['project.file.download.prompt'];
            var url = metaData.rmsURL + "/rs/usr/profile";
            showConfirmDialog(null, msg, true, function(data) {
                if (data == true) {
                    var params = {
                        parameters: {
                            preferences: {
                                disablePromptProjFileDownload: true
                            }
                        }
                    };
                    $.ajax({
                        url: url,
                        async: true,
                        cache: false,
                        type: 'POST',
                        data: JSON.stringify(params),
                        headers: getJsonHeaders()
                    }).done(function(data) {
                        promptProjDownload = false;
                    });
                }
                downloadProjectFile(rmsUrl, projectId, filePath);
            });
        } else {
            downloadProjectFile(rmsUrl, projectId, filePath);
        }
    } else if (isWorkspaceFile) {
        if (promptEWSDownload) {
            var msg = i18n_data['workspace.file.download.prompt'];
            var url = metaData.rmsURL + "/rs/usr/profile";
            showConfirmDialog(null, msg, true, function(data) {
                if (data == true) {
                    var params = {
                        parameters: {
                            preferences: {
                                disablePromptEWSFileDownload: true
                            }
                        }
                    };
                    $.ajax({
                        url: url,
                        async: true,
                        cache: false,
                        type: 'POST',
                        data: JSON.stringify(params),
                        headers: getJsonHeaders()
                    }).done(function(data) {
                        promptEWSDownload = false;
                    });
                }
                downloadWorkspaceFile(rmsUrl, filePath);
            });
        } else {
            downloadWorkspaceFile(rmsUrl, filePath);
        }
    } else {
        downloadRepoFile(rmsUrl, repoType, repoId, filePath, filePathDisplay, isNXL, fileName);
    }
}

function getRepoTypeStyle(repoType) {
    var repoTypeCss = '';
    if (repoType == 'DROPBOX') {
        repoTypeCss = 'dropboxicon-selected';
    } else if (repoType == 'GOOGLE_DRIVE') {
        repoTypeCss = 'googleicon-selected';
    } else {
        repoTypeCss = 'onedriveicon-selected';
    }
    return repoTypeCss;
}

function isSharedFileRevoked(metadata) {
    if (metadata.transactionId != null || metadata.repoId == null || metadata.filePath == null) {
        return false;
    }
    var url = metadata.rmsURL + "/RMSViewer/GetFileDetails";
    var userId = getUserId();
    var ticket = getTicket();
    var data = {
        filePath: metadata.filePath,
        repoId: metadata.repoId,
        userId: userId,
        ticket: ticket,
    };
    var revoked = false;
    $.ajax({
        url: url,
        async: false,
        cache: false,
        type: 'POST',
        data: data
    }).done(function(data) {
        revoked = data.hasOwnProperty('revoked') && data.revoked;
    }).fail(function(jqXHR, textStatus) {});
    return revoked;
}

function showShareFileDialog(metadata) {
    closeViewerDialog();
    $("#loading").show();
    var owner = metadata.owner;
    var rights = metadata.rights;
    if (owner || (rights && rights.indexOf('SHARE') >= 0)) {
        var isRevoked = isSharedFileRevoked(metadata);
        $.ajax({
            url: "/viewer/ShareFile.jsp?v=" + version,
            async: true,
            success: function(data) {
                viewer_dialog.html(data);
                viewer_dialog.dialog('open');
                displayShareFile(metadata);
                if (isRevoked) {
                    var msgDiv = $('#viewer-share-msg');
                    msgDiv.addClass('alert alert-danger');
                    msgDiv.append("<div>" + i18n_data['file.revoked'] + "</div>");
                    $('#rms-shareFile-shareFileWith').hide();
                    $("#rms-shareFile-shareButton").hide();
                    $("#rms-shareFile-cancelButton").hide();
                    $("#rms-shareFile-okButton").show();
                }
            },
            complete: function() {
                $("#loading").hide();
            }
        });
    } else {
        $("#loading").hide();
        var msg = i18n_data['share.file.unauthorized.reshare'];
        handleError(msg);
    }
}

function showProtectDialog(metadata) {
    closeViewerDialog();
    $("#loading").show();
    if (isProtectEnabled(metadata)) {
        $.ajax({
            url: "/viewer/ProtectFile.jsp?v=" + version,
            async: true,
            success: function(data) {
                viewer_dialog.html(data);
                viewer_dialog.dialog('open');
                displayProtectFile(metadata);
            },
            complete: function() {
                $("#loading").hide();
            }
        });
    }
}

function showDisplayInfoDialog(metadata) {
    closeViewerDialog();
    $("#loading").show();
    $.ajax({
        url: "/viewer/DisplayInfo.jsp?v=" + version,
        async: true,
        success: function(data) {
            viewer_dialog.html(data);
            viewer_dialog.dialog('open');
            displayInfo(metadata);
        },
        complete: function() {
            $("#loading").hide();
        }
    });
}

function closeViewerDialog() {
    var div = $('#viewer-dialog');
    div.dialog("close");
    div.empty();
}

function getTagsFromMetaData(metaData) {
    var tagsMap = generateTagListFromTagMap(metaData.tagsMap);
    var tags = "";
    Object.keys(tagsMap).forEach(function(tagKey) {
        tags += '<li><span>' + tagKey + '</span></br><b>' + tagsMap[tagKey] + '</b></li>';
    })
    if ((Object.keys(tagsMap)) && ((Object.keys(tagsMap).length % 2) == 1)) {
        tags += '<li><span> </span></br><b> </b></li>';
    }
    if (tags.length == 0) {
        tags = '<span>' + i18n_data['file.noclassification'] + '</span>';
    }
    return tags;
}

function sendActivityLog(metaData, ticket, userId) {
    url = "/viewer/RMSViewer/SendActivityLog";
    var params = {
        ticket: ticket,
        userId: userId,
        duid: metaData.duid,
        repoId: metaData.repoId,
        filePath: metaData.filePath,
        ownerId: metaData.ownerId,
        isProjectFile: metaData.projectFile,
        deviceId: readCookie('deviceId'),
        platformId: readCookie('platformId'),
        clientId: readCookie('clientId')
    };
    $.post(url, params).done(function(data) {
        return;
    });
}

function getRightsFromMetaData(metaData) {
    return displayRights(metaData.rights, metaData.protectionType);
}

function displayRights(rights, protectionType) {
    var result = '';
    result += '<div class="rms-info-rights text-align-center file-rights-container">';
    if (rights && $.isArray(rights)) {
        if (rights.indexOf("VIEW") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/View_P.svg"><h6>View</h6></br></div></li>';
        }
        if (rights.indexOf("EDIT") >= 0) {
        	result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/EditRight_P.svg"><h6>Edit</h6></br></div></li>';
        }
        if (rights.indexOf("PRINT") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/Print_P.svg"><h6>Print</h6></br></div></li>';
        }
        if (rights.indexOf("SHARE") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/SharedFilesInfo_P.svg"><h6>ReShare</h6></br></div></li>';
        }
        if (rights.indexOf("DECRYPT") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/Extract_P.svg"><h6>Extract</h6></br></div></li>';
        }
        if (rights.indexOf("DOWNLOAD") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/SaveAsRight_P.svg"><h6>Save As</h6></br></div></li>';
        }
        if (rights.indexOf("WATERMARK") >= 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/WaterMark_P.svg"><h6>Watermark</h6></br></div></li>';
        }
        if (protectionType == 0) {
            result += '<li><div><img class="rms-sharefile-icon-50px" src="/viewer/ui/img/Validity_P.svg"><h6>Validity</h6></br></div></li>';
        }
    }
    result += '</div>';
    return result;
}

function getReadableFileSize(bytes, precision) {
    var units = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
        return '-';
    }
    if (typeof precision === 'undefined') {
        precision = 1;
    }
    var unit = 0;

    while (bytes >= 1024) {
        bytes /= 1024;
        unit++;
    }
    if (units[unit] == 'KB') {
        return bytes.toFixed() + ' ' + units[unit];
    }
    return bytes.toFixed(+precision) + ' ' + units[unit];
}

function getReadableDate(time) {
    var shortFormat = "h:MM TT"; //"h:m a"
    var fullFormat = "d mmm yyyy, h:MM TT"; //"d MMM yyyy,h:m a";

    var date = null;
    date = new Date(time);
    var now = new Date();
    var content = '';
    if (now.getFullYear() == date.getFullYear() && now.getMonth() == date.getMonth() && now.getDate() == date.getDate()) {
        // today
        if (now.getHours() > date.getHours()) {
            var hrPassed = now.getHours() - date.getHours();
            content = 'today, ' + hrPassed + ' ' + (hrPassed == 1 ? 'hour' : 'hours') + ' ago'
        } else if (now.getMinutes() > date.getMinutes()) {
            var minPassed = now.getMinutes() - date.getMinutes();
            content = 'today, ' + minPassed + ' ' + (minPassed == 1 ? 'min' : 'mins') + ' ago'
        } else {
            content = 'a moment ago';
        }
    } else {
        var yesterday = new Date();
        yesterday.setDate(now.getDate() - 1);
        if (now.getFullYear() == date.getFullYear() && now.getMonth() == date.getMonth() && now.getDate() == date.getDate() + 1) {
            // yesterday
            content = 'yesterday, ' + dateFormat(time, shortFormat);
        } else {
            content = dateFormat(time, fullFormat);
        }
    }
    return content;
}

/* Please also update the file in networkService.js */
var errorHandler = function(response) {
    switch (response.status) {
        case 400:
            // not found;
            break;
        case 401:
            window.location.href = "TimeOut.jsp";
            break;
        case 500:
            // internal server error
            break;
        default:
            // unknown    
    }
}

function displayingOnMobile() {
    var check = false;
    (function(a) {
        if (/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0, 4))) check = true
    })(navigator.userAgent || navigator.vendor || window.opera);
    return check;
}

function getShortName(data, DATA_MAX_LENGTH) {
    if (data.length > DATA_MAX_LENGTH) {
        var str = data.slice(0, DATA_MAX_LENGTH - 1);
        str = str + "...";
    } else {
        str = data;
    }
    return str;
}

function getParameters() {
    var searchString = window.location.search.substring(1),
        params = searchString.split("&"),
        hash = {};
    if (searchString == "") return {};

    for (var i = 0; i < params.length; i++) {
        var val = params[i].split("=");
        hash[unescape(val[0])] = unescape(val[1]);
    }
    return hash;
}

function copyToClipboard(elem) {
    var targetId = "_hiddenCopyText_";
    var isInput = elem.tagName === "INPUT" || elem.tagName === "TEXTAREA";
    var origSelectionStart, origSelectionEnd;
    if (isInput) {
        // can just use the original source element for the selection and copy
        target = elem;
        origSelectionStart = elem.selectionStart;
        origSelectionEnd = elem.selectionEnd;
    } else {
        // must use a temporary form element for the selection and copy
        target = document.getElementById(targetId);
        if (!target) {
            var target = document.createElement("textarea");
            target.style.position = "absolute";
            target.style.left = "-9999px";
            target.style.top = "0";
            target.id = targetId;
            document.body.appendChild(target);
        }
        target.textContent = elem.textContent;
    }
    // select the content
    var currentFocus = document.activeElement;
    target.focus();
    target.setSelectionRange(0, target.value.length);

    // copy the selection
    var succeed;
    try {
        succeed = document.execCommand("copy");
    } catch (e) {
        succeed = false;
    }
    // restore original focus
    if (currentFocus && typeof currentFocus.focus === "function") {
        currentFocus.focus();
    }

    if (isInput) {
        // restore prior selection
        elem.setSelectionRange(origSelectionStart, origSelectionEnd);
    } else {
        // clear temporary content
        target.textContent = "";
    }
    return succeed;
}

function getUserId() {
    return readCookie("userId");
}

function getTicket() {
    return readCookie("ticket");
}

function getClientId() {
   return readCookie("clientId");
}

function getSessionId() {
    return getParameterByName("s");
}

function getDocId() {
    return getParameterByName("d");
}

function getViewSource() {
    return getParameterByName("source");
}

function getJsonHeaders() {
    return {
        'Content-Type': 'application/json',
        'userId': readCookie("userId"),
        'ticket': readCookie("ticket"),
        'clientId': readCookie("clientId"),
        'platformId': readCookie("platformId")
    };
}

function validateEmail(id,$scope) {
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    $scope.currentIds = $("#"+id).tagit("assignedTags");
    if ($scope.currentIds.length < 1) {
        if($scope.optional === true) {
            $("#error-label").hide();
            $scope.mailPristine = false;
        } else {
            $("#error-label").show();
            $("#error-label").css('color', 'red');
            $("#error-label").text("Email is required");
            $scope.mailPristine = true;
        }
        $scope.$parent.mailPristine = $scope.mailPristine;
        if ($scope.doApply) {
            $scope.$evalAsync();
        }
        return $scope.mailPristine;
    }
    var valid = true;
    for (var i=0; i < $scope.currentIds.length; i++) {
        if (!re.test($scope.currentIds[i])) {
            valid = false;
            break;
        }
    }
    $scope.mailPristine = !valid;
    if (valid) {
        $("#error-label").hide();
    } else {
        $("#error-label").show();
        $("#error-label").css('color', 'red');
        $("#error-label").text("Email is not valid");
    }
    if ($scope.doApply) {
      $scope.$parent.mailPristine = $scope.mailPristine;
        $scope.$evalAsync();
    }
    return $scope.mailPristine;
}
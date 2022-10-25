var zoomLevels = [25, 33, 50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300, 400];
var MODE_BEST_FIT = 0;
var MODE_FIT_TO_WIDTH = 1;
var MODE_FIT_TO_HEIGHT = 2;
var cache = {};
var printUrl = "";
var metaData;
var docId;
var rotation = 0;
var zoomLevel = 3;
var dpi = 0;
var pageNumberDisplay = 1;
var pageNumberReal = 1;
var numPages;
var sessionId;
var origWidth;
var origHeight;
var clickCount = 0;
var callbackTimer;
var resetTimer;

$(window).bind('orientationchange', function(e) {
    resizeWindow(MODE_BEST_FIT);
    fitImagePosition();
    closeViewerDialog();
});

function translateCallback(data) {
    var hideRMSOperations = getParameterByName("hideOperations");
    if (hideRMSOperations != "true") {
        $("#rms-print-button").attr('title', data['viewer.toolbar.print']);
        $("#rms-help-button").attr('title', data['viewer.toolbar.help']);
        $("#rms-download-button").attr('title', data['viewer.toolbar.download']);
        $("#rms-protect-button").attr('title', data['viewer.toolbar.protect']);
        $("#rms-share-button").attr('title', data['viewer.toolbar.share']);
        $("#rms-info-button").attr('title', data['viewer.toolbar.info']);
    }
    $("#rms-rotate-left-button").attr('title', data['viewer.toolbar.rotate.left']);
    $("#rms-rotate-right-button").attr('title', data['viewer.toolbar.rotate.right']);
    $("#rms-prev-button").attr('title', data['viewer.toolbar.prev']);
    $("#rms-next-button").attr('title', data['viewer.toolbar.next']);
    $("#rms-fit-height-button").attr('title', data['viewer.toolbar.fit.height']);
    $("#rms-fit-width-button").attr('title', data['viewer.toolbar.fit.width']);
    $("#rms-zoom-in-button").attr('title', data['viewer.toolbar.zoom.in']);
    $("#rms-zoom-out-button").attr('title', data['viewer.toolbar.zoom.out']);
}

$(document).ready(function() {
    showLoading();
    var url = "/viewer/RMSViewer/GetDocMetaData";
    var TITLE_MAX_LENGTH_DESKTOP = 50;
    var TITLE_MAX_LENGTH_MOBILE = 15;
    docId = getDocId();
    sessionId = getSessionId();
    var source = getViewSource();
    var userId = getUserId();
    var ticket = getTicket();
    $.post(url, {
        documentId: docId,
        s: sessionId,
        source: source
    }).done(function(data) {
        if ((data.errMsg && data.errMsg.length > 0) || data.numPages == undefined || data.numPages <= 0) {
            window.location.href = '/viewer/ShowError.jsp?errMsg=' + data.errMsg;
            return;
        }
        metaData = data;
        numPages = data.numPages;

        loadNewPage();
        adjustImageContainerSize(null);
        $("#titleDesktop").text(getShortName(metaData.originalFileName, TITLE_MAX_LENGTH_DESKTOP));
        $("#titleDesktop").attr("title", metaData.originalFileName);
        $("#titleMobile").text(getShortName(metaData.originalFileName, TITLE_MAX_LENGTH_MOBILE));
        $("#titleMobile").attr("title", metaData.originalFileName);
        $('[data-toggle="tooltip"]').tooltip();
        var $input = $("#pageNumberDisplayTextBox");
        $input.val(1);
        var textBoxSize = numPages.toString().length;
        $input.attr('maxlength', textBoxSize);
        $input.css('width', '30px');
        $("#totalPageNum").text(numPages);
        $("#pageNumContainer").show();
        if (numPages <= 1) {
            $("#next-btn").addClass("disabled");
        }
        checkPrintEnabled(metaData);
        checkFileInfoEnabled(metaData);
        checkDownloadEnabled(metaData);
        checkShareEnabled(metaData);
        checkDecryptEnabled(metaData);
        checkProtectEnabled(metaData);
        translateIfRequired(function callback(){
            showShareIntro();
        });
    });
});

function resizeWindow(mode) {
    $('#pageWrapper').css("width", $(window).width());
    $('#pageWrapper').css("height", $(window).height());
    zoomLevel = 3;
    adjustImageWrapperSize();
    var containerWidth = $('#imageWrapper').width();
    var containerHeight = $('#imageWrapper').height();
    var width = $('#mainImg').width(); // Current image width
    var height = $('#mainImg').height(); // Current image height
    if (width == 0 || height == 0) {
        return;
    }
    var fitToHeight;
    var isRotated = (rotation && ((rotation / 90) % 4) == 1 || ((rotation / 90) % 4) == 3);
    switch (mode) {
        case MODE_BEST_FIT:
            fitToHeight = isFitToHeight(width, height, containerWidth, containerHeight);
            break;
        case MODE_FIT_TO_WIDTH:
            fitToHeight = false;
            break;
        case MODE_FIT_TO_HEIGHT:
            fitToHeight = true;
    }
    var newWidth, newHeight, scaleRatio;
    if (!isRotated) {
        if (fitToHeight) {
            scaleRatio = containerHeight / height;
            if (containerHeight > origHeight && mode == MODE_BEST_FIT) {
                newWidth = origWidth;
                newHeight = origHeight;
            } else {
                newWidth = width * scaleRatio;
                newHeight = containerHeight;
            }
        } else {
            scaleRatio = containerWidth / width;
            if (containerWidth > origWidth && mode == MODE_BEST_FIT) {
                newWidth = origWidth;
                newHeight = origHeight;
            } else {
                newWidth = containerWidth;
                newHeight = height * scaleRatio;
            }
        }
        $('#mainImg').css("width", newWidth);
        $('#mainImg').css("height", newHeight);
    } else {
        if (fitToHeight) {
            scaleRatio = containerHeight / $('#imageContainer').height();
            if (containerHeight > origWidth && mode == MODE_BEST_FIT) {
                newWidth = origWidth;
                newHeight = origHeight;
            } else {
                newWidth = width * scaleRatio;
                newHeight = height * scaleRatio;
            }
            $('#mainImg').css("height", newHeight);
            $('#mainImg').css("width", newWidth);
        } else {
            scaleRatio = containerWidth / $('#imageContainer').width();
            if (containerWidth > origHeight && mode == MODE_BEST_FIT) {
                newWidth = origWidth;
                newHeight = origHeight;
            } else {
                newWidth = width * scaleRatio;
                newHeight = height * scaleRatio;
            }
        }
        $('#mainImg').css("width", newWidth);
        $('#mainImg').css("height", newHeight);
    }
    if(width != $('#mainImg').width() || height != $('#mainImg').height()){
        if (initialLoading) {
            adjustImageContainerSize();
        } else {
            adjustImageContainerSize(scaleRatio);
        }
    }
};

function adjustImageWrapperSize() {
    var pageWrapperWidth = $('#pageWrapper').width();
    var pageWrapperHeight = $('#pageWrapper').height();
    var toolbarContainerHeight = $('.toolbarContainer').height();
    var nxlHeaderHeight = $('.cc-header').height();
    var imageWrapperHeight = pageWrapperHeight - toolbarContainerHeight - nxlHeaderHeight;
    $('#imageWrapper').css("height", imageWrapperHeight);
    $('#imageWrapper').css("width", pageWrapperWidth);
}

function adjustImageContainerSize(scaleRatio) {
    var h = $("#mainImg").height();
    var w = $("#mainImg").width();
    if (scaleRatio) {
        $('#imageContainer').css("width",$('#imageContainer').width()*scaleRatio+"px");
        $('#imageContainer').css("height",$('#imageContainer').height()*scaleRatio+"px");
    } else {
        if (h != $('#imageContainer').width() && w != $('#imageContainer').height()) {
            $('#imageContainer').css("width",w+"px");
            $('#imageContainer').css("height",h+"px");
        }
    }
}

function jumpToPage() {
    var pgNumString = $("#pageNumberDisplayTextBox").val();
    if (isNaN(pgNumString) || parseInt(Number(pgNumString)) != pgNumString || isNaN(pageNumberDisplay)) {
        handleError(pgNumString + ' is not a valid page number.');
        pageNumberDisplay = pageNumberReal;
    }
    pageNumberDisplay = parseInt(Number(pgNumString));
    if (pageNumberDisplay >= 1 && pageNumberDisplay <= numPages) {
        pageNumberReal = pageNumberDisplay;
        loadNewPage();
    } else {
        handleError('There is no page number ' + pgNumString + ' in this document.');
        pageNumberDisplay = pageNumberReal;
    }
    checkNavigationButtons();
}

$("#pageNumberDisplayTextBox").keydown(function(e) {
    if (e.keyCode === 13) {
        jumpToPage();
    }
});

function handleSuccessiveClicks(callback) {
    if (clickCount > 2) {
        showLoading();
        clearTimeout(resetTimer);
        clearTimeout(callbackTimer);
        callbackTimer = setTimeout(function() {
            clickCount = 0;
            callback();
        }, 200);
    } else {
        clearTimeout(resetTimer);
        resetTimer = setTimeout(function() {
            clickCount = 0;
        }, 200);
        callback();
    }
}

function navigate(param) {
    if (param == 'previous') {
        pageNumberDisplay = pageNumberReal;
        if (pageNumberDisplay > 1) {
            pageNumberDisplay--;
            pageNumberReal--;
            clickCount++;
            handleSuccessiveClicks(loadNewPage);
        }
    } else if (param == 'next') {
        pageNumberDisplay = pageNumberReal;
        if (pageNumberDisplay < numPages) {
            pageNumberDisplay++;
            pageNumberReal++;
            clickCount++;
            handleSuccessiveClicks(loadNewPage);
        }
    }
    $("#pageNumberDisplayTextBox").val(pageNumberDisplay);
    checkNavigationButtons();
}

function checkNavigationButtons() {
    if (pageNumberDisplay <= 1) {
        $("#prev-btn").addClass("disabled");
        $("#next-btn").removeClass("disabled");
    } else if (pageNumberDisplay >= numPages) {
        $("#next-btn").addClass("disabled");
        $("#prev-btn").removeClass("disabled");
    } else {
        $("#next-btn").removeClass("disabled");
        $("#prev-btn").removeClass("disabled");
    }
    if (numPages <= 1) {
        $("#prev-btn").addClass("disabled");
        $("#next-btn").addClass("disabled");
    }
}

var initialLoading = true;
$("#mainImg").on('load', function() {
    if (initialLoading) {
        resizeWindow(MODE_BEST_FIT);
        initialLoading = false;
    }
    $("#imageWrapper").scrollTo(0);
    self.removeRendering();
    self.removeLoading();

    $(window).resize(function() {
        resizeWindow(MODE_BEST_FIT);
        fitImagePosition();
    });
});

function loadPageFromServer(pageNo) {
    if (!cache[pageNo]) {
        cache[pageNo] = {};
        showLoading();
    }
    var img = cache[pageNo][dpi];
    var self = this;
    if (!img) {
        var img = new Image();
        img.setAttribute('data-p', pageNo);
        img.setAttribute('data-z', dpi);
        img.onload = function() {
            if (pageNo === pageNumberReal) {
                $("#mainImg").attr("src", this.src);
                if (initialLoading) {
                    $("#mainImg").width(this.width);
                    $("#mainImg").height(this.height);
                }
            };
            var p = this.getAttribute('data-p');
            var z = this.getAttribute('data-z');
            cache[p][z] = this;
        };
        var url = "/viewer/RMSViewer/GetDocContent?p=" + pageNo + "&d=" + docId + "&s=" + sessionId + "&z=" + dpi;
        toDataURL(url, img, function(image, dataUrl) {
            image.src = dataUrl;
        }, function(image, statusCode) {
            image.code = statusCode;
            image.src = "";
        });
        img.onerror = function(){
            var errMsg = '/viewer/ShowError.jsp?code=';
            if (img.code && img.code === 404) {
                errMsg += 'err.cache.not.found';
            } else {
                errMsg += 'err.generic.viewer';
            }
            window.location.href = errMsg;
        };
    } else {
        if (pageNo === pageNumberReal) {
            $("#mainImg").attr("src", img.src);
        }
    }
    adjustImageContainerSize();
}

function toDataURL(url, img, success, error) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'blob';
    xhr.onload = function() {
        var reader = new FileReader();
        reader.onloadend = function() {
            200 === xhr.status ? success(img, reader.result) : error(img, xhr.status);
        }
        reader.readAsDataURL(xhr.response);
    };
    xhr.send();
}

function prefetchPagesFromServer() {
    var start = pageNumberReal - 2;
    start = start < 1 ? 1 : start;
    var end = pageNumberReal + 2;
    end = end >= numPages ? numPages : end;
    for (var i = start; i <= end; i++) {
        if (i === pageNumberReal) {
            continue;
        }
        if (!cache[i]) {
            cache[i] = {};
        }
        var img = cache[i][dpi];
        if (!img) {
            img = new Image();
            img.setAttribute('data-p', i);
            img.setAttribute('data-z', dpi);
            img.onload = function(){
                var p = this.getAttribute('data-p');
                var z = this.getAttribute('data-z');
                cache[p][z] = this;
            };
            var url = "/viewer/RMSViewer/GetDocContent?p=" + i + "&d=" + docId + "&s=" + sessionId + "&z=" + dpi;
            toDataURL(url, img, function(image, dataUrl) {
                image.src = dataUrl;
            }, function(image, statusCode) {
                image.code = statusCode;
                image.src = "";
            });
        }
    }
}

function isFitToHeight(imgWidth, imgHeight, conWidth, conHeight) {
    var imgratio = imgHeight / imgWidth;
    var winratio = conHeight / conWidth;
    return winratio <= imgratio;
}

function loadNewPage() {
    loadPageFromServer(pageNumberReal);
    prefetchPagesFromServer();
}

function printAllPages(metaData) {
    url = "/viewer/RMSViewer/PrintFile";
    showLoading();
    var self = this;
    $.post(url, {
        documentId: docId,
        s: sessionId
    }).done(function(data) {
        var settings = "scrollbars=yes, location=no, directories=no, status=no, menubar=no, toolbar=no, resizable=yes, dependent=no";
        var win = window.open(data.url, "PrintFile", settings);
        self.removeLoading();
        return;
    });
}

function fitToHeight() {
    resizeWindow(MODE_FIT_TO_HEIGHT);
    adjustImageWrapperSize();
    adjustImageContainerSize(null);
}

function fitToWidth() {
    resizeWindow(MODE_FIT_TO_WIDTH);
    adjustImageWrapperSize();
    adjustImageContainerSize(null);
}

function zoomIn() {
    if (zoomLevel >= zoomLevels.length - 1) {
        return;
    }
    zoomLevel++;
    var width = $('#mainImg').width();
    var height = $('#mainImg').height();
    var scaleRatio = zoomLevels[zoomLevel] / zoomLevels[zoomLevel - 1];
    $('#mainImg').css("width", width * scaleRatio);
    $('#mainImg').css("height", height * scaleRatio);
    adjustImageWrapperSize();
    adjustImageContainerSize(scaleRatio);
    fitImagePosition();

    if (!metaData.isSingleImageFile && Math.floor(zoomLevel / 4) > dpi) {
        dpi++;
        showRendering();
        loadPageFromServer(pageNumberReal);
        prefetchPagesFromServer();
    }
}

function zoomOut() {
    if (zoomLevel <= 0)
        return;
    zoomLevel--;
    var width = $('#mainImg').width();
    var height = $('#mainImg').height();
    var scaleRatio = zoomLevels[zoomLevel] / zoomLevels[zoomLevel + 1];
    $('#mainImg').css("width", width * scaleRatio);
    $('#mainImg').css("height", height * scaleRatio);
    adjustImageWrapperSize();
    adjustImageContainerSize(scaleRatio);
    fitImagePosition();
    if (!metaData.isSingleImageFile && Math.floor(zoomLevel / 4) < dpi) {
        dpi--;
        prefetchPagesFromServer();
    }
}

function fitImagePosition() {
    var containerH = $("#imageContainer").height();
    var containerW = $("#imageContainer").width();
    var newW = $('#mainImg').width();
    var newH = $('#mainImg').height();
    if (containerH > newH && containerW > newW) {
        if ($("#imageContainer").height() > $("#imageContainer").width()) {
            $('#mainImg').offset({top: $("#imageContainer").offset().top + (containerH - newW)/2, left: $("#imageContainer").offset().left});
        } else {
            $('#mainImg').offset({top: $("#imageContainer").offset().top, left: $("#imageContainer").offset().left + (containerW - newH)/2});
        }
    } else {
        $('#mainImg').offset({top: $("#imageContainer").offset().top, left: $("#imageContainer").offset().left});
    }
}

function rotate(angle) {
    var rotationAngle = Number(angle);
    if (rotationAngle) {
        rotation += rotationAngle;
        adjustImageSize();
        $("#mainImg").rotate({
            animateTo: rotation,
            callback: function () {
                fitImagePosition();
            }
        });
    }
}

function adjustImageSize() {
    var imgH = $("#mainImg").height();
    var imgW = $("#mainImg").width();
    var containerH = $("#imageContainer").height();
    var containerW = $("#imageContainer").width();
    var containerTop = $('#imageContainer').offset().top;
    var containerLeft = $('#imageContainer').offset().left;
    if (containerH > imgH && containerW > imgW) {
         $('#mainImg').css("width",containerW+"px");
         $('#mainImg').css("height",containerH+"px");
         $('#mainImg').offset({top:containerTop - (containerW - containerH)/2,left:containerLeft + (containerW - containerH)/2});
    } else {
        if (containerW > containerH) {
            $('#mainImg').css("width",containerH+"px");
            $('#mainImg').css("height",containerH/(containerW/containerH)+"px");
        } else {
            $('#mainImg').css("height",containerW+"px");
            $('#mainImg').css("width",containerW/(containerH/containerW)+"px");
        }
        
        $('#mainImg').offset({top:containerTop + (containerH - $("#mainImg").height())/2,left:containerLeft + (containerW - $("#mainImg").width())/2});
    }
}

function showLoading() {
    $('#loading').show();
}

function removeLoading() {
    $('#loading').hide();
    $('#imageWrapper').css('visibility', 'visible');
}

function showRendering() {
    $('#rendering').show();
}

function removeRendering() {
    $('#rendering').hide();
}
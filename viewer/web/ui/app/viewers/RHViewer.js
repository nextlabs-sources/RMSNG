function AXOrNull(progId) {
    try {
        var browser = window.jscd.browser;
        if (browser != "Microsoft Internet Explorer") {
            window.location.href = '/viewer/ShowError.jsp?errMsg= This file can only be viewed from Internet Explorer 10 and above.';
            return null;
        }
        var control = new ActiveXObject(progId);
        return control;
    } catch (ex) {
        window.location.href = '/viewer/ShowError.jsp?code=err.sap.ve.missing';
        return null;
    }
}

function addTitle(fileName) {
    var TITLE_MAX_LENGTH_DESKTOP = 50;
    var TITLE_MAX_LENGTH_MOBILE = 15;
    var titleDesktopSpan = document.getElementById('titleDesktop');
    var titleMobileSpan = document.getElementById('titleMobile');
    titleDesktopSpan.innerHTML += getShortName(fileName, TITLE_MAX_LENGTH_DESKTOP);
    titleMobileSpan.innerHTML += getShortName(fileName, TITLE_MAX_LENGTH_MOBILE);
    $("#titleDesktop").attr("title", fileName);
    $("#titleMobile").attr("title", fileName);
    $('[data-toggle="tooltip"]').tooltip();
}

function resetStep() {
    var step = document.getElementById("DeepView").Scene.Steps.GetByIndex(0);
    step.Play();
}

function play() {
    document.getElementById("DeepView").ExecuteCommand("M1251");
}

function pauseStep() {
    var isPaused = rh().Scene.IsCurrentStepPaused;
    if (isPaused) {
        rh().Scene.PauseCurrentStep(false);
    } else {
        rh().Scene.PauseCurrentStep(true);
    }
}

function playall() {
    document.getElementById("DeepView").ExecuteCommand("M1302");
    document.getElementById("DeepView").ExecuteCommand("M1252");
}

function measurePoint() {
    document.getElementById("DeepView").ExecuteCommand("M2800");
}

function measureAngle() {
    document.getElementById("DeepView").ExecuteCommand("M2801");
}

function print() {
    document.getElementById("DeepView").ExecuteCommand("M1279");
    sendActivityLog(metaData, getTicket(), getUserId());
}

function getFileName(name) {
    var pathArray = name.split("/");
    var result = pathArray[pathArray.length - 1]

    if (result == null)
        return null;
    else
        return decodeURIComponent(result.replace(/\+/g, " "));
}

function translateCallback(data) {
    $("#rms-print-button").attr('title', data['viewer.toolbar.print']);
    $("#rms-help-button").attr('title', data['viewer.toolbar.help']);
    $("#rms-download-button").attr('title', data['viewer.toolbar.download']);
    $("#rms-protect-button").attr('title', data['viewer.toolbar.protect']);
    $("#rms-share-button").attr('title', data['viewer.toolbar.share']);
    $("#rms-info-button").attr('title', data['viewer.toolbar.info']);
}

function closeDialog() {
    var fromDiv = document.getElementById("error");
    var toDiv = document.getElementById("overlay-iframe-2");
    fromDiv.style.display = "none";
    cloneCSSProperties(fromDiv, toDiv);
}

function handleError(message) {
    if (document.getElementById("all") != undefined) {
        if (document.getElementById("viewer-dialog") == undefined) {
            closeViewerDialog();
        }
        var fromDiv = document.getElementById("error");
        var toDiv = document.getElementById("overlay-iframe-2");
        document.getElementById("errmsg").innerHTML = message;
        fromDiv.style.zIndex = '40002';
        fromDiv.style.display = 'block';
        cloneCSSProperties(fromDiv, toDiv);
    }
}

function displayMessage(message) {
    if (document.getElementById("all") != undefined) {
        closeViewerDialog();
        var fromDiv = document.getElementById("error");
        var toDiv = document.getElementById("overlay-iframe-2");
        document.getElementById("errmsg").innerHTML = message;
        fromDiv.style.display = 'block';
        $("#error").removeClass("alert-danger");
        $("#error").addClass("alert-success");
        cloneCSSProperties(fromDiv, toDiv);
    }
}

function showDownloadFile() {
    downloadFile(metaData);
    var fromDiv = document.getElementById("viewer-dialog");
    var toDiv = document.getElementById("overlay-iframe");
    fromDiv.style.top = "0px";
    fromDiv.style.right = "50%";
    fromDiv.style.marginRight = "-300px";
    fromDiv.style.marginTop = "20px";
    cloneCSSProperties(fromDiv, toDiv);
    toDiv.style.width = "600px";
}

function resetViewerDialogStyle() {
    var fromDiv = document.getElementById("viewer-dialog");
    fromDiv.style.top = "50px";
    fromDiv.style.right = "0px";
    fromDiv.style.marginRight = "0px";
    fromDiv.style.marginTop = "30px";
}

function showInfo() {
    showDisplayInfoDialog(metaData);
    var fromDiv = document.getElementById("viewer-dialog");
    var toDiv = document.getElementById("overlay-iframe");
    cloneCSSProperties(fromDiv, toDiv);
    toDiv.style.height = "100%";
    toDiv.style.width = "600px";
}

function showShareFile() {
    showShareFileDialog(metaData);
    var fromDiv = document.getElementById("viewer-dialog");
    var toDiv = document.getElementById("overlay-iframe");
    cloneCSSProperties(fromDiv, toDiv);
    toDiv.style.height = "100%";
    toDiv.style.width = "600px";
}

function showProtectFile() {
    showProtectDialog(metaData);
    var fromDiv = document.getElementById("viewer-dialog");
    var toDiv = document.getElementById("overlay-iframe");
    cloneCSSProperties(fromDiv, toDiv);
    toDiv.style.height = "100%";
    toDiv.style.width = "600px";
}

function closeViewerDialog() {
    var div = $('#viewer-dialog');
    div.dialog("close");
    div.empty();
    $("#overlay-iframe").hide();
    resetViewerDialogStyle();
}

function cloneCSSProperties(fromDiv, toDiv) {
    var computed_style_object = window.getComputedStyle(fromDiv, null);
    if (!computed_style_object) {
        return null;
    }
    var stylePropertyValid = function(name, value) {
        //checking that the value is not a undefined
        return typeof value !== 'undefined' &&
            //checking that the value is not a object
            typeof value !== 'object' &&
            //checking that the value is not a function
            typeof value !== 'function' &&
            //checking that we dosent have empty string
            value.length > 0 &&
            //checking that the property is not int index ( happens on some browser
            value != parseInt(value)

    };

    //we iterating the computed style object and compy the style props and the values 
    for (property in computed_style_object) {
        //checking if the property and value we get are valid sinse browser have different implementations
        if (stylePropertyValid(property, computed_style_object[property])) {
            //applying the style property to the target element
            toDiv.style[property] = computed_style_object[property];

        }
    }
}
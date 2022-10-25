var NUM_OF_HYPHEN_IN_NXL_EXTENSION = 6;
var popupWindow; 

function getParameterByName( name ){
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");  
  var regexS = "[\\?&]"+name+"=([^&#]*)";  
  var regex = new RegExp( regexS );  
  var results = regex.exec( window.location.href ); 
  if( results == null )    
    return "";  
  else    
    return results[1];
}

function generateTagListFromTagMap(tags){
    var tagList ={};
    var tagPair={};
	if(tags!=null) {
		Object.keys(tags).forEach(function(key,index) {
			var value = "";
			var values = tags[key];
			for(var i = 0; i<values.length;i++){
			  value+=values[i];
			  if(i!=values.length-1){
				value+=",";
			  }
			}
			tagList[key] = value;
		});
    }
	return tagList;
}

function downloadRepoFile(repoId, filePathId, filePath){
	window.open(CONTEXT_PATH + "/RMSViewer/DownloadFile?filePath="+encodeURIComponent(filePathId)+
		"&filePathDisplay="+encodeURIComponent(filePath)+
		"&repoId="+encodeURIComponent(repoId));
}

function downloadProjectFile(rmsUrl, projectId, filePathId) {
	window.open(rmsUrl + "/RMSViewer/DownloadFileFromProject?path=" + encodeURIComponent(filePathId) +
		"&projectId=" + encodeURIComponent(projectId));
}

function openSecurePopup(url){ 
      settings = "scrollbars=yes, location=no, directories=no, status=no, menubar=no, toolbar=no, resizable=yes, dependent=no";   
      if(popupWindow != undefined){
        popupWindow.close();
      } 
      popupWindow = window.open(url, "NextLabsRMS", settings);
      if(popupWindow==undefined){
        var fileLink = " Click <a class='color-light-blue underline' target='_blank' href='JavaScript:void(0);' onclick=\"openSecurePopup('" + url + "')\">here</a> to access the file after disabling the pop up blocker."
        if(i18n_data == undefined) {
          handleError("Your web browser's pop up blocker might be preventing SkyDRM from displaying this file. In order to view this file, you might want to disable your pop up blocker for SkyDRM."+fileLink);
        } else {
          handleError(i18n_data['err.popup.blocked']+fileLink);
        }
      }
  }

  function handleError(message){
    if(document.getElementById("rms-main-view")!=undefined){ 
        document.getElementById("rms-main-view").style.opacity=0.4;
        document.getElementById("errmsg").innerHTML = message;
        document.getElementById("error").style.display='block'
        document.getElementById("error").style.position='fixed';
      }
   }

  function closeDialog() {
	  $("#error").css("display", "none");
	  $("#rms-main-view").css("opacity", "1.0");
  }

function openCenteredPopup(url,winName,w,h,scroll){
    LeftPosition = (screen.width) ? (screen.width-w)/2 : 0;
    TopPosition = (screen.height) ? (screen.height-h)/2 : 0;
    settings = 'height='+h+',width='+w+',top='+TopPosition+',left='+LeftPosition+',scrollbars='+scroll+',resizable';
    popupWindow = window.open(url,winName,settings);
    popupWindow.focus();
}

function openNewWindow(url,winName) {
    helpWindowReference=window.open(url,winName);
    helpWindowReference.focus();
}
function getOriginalFileName(fileName, displayFileName) {
  if(fileName.endsWith('.nxl')  && !displayFileName.endsWith('.nxl')) {
    var count = (fileName.match(/-/g) || []).length;
    var extension = fileName.split('.');
    var ext = extension.length > 2 ? extension[extension.length - 2] : '';
    var nth = count < NUM_OF_HYPHEN_IN_NXL_EXTENSION ? count : count + 1 - NUM_OF_HYPHEN_IN_NXL_EXTENSION;
    var index = getPosition(fileName, '-', nth);
    fileName = fileName.slice(0,index);
    fileName = fileName + "." + ext;
  }
  return fileName;
}

function getPosition(string, subString, nth) {
   return string.split(subString, nth).join(subString).length;
}

function getShortName(fileName, length) {
    var resultStr;
    if(fileName.length < length) {
      resultStr = fileName;
    } else {
      resultStr = fileName.substring(0,length).concat("...");
    }
    return resultStr;
}

function deleteCookie(name, domain) {
    if(domain){
        document.cookie = name + '=; ;path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;' + ';domain='+ domain;
    } else {
        document.cookie = name + '=; ;path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    }
}

function setCookie(name, value, expiryDate, domain) {
    if(domain){
      var cookie = name + "=" + encodeURIComponent(value) + ";domain="+ domain + ";path=/;";
    }else{
      var cookie = name + "=" + encodeURIComponent(value) + ";path=/;";
    }
    document.cookie = expiryDate ? (cookie + "expires=" + expiryDate.toUTCString()) : cookie;
}

function validateEmail(id,$scope) {
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    $scope.currentIds = $("#"+id).tagit("assignedTags");
    if ($scope.currentIds.length < 1) {
        if($scope.optional === true) {
            $("p#error-label:last").hide();
            $scope.mailPristine = false;
        } else {
            $("p#error-label:last").show();
            $("p#error-label:last").css('color', 'red');
            $("p#error-label:last").text("Email is required");
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
        $("p#error-label:last").hide();
    } else {
        $("p#error-label:last").show();
        $("p#error-label:last").css('color', 'red');
        $("p#error-label:last").text("Email is not valid");
    }
    if ($scope.doApply) {
      $scope.$parent.mailPristine = $scope.mailPristine;
        $scope.$evalAsync();
    }
    return $scope.mailPristine;
}

/*
taken from https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
*/
function htmlEntityEncode(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#x27;').replace(/\//g, '&#x2F;');
}

/*
taken from http://stackoverflow.com/q/5639346/404165 and modified
*/
(function(){

    function readCookie(name,c,C,i){

        c = document.cookie.split('; ');
        var cookies = {};

        for(i=c.length-1; i>=0; i--){
           C = c[i].split('=');
           cookies[C[0]] = C[1];
        }

        return cookies[name] ? decodeURIComponent(cookies[name]) : cookies[name];
    }

    window.readCookie = readCookie; // or expose it however you want
})();

function readAllCookies(){
	c = document.cookie.split('; ');
	var cookies = {};
	for(i=c.length-1; i>=0; i--){
		C = c[i].split('=');
		cookies[C[0]] = C[1];
	}
	return cookies;
}

//Polyfill that implements String.endsWith
if (!String.prototype.endsWith) {
  String.prototype.endsWith = function(searchString, position) {
      var subjectString = this.toString();
      if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
        position = subjectString.length;
      }
      position -= searchString.length;
      var lastIndex = subjectString.lastIndexOf(searchString, position);
      return lastIndex !== -1 && lastIndex === position;
  };
}

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
        var rmsContainerNoPanelWidth = $("#rms-inner-container-no-panels").width();
        var left = 0;
        if ($("#superadmin-body-section").width() != null) {
            $("#superadmin-body-section").append(snackbarHTML);
            left = ($("#superadmin-body-section").width() - $('#rms-snackbar').width() - 16) / 2;
        } else if ($("#rms-inner-container").width() != null) {
          $("#rms-inner-container").append(snackbarHTML);
          left = ($("#rms-inner-container").width() - $('#rms-snackbar').width() - 16) / 2;
        } else if (rmsContainerNoPanelWidth != null) {
          $("#rms-inner-container-no-panels").append(snackbarHTML);
          if(rmsContainerNoPanelWidth > 480) {
            left = (rmsContainerNoPanelWidth - $('#rms-snackbar').width() - 216)/2;
          } else {
            left = (rmsContainerNoPanelWidth - $('#rms-snackbar').width() - 16)/2; 
          }
        }
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

function checkRoles(roles, role) {
  if (roles.indexOf(role) > -1) {
    return true ;
  } else {
    return false;
  }
}
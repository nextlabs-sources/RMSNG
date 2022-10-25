function validPassword() {
	var password = document.getElementById('password').value;
	var pattern = new RegExp(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[\W])[A-Za-z\d\W]{8,}$/);
	if(password.length<8 || !pattern.test(password) || password.length>30){
		displayError("Password must be at least 8 characters long and contain at least one alphabet, one number and one special character.");
		$("#password").focus();
		return false;
	}
	return true;
}

function validEmail() { 
	var userEmail=document.getElementById('username').value;
	var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
	var result= re.test(userEmail);
	if(!result){
		displayError("Please enter a valid Email Address.");
		$("#username").focus();
		return false;
	}
	return true;
}
function validName(){
	var emailRegex = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
	var regex = /^((?![\~\!\@\#\$\%\^\&\*\(\)\_\+\=\[\]\{\}\;\:\"\\\/\<\>\?]).)+$/;
	var displayName = $('#displayName').val().trim();
	if(displayName!=null && displayName!="" && !emailRegex.test(displayName) && !regex.test(displayName)){
		displayError("Display Name cannot contain special characters.");
		$('#displayName').focus();
		return false;
	}
	return true;		
}

function translateCode(code){
	switch(code){
		case "404": displayError("The account does not exist.");
					break;
		case "403": displayError("This link has expired. Please request a new one.");
					break;
		case "303": displayError("This account has not been activated.");
					break;
		case "304": displayError("This account has already been activated.");
					break;
		case "204": displaySuccess("Your account has been successfully unregistered.");
					break;
		case "200": displaySuccess("Your account has been activated. Please log in below with your credentials.");
					break;
		case "403f": displayError("You need to allow SkyDRM to access your profile information to log in with Facebook.");
					 break;
		case "403g": displayError("You need to allow SkyDRM to access your profile information to log in with Google.");
					 break;
		case "500f": displayError("Error occurred while logging in with Facebook.");
					 break;
		case "500g": displayError("Error occurred while logging in with Google.");
					 break;
        case "500s": displayError("Error occurred while logging in with SAML.");
                     break;
        case "500l": displayError("Error occurred while logging in with LDAP.");
                     break;
        case "401": displayError("The username or password you entered is incorrect.");
                     break;
		default: closeDialog();
	}
}

function displayMessage(message, type){
	element=document.getElementById("display-error");
	element.style.display="block";
	if(type=="success"){
		$("#display-error").addClass('alert-success');
		$("#display-error").removeClass('alert-danger');
	}else{
		$("#display-error").addClass('alert-danger');
		$("#display-error").removeClass('alert-success');
	}
	element=document.getElementById("errmsg");
	element.innerHTML=message;
	adjustRightColumnHeight();
}

function displayError(errorMessage){
	displayMessage(errorMessage, "error");
}

function displaySuccess(message){
	displayMessage(message, "success");
}
 
function adjustRightColumnHeight(){
    var wrapper = document.getElementsByClassName("wrapper")[0];
    if(!wrapper){
        return;
    }
    var logo = document.getElementById("smaller-browser-control-partner-logos");
    var logoHeight = logo ? logo.clientHeight : 0;
    var footerHeight = 30;
    var windowHeight = window.innerHeight;
    var totalHeight = $('.wrapper').first().height() + footerHeight;
    if(totalHeight > 0 && logoHeight <= 0 && totalHeight < windowHeight) {
        var padding = (windowHeight - totalHeight)/2;
        if(padding > 100){
            wrapper.style.paddingTop = "100px";
            wrapper.style.paddingBottom =  (2*padding-100)+"px";
        } else {
            wrapper.style.paddingTop = padding + "px";
            wrapper.style.paddingBottom = padding + "px";
        }
    } else {
        wrapper.style.paddingTop = "10px";
        wrapper.style.paddingBottom = "10px";
    }
}

var isIE9OrBelow = function() {
	return /MSIE\s/.test(navigator.userAgent) && parseFloat(navigator.appVersion.split("MSIE")[1]) < 10;
}

var isIE11 = function() {
	return /MSIE\s/.test(navigator.userAgent) && parseFloat(navigator.appVersion.split("MSIE")[1]) == 11;
}

function closeDialog(){
	document.getElementById("display-error").style.display="none";
}

$(window).resize(function() {
	adjustRightColumnHeight();
});

$(function() {
    adjustRightColumnHeight();
});
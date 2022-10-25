mainApp.service('IdentityProvidersService', ['networkService', 'initSettingsService', function(networkService, initSettingsService){
	
	var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

	var getIDPDetails = function(callback) {
		networkService.get(RMS_CONTEXT_NAME + "/rs/tenant/" + window.readCookie("ltId") + "/idp/details",  getJsonHeaders(), function(data){
			if (callback && typeof(callback) == "function") {
                callback(data);
            }
		});
	}

	var getAllowedIDPs = function (callback) {
		networkService.get(RMS_CONTEXT_NAME + "/rs/idp/" + window.readCookie("ltId") + "/allow", getJsonHeaders(), function (data) {
			if (callback && typeof(callback) == "function") {
				callback(data);
			}
		});
	};

	var deleteAnIDP = function (idpId, callback) {
		networkService.deleteRequest(RMS_CONTEXT_NAME + "/rs/idp/" + window.readCookie("ltId") + "/" + idpId, null, getJsonHeaders(), function(data) {
			if (callback && typeof(callback) == "function") {
				callback(data);
			}
		});
	};

	return {
		getIDPDetails: getIDPDetails,
		getAllowedIDPs: getAllowedIDPs,
		deleteAnIDP: deleteAnIDP
	}
}]);
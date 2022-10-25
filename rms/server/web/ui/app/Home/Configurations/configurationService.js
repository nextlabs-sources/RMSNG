mainApp.factory('configurationService',['networkService', 'initSettingsService',
	function(networkService, initSettingsService) {

		var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

		var getUserAttribute = function(callback) {
			networkService.get(RMS_CONTEXT_NAME + "/rs/tenant/" + window.readCookie("ltId") + "/userAttr", getJsonHeaders(), function(data){
				if (callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		var setUserAttribute = function(parameter, callback) {
			networkService.post(RMS_CONTEXT_NAME + "/rs/tenant/" + window.readCookie("ltId") + "/userAttr", parameter, getJsonHeaders(), function(data) {
				if(callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		var getProjectAdmin = function(callback) {
			networkService.get(RMS_CONTEXT_NAME + "/rs/tenant/" + window.readCookie("ltId") + "/projectAdmin", getJsonHeaders(), function(data){
				if (callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		var setProjectAdmin = function(parameter, callback) {
			networkService.put(RMS_CONTEXT_NAME + "/rs/tenant/" + window.readCookie("ltId") + "/projectAdmin", parameter, getJsonHeaders(), function(data) {
				if(callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		var getIdpUserAttributesMap = function(callback) {
			var url = RMS_CONTEXT_NAME + "/rs/idp/"+window.readCookie("ltId")+"/userAttrMap";
			networkService.get(url, getJsonHeaders(),  function(data){
				if (callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}


		var setIdpUserAttributesMap = function(attrMap, invalidateSessions, callback) {
			var input = {
				"parameters": {
					"attrMap": attrMap,
					"invalidateSessions": invalidateSessions
				}
			};
			networkService.post(RMS_CONTEXT_NAME + "/rs/idp/"+window.readCookie("ltId")+"/userAttrMap", input, getJsonHeaders(), function(data) {
				if (callback && typeof(callback) == "function") {
					callback(data, invalidateSessions);
				}
			});
		};

		return {
			getUserAttribute: getUserAttribute,
			setUserAttribute: setUserAttribute,
			getProjectAdmin: getProjectAdmin,
			setProjectAdmin: setProjectAdmin,
			getIdpUserAttributesMap: getIdpUserAttributesMap,
			setIdpUserAttributesMap: setIdpUserAttributesMap
		}
	}
]);
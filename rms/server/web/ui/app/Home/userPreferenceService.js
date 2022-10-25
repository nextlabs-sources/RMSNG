mainApp.factory('userPreferenceService',['networkService', 'initSettingsService', '$stateParams',
	function(networkService, initSettingsService, $stateParams) {

		var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

		var getPreference = function(callback) {
			var url = (!$stateParams.projectId) ? RMS_CONTEXT_NAME + "/rs/usr/preference" : RMS_CONTEXT_NAME + "/rs/project/" + $stateParams.projectId + "/preference";
			networkService.get(url, getJsonHeaders(), function(data){
				if (callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		var putPreference = function(parameter, callback) {
			var url = (!$stateParams.projectId) ? RMS_CONTEXT_NAME + "/rs/usr/preference" : RMS_CONTEXT_NAME + "/rs/project/" + $stateParams.projectId + "/preference";
			networkService.put(url, parameter, getJsonHeaders(), function(data) {
				if(callback && typeof(callback) == "function") {
					callback(data);
				}
			});
		}

		return {
			getPreference: getPreference,
			putPreference: putPreference
		}
	}
]);
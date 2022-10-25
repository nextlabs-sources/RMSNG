mainApp.service('RightsProtectionMethodService', ['networkService' , 'initSettingsService', function(networkService, initSettingsService){

    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

    var setAdhocRights = function(parameter, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/rs/tenant/preference/" + window.readCookie("ltId"), parameter,  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getAdhocRights = function(callback) {
        networkService.get(RMS_CONTEXT_NAME + "/rs/tenant/v2/" + window.readCookie("ltId"),  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    return {
        setAdhocRights: setAdhocRights,
        getAdhocRights: getAdhocRights
    }
}]);
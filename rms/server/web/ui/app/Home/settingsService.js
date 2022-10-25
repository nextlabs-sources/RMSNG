mainApp.factory('settingsService', ['$http', 'networkService', 'initSettingsService', function($http, networkService, initSettingsService) {

    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

    var getSettings = function(callback) {
        networkService.get(RMS_CONTEXT_NAME + "/RMSViewer/FetchConfiguration", null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var getSystemSettings = function(callback) {
        networkService.get(RMS_CONTEXT_NAME + "/RMSViewer/FetchSystemSettings", null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var saveSettings = function(callback, params, failure) {
        networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/SaveConfiguration", params, null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        }, function(response) {
            if (failure && typeof(failure) == "function") {
                failure(response);
            }
        });
    }

    var saveSystemSettings = function(callback, params, failure) {
        networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/SaveSystemSettings", params, null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        }, function(response) {
            if (failure && typeof(failure) == "function") {
                failure(response);
            }
        });
    }

    var checkPCConnection = function(callback, params, failure) {
        networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/CheckPCConnection", params, null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }
    return {
        getSettings: getSettings,
        getSystemSettings: getSystemSettings,
        saveSettings: saveSettings,
        saveSystemSettings: saveSystemSettings,
        checkPCConnection: checkPCConnection
    }
}]);
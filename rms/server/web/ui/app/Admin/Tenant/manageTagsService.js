mainApp.service('manageTagsService',['networkService', 'initSettingsService', function(networkService, initSettingsService){
    
    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
    
    var getTenantTags = function(callback, type) {
        networkService.get(RMS_CONTEXT_NAME + "/rs/tags/tenant/" + window.readCookie("ltId") + "?type=" + type,  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getProjectTags = function(callback, projectId) {
        networkService.get(RMS_CONTEXT_NAME + "/rs/tags/project/" + projectId,  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    } 

    var updateTenantTags = function(callback, params) {
        networkService.post(RMS_CONTEXT_NAME + "/rs/tags/tenant/" + window.readCookie("ltId"), params,  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var updateProjectTags = function(callback, projectId, params) {
        networkService.post(RMS_CONTEXT_NAME + "/rs/tags/project/" + projectId, params,  getJsonHeaders(), function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    return {
        getTenantTags: getTenantTags,
        getProjectTags: getProjectTags,
        updateTenantTags: updateTenantTags,
        updateProjectTags: updateProjectTags
    }
}]);
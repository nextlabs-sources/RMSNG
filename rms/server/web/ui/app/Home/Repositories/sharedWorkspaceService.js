mainApp.factory('sharedWorkspaceService', ['networkService', 'initSettingsService', function(networkService, initSettingsService) {

    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
    var headers = {
        'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
    };

    var getFiles = function(queryParams, repoId, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/sharedws/v1/" + repoId + "/files";
        var query = [];
        if (queryParams) {
            if (queryParams.path) {
                query.push('path=' + encodeURIComponent(queryParams.path));
            }
            if (queryParams.searchString) {
                query.push('&searchString=' + encodeURIComponent(queryParams.searchString));
            }
            if(queryParams.hideFiles) {
                query.push('&hideFiles=' + encodeURIComponent(queryParams.hideFiles));
            }
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getBasicHeaders(), function(data) {
            if (callback && typeof(callback) === 'function') {
                callback(data);
            }
        });
    };

    var showFile = function(params, callback) {
        var url = VIEWER_URL + "/RMSViewer/ShowSharedWorkspaceFile";

        networkService.post(url, params, headers, function(data) {
            if(callback && typeof(callback) === 'function') {
                callback(data);
            }
        });
    };

    var getFileMetadata = function(params, repoId, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/sharedws/v1/" + repoId + "/file/metadata";

        networkService.post(url, params, getJsonHeaders(), function(data) {
            if(callback && typeof(callback) === 'function') {
                callback(data);
            }
        });
    };

    var protectFileInPlace = function(params, repoId, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/sharedws/v1/" + repoId + "/protect";

        networkService.post(url, params, getJsonHeaders(), function(data) {
            if(callback && typeof(callback) === 'function') {
                callback(data);
            }
        });
    };

    var checkIfFileExists = function(params, repoId, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/sharedws/v1/" + repoId + "/file/checkIfExists";

        networkService.post(url, params, getJsonHeaders(), function(data) {
            if(callback && typeof(callback) === 'function') {
                callback(data);
            }
        });
    };

    return {
        getFiles: getFiles,
        showFile: showFile,
        getFileMetadata: getFileMetadata,
        protectFileInPlace: protectFileInPlace,
        checkIfFileExists: checkIfFileExists
    };
}]);
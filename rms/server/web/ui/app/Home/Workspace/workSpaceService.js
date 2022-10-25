mainApp.factory('workSpaceService', ['networkService', 'initSettingsService', function(networkService, initSettingsService) {

    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

    var getFiles = function(queryParams, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/enterprisews/files";
        var query = [];
        if (queryParams) {
            if (queryParams.page) {
                query.push('page=' + encodeURIComponent(queryParams.page));
            }
            if (queryParams.size) {
                query.push('size=' + encodeURIComponent(queryParams.size));
            }
            if (queryParams.orderBy) {
                query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
            }
            if (queryParams.pathId) {
                query.push('pathId=' + encodeURIComponent(queryParams.pathId));
            }
            if (queryParams.searchString) {
                var searchFields = ["name"];
                query.push('q=' + searchFields + '&searchString=' + encodeURIComponent(queryParams.searchString));
            }
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getBasicHeaders(), function(data) {
            if (callback && typeof(callback) == 'function') {
                callback(data);
            }
        })
    }

    var createFolder = function(parameter, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/enterprisews/createFolder";
        networkService.post(url, parameter, getBasicHeaders(), function(data) {
            if (callback && typeof(callback) == 'function') {
                callback(data);
            }
        })
    }

    var checkIfFilePathExists = function(pathId, callback) {

        if(!pathId.endsWith(".nxl")){
            pathId = pathId + ".nxl";
        }
        
        var parameter = {
            "parameters": {
                "pathId": pathId
            }
        };

        var url = RMS_CONTEXT_NAME + "/rs/enterprisews/file/checkIfExists";
        networkService.post(url, parameter, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var deleteFilesFolders = function(parameter, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/rs/enterprisews/delete", parameter, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getFileDetails = function(parameter, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/rs/enterprisews/file/metadata", parameter, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var modifyRights = function(parameter, callback) {
        networkService.put(RMS_CONTEXT_NAME + "/rs/enterprisews/file/classification", parameter, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var showFile = function(folder, params, callback) {
        networkService.post(VIEWER_URL + "/RMSViewer/ShowWorkspaceFile", params, {
            'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
        }, callback);
    };

    var downloadWorkspaceFile = function(filePath) {
        window.open(RMS_CONTEXT_NAME + "/RMSViewer/DownloadFileFromWorkspace?pathId=" + encodeURIComponent(filePath));
    }

    var decryptWorkspaceFile = function(filePath) {
        window.open(RMS_CONTEXT_NAME + "/RMSViewer/DownloadFileFromWorkspace?pathId=" + encodeURIComponent(filePath) + "&decrypt=true");
    }

    return {
        getFiles: getFiles,
        createFolder: createFolder,
        getFileDetails: getFileDetails,
        modifyRights: modifyRights,
        showFile: showFile,
        downloadWorkspaceFile: downloadWorkspaceFile,
        decryptWorkspaceFile: decryptWorkspaceFile,
        deleteFilesFolders: deleteFilesFolders,
        checkIfFilePathExists: checkIfFilePathExists
    }

}]);
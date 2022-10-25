mainApp.factory('repositoryService', ['$http', '$filter', '$cookies', 'networkService', 'initSettingsService', function($http, $filter, $cookies, networkService, initSettingsService) {

    var message = "";
    var success = false;
    var display = false;
    var reposFirstTimeAdded = false;
    var headers = {
        'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
    };
    var getJsonHeaders = function() {
        return {
            'Content-Type': 'application/json',
            'userId': window.readCookie("userId"),
            'ticket': window.readCookie("ticket"),
            'clientId': window.readCookie("clientId"),
            'platformId': window.readCookie("platformId")
        };
    }
    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
    var managedRepositoryHashSet = {};

    var FILE_TYPE = {
        '3dxml': true,
        'catpart': true,
        'catshape': true,
        'cgr': true,
        'iges': true,
        'igs': true,
        'ipt': true,
        'par': true,
        'prt': true,
        'psm': true,
        'rh': true,
        'step': true,
        'stl': true,
        'stp': true,
        'tiff': true,
        'x_b': true,
        'x_t': true,
        'xmt_txt': true,
        'bmp': true,
        'doc': true,
        'docx': true,
        'dwg': true,
        'dxf': true,
        'ext': true,
        'gif': true,
        'jpg': true,
        'jt': true,
        'pdf': true,
        'png': true,
        'ppt': true,
        'pptx': true,
        'rtf': true,
        'tif': true,
        'txt': true,
        'vds': true,
        'vsd': true,
        'vsdx': true,
        'xls': true,
        'xlsx': true,
        'zip': true,
        'c': true,
        'h': true,
        'js': true,
        'xml': true,
        'log': true,
        'vb': true,
        'm': true,
        'swift': true,
        'py': true,
        'java': true,
        'cpp': true,
        'err': true,
        'md': true,
        'sql': true,
        'csv': true,
        'dotx': true,
        'docm': true,
        'potm': true,
        'potx': true,
        'xltm': true,
        'xlsb': true,
        'xlsm': true,
        'xlt': true,
        'xltx': true,
        'json':true,
        'sldasm':true,
        'model':true,
        'properties':true,
        'sldprt':true
    }
    var getRepositoryList = function(callback) {
        networkService.get(RMS_CONTEXT_NAME + "/RMSViewer/GetRepositories", null, function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var getRepositories = function(successCallback, errorCallback) {
        var url = RMS_CONTEXT_NAME + '/rs/repository';

        networkService.get(url, getJsonHeaders(), function(data) {
            if (successCallback && typeof(successCallback) == "function") {
                successCallback(data);
            }
        }, function(response) {
            if (errorCallback && typeof(errorCallback) == "function") {
                errorCallback(response);
            }
        });
    };

    var removeRepository = function(repositoryId, successCallback, errorCallback) {
        var repoDetails = $.param({
            repoId: repositoryId
        });
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/RemoveRepository", repoDetails, headers, function(data) {
            if (successCallback && typeof(successCallback) == "function") {
                successCallback(data);
            }
        }, function(response) {
            if (errorCallback && typeof(errorCallback) == "function") {
                errorCallback(response);
            }
        });
    };

    var updateRepository = function(repository, repoName, successCallback, errorCallback) {
        if (managedRepositoryHashSet[repoName]) {
            successCallback({
                "result": false,
                "message": $filter('translate')('managerepo.duplicate.name') + repoName
            })
            return;
        }

        var repoDetails = $.param({
            repoId: repository.repoId,
            repoName: repoName
        });
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/UpdateRepository", repoDetails, headers, function(data) {
            if (successCallback && typeof(successCallback) == "function") {
                successCallback(data);
            }
        }, function(response) {
            if (errorCallback && typeof(errorCallback) == "function") {
                errorCallback(response);
            }
        });
    };


    var addRepository = function(params, callback) {
        var parameters = params;
        if (managedRepositoryHashSet[parameters.repoName]) {
            callback({
                "result": false,
                "message": $filter('translate')('managerepo.duplicate.name') + parameters.repoName
            })
            return;
        }

        var repoDetails = {
            "parameters": {
                "type": params.repoType,
                "siteURL": parameters.sitesUrl,
                "name": parameters.repoName,
                "redirect": true,
                "isShared": parameters.allow_all
            }
        };
        networkService.post(RMS_CONTEXT_NAME + "/rs/repository/authURL", repoDetails, getJsonHeaders(), function(data) {
            if (data.results != null && data.results.authURL != null && data.results.authURL != "") {
                window.location.replace(data.results.authURL);
            } else {
                callback({
                    "result": false,
                    "message": $filter('translate')('managerepo.add.error')
                })
            }
        });
    };

    var addSharePointOnPremiseRepo = function(params, callback){
        if (managedRepositoryHashSet[params.repoName]) {
            callback({
                "result": false,
                "message": $filter('translate')('managerepo.duplicate.name') + params.repoName
            })
            return;
        }
        var repo = {
            "name": params.repoName,
            "type": params.repoType,
            "isShared": params.allow_all,
            "accountName":params.sitesUrl,
            "accountId": "",
            "token": ""
        };
        repoDetails = {
            "parameters": {
                "repository": JSON.stringify(repo)
            }
        };
        networkService.post(RMS_CONTEXT_NAME+"/rs/repository", repoDetails, getJsonHeaders(), function(data){
            var success = data.statusCode === 200;
            var respMsg;
            if (success) {
                respMsg = $filter('translate')('managerepo.add.success');
            } else if([304, 409, 4001, 4003].indexOf(data.statusCode) >= 0) {
                respMsg = data.message;
            } else {
                respMsg = $filter('translate')('managerepo.add.error');
            }
            callback({
                "result": success,
                "message": respMsg
            })
        });
    };

    var addedRepository = function(data) {
        if (data.url != null && data.url != "") {
            window.location.replace(data.url + "?name=" + $("#addDropboxName")[0].value + "&repoType=" + repoType);
        }
    };

    var getAllFiles = function(params, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/GetAllFiles", params, headers, callback);
    };

    var getFilesWithPath = function(params, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/GetFilesWithPath", params, headers, callback);
    };

    var getSearchResults = function(params, callback) {
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/GetSearchResults", params, headers, callback);
    };

    var showFile = function(folder, params, callback) {
        networkService.post(VIEWER_URL + "/RMSViewer/ShowFile", params, headers, callback);
    };

    var showSharedFile = function(params, callback) {
        networkService.post(VIEWER_URL + "/RMSViewer/ShowSharedFile", params, headers, callback);
    };

    var getFileDetails = function(file, callback) {
        var params = $.param({
            filePath: file.pathId,
            filePathDisplay: file.path,
            repoId: file.repoId
        });
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/GetFileDetails", params, headers, callback);
    };

    var getRepositoryFileDetails = function(file, callback) {
        var params = $.param({
            filePath: file.pathId,
            filePathDisplay: file.path,
            repoId: file.repoId
        });
        networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/GetRepositoryFileDetails", params, headers, callback);
    };

    var convertFavFileToJsonParams = function(file) {
        return {
            parameters: {
                files: [{
                    pathId: file.fileId,
                    pathDisplay: file.path,
                    parentFileId: file.parentFolderId,
                    fileSize: file.fileSize,
                    fileLastModified: file.lastModifiedTime
                }]
            }
        };
    }

    var markFileAsFavorite = function(file, successCallback, errorCallback) {
        var jsonParams = convertFavFileToJsonParams(file);
        var repoId = file.repoId;
        networkService.post(RMS_CONTEXT_NAME + "/rs/favorite/" + repoId, jsonParams, getJsonHeaders(), function(data) {
            if (data.statusCode === 200) {
                successCallback();
            } else {
                errorCallback();
            }
        });
    }

    var unmarkFileAsFavorite = function(file, successCallback, errorCallback) {
        var jsonParams = convertFavFileToJsonParams(file);
        var repoId = file.repoId;
        networkService.deleteRequest(RMS_CONTEXT_NAME + "/rs/favorite/" + repoId, jsonParams, getJsonHeaders(), function(data) {
            if (data.statusCode === 200) {
                successCallback();
            } else {
                errorCallback();
            }
        });
    }

    var getManagedRepositories = function(callback) {
        networkService.get(RMS_CONTEXT_NAME + "/RMSViewer/GetManagedRepositories", null, function(data) {
            managedRepositoryHashSet = {};
            if (data) {
                for (i = 0; i < data.length; i++) {
                    managedRepositoryHashSet[data[i].repoName] = true;
                }
            }
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var getFavoriteFiles = function(page, size, orderBy, searchString, successCallback) {
        var url = RMS_CONTEXT_NAME + "/rs/favorite/list";
        var query = [];
        if (page) {
            query.push('page=' + page);
        }
        if (size) {
            query.push('size=' + size);
        }
        if (orderBy) {
            query.push('orderBy=' + orderBy);
        }
        if (searchString) {
            query.push('q.fileName=' + searchString);
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getJsonHeaders(), function(data) {
            if (data.statusCode === 200) {
                successCallback(data.results);
            }
        });
    }

    var getMyDriveUsage = function(callback) {
        var params = {};
        params["parameters"] = {
            'userId': window.readCookie("userId"),
            'ticket': window.readCookie("ticket")
        };
        networkService.post(RMS_CONTEXT_NAME + "/rs/myDrive/getUsage", JSON.stringify(params), getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getMyDriveFiles = function(callback) {
        var params = {
            "parameters": {
                "pathId": "/",
                "userId": window.readCookie("userId"),
                "ticket": window.readCookie("ticket")
            }
        };

        networkService.post(RMS_CONTEXT_NAME + "/rs/myDrive/list", JSON.stringify(params), getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getMyDriveFileCount = function(callback) {
        var url = RMS_CONTEXT_NAME + '/rs/myDrive/fileCount';
        networkService.get(url, getJsonHeaders(), callback);
    }

    var getShortRepoName = function(data, DISPLAY_NAME_MAX_LENGTH) {
        if (data.length > DISPLAY_NAME_MAX_LENGTH) {
            var str = data.slice(0, DISPLAY_NAME_MAX_LENGTH - 1);
            str = str + "..."
        } else {
            str = data;
        }
        return str;
    }

    var searchActivatedAlias = false;
    var getSearchActivatedAlias = function() {
        return searchActivatedAlias;
    }
    var setSearchActivatedAlias = function(value) {
        searchActivatedAlias = value;
    }

    var getMessage = function() {
        return message;
    }

    var isSuccess = function() {
        return success;
    }

    var shouldDisplay = function() {
        return display;
    }

    var setMessageParams = function(isDisplay, isSuccess, msg) {
        display = isDisplay;
        success = isSuccess;
        message = msg;
    }

    var setRepoAddedFirstTime = function(isRepoFirstTimeAdded) {
        reposFirstTimeAdded = isRepoFirstTimeAdded;
    }

    var getRepoAddedFirstTime = function() {
        return reposFirstTimeAdded;
    }

    var getUsageBarWidth = function(usage, quota) {
        usage = usage != null ? usage : 0;
        quota = quota != null ? quota : 0;
        var width = 0;
        if (quota === 0) {
            width = 0;
        } else if (usage >= quota) {
            width = 100;
        } else {
            width = usage / quota * 100;
        }
        return width;
    }

    var getIconName = function(file) {
        var protected = file.protectedFile ? '_p' : '';
        var fileType = FILE_TYPE.hasOwnProperty(file.fileType) ? file.fileType : 'missing';
        return 'ui/img/file_' + fileType + protected + '.svg';
    }

    var checkInvalidCharacters = function(fileName) {
        var settings = initSettingsService.getSettings();
        var inbuiltServiceProvider = settings.inbuiltServiceProvider;
        var invalidCharactersInFilename = settings.invalidCharactersInFilename;
        
        if (inbuiltServiceProvider === 'ONEDRIVE_FORBUSINESS' && invalidCharactersInFilename.length > 0) {
            var OD4B_INVALIDCHARACTERS_FILENAME_S = '^.*[' + invalidCharactersInFilename.join('') + '].*$';
            var regex = new RegExp( OD4B_INVALIDCHARACTERS_FILENAME_S );            
            var results = regex.exec( fileName ); 
            return results == null ? false : true;          
        }            
        return false;
    }

    var checkIfMyVaultFilePathExists = function(pathId, callback) {

        if(!pathId.endsWith(".nxl")){
            pathId = pathId + ".nxl";
        }
        
        var parameter = {
            "parameters": {
                "pathId": pathId
            }
        };
        
        var rms_context_name_url = initSettingsService.getRMSContextName();
        networkService.post(rms_context_name_url + "/rs/myVault/file/checkIfExists", parameter, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    return {
        getRepositoryList: getRepositoryList,
        getRepositories: getRepositories,
        addRepository: addRepository,
        addSharePointOnPremiseRepo: addSharePointOnPremiseRepo,
        getSearchActivatedAlias: getSearchActivatedAlias,
        setSearchActivatedAlias: setSearchActivatedAlias,
        removeRepository: removeRepository,
        setMessageParams: setMessageParams,
        getMessage: getMessage,
        setRepoAddedFirstTime: setRepoAddedFirstTime,
        getRepoAddedFirstTime: getRepoAddedFirstTime,
        isSuccess: isSuccess,
        shouldDisplay: shouldDisplay,
        updateRepository: updateRepository,
        getAllFiles: getAllFiles,
        getFilesWithPath: getFilesWithPath,
        getSearchResults: getSearchResults,
        getFavoriteFiles: getFavoriteFiles,
        showFile: showFile,
        showSharedFile: showSharedFile,
        getFileDetails: getFileDetails,
        getRepositoryFileDetails: getRepositoryFileDetails,
        getManagedRepositories: getManagedRepositories,
        getMyDriveUsage: getMyDriveUsage,
        getMyDriveFiles: getMyDriveFiles,
        getShortRepoName: getShortRepoName,
        markFileAsFavorite: markFileAsFavorite,
        unmarkFileAsFavorite: unmarkFileAsFavorite,
        getUsageBarWidth: getUsageBarWidth,
        getIconName: getIconName,
        checkInvalidCharacters: checkInvalidCharacters,
        checkIfMyVaultFilePathExists: checkIfMyVaultFilePathExists,
        getMyDriveFileCount: getMyDriveFileCount
    }
}]);
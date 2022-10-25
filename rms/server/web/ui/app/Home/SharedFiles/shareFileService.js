mainApp.factory('shareFileService', ['$http', 'networkService', 'initSettingsService', function($http, networkService, initSettingsService) {


    var message = "";
    var success = false;
    var display = false;
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

    var getSharedWithMeFileDetails = function(file, spaceId, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/sharedWithMe/metadata/" + file.transactionId + "/" + file.transactionCode;
        var query = [];
        if (spaceId) {
            query.push('spaceId=' + encodeURIComponent(spaceId));
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getJsonHeaders(),  function(data){
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getSharedFileDetails = function(file, callback) {
        var url = RMS_CONTEXT_NAME + "/rs/myVault/" + file.duid + "/metadata";
        var params = {
            "parameters": {
                "pathId": file.pathId
            }
        };
        networkService.post(url, params, getJsonHeaders(), callback);
    };

    var updateSharedFile = function(params, callback) {
        var duid = params.duid;
        var input = {
            "parameters": {
                "newRecipients": params.addedRecipients,
                "removedRecipients": params.removedRecipients
            }
        };
        if (params.comment && params.comment.length > 0) {
            input.parameters.comment = params.comment;
        }
        networkService.post(RMS_CONTEXT_NAME + "/rs/share/" + duid + "/update", input, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var revokeFile = function(duid, callback) {
        networkService.deleteRequest(RMS_CONTEXT_NAME + "/rs/share/" + duid + "/revoke", null, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var reshareFile = function(params, callback) {
        RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        networkService.post(RMS_CONTEXT_NAME + "/rs/sharedWithMe/reshare", params, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    }

    var getAllSharedFiles = function(params, callback) {
        var url = RMS_CONTEXT_NAME + '/rs/myVault';
        var query = [];
        if (params) {
            if (params.page) {
                query.push('page=' + encodeURIComponent(params.page));
            }
            if (params.size) {
                query.push('size=' + encodeURIComponent(params.size));
            }
            if (params.orderBy) {
                query.push('orderBy=' + encodeURIComponent(params.orderBy));
            }
            if (params.fileName) {
                query.push('q.fileName=' + encodeURIComponent(params.fileName));
            }
            if (params.filterOptions) {
                query.push('filter=' + encodeURIComponent(params.filterOptions));
            }
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getJsonHeaders(), callback);
    };

    var getMyVaultFileCount = function(callback) {
        var url = RMS_CONTEXT_NAME + '/rs/myVault/fileCount';
        networkService.get(url, getJsonHeaders(), callback);
    }

    var getSharedWithMeFiles = function(params, callback) {
        var url = RMS_CONTEXT_NAME + '/rs/sharedWithMe/list';
        var query = [];
        if (params) {
            if (params.page) {
                query.push('page=' + encodeURIComponent(params.page));
            }
            if (params.size) {
                query.push('size=' + encodeURIComponent(params.size));
            }
            if (params.orderBy) {
                query.push('orderBy=' + encodeURIComponent(params.orderBy));
            }
            if (params.fileName) {
                query.push('q=name');
                query.push('searchString=' + encodeURIComponent(params.fileName));
            }
            if (params.fromSpace) {
                query.push('fromSpace=' + encodeURIComponent(params.fromSpace));
            }
            if (params.spaceId) {
                query.push('spaceId=' + encodeURIComponent(params.spaceId));
            }
        }
        if (query.length > 0) {
            url = url + '?' + query.join('&');
        }
        networkService.get(url, getJsonHeaders(), callback);
    };

    var getSharedFileActivityLog = function(params, callback) {

        var url = RMS_CONTEXT_NAME + "/rs/log/v2/activity/" + params.duid;
        url = url + "?start=" + params.start + "&count=" + params.count;
        url = url + "&orderBy=" + params.orderBy + "&orderByReverse=" + params.orderByReverse;
        if (params.searchField && params.searchField.trim() != "") {
            url = url + "&searchField=" + encodeURIComponent(params.searchField);
            url = url + "&searchText=" + encodeURIComponent(params.searchText);
        }
        url = url + "&t=" + new Date().getTime();
        networkService.get(url, getJsonHeaders(), function(data) {
            if (callback && typeof(callback) == "function") {
                callback(data);
            }
        });
    };

    var getSortOptions = function(isSharedWithMe) {
        if (isSharedWithMe) {
            return [{
                    'lookupCode': '-sharedDate',
                    'description': 'last.modified'
                },
                {
                    'lookupCode': 'sharedDate',
                    'description': 'first.modified'
                },
                {
                    'lookupCode': 'name',
                    'description': 'filename.ascending'
                },
                {
                    'lookupCode': '-name',
                    'description': 'filename.descending'
                },
                {
                    'lookupCode': 'size',
                    'description': 'file.size.ascending'
                },
                {
                    'lookupCode': '-size',
                    'description': 'file.size.descending'
                },
                {
                    'lookupCode': '-sharedBy',
                    'description': 'sharedby.descending'
                },
                {
                    'lookupCode': 'sharedBy',
                    'description': 'sharedby.ascending'
                }
            ];
        } else {
            return [{
                    'lookupCode': '-creationTime',
                    'description': 'last.modified'
                },
                {
                    'lookupCode': 'creationTime',
                    'description': 'first.modified'
                },
                {
                    'lookupCode': 'fileName',
                    'description': 'filename.ascending'
                },
                {
                    'lookupCode': '-fileName',
                    'description': 'filename.descending'
                },
                {
                    'lookupCode': 'size',
                    'description': 'file.size.ascending'
                },
                {
                    'lookupCode': '-size',
                    'description': 'file.size.descending'
                }
            ];
        }
    }

    return {
        getSharedFileDetails: getSharedFileDetails,
        updateSharedFile: updateSharedFile,
        revokeFile: revokeFile,
        reshareFile: reshareFile,
        getAllSharedFiles: getAllSharedFiles,
        getSharedFileActivityLog: getSharedFileActivityLog,
        getSharedWithMeFiles: getSharedWithMeFiles,
        getSortOptions: getSortOptions,
        getSharedWithMeFileDetails: getSharedWithMeFileDetails,
        getMyVaultFileCount: getMyVaultFileCount
    }
}]);
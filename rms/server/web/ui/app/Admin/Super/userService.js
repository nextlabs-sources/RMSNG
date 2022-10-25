mainApp.factory('userService', ['$http', 'networkService', 'initSettingsService',
    function($http, networkService, initSettingsService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var getUserList = function (queryParams, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/usr/list";
            var query = [];
            if (queryParams) {
                if (queryParams.searchString) {
                    query.push('searchString=' + encodeURIComponent(queryParams.searchString));
                }
            }
            if (query.length > 0) {
                url += "?" + query.join('&');
            }
            networkService.get(url, getJsonHeaders(), function (data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var assignAPIUser = function (userId, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/usr/apiUser";
            var params = {
                parameters: {
                    apiUserId: userId
                }
            };
            networkService.put(url, JSON.stringify(params), getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var removeAPIUserCert = function (userId, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/usr/apiUser/cert";
            var params = {
                parameters: {
                    apiUserId: userId
                }
            };
            networkService.deleteRequest(url, JSON.stringify(params), getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        return {
            getUserList: getUserList,
            assignAPIUser: assignAPIUser,
            removeAPIUserCert: removeAPIUserCert
        }
    }]);
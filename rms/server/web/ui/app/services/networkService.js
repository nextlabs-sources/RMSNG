/*
  This is a shared service is a gateway for network calls (ajax)
*/
/* Please also update the file in Viewer.js */
var errorHandler = function(response) {
    // called asynchronously if an error occurs or server returns response with an error status.
    // hadle generic network errors 
    switch (response.status) {
        case 400:
            // not found;
            break;
        case 401:
            /*
            Accessing the global variable CONTEXT_PATH and initSettingsData from index.jsp instead of injecting 
            initSettingsService to prevent circular dependencies
            */
            window.location.href = CONTEXT_PATH + "/timeout";
            break;
        case 500:
            // internal server error
            break;
        default:
            // unknown    
    }
}

var getBasicHeaders = function() {
    return {
        'userId': window.readCookie("userId"),
        'ticket': window.readCookie("ticket"),
        'clientId': window.readCookie("clientId"),
        'platformId': window.readCookie("platformId")
    };
}

var getTextPlainHeaders = function() {
    var result = getBasicHeaders.apply(this, arguments);
    result['Content-Type'] = 'text/plain';
    return result;
}

var getJsonHeaders = function() {
    var result = getBasicHeaders.apply(this, arguments);
    result['Content-Type'] = 'application/json';
    return result;
}

var getJsonHeadersPolicy = function(bool) {
    var result = getBasicHeaders.apply(this, arguments);
    result['Content-Type'] = 'application/json';
    result['membershipPolicy'] = bool;
    return result;
}

var successCallback = function(response, callback) {
    if ((response.data && response.data.statusCode === 401)) {
        window.location.href = CONTEXT_PATH + "/timeout";
        return;
    }
    if (callback && typeof(callback) == "function") {
        callback(response.data);
    }
}

mainApp.factory('networkService', ['$http', '$state', '$window', function($http, $state, $window) {

    var get = function(url, headers, callback, failureCallback) {
        var uniqueParamStr = "t=" + new Date().getTime();
        if (url.indexOf("?") >= 0) {
            url = url + "&" + uniqueParamStr;
        } else {
            url = url + "?" + uniqueParamStr;
        }

        $http({
            method: 'GET',
            url: url,
            headers: headers
        }).then(function(response) {
            successCallback(response, callback)
        }, function error(response) {
            failureCallback ? failureCallback(response) : errorHandler(response);
        });
    }

    var post = function(url, data, headers, callback, failureCallback) {
        $http({
            withCredentials: true,
            method: 'POST',
            url: url,
            headers: headers,
            data: data
        }).then(function(response) {
            successCallback(response, callback)
        }, function error(response) {
            failureCallback ? failureCallback(response) : errorHandler(response);
        });
    }

    var put = function(url, data, headers, callback, failureCallback) {
        $http({
            method: 'PUT',
            url: url,
            headers: headers,
            data: data
        }).then(function(response) {
            successCallback(response, callback)
        }, function error(response) {
            failureCallback ? failureCallback(response) : errorHandler(response);
        });
    }

    var deleteRequest = function(url, data, headers, callback, failureCallback) {
        $http({
            url: url,
            method: 'DELETE',
            headers: headers,
            data: data
        }).then(function(response) {
            successCallback(response, callback)
        }, function error(response) {
            failureCallback ? failureCallback(response) : errorHandler(response);
        });
    }

    var postAsFormData = function(url, data, headers, callback, failureCallback) {
        var postHeaders;
        if (!headers) {
            postHeaders = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };
        } else {
            postHeaders = headers;
            postHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
        }
        $http({
            withCredentials: true,
            method: 'POST',
            url: url,
            headers: postHeaders,
            transformRequest: function(obj) {
                var str = [];
                for (var p in obj)
                    str.push($window.encodeURIComponent(p) + "=" + $window.encodeURIComponent(obj[p]));
                return str.join("&");
            },
            data: data
        }).then(function(response) {
            successCallback(response, callback)
        }, function error(response) {
            failureCallback ? failureCallback(response) : errorHandler(response);
        });
    }

    return {
        get: get,
        deleteRequest: deleteRequest,
        put: put,
        post: post,
        postAsFormData: postAsFormData,
    }
}]);
mainApp.factory('policyService', ['$http', '$filter', 'networkService', 'initSettingsService', 'projectStateService',
    function($http, $filter, networkService, initSettingsService, projectStateService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var getPoliciesBase = function (parameter, callback, header) {
            var url = RMS_CONTEXT_NAME + "/rs/policy/" + parameter.tokenGroupName + "/policies";
            url = parameter.query? url + '?' + parameter.query : url;
            networkService.get(url, header, function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var getPolicies = function(parameter, callback) {
            getPoliciesBase(parameter, callback, getJsonHeaders());
        }

        var getProjectPolicies = function (parameter, callback) {
            var headers = getJsonHeaders();
            headers.membershipPolicy = true;
            getPoliciesBase(parameter, callback, headers);
        }

        return {
            getPolicies: getPolicies,
            getProjectPolicies: getProjectPolicies
        }
    }
]);
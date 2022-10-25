mainApp.factory('tenantService', ['$http', 'networkService', 'initSettingsService',
    function($http, networkService, initSettingsService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var superAdminList;
        
        var getSuperAdminList = function () {
            return superAdminList;
        }
        
        var setSuperAdminList = function (value) {
            superAdminList = value;
        }

        var getSortOptions = function () {
            return [
                {
                    'lookupCode': '-creationTime',
                    'description': 'last.modified'
                },
                {
                    'lookupCode': 'creationTime',
                    'description': 'first.modified'
                },
                {
                    'lookupCode': 'tenantName',
                    'description': 'filename.ascending'
                },
                {
                    'lookupCode': '-tenantName',
                    'description': 'filename.descending'
                },
            ];
        }

        var getTenantList = function(queryParams, callback) {
            var query = [];
            var url = RMS_CONTEXT_NAME + "/rs/tenant/list";
            if (queryParams) {
                if (queryParams.orderBy) {
                    query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
                }
            }
            if (query.length > 0) {
                url += "?" + query.join('&');
            }
            networkService.get(RMS_CONTEXT_NAME + "/rs/tenant/list", getJsonHeaders(), function(data) {
                if (data != null && data.statusCode == 200) {
                    callback(data.results);
                } else {
                    callback();
                }
            });
        }

        var deleteTenant = function(tenantName, callback) {
            networkService.deleteRequest(RMS_CONTEXT_NAME + "/rs/tenant/" + tenantName, null, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        };

        var createTenant = function(tenant, callback) {
            var params = {
                parameters: {
                    tenantName: tenant.name,
                    admin: tenant.adminListStr,
                    server: "https://" + tenant.dnsName + "/rms",
                    dns: tenant.dnsName,
                    description: tenant.description
                }
            };
            networkService.post(RMS_CONTEXT_NAME + "/rs/tenant/create", JSON.stringify(params), getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var updateTenant = function(tenant, callback) {
            var params = {
                parameters: {
                    admin: tenant.adminListStr,
                    description: tenant.description
                }
            };
            networkService.post(RMS_CONTEXT_NAME + "/rs/tenant/" + tenant.name, JSON.stringify(params), getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            })
        }

        return {
            getSortOptions: getSortOptions,
            getTenantList: getTenantList,
            deleteTenant: deleteTenant,
            createTenant: createTenant,
            updateTenant: updateTenant,
            getSuperAdminList: getSuperAdminList,
            setSuperAdminList: setSuperAdminList
        }
}]);
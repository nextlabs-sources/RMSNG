mainApp.controller('manageProjectPoliciesController', ['$scope', '$rootScope', 'policyService', '$filter', '$state', '$stateParams', 'initSettingsService', 'projectStateService', 'networkService',
    function ($scope, $rootScope, policyService, $filter, $state, $stateParams, initSettingsService, projectStateService, networkService) {
        const POLICY_PAGE = 0;
        const POLICY_SIZE = 0;
        const RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var bool = ($state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES || $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE || $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT || $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST) ? true : false;

        $scope.projectPoliciesList = [];
        $scope.policyFilter = {};

        $scope.getProjectPolicies = function () {
            var query = [];
            $scope.page = POLICY_PAGE;
            $scope.size = POLICY_SIZE;
            query.push('page=' + encodeURIComponent($scope.page));
            query.push('size=' + encodeURIComponent($scope.size));
            if ($scope.policyFilter.orderPolicyBy) {
                query.push('orderBy=' + encodeURIComponent($scope.policyFilter.orderPolicyBy));
            }
            if ($scope.policyFilter.searchPolicyString) {
                query.push('searchString=' + encodeURIComponent($scope.policyFilter.searchPolicyString));
            }
            $rootScope.isLoading = true;
            policyService.getProjectPolicies({query : query.join('&'), tokenGroupName : readCookie('lt')} ,function(data){
                $rootScope.isLoading = false;
                var message;
                if(data.statusCode === 200) {
                    $scope.projectPoliciesList = data.results.policies;
                } else {
                    message = $filter('translate')('policy.list.error');
                }
                if (message) {
                    showSnackbar({
                        isSuccess: false,
                        messages: message
                    });
                }
            });
        };

        $scope.policyFilter.searchPolicyString = "";

        $scope.sortPolicyOptions = [
            {
                'lookupCode': '-lastUpdatedDate',
                'description': 'last.modified'
            },
            {
                'lookupCode': 'lastUpdatedDate',
                'description': 'first.modified'
            },
            {
                'lookupCode': 'name',
                'description': 'filename.ascending'
            },
            {
                'lookupCode': '-name',
                'description': 'filename.descending'
            }
        ];

        $scope.onClickMenu = function(id) {
            $scope.selectedFileId = id;
            $scope.toggleMenuMode();
        };

        $scope.showMenu = function(id) {
            $scope.selectedFileId = id;
            $scope.MenuClickedMode = true;
        };

        $scope.hideMenu = function(id) {
            $scope.selectedFileId = id;
            $scope.MenuClickedMode = false;
        };

        $scope.MenuClickedMode = false;

        $scope.toggleMenuMode = function() {
            $scope.MenuClickedMode = !$scope.MenuClickedMode;
        };

        $scope.deployPolicy = function (id) {
            var url = RMS_CONTEXT_NAME + "/rs/policy/" + readCookie('lt') + "/policies/deploy";
            var ids = [];
            ids.push(id);
            var params = {
                parameters: {
                    ids: ids
                }
            };
            var headers = getJsonHeadersPolicy(bool);
            headers.membershipPolicy = true;
            $scope.isLoading = true;
            networkService.post(url, params, headers, function (data) {
                $scope.isLoading = false;
                var isSuccess;
                var message;
                if (data.statusCode === 200) {
                    isSuccess = true;
                    message = $filter('translate')('policy.deploy.success');
                    $scope.getProjectPolicies();
                } else {
                    isSuccess = false;
                    message = $filter('translate')('policy.deploy.error');
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: message
                });
            });
        }

        $scope.undeployPolicy = function (id) {
            var url = RMS_CONTEXT_NAME + "/rs/policy/" + readCookie('lt') + "/policies/undeploy";
            var ids = [];
            ids.push(id);
            var params = {
                parameters: {
                    ids: ids
                }
            };
            var headers = getJsonHeadersPolicy(bool);
            headers.membershipPolicy = true;
            $scope.isLoading = true;
            networkService.post(url, params, headers, function (data) {
                $scope.isLoading = false;
                var isSuccess;
                var message;
                if (data.statusCode === 200) {
                    isSuccess = true;
                    message = $filter('translate')('policy.undeploy.success');
                    $scope.getProjectPolicies();
                } else {
                    isSuccess = false;
                    message = $filter('translate')('policy.undeploy.error');
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: message
                });
            });
        }

        $scope.clearPolicySearch = function() {
            $scope.policyFilter.searchPolicyString = "";
            $scope.getProjectPolicies();
        }

        $scope.deletePolicy = function (id) {
            var url = RMS_CONTEXT_NAME + "/rs/policy/" + readCookie('lt') + "/policies/" + id;
            var headers = getJsonHeadersPolicy(bool);
            headers.membershipPolicy = true;
            $scope.isLoading = true;
            networkService.deleteRequest(url, null, headers, function (data) {
                $scope.isLoading = false;
                var isSuccess;
                var message;
                if (data.statusCode === 200) {
                    isSuccess = true;
                    message = $filter('translate')('policy.delete.success');
                    $scope.getProjectPolicies();
                } else {
                    isSuccess = false;
                    message = $filter('translate')('policy.delete.error');
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: message
                });
            });
        }


        $scope.init = function () {
            $scope.policyFilter.orderPolicyBy = $scope.sortPolicyOptions[0].lookupCode;

            if ($stateParams.tenantId) {
                projectStateService.setTenantId($stateParams.tenantId);
            }
            $scope.getProjectPolicies();
        };

        $scope.init();
}]);
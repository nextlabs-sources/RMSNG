mainApp.controller('adminHomeController', ['$interval', '$scope', '$rootScope', '$http', '$cookies', '$location', '$state', 'initSettingsService', 'navService', '$stateParams', '$filter', '$uibModal', '$uibModalStack',
    function($interval, $scope, $rootScope, $http, $cookies, $location, $state, initSettingsService, navService, $stateParams, $filter, $uibModal, $uibModalStack) {
        $scope.checked = true;
        $scope.isDefaultTenant = readCookie('ltId') === readCookie('tenantId');
        //Decoding the value of tenant name
        $scope.loginTenant = readCookie('lt').replace("+"," ");

        //$rootScope.$on("reloadSettings", function() {
            initSettingsService.reloadScopeSettings($scope);
        //});

         $scope.onClickTab = function(tab) {
            navService.setCurrentTab(tab);
            $rootScope.$emit("toggleSideBar");
            var tenantId = readCookie("ltId");
            if (tab == 'policies') {
                navService.setIsInAllFilesPage(false);
                $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_TENANT_POLICIES_LIST, {tenantId: tenantId}, {reload: true}) : $state.go(STATE_TENANT_ADMIN_TENANT_POLICIES_LIST, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'classification') {
                navService.setIsInAllFilesPage(false);
                $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_TENANT_CLASSIFICATION, {tenantId: tenantId}, {reload: true}) : $state.go(STATE_TENANT_ADMIN_TENANT_CLASSIFICATION, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'systemConfiguration') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_SYSTEM_CONFIGURATION, {reload: true});
                return;
            } else if (tab == 'tenantConfiguration') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_CONFIGURATION, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'userAttributeMapping') {
                navService.setIsInAllFilesPage(false);
                $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_SELECT_USER_ATTRIBUTE, {tenantId: tenantId}, {reload: true}) : $state.go(STATE_TENANT_ADMIN_SELECT_USER_ATTRIBUTE, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'assignProjectAdmin') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_PROJECT_ADMIN_ASSIGN, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'configureServiceProvider') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_CONFIGURE_SERVICE_PROVIDER, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'tenantList') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_LIST, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'configureIdentityProvider') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_CONFIGURE_IDENTITY_PROVIDER, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'manageProjectTags') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_TAGS, {tenantId: tenantId}, {reload: true});
                return;
            } else if (tab == 'manageProjectPolicies') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST, {tenantId: tenantId}, {reload: true});
                return;
            }
        }

        $scope.isActiveTab = function(tab) {
            return navService.getCurrentTab() == tab;
        }


        $scope.goToLanding = function() {
            if ($scope.switchDialog) {
                $scope.switchDialog.close();
            }
            initSettingsService.reloadSettings(function success() {
                    //$rootScope.$emit("reloadSettings");
                    initSettingsService.reloadScopeSettings($scope);
                }, function error() {                    
            });
            $state.go(STATE_ADMIN_LANDING);
        }

        var serverUrl = function() {
            var borderIndex = $location.absUrl().indexOf(initSettingsService.getRMSContextName());
            return $location.absUrl().substring(0, borderIndex);
        }
		
        $scope.toggleSideBar = function() {
            $scope.checked = !$scope.checked;
            if (!$scope.checked) {
                $("#rms-home-footer-mobile").css("visibility", "hidden");
            } else {
                $("#rms-home-footer-mobile").css("visibility", "visible");
            }
        }


        $(window).on('hashchange', function() {
            $uibModalStack.dismissAll();
        });

        $rootScope.$on("toggleSideBar", function() {
            $scope.toggleSideBar();
        });

        $scope.doLogout = function() {
            initSettingsService.logout();
        };

        $scope.$on('$stateChangeStart', function() {
            $scope.scrollToTop = true;
        });

        $scope.getUserFullDisplayName = function(displayName, email) {
            return displayName === email ? displayName : displayName + ' (' + email + ')';
        }
    }
]);
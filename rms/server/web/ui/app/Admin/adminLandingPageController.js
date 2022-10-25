mainApp.controller('adminLandingPageController', ['$scope', '$rootScope', '$cookies', '$state', '$uibModal', 'networkService', 'dialogService', '$location', 'initSettingsService', 'navService', '$filter', 'landingPageService', '$anchorScroll',
    function($scope, $rootScope, $cookies, $state, $uibModal, networkService, dialogService, $location, initSettingsService, navService, $filter, landingPageService, $anchorScroll) {
        //Decoding the value of tenant name
        $scope.loginTenant = readCookie('lt').replace("+"," ");
        $scope.isDefaultTenant = readCookie('ltId') === readCookie('tenantId');
        $scope.isSetting = true;
        $scope.isUserMgmt = false;
        $scope.isOverview = false;
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        $scope.setMenuFlag = function(tab) {
            switch (tab) {
                case 'setting':
                    var previousTab = $scope.isSetting;
                    $scope.isSetting = true;
                    $scope.isUserMgmt = false;
                    $scope.isOverview = false;
                    if (!previousTab) {
                        $scope.onClickTab('adminDashboard');
                    }
                    break;
                case 'user':
                    var previousTab = $scope.isUserMgmt;
                    $scope.isSetting = false;
                    $scope.isUserMgmt = true;
                    $scope.isOverview = false;
                    if (!previousTab) {
                        $scope.onClickTab('manageUser');
                    }
                    break;
                case 'overview':
                    $scope.isSetting = false;
                    $scope.isUserMgmt = false;
                    $scope.isOverview = true;
                    break;
                default:
                    break;
            }
        }

        $scope.init = function() {
            $scope.isLoading = true;
            switch ($state.current.name) {
                case STATE_ADMIN_LANDING:
                    navService.setCurrentTab("adminDashboard");
                    break;
                case STATE_ADMIN_TENANT_CREATE:
                    $scope.isAddTenant = true;
                    $scope.tenant = {};
                    break;
                case STATE_ADMIN_TENANT_EDIT:
                    $scope.isAddTenant = false;
                    $scope.tenant = $state.params.tenant;
                    break;
                case STATE_ADMIN_LANDING:
                    $scope.showMenuFlag = false;
                    $scope.getTenantList();
                    break;
                case STATE_MANAGE_USER:
                    $scope.setMenuFlag('user');
                    break;
                default:
                    break;
            }
            $scope.isLoading = false;
        }

        $scope.isActiveTab = function(tab) {
            return navService.getCurrentTab() == tab;
        }

        $scope.onClickTab = function(tab) {
            navService.setCurrentTab(tab);
            switch (tab) {
                case 'adminDashboard':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_LANDING, {tenantId: readCookie('ltId')});
                    break;
                case 'policies':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_TENANT_POLICIES_LIST, {tenantId: readCookie('ltId')}) : $state.go(STATE_TENANT_ADMIN_TENANT_POLICIES_LIST, {tenantId: readCookie('ltId')});
                    break;
                case 'tenantList':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_LIST, {tenantId: readCookie('ltId')});
                    break;
                case 'classification':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_TENANT_CLASSIFICATION, {tenantId: readCookie('ltId')}, {reload: true}) : $state.go(STATE_TENANT_ADMIN_TENANT_CLASSIFICATION, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'systemConfiguration':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_SYSTEM_CONFIGURATION, {reload: true});
                    break;
                case 'tenantConfiguration':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_CONFIGURATION, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'userAttributeMapping':
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $scope.isDefaultTenant && !$scope.showTenantConsole ? $state.go(STATE_ADMIN_SELECT_USER_ATTRIBUTE, {tenantId: readCookie('ltId')}, {reload: true}) : $state.go(STATE_TENANT_ADMIN_SELECT_USER_ATTRIBUTE, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'assignProjectAdmin':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_PROJECT_ADMIN_ASSIGN, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'configureServiceProvider':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_CONFIGURE_SERVICE_PROVIDER, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'configureIdentityProvider':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_CONFIGURE_IDENTITY_PROVIDER, {tenantId: readCookie('ltId')}, {reload: true});
                    break;
                case 'manageProjectTags':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_TAGS, {tenantId: readCookie('ltId')}, {reload: true});
                    break;   
                case 'manageUser':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_MANAGE_USER);
                    break;     
                case 'manageProjectPolicies':
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST, {tenantId: readCookie('ltId')}, {reload: true});
                    break;  
                default:
                    break;
            }
            return;
        }

        $scope.init();

        //$rootScope.$on("reloadSettings", function() {
            initSettingsService.reloadScopeSettings($scope);
        //});

        var getHeaders = function() {
            return {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8',
                'userId': $cookies.get('userId'),
                'ticket': $cookies.get('ticket'),
                'clientId': $cookies.get('clientId'),
                'platformId': $cookies.get('platformId')
            };
        }

        var getProfile = function() {
            networkService.get(RMS_CONTEXT_NAME + "/rs/usr/v2/profile/", getHeaders(), function(data) {
                // isLoadingProfile = false;
                // $scope.isLoadingSummary = isLoadingRepoList || isLoadingProfile;
                if (data != null && data.statusCode == 200) {
                    $scope.displayName = data.extra.name;
                    $scope.email = data.extra.email;
                    // if (data.extra.preferences && data.extra.preferences.profile_picture) {
                    //     $scope.profilePictureUrl = data.extra.preferences.profile_picture;
                    // }
                    // if (!data.extra.preferences || !data.extra.preferences.homeTour) {
                    //     $scope.skipHomeTour = false;
                    // }
                }
            });
        }

        getProfile();
    }
]);

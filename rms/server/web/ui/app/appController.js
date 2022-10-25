mainApp.controller('appController', ['$scope', '$state', 'networkService', '$location', 'initSettingsService', '$rootScope',
    function($scope, $state, networkService, $location, initSettingsService, $rootScope) {

        $rootScope.$on("reloadSettings", function() {
            initSettingsService.reloadScopeSettings($scope);
        });
        
        var settings = initSettingsService.getSettings();
        $scope.rmsVersion = settings.rmsVersion;
        $scope.isRMCConfigured = settings.isRMCConfigured;
        $rootScope.isSystemAdmin = $scope.isSystemAdmin = checkRoles(settings.roles,'SYSTEM_ADMIN');
        $rootScope.isTenantAdmin = $scope.isTenantAdmin = checkRoles(settings.roles,'TENANT_ADMIN');
        $rootScope.isProjectAdmin = $scope.isProjectAdmin = checkRoles(settings.roles,'PROJECT_ADMIN');
        $rootScope.saasMode = $scope.saasMode = settings.isSaasMode;
        $rootScope.hideWorkspace = $scope.hideWorkspace = settings.isHideWorkspace;

        if (!$rootScope.isSystemAdmin && !$rootScope.isTenantAdmin && APPNAME === 'admin') {
            $state.go(STATE_ADMIN_UNAUTHORIZED);
        }

        (function() {
            window.addEventListener("resize", resizeThrottler, false);
            var resizeTimeout;

            function resizeThrottler() {
                // ignore resize events as long as an actualResizeHandler execution is in the queue
                if (!resizeTimeout) {
                    resizeTimeout = setTimeout(function() {
                        resizeTimeout = null;
                        actualResizeHandler();
                        // The actualResizeHandler will execute at a rate of 15fps
                    }, 66);
                }
            }

            function actualResizeHandler() {
                initSettingsService.setRightPanelMinHeight();
            }
        }());


        /*
        if($location.path().indexOf('/home/viewSharedFile') != 0 && $location.path().indexOf('/home/manageRepositories') != 0 && $location.path().indexOf('/home/repositories') != 0 &&
        	$location.path().indexOf('/home/sharedFiles') != 0 && $location.path().indexOf('/home/serviceProviders') != 0 && $location.path().indexOf('/home/settings') && $location.path().indexOf('/home/manageLocalFile')){
        	switch(settings.landingPage){
        		case "WELCOME":
        			$state.go(STATE_WELCOME_PAGE);
        			return;
        		case "HOME":
        			$state.go(STATE_ALL_REPOSITORIES);
        			return;
        		case "SP":
        			$state.go(STATE_SERVICE_PROVIDERS);
        			return;
        		case "MANAGE_REPO":
        			$state.go(STATE_MANAGE_REPOSITORIES);
        			return;
        		default:
        			$state.go(STATE_ALL_REPOSITORIES);
        	}
        }
        */
    }
]);
mainApp.controller('loginAdminController', ['$scope', '$rootScope', '$state', '$http', '$filter', 'dialogService', 'initSettingsService',
    function($scope, $rootScope, $state, $http, $filter, dialogService, initSettingsService) {

        $scope.isRMDDownloadable = initSettingsService.getClientSettings().isRMDDownloadable;
        $scope.isManageProfileAllowed = initSettingsService.getSettings().isManageProfileAllowed;
        $scope.isAdmin = initSettingsService.getSettings().isAdmin;
        $scope.loggedInUserName = initSettingsService.getSettings().userDisplayName;

        var rmsContextName = initSettingsService.getRMSContextName();
        $scope.toggleSideBar = function() {
            $rootScope.$emit("toggleSideBar");
        }
        $scope.doLogout = function() {
            initSettingsService.logout();
        };

        $scope.manageProfile = function() {
            dialogService.displayProfile();
        };

        $scope.managePreference = function() {
            $state.go(STATE_USER_PREFERENCE);
        }

        $scope.manageConfigurations = function() {
            $state.go(STATE_TENANT_CONFIG);
            $scope.$broadcast("onTenantConfigurationTabChange", 0);
        }

        $scope.welcome = function() {
            $state.go(STATE_WELCOME_PAGE);
        };

        $scope.submitFeedBack = function() {
            dialogService.submitFeedBack({}, function(data) {})
        };

        $rootScope.$on('profile.displayName.updated', function(event, data) {
            $scope.loggedInUserName = data;
        });

        $scope.help = function(page) {
            openNewWindow(rmsContextName + '/help/index.html', 'myWindow');
        };
    }
])
mainApp.controller('welcomeController', ['$scope', '$rootScope', '$filter', '$state', 'networkService', '$location',
    'navService', 'initSettingsService',
    function($scope, $rootScope, $filter, $state, networkService, $location,
        navService, initSettingsService) {
        navService.setCurrentTab(-1);
        navService.setIsInAllFilesPage(false);
        $scope.SHOW_HIDE_WELCOME_PAGE_CONTROL = true;

        $scope.isRMCDownloadable = initSettingsService.getClientSettings().isRMCDownloadable;

        var headers = {
            'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
        };
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var init = function() {
            var initData = initSettingsService.getSettings();
            if (initData != null) {
                $scope.videoURL = initData.welcomePageVideoURL;
                if ("WELCOME" != initData.landingPage) {
                    $scope.SHOW_HIDE_WELCOME_PAGE_CONTROL = false;
                }
            }
        }

        init();

        $scope.leaveWelcomePage = function() {

            if ($scope.HIDE_WELCOME_PAGE) {
                $scope.hideWelcomePage();
            }
            var initData = initSettingsService.getSettings();
            if (initData != null) {
                var redirectPage = initData.redirectPageFromWelcome;
                switch (redirectPage) {
                    case "HOME":
                        $state.go(STATE_LANDING);
                        return;
                    case "SP":
                        navService.setCurrentTab('service_providers');
                        navService.setIsInAllFilesPage(false);
                        $state.go(STATE_SERVICE_PROVIDERS);
                        return;
                    case "MANAGE_REPO":
                        navService.setCurrentTab('manage_repositories');
                        navService.setIsInAllFilesPage(false);
                        $state.go(STATE_MANAGE_REPOSITORIES);
                        return;
                }
            } else {
                $state.go(STATE_LANDING);
            }
        }

        $scope.launchURL = function(url, isAbsolute) {
            var targetURL = url;
            if (!isAbsolute) {
                targetURL = RMS_CONTEXT_NAME + url;
            }

            openCenteredPopup(targetURL, 'myWindow', '800', '600', 'yes');
        }

        $scope.downloadRMC = function() {
            initSettingsService.downloadRMC();
        }

        $scope.hideWelcomePage = function() {
            networkService.get(RMS_CONTEXT_NAME + "/RMSViewer/UpdatePref?prefName=LANDING_PAGE", headers, function(data) {
                if (data != null && data.result == true) {
                    $scope.messageStatus = 0;
                    $scope.SHOW_HIDE_WELCOME_PAGE_CONTROL = false;
                    var initData = initSettingsService.getSettings();
                    /*clear the landingPage in initSettings on client, (else we have to fetch initsettings from server-side)*/
                    initData.landingPage = null;
                } else {
                    $scope.SHOW_HIDE_WELCOME_PAGE_CONTROL = true;
                    handleError($filter('translate')('user.preferences.update.error'));
                }
            });
        }

        function handleError(message) {
            $scope.isLoading = false;
            $scope.messageStatus = 1;
            if (message == null) {
                $scope.message = $filter('translate')('user.preferences.update.error');
            } else {
                $scope.message = message;
            }
        }
    }
]);
mainApp.controller('downloadRMDController', ['$scope', '$rootScope', '$filter', '$state', 'networkService', '$location',
    'initSettingsService', 'settingsService', 'navService',
    function($scope, $rootScope, $filter, $state, networkService, $location,
        initSettingsService, settingsService, navService) {

        navService.setCurrentTab('download_rmc');
        navService.setIsInAllFilesPage(false);

        var clientSettings = initSettingsService.getClientSettings();

        $scope.download32Enabled = initSettingsService.getClientSettings().isRMDWin32Downloadable;
        $scope.download64Enabled = initSettingsService.getClientSettings().isRMDWin64Downloadable;
        $scope.downloadMacEnabled = initSettingsService.getClientSettings().isRMDMacDownloadable;
        $scope.isWin64 = initSettingsService.getClientSettings().isWin64;
        $scope.isWindows = initSettingsService.getClientSettings().os === "Windows";
        $scope.isMac = initSettingsService.getClientSettings().os === "Mac OS X";
        $scope.message = "";
        $scope.isLoading = false;

        $scope.dismissStatus = function() {
            $scope.messageStatus = 0;
        }

        $scope.downloadRMD = function(type) {
            initSettingsService.downloadRMD(type);
        }
    }
]);
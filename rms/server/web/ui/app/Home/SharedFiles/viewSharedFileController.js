mainApp.controller('viewSharedFileController', ['$scope', 'networkService', '$location', 'initSettingsService', 'repositoryService', '$filter',
    function($scope, networkService, $location, initSettingsService, repositoryService, $filter) {
        $scope.isLoading = true;
        $scope.loadingPopup = false;
        $scope.loadingPage = true;
        $scope.success = true;
        $scope.msg = '';
        $scope.isDisplay = false;
        var queryParams = $location.search();
        var sharedFileData;

        var openViewer = function(data) {
            var redirectUrl = data.viewerUrl;
            openSecurePopup(redirectUrl);
            msg = $filter('translate')('view.shared.file.success');
            $scope.loadingPopup = false;
            if (!$scope.loadingPage) {
                $scope.isLoading = false;
            }
        }

        $scope.getURLandViewFile = function() {
            $scope.isLoading = true;
            $scope.loadingPopup = true;

            var settings = initSettingsService.getSettings();
            var userIdFromCookie = window.readCookie("userId");
            var ticketFromCookie = window.readCookie("ticket");
            var clientId = window.readCookie("clientId");
            var platformId = window.readCookie("platformId");
            var params = $.param({
                d: queryParams["d"],
                c: queryParams["c"],
                userName: settings.userName,
                offset: new Date().getTimezoneOffset(),
                tenantId: settings.tenantId,
                tenantName: settings.tenantName,
                userId: userIdFromCookie,
                ticket: ticketFromCookie,
                clientId: clientId,
                platformId: platformId
            });

            repositoryService.showSharedFile(params, openViewer);
            msg = 'view.shared.file.loading';
        }

        var init = function() {
            if (queryParams["d"] && queryParams["c"]) {
                $scope.getURLandViewFile();
            } else {
                msg = 'view.shared.file.no.file';
                if (!$scope.loadingPage) {
                    $scope.isLoading = false;
                }
            }
            /*
            else {
                msg = 'view.shared.file.no.file';
                $scope.success = false;
                $scope.loadingPopup = false;
                if (!$scope.loadingPage) {
                    $scope.isLoading = false;
                }
            }
            */
        }

        init();

        $scope.getMessage = function() {
            return $filter('translate')(msg);
        }

        angular.element(document).ready(function() {
            $scope.loadingPage = false;
            if (!$scope.loadingPopup) {
                $scope.isLoading = false;
            }
        });
    }
]);
mainApp.controller('emailMismatchController', ['$scope', '$rootScope', '$cookies', '$state', 'networkService', 'dialogService', '$location', 'repositoryService', 'initSettingsService', 'navService',
    function($scope, $rootScope, $cookies, $state, networkService, dialogService, $location, repositoryService, initSettingsService, navService) {

        $scope.mydrive = null;
        $scope.params = $location.search();

        var repositoryList = function() {
            repositoryService.getRepositoryList(function(data) {
                var repoList = data;
                for (var i = 0; i < repoList.length; i++) {
                    if (repoList[i].quota !== undefined) {
                        $scope.mydrive = repoList[i];
                        break;
                    }
                }
            });
        }

        $scope.gotoMyDrive = function() {
            navService.setCurrentTab($scope.mydrive.repoId);
            var nextState = STATE_REPOSITORIES;
            var params = {
                repoId: $scope.mydrive.repoId,
                repoName: $scope.mydrive.repoName,
                repoType: $scope.mydrive.repoType
            };

            navService.setIsInAllFilesPage(false);
            $state.go(nextState, params, {
                reload: nextState
            });
        }

        $scope.signup = function() {
            window.location.href = initSettingsService.getRMSContextName() + "/register";
        }

        var getProjectName = function() {
            var projectName = $cookies.get("projectName");
            if (projectName) {
                $scope.projectName = projectName;
                $cookies.remove('projectName', {
                    path: '/'
                });
            } else {
                $state.go(STATE_LANDING);
            }
        }

        getProjectName();
        repositoryList();
    }
]);
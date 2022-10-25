mainApp.controller('mySpaceController', ['$scope', '$state',
    'repositoryService', '$filter', 'navService',
    function($scope, $state, repositoryService,
        $filter, navService) {
            // SCOPE INIT
            $scope.pageTitleDesktopShortened = $filter('translate')('myspace');
            $scope.pageTitleMobileShortened = $filter('translate')('myspace');
            $scope.pageTitle = 'MySpace';
            $scope.repos = [];
            $scope.quota = 0;
            $scope.usage = 0;
            $scope.myDriveUsage = 0;
            $scope.myVaultUsage = 0;
            $scope.vaultQuota = 0;
            $scope.mydrive = {};
            $scope.myDriveWidth = 0;
            $scope.myDriveActualWidth = 0;
            $scope.myVaultWidth = 0;
            $scope.myVaultActualWidth = 0;

            // BLOCK: This initiates the mydrive object
            repositoryService.getRepositoryList(function(data) {
                var repoList = data;
                for (var i = 0; i < repoList.length; i++) {
                    if (repoList[i].quota !== undefined) {
                        $scope.mydrive = repoList[i];
                        break
                    }
                }
            });

            // BLOCK: This is the method for navigating to another state on click
            $scope.onClickTab = function(tab) {
                navService.setCurrentTab(tab);
                if (tab == 'shared_files') {
                    navService.setCollapseStatus(false);
                    navService.setIsInAllFilesPage(false);
                    $state.go(STATE_SHARED_FILES);
                    return;
                } else {
                    var currentTab = tab.repoObj ? tab.repoObj : tab;
                    navService.setCurrentTab(currentTab.repoId);
                    var nextState = STATE_REPOSITORIES;
                    var params = {
                        repoId: currentTab.repoId,
                        repoName: currentTab.repoName,
                        repoType: currentTab.repoType
                    };
    
                    navService.setIsInAllFilesPage(false);
                    $state.go(nextState, params, {
                        reload: nextState
                    });
                }
            }

            $scope.$watch("usage", function() {
                $scope.myDriveUsage = $scope.usage - $scope.myVaultUsage;
                $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.vaultQuota) + '%';
                $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.usage) + '%';
                $scope.myDriveActualWidth = repositoryService.getUsageBarWidth($scope.myDriveUsage, $scope.usage) + '%';
                $scope.myVaultActualWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.quota) + '%';
                checkDriveExceeded();
            });
    
            $scope.$watch(function() {
                return $scope.usage > $scope.quota;
            }, function(newValue) {
                $scope.driveExceeded = newValue;
            });
    
            var checkDriveExceeded = function() {
                $scope.driveExceeded = $scope.usage > $scope.quota;
            }
    
            $scope.$watch("myVaultUsage", function() {
                $scope.myDriveUsage = $scope.usage - $scope.myVaultUsage;
                $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.vaultQuota) + '%'; // no idea why vaultQuota is the upper ceiling limit for storage (╯°□°）╯︵ ┻━┻
                $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.usage) + '%'; // vault width is taken against the $scope.usage. $scope.usage is the sum of mydrive and myvault usages. (╯°□°）╯︵ ┻━┻
                $scope.myDriveActualWidth = repositoryService.getUsageBarWidth($scope.myDriveUsage, $scope.usage) + '%';
                $scope.myVaultActualWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.quota) + '%';
                checkVaultExceeded();
            });
    
            var checkVaultExceeded = function() {
                $scope.vaultExceeded = $scope.usage > $scope.vaultQuota;
            }
            
            var repositoryList = function() {
                repositoryService.getRepositoryList(function(data) {
                    var repoList = data;
                    for (var i = 0; i < repoList.length; i++) {
                        if (repoList[i].usage != null && repoList[i].quota != null) {
                            $scope.usage = repoList[i].usage;
                            $scope.quota = repoList[i].quota;
                            $scope.myVaultUsage = repoList[i].myVaultUsage != null ? repoList[i].myVaultUsage : 0;
                            $scope.vaultQuota = repoList[i].vaultQuota != null ? repoList[i].vaultQuota : 0;
                            $scope.myDriveUsage = $scope.usage - $scope.myVaultUsage;
                        }
                        $scope.repos.push({
                            repoObj: repoList[i]
                        });
                    }
                });
            }
            repositoryList();
    }
]);
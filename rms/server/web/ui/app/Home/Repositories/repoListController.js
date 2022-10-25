mainApp.controller('repoListController', ['$scope', '$rootScope', '$state', '$stateParams',
    'repositoryService', 'initSettingsService', 'navService', 'protectWidgetService', 'serviceProviderService', '$filter',
    function($scope, $rootScope, $state, $stateParams,
        repositoryService, initSettingsService, navService, protectWidgetService, serviceProviderService, $filter) {

        var DISPLAY_NAME_MAX_LENGTH = 20;
        $scope.repoList = [];
        $rootScope.isAdmin = false;
        $rootScope.isSystemAdmin = false;
        $scope.toggleMySpaceTab = false;
        $scope.isRMCDownloadable = initSettingsService.getClientSettings().isRMCDownloadable;
        $scope.collapseStatus = true;

        var init = function() {
            var initData = initSettingsService.getSettings();
            if (initData != null) {
                $rootScope.isAdmin = initData.isAdmin;
                $rootScope.isSystemAdmin = initData.role == 'SYSTEM_USER';
                $scope.inbuiltServiceProvider = initData.inbuiltServiceProvider;
            }
            switch ($state.current.name) {
                case STATE_MANAGE_REPOSITORIES:
                    navService.setCurrentTab('manage_repositories');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_MANAGE_lOCAL_FILE:
                    navService.setCurrentTab('manage_local_files');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_SHARED_FILES:
                    navService.setCurrentTab('shared_files');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_SHARED_ACTIVE_FILES:
                    navService.setCurrentTab('active_shares');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_SETTINGS:
                    navService.setCurrentTab('configuration');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_DELETED_FILES:
                    navService.setCurrentTab('deleted_files');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_SYSTEM_SETTINGS:
                    navService.setCurrentTab('system_settings');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_FAVORITES:
                    navService.setCurrentTab('favorites');
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_DOWNLOAD_RMD:
                case STATE_WELCOME_PAGE:
                case STATE_FEEDBACK_PAGE:
                case STATE_VIEW_SHARED_FILE:
                    navService.setCurrentTab(-1);
                    navService.setIsInAllFilesPage(false);
                    break;
                case STATE_REPOSITORIES:
                    navService.setIsInAllFilesPage(false);
                    navService.setCurrentTab($stateParams.repoId);
                    break;
                case STATE_MYSPACE:
                    navService.setCurrentTab('myspace');
                    navService.setIsInAllFilesPage(false);
                    break;
                default:
                    navService.setCurrentTab(0);
            }
        }

        init();

        var repositoryList = function() {
            serviceProviderService.getSupportedServiceProviders(function(supportedResponse){
                var supportedServiceProvidersMap = supportedResponse.results.supportedProvidersMap;

                repositoryService.getRepositories(function(data) {
                    var allRepositories = data.results.repoItems;
                    var myDriveRepoIndex = -1;

                    for(var i = 0; i < allRepositories.length; i++) {
                        var repository = allRepositories[i];
                        
                        allRepositories[i].showToolTip = allRepositories[i].name.length > DISPLAY_NAME_MAX_LENGTH;

                        if(myDriveRepoIndex === -1 && initSettingsService.isDefaultRepo({
                            repoType: repository.type,
                            repoName: repository.name
                        })) {
                            $scope.mydrive = repository;
                            myDriveRepoIndex = i;
                            continue;
                        }

                        repository.provider = supportedServiceProvidersMap[repository.type].provider;
                    }

                    if(myDriveRepoIndex > -1) {
                        allRepositories.splice(myDriveRepoIndex, 1);
                    }

                    $scope.repoList = allRepositories;
                });
            });
        };

        $rootScope.$on("refreshRepoList", function(event, args) {
            $scope.repoList = [];
            repositoryList();
        });

        $scope.toggleActiveTab = function() {
            $scope.toggleMySpaceTab = true;
            $scope.collapseStatus = !$scope.collapseStatus;
        }

        $scope.onClickMyDrive = function(index) {
            $rootScope.$emit("toggleSideBar");
            var nextState = STATE_REPOSITORIES;
            var params = {
                repoId: $scope.mydrive.repoId,
                repoName: $scope.mydrive.repoName,
                repoType: $scope.mydrive.repoType
            };
            navService.setCurrentTab($scope.mydrive.repoId);
            navService.setIsInAllFilesPage(false);
            $state.go(nextState, params, {
                reload: nextState
            });
        }

        $scope.goToWorkSpace = function() {
            $state.go(STATE_WORKSPACE_FILES);
        };

        $scope.onClickTab = function(tab) {
            if ($scope.toggleMySpaceTab) {
                $scope.toggleMySpaceTab = false;
                return;
            }
            navService.setCurrentTab(tab);
            $rootScope.$emit("toggleSideBar");
            if (tab == 'manage_repositories') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_MANAGE_REPOSITORIES);
                return;
            } else if (tab == 'shared_files') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_SHARED_FILES);
                return;
            } else if (tab == "myspace") {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_MYSPACE);
                return;
            } else if (tab == 'manage_local_files') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_MANAGE_lOCAL_FILE);
                return;
            } else if (tab == 'active_shares') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_SHARED_ACTIVE_FILES);
                return;
            } else if (tab == 'deleted_files') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_DELETED_FILES);
                return;
            } else if (tab == 'download_rmc') {
                navService.setIsInAllFilesPage(false);
                initSettingsService.downloadRMC();
                return;
            } else if (tab == 'configuration') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_SETTINGS);
                return;
            } else if (tab == 'system_settings') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_SYSTEM_SETTINGS);
                return;
            } else if (tab == 'favorites') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_FAVORITES);
                return;
            } else {
                navService.setCurrentTab(tab == 0 ? tab : tab.repoId);
                var nextState = tab == 0 ? STATE_ALL_REPOSITORIES : STATE_REPOSITORIES;
                var params = tab == 0 ? {} : {
                    repoId: tab.repoId,
                    repoName: tab.name,
                    repoType: tab.type
                };
                if ((nextState == STATE_ALL_REPOSITORIES)) {
                    navService.setIsInAllFilesPage(true);
                    $state.go(nextState, params, {
                        reload: STATE_HOME
                    });
                } else {
                    navService.setIsInAllFilesPage(false);
                    $state.go(nextState, params, {
                        reload: nextState
                    });
                }
            }
        }

        $scope.isActiveTab = function(repoId) {
            return navService.getCurrentTab() == repoId;
        }

        $scope.openProtectWidget = function(selectedFiles) {
            $rootScope.$emit("toggleSideBar");
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "protect",
                header: $filter('translate')('widget.protect.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles);
        }

        $scope.openShareWidget = function(selectedFiles) {
            $rootScope.$emit("toggleSideBar");
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "share",
                header: $filter('translate')('widget.share.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles);
        }

        function shortenRepoName(data) {
            if (data.repoName.length > DISPLAY_NAME_MAX_LENGTH) {
                var str = data.repoName.slice(0, DISPLAY_NAME_MAX_LENGTH - 1);
                data.shortenedDisplayName = str + "..."
            } else {
                data.shortenedDisplayName = data.repoName;
            }
            return data;
        }

        repositoryList();
    }
]);
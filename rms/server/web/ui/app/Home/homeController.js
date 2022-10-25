mainApp.controller('homeController', ['$interval', '$scope', '$rootScope', '$http', '$cookies', '$location', '$state', 'initSettingsService', 'dialogService', 'uploadFileService', 'projectStateService', '$stateParams', '$filter', 'projectService', '$uibModal', '$uibModalStack', 'landingPageService',
    function($interval, $scope, $rootScope, $http, $cookies, $location, $state, initSettingsService, dialogService, uploadFileService, projectStateService, $stateParams, $filter, projectService, $uibModal, $uibModalStack, landingPageService) {
        $scope.checked = true;
        $scope.previousState = $state.current;
        $scope.networkErrorCount = 0;
        $scope.showPromotion = false;
        
        $rootScope.$on("reloadSettings", function() {
            initSettingsService.reloadScopeSettings($scope);
        });  

        $scope.init = function() {
            $interval(function() {
                testConnection();
            }, 60000);
            $scope.workspace = landingPageService.getWorkSpaceStatus();
        }

        $scope.getExpireDateText = function(trialEndTime) {
            return projectService.getExpireDateText(trialEndTime);
        }

        $scope.goToLanding = function() {
            if ($scope.switchDialog) {
                $scope.switchDialog.close();
            }
            initSettingsService.reloadSettings(function success() {
                    $rootScope.$emit("reloadSettings");
                }, function error() {                    
            });
            $state.go(STATE_LANDING);
        }

        $scope.getProjectName = function() {
            if ($state.current.name === STATE_PROJECT_SUMMARY) {
                return $filter('translate')('projects.all.title');
            }
            if ($scope.project) {
                return $scope.project.name;
            }
        }

        $scope.dismissPromotion = function() {
            $cookies.put("showPromotion", $cookies.get("userId") + ":false");
            $scope.showPromotion = false;
        }

        var serverUrl = function() {
            var borderIndex = $location.absUrl().indexOf(initSettingsService.getRMSContextName(), $location.protocol().concat("://").length);
            return $location.absUrl().substring(0, borderIndex).concat(initSettingsService.getRMSContextName());
        }

        var testConnection = function() {
            $http({
                method: 'HEAD',
                timeout: 5000,
                url: serverUrl()
            }).then(function successCallback(response) {
                $scope.networkErrorCount = 0;
                if ($state.current.name === STATE_NETWORKERROR) {
                    if ($scope.previousRepoId != undefined) {
                        var previousRepoId = $scope.previousRepoId;
                        $scope.previousRepoId = undefined;
                        $state.go($scope.previousState, {
                            repoId: previousRepoId
                        });
                    } else {
                        $state.go($scope.previousState);
                    }
                }
            }, function errorCallback(response) {
                $scope.networkErrorCount++;
                if ($scope.networkErrorCount >= 3 && $state.current.name != STATE_NETWORKERROR) {
                    $scope.previousState = $state.current;
                    $scope.previousRepoId = $stateParams.repoId;
                    $state.go(STATE_NETWORKERROR);
                    uploadFileService.setUploadFileList([]);
                    uploadFileService.setCloseStatus(true);
                }
            });
        }

        $scope.init();

        $scope.toggleSideBar = function() {
            $scope.checked = !$scope.checked;
            if (!$scope.checked) {
                $("#rms-home-footer-mobile").css("visibility", "hidden");
            } else {
                $("#rms-home-footer-mobile").css("visibility", "visible");
            }
        }

        $scope.switchProject = function() {
            initSettingsService.reloadSettings(function success() {
                    $rootScope.$emit("reloadSettings");
                }, function error() {                    
            });
            if ($scope.collapseStatus && $scope.switchDialog) {
                $scope.switchDialog.dismiss();
                $scope.switchDialog = null;
                $scope.collapseStatus = false;
                return;
            }

            $scope.collapseStatus = true;
            var projectHomePage = $state.current.name.indexOf(STATE_PROJECT) === 0;
            var workspaceHomePage = $state.current.name.indexOf(STATE_WORKSPACE_FILES) === 0;
            var currentProjectId = projectStateService.getProjectId();
            var projectsCreatedFiltered = $scope.projectsCreated.filter(function(obj) {
                if (!projectHomePage) {
                    return true;
                } else {
                    return obj.id != currentProjectId;
                }
            });
            var projectsInvitedFiltered = $scope.projectsInvited.filter(function(obj) {
                if (!projectHomePage) {
                    return true;
                } else {
                    return obj.id != currentProjectId;
                }
            });
            $scope.switchDialog = displaySwitchDialog({
                projectsCreatedNotFiltered: $scope.projectsCreated,
                projectsCreatedFiltered: projectsCreatedFiltered,
                projectsInvitedFiltered: projectsInvitedFiltered,
                workspaceHomePage:workspaceHomePage,
                projectHomePage: projectHomePage
            });

            $scope.switchDialog.result.then(function() {
                $scope.collapseStatus = false;
            }, function() {
                $scope.collapseStatus = false;
            });
        }

        $scope.goToUpgradeProject = function() {
            $state.go(STATE_UPGRADE_PROJECT);
        }

        $scope.goToCreateProject = function() {
            $state.go(STATE_CREATE_PROJECT);
        }

        var displaySwitchDialog = function(parameters) {
            return $uibModal.open({
                animation: true,
                backdrop: false,
                size: 'lg',
                windowClass: 'app-modal-window-switch-project',
                templateUrl: 'ui/app/Home/Projects/partials/switchProject.html',
                controller: ['$uibModalInstance', '$scope', '$filter', function($uibModalInstance, $scope, $filter) {
                    $scope.projectHomePage = parameters.projectHomePage;
                    $scope.workspaceHomePage = parameters.workspaceHomePage;
                    $scope.projectsCreatedNotFiltered = parameters.projectsCreatedNotFiltered;
                    $scope.projectsCreated = parameters.projectsCreatedFiltered;
                    $scope.projectsInvited = parameters.projectsInvitedFiltered;
                    $scope.dismiss = function() {
                        initSettingsService.reloadSettings(function success() {
                                $rootScope.$emit("reloadSettings");
                            }, function error() {                    
                        });
                        $uibModalInstance.dismiss('cancel');
                    };

                    $scope.getExpireDateText = function(trialEndTime) {
                        return projectService.getExpireDateText(trialEndTime);
                    }

                    $scope.goToProject = function(id) {
                        $scope.dismiss();
                        $state.go(STATE_PROJECT_HOME, {
                            projectId: id
                        });
                    }

                    $scope.goToSkyDRM = function() {
                        $scope.dismiss();
                        $state.go(STATE_MYSPACE);
                        $scope.workspace = false;
                    }

                    $scope.goToWorkSpace = function() {
                        $scope.dismiss();
                        $state.go(STATE_WORKSPACE_FILES);
                        $scope.workspace = true;
                    }

                    $scope.goToCreateProject = function() {
                        $scope.dismiss();
                        $state.go(STATE_CREATE_PROJECT);
                    }

                    $scope.gotoAllProjects = function() {
                        projectStateService.setProjectId(-1);
                        $scope.dismiss();
                        $state.go(STATE_PROJECT_SUMMARY);
                    }

                    $scope.getUserFullDisplayName = function(displayName, email) {
                        return displayName === email ? displayName : displayName + ' (' + email + ')';
                    }
                }]
            });
        };

        var checkPromotionStatus = function() {
            if ($scope.projectsCreated.length > 0) {
                $scope.showPromotion = false;
                return;
            }
            var promotionCookie = $cookies.get("showPromotion");
            if (promotionCookie != null) {
                var params = promotionCookie.split(':');
                var userId = params[0];
                var promotionStatus = params[1];
                if (userId === $cookies.get("userId")) {
                    $scope.showPromotion = promotionStatus === "true" ? true : false;
                } else {
                    $cookies.put("showPromotion", $cookies.get("userId") + ":true");
                    $scope.showPromotion = true;
                }
            } else {
                $cookies.put("showPromotion", $cookies.get("userId") + ":true");
                $scope.showPromotion = true;
            }
        }

        var getProjectList = function() {
            $scope.projectsCreated = [];
            $scope.projectsInvited = [];
            var queryParams = {
                size: $state.current.name === STATE_PROJECT_SUMMARY ? 3 : 4,
                page: 1,
                orderBy: '-lastActionTime'
            }
            projectService.getProjectList(queryParams, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsCreated.push(project);
                    }
                }
                checkPromotionStatus();
            }, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsInvited.push(project);
                    }
                }
            });
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
        })

        if ($state.current.name !== STATE_CREATE_PROJECT && $state.current.name !== STATE_LANDING) {
            getProjectList();
        }
    }
]);
mainApp.controller('landingPageController', ['$scope', '$rootScope', '$cookies', '$state', '$uibModal', 'networkService', '$location', 'repositoryService', 'shareFileService', 'initSettingsService', 'navService', 'projectService', '$filter', 'landingPageService', 'protectWidgetService', '$anchorScroll', 'projectStateService', 'serviceProviderService',
    function($scope, $rootScope, $cookies, $state, $uibModal, networkService, $location, repositoryService, shareFileService, initSettingsService, navService, projectService, $filter, landingPageService, protectWidgetService, $anchorScroll, projectStateService, serviceProviderService) {

        var DISPLAY_NAME_MAX_LENGTH = 20;

        $scope.repoList = [];
        $scope.projectsCreated = [];
        $scope.projectsInvited = [];
        $scope.pendingInvitations = [];
        $scope.profilePictureUrl = "ui/img/Default_Profile_Picture.svg";
        $scope.isProjectAccount = true;
        $scope.hasCreatedProjects = false;
        $scope.hasJointProjects = false;
        var isLoadingProfile = false;
        var isLoadingRepoList = false;
        $scope.inLandingPage = true;
        $scope.skipHomeTour = true;
        $scope.mobile = jscd.mobile;
        $scope.showMoreCreatedProjects = true;
        $scope.showMoreInvitedProjects = true;
        var maxNumOfProjectsInHomePage = 5;
        var projectPageSize = 10;
        landingPageService.setWorkSpaceStatus(false);

        $scope.mySpaceFileCount = 0;
        $scope.myDriveFileCount = 0;
        $scope.myVaultFileCount = 0;

        repositoryService.getMyDriveFileCount(function(myDriveData) {
            if (myDriveData.results && myDriveData.statusCode === 200) {
                $scope.myDriveFileCount = myDriveData.results.totalFileCount;
                shareFileService.getMyVaultFileCount(function(myVaultData) {
                    if (myVaultData.results && myVaultData.statusCode === 200) {
                        $scope.myVaultFileCount = myVaultData.results.totalFileCount;
                        $scope.mySpaceFileCount = $scope.myDriveFileCount + $scope.myVaultFileCount;
                    } else {
                        console.log("Unable to retrieve total file count for MyVault. Error " + myVaultData.statusCode + ": " + myVaultData.message);
                    }
                });
            } else {
                console.log("Unable to retrieve total file count for MyDrive. Error " + myDriveData.statusCode + ": " + myDriveData.message);
            }
        });

        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        $rootScope.$on("reloadSettings", function() {
            initSettingsService.reloadScopeSettings($scope);
        });
                
        var getHeaders = function() {
            return {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8',
                'userId': $cookies.get('userId'),
                'ticket': $cookies.get('ticket'),
                'clientId': $cookies.get('clientId'),
                'platformId': $cookies.get('platformId')
            };
        }

        $scope.gotoAllProjects = function() {
            projectStateService.setProjectId(-1);
            $state.go(STATE_PROJECT_SUMMARY);
        }

        $scope.gotoAnchor = function(newHash) {
            if ($location.hash() !== newHash) {
                $location.hash(newHash);
            } else {
                $anchorScroll();
            }
        };

        $rootScope.$on('profile.displayName.updated', function(event, data) {
            $scope.displayName = data;
        });

        $rootScope.$on('updateMyDriveUsage', function(event, data) {
            repositoryService.getMyDriveUsage(function(data) {
                $scope.usage = data.results.usage;
                $scope.quota = data.results.quota;
                $scope.myVaultUsage = data.results.myVaultUsage;
            });
        });

        var getProfile = function() {
            networkService.get(RMS_CONTEXT_NAME + "/rs/usr/v2/profile/", getHeaders(), function(data) {
                isLoadingProfile = false;
                $scope.isLoadingSummary = isLoadingRepoList || isLoadingProfile;
                if (data != null && data.statusCode == 200) {
                    $scope.displayName = data.extra.name;
                    $scope.email = data.extra.email;
                    if (data.extra.preferences && data.extra.preferences.profile_picture) {
                        $scope.profilePictureUrl = data.extra.preferences.profile_picture;
                    }
                    if (!data.extra.preferences || !data.extra.preferences.homeTour) {
                        $scope.skipHomeTour = false;
                    }
                }
            });
        }

        $scope.getUserFullDisplayName = function(displayName, email) {
            return displayName === email ? displayName : displayName + ' (' + email + ')';
        }

        var getRepoList = function() {
            // TODO use rest api to obtain usages and quotas for mydrive and myvault
            repositoryService.getRepositoryList(function(data) {
                isLoadingRepoList = false;
                $scope.isLoadingSummary = isLoadingRepoList || isLoadingProfile;
                var repoList = data;
                for (var i = 0; i < repoList.length; i++) {
                    if(initSettingsService.isDefaultRepo(repoList[i])) {
                        $scope.showUsage = true;
                        $scope.usage = repoList[i].usage ? repoList[i].usage : 0;
                        $scope.myVaultUsage = repoList[i].myVaultUsage ? repoList[i].myVaultUsage : 0;
                        $scope.quota = repoList[i].quota ? repoList[i].quota : 0;
                        $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.quota) + '%';
                        $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.usage) + '%';
                    }
                }
            });

            serviceProviderService.getSupportedServiceProviders(function(supportedResponse){
                var supportedServiceProvidersMap = supportedResponse.results.supportedProvidersMap;

                repositoryService.getRepositories(function(data) {
                    var allRepositories = data.results.repoItems;
                    var myDriveRepoIndex = -1;
    
                    for(var i = 0; i < allRepositories.length; i++) {
                        var repository = allRepositories[i];
    
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

        $scope.getExpireDateText = function(trialEndTime) {
            return projectService.getExpireDateText(trialEndTime);
        }

        $scope.onClickTab = function(tab) {
            navService.setCurrentTab(tab);
            if (tab == 'shared_files') {
                navService.setCollapseStatus(false);
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_SHARED_FILES);
                return;
            } else if (tab == "workspace_files") {
                landingPageService.setWorkSpaceStatus(true);
                $state.go(STATE_WORKSPACE_FILES);
                return;
            } else if (tab == "myspace") {
                navService.setCollapseStatus(false);
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_MYSPACE);
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

        $scope.openProtectWidget = function(selectedFiles) {
            hopscotch.endTour(false);
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "protect",
                header: $filter('translate')('widget.protect.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles);
        }

        $scope.openShareWidget = function(selectedFiles) {
            hopscotch.endTour(false);
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "share",
                header: $filter('translate')('widget.share.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles);
        }

        $scope.goToProject = function(id) {
            $state.go(STATE_PROJECT_HOME, {
                projectId: id
            });
        }

        $scope.goToCreateProject = function() {
            $state.go(STATE_CREATE_PROJECT);
        }

        $scope.goToUpgradeProject = function() {
            $state.go(STATE_UPGRADE_PROJECT);
        }

        $scope.goToRepoConfig = function() {
            $state.go(STATE_MANAGE_REPOSITORIES);
        }

        $scope.$on("$stateChangeStart", function(event, toState, toParams, fromState, fromParams, options) {
            hopscotch.endTour(false);
        });

        $scope.dismissMessage = function() {
            $scope.errorMessage = "";
        }

        $scope.acceptInvitation = function(index, id, code) {
            $scope.pendingInvitations[index].isLoading = true;
            $scope.errorMessage = "";
            networkService.get(RMS_CONTEXT_NAME + "/rs/project/accept?id=" + id + "&code=" + code, getHeaders(), function(data) {
                $scope.pendingInvitations[index].isLoading = false;
                if (data) {
                    switch (data.statusCode) {
                        case 200:
                            $scope.pendingInvitations[index].status = "accepted";
                            break;
                        case 4001:
                            $scope.errorMessage = "projects.join.error.expired";
                            getPendingInvitations();
                            break;
                        case 4002:
                            $scope.errorMessage = "projects.join.error.declined";
                            getPendingInvitations();
                            break;
                        case 4005:
                            $scope.errorMessage = "projects.join.error.accepted";
                            getPendingInvitations();
                            break;
                        case 4006:
                            $scope.errorMessage = "projects.join.error.revoked";
                            getPendingInvitations();
                            break;
                        default:
                            $scope.errorMessage = "project.invite.accept.error";
                    }
                } else {
                    $scope.errorMessage = "project.invite.accept.error";
                }
            });
        }

        $scope.declineInvitation = function(index, id, code) {
            hopscotch.endTour(false);
            var pendingInvitations = $scope.pendingInvitations;
            var parentScope = $scope;
            $uibModal.open({
                animation: true,
                windowClass: 'app-modal-window-centered',
                templateUrl: 'ui/app/Home/Projects/partials/declineInvitation.html',
                controller: ['$uibModalInstance', '$scope',
                    function($uibModalInstance, $scope) {
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.decline = function() {
                            pendingInvitations[index].isLoading = true;
                            parentScope.errorMessage = "";
                            var data = {
                                id: id,
                                code: code,
                                declineReason: $scope.declinedReason ? $scope.declinedReason : ''
                            }
                            networkService.postAsFormData(RMS_CONTEXT_NAME + "/rs/project/decline", data, getHeaders(), function(data) {
                                pendingInvitations[index].isLoading = false;
                                if (data) {
                                    switch (data.statusCode) {
                                        case 200:
                                            pendingInvitations[index].status = "declined";
                                            break;
                                        case 4001:
                                            parentScope.errorMessage = "projects.invitation.decline.error.expired";
                                            getPendingInvitations();
                                            break;
                                        case 4002:
                                            parentScope.errorMessage = "projects.invitation.decline.error.declined";
                                            getPendingInvitations();
                                            break;
                                        case 4005:
                                            parentScope.errorMessage = "projects.invitation.decline.error.accepted";
                                            getPendingInvitations();
                                            break;
                                        case 4006:
                                            parentScope.errorMessage = "projects.invitation.decline.error.revoked";
                                            getPendingInvitations();
                                            break;
                                        default:
                                            parentScope.errorMessage = "project.invite.decline.error";
                                    }
                                } else {
                                    parentScope.errorMessage = "project.invite.decline.error";
                                }
                            });
                            $uibModalInstance.dismiss('cancel');
                        }
                    }
                ]
            });
        }

        var getProjectList = function() {
            $scope.createdPageOffset = 1;
            $scope.invitedPageOffset = 1;
            $scope.isLoadingCreatedProjects = true;
            $scope.isLoadingJointProjects = true;
            var queryParams = {
                orderBy: $state.current.name === STATE_LANDING ? "-lastActionTime" : "name",
                page: 1,
                size: $state.current.name === STATE_LANDING ? maxNumOfProjectsInHomePage : projectPageSize
            }
            projectService.getProjectList(queryParams, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsCreated.push(project);
                    }
                    $scope.totalProjectsCreated = data.totalProjects;
                    var remaining = $scope.totalProjectsCreated - queryParams.size * $scope.createdPageOffset;
                    if (remaining <= 0) {
                        $scope.showMoreCreatedProjects = false;
                    }
                }
                $scope.isLoadingCreatedProjects = false;
            }, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsInvited.push(project);
                    }
                    $scope.totalProjectsInvited = data.totalProjects;
                    var remaining = $scope.totalProjectsInvited - queryParams.size * $scope.invitedPageOffset;
                    if (remaining <= 0) {
                        $scope.showMoreInvitedProjects = false;
                    }
                }
                $scope.isLoadingJointProjects = false;
            });
        }

        $scope.loadMoreCreatedProjects = function() {
            $scope.createdPageOffset = $scope.createdPageOffset + 1;
            $scope.isLoadingCreatedProjects = true;
            var queryParams = {
                page: $scope.createdPageOffset,
                size: projectPageSize
            }
            projectService.getProjectsCreated(queryParams, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsCreated.push(project);
                    }
                    $scope.totalProjectsCreated = data.totalProjects;
                    var remaining = $scope.totalProjectsCreated - queryParams.size * $scope.createdPageOffset;
                    if (remaining <= 0) {
                        $scope.showMoreCreatedProjects = false;
                    }
                }
                $scope.isLoadingCreatedProjects = false;
            });
        }

        $scope.loadMoreInvitedProjects = function() {
            $scope.invitedPageOffset = $scope.invitedPageOffset + 1;
            $scope.isLoadingJointProjects = true;
            var queryParams = {
                page: $scope.invitedPageOffset,
                size: projectPageSize
            }
            projectService.getProjectsInvited(queryParams, function(data) {
                if (data) {
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        $scope.projectsInvited.push(project);
                    }
                    $scope.totalProjectsInvited = data.totalProjects;
                    var remaining = $scope.totalProjectsInvited - queryParams.size * $scope.invitedPageOffset;
                    if (remaining <= 0) {
                        $scope.showMoreInvitedProjects = false;
                    }
                }
                $scope.isLoadingJointProjects = false;
            });
        }

        var getPersAcctSummary = function() {
            $scope.isLoadingSummary = true;
            isLoadingProfile = true;
            isLoadingRepoList = true;
            getProfile();
            getRepoList();
        }

        var getPendingInvitations = function() {
            networkService.get(RMS_CONTEXT_NAME + "/rs/project/user/invitation/pending/", getHeaders(), function(data) {
                if (data != null && data.statusCode == 200) {
                    $scope.pendingInvitations = data.results.pendingInvitations;
                    $scope.totalPendingInvitations = data.results.totalPendingInvitations;
                    $scope.pendingInvitations.forEach(function(invitation) {
                        invitation.isLoading = false;
                    })
                }
            });
        }

        var getStatusCode = function() {
            var statusCode = $cookies.get('statusCode');
            if (statusCode) {
                switch (statusCode) {
                    case '4001':
                        $scope.errorMessage = $filter('translate')('projects.join.error.expired');
                        break;
                    case '4002':
                        $scope.errorMessage = $filter('translate')('projects.join.error.declined');
                        break;
                    case '4005':
                        $scope.errorMessage = $filter('translate')('projects.join.error.accepted');
                        break;
                    case '4006':
                        $scope.errorMessage = $filter('translate')('projects.join.error.revoked');
                        break;
                    default:
                        $scope.errorMessage = $filter('translate')('projects.join.error.default');
                        break;
                }
                $cookies.remove('statusCode', {
                    path: '/'
                });
            }
        }

        $scope.$watch('projectsCreated.length', function(newVaule) {
            $scope.hasCreatedProjects = $scope.projectsCreated.length > 0;
        })

        $scope.$watch('projectsInvited.length', function(newVaule) {
            $scope.hasJointProjects = $scope.projectsInvited.length > 0;
        })

        $scope.$watch('pendingInvitations.length', function(newVaule) {
            $scope.hasInvitation = $scope.pendingInvitations.length > 0;
        })

        $scope.$watch("usage", function() {
            $scope.myProjectWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.quota) + '%';
        });

        getPersAcctSummary();
        getProjectList();
        getPendingInvitations();
        getStatusCode();
        landingPageService.afterBindings(function() {
            if (!$scope.skipHomeTour) {
                var steps = [{
                            title: $filter('translate')('tour.title.myspace'),
                            content: $filter('translate')('tour.content.myspace'),
                            target: "myspace-btn",
                            placement: "bottom",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.mydrive'),
                            content: $filter('translate')('tour.content.mydrive'),
                            target: "mydrive-btn",
                            placement: "bottom",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.workspace'),
                            content: $filter('translate')('tour.content.workspace'),
                            target: "workspace-btn",
                            placement: "bottom",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.myvault'),
                            content: $filter('translate')('tour.content.myvault'),
                            target: "myvault-btn",
                            placement: "bottom",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.all-files'),
                            content: $filter('translate')('tour.content.all-files'),
                            target: "all-files-btn",
                            placement: "bottom",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.protect'),
                            content: $filter('translate')('tour.content.protect'),
                            target: "protect-widget",
                            placement: "top",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.share'),
                            content: $filter('translate')('tour.content.share'),
                            target: "share-widget",
                            placement: "top",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        },
                        {
                            title: $filter('translate')('tour.title.add.repo'),
                            content: $filter('translate')('tour.content.add.repo'),
                            target: "add-repo-widget",
                            placement: "top",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                        }
                    ];
                if ($scope.saasMode || $scope.isSystemAdmin || $scope.isTenantAdmin || $scope.isProjectAdmin) {
                    steps.push({
                            title: $filter('translate')('tour.title.create.project'),
                            content: $filter('translate')('tour.content.create.project'),
                            target: "create-project-card",
                            placement: "top",
                            xOffset: "center",
                            arrowOffset: "center",
                            showCTAButton: true,
                            ctaLabel: $filter('translate')('tour.button.skip'),
                            onCTA: skipTour
                    });
                }
                var tour = {
                    id: "welcome-tour",
                    showPrevButton: true,
                    steps: steps,
                    onNext: function() {
                        $scope.currentStepNum = hopscotch.getCurrStepNum();
                    },
                    onEnd: function() {
                        if ($scope.currentStepNum && $scope.currentStepNum === tour.steps.length - 1) {
                            finishTour();
                        }
                    }
                };
                if (STATE_LANDING == $state.current.name) {
                    hopscotch.startTour(tour);
                }

                function finishTour() {
                    var header = {
                        'Content-Type': 'application/json',
                        'userId': window.readCookie("userId"),
                        'ticket': window.readCookie("ticket"),
                        'clientId': window.readCookie("clientId"),
                        'platformId': window.readCookie("platformId")
                    };
                    var data = {};
                    data['homeTour'] = true;
                    var params = {};
                    params['parameters'] = data;
                    networkService.post(RMS_CONTEXT_NAME + "/rs/usr/tour/homeTour", JSON.stringify(params), header, function(data) {
                        if (data != null && data.statusCode == 200) {
                            $scope.skipHomeTour = true;
                        }
                    });
                }

                function skipTour() {
                    hopscotch.endTour(true);
                    finishTour();
                }
            }
        });
    }
]);
mainApp.controller('projectController', ['$scope', '$rootScope', '$timeout', '$state', '$stateParams', '$uibModal', 'networkService', 'dialogService',
    'repositoryService', 'initSettingsService', 'navService', 'projectService', 'shareFileService', 'projectStateService', '$filter', '$cookies', 'shareDialogService', 'protectWidgetService', 'serviceProviderService',
    function($scope, $rootScope, $timeout, $state, $stateParams, $uibModal, networkService, dialogService,
        repositoryService, initSettingsService, navService, projectService, shareFileService, projectStateService, $filter, $cookies, shareDialogService, protectWidgetService, serviceProviderService) {

        $rootScope.isAdmin = false;
        $rootScope.isSystemAdmin = false;
        $scope.isRMCDownloadable = initSettingsService.getClientSettings().isRMCDownloadable;
        $scope.searchActivated = false;
        $scope.fileupload;
        $scope.parentFolder = null;
        $scope.repoList = [];
        $scope.members = [];
        $scope.pendingMembers = [];
        $scope.recentFiles = [];
        var maxNumOfMemberToShow = 6;
        var maxNumOfFileToShow = 6;
        var SIZE = 10;
        var PAGE = 1;
        var projectSaved = false;
        $scope.contents = [];
        $scope.showRepoTitle = true;
        $scope.showSearch = false;
        $scope.showSort = false;
        $scope.filePageOffset = PAGE;
        $scope.pageOffset = 1;
        $scope.pendingPageOffset = 1;
        $scope.showMoreMembers = true;
        $scope.showMorePendingMembers = true;
        $scope.showMoreFiles = true;
        $scope.optional = true;
        $scope.noSearchResult = false;
        $scope.searchString = "";
        $scope.createPrjEmailInvalid = false;
        $scope.canResetSearchResults = false;
        $scope.updateParams = {};
        $scope.expiryInfo = false;
        $scope.disableAddPolicy = false;
        $scope.projectTagsStep = false;
        $scope.selectedTab = 'allFiles';

        $scope.projectSharingEnabled = false; // SC-2827: File sharing across projects disabled due to the inconsistent behaviour with RMD client

        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var rootFolder = {
            pathId: "/",
            pathDisplay: "/",
            folder: true
        }

        $scope.isMemberAdmin = function(member) {
            return member.userId === $scope.project.owner.userId;
        }

        var init = function() {
            if (!$stateParams.projectId) {
                return;
            }
            projectStateService.setProjectId($stateParams.projectId);
            projectStateService.setFromCreateProject($stateParams.fromCreateProject);
            $scope.fromCreateProject = $stateParams.fromCreateProject;
            getProjectDetails(); 
            switch ($state.current.name) {
                case STATE_PROJECT_HOME:
                    navService.setCurrentTab('summary');
                    refreshSummaryPage();
                    checkInvitationCookie();
                    break;
                case STATE_PROJECT_FILES:
                    navService.setCurrentTab('all_files');
                    getRecentFiles();
                    $scope.refreshFilePage(rootFolder);
                    break;
                case STATE_PROJECT_USERS:
                    navService.setCurrentTab('users');
                    $scope.refreshUsersPage();
                    break;
                case STATE_PROJECT_CONFIGURATION:
                case STATE_PROJECT_CONFIGURATION_INFO:
                    $state.go(STATE_PROJECT_CONFIGURATION_INFO);
                    navService.setCurrentTab('configuration');
                    $scope.configChanged = false;
                    break;
                case STATE_PROJECT_CONFIGURATION_CLASSIFICATION:
                    navService.setCurrentTab('configuration');
                    projectStateService.setTokenGroupName($stateParams.tenantId);
                    $scope.configChanged = false;
                    if ($scope.fromCreateProject) {
                        $timeout(function() {
                            showSnackbar({
                                isSuccess: true,
                                messages: $filter('translate')('project.create.success.msg')
                            });
                        });
                    }
                    break;
                case STATE_PROJECT_CONFIGURATION_PREFERENCE:
                    $state.go(STATE_PROJECT_CONFIGURATION_PREFERENCE);
                    navService.setCurrentTab('configuration');
                    $scope.configChanged = false;
                    break;    
                case STATE_PROJECT_POLICIES:
                case STATE_PROJECT_POLICIES_LIST:
                    $state.go(STATE_PROJECT_POLICIES_LIST);
                    navService.setCurrentTab('policies');
                    break;
                case STATE_PROJECT_POLICIES_CREATE:
                    navService.setCurrentTab('policies');
                    break;
                default:
                    navService.setCurrentTab(0);
            }
        }

        $scope.$on('projectSaved',function(event, result){
            projectSaved = result.data;
        });

        $scope.onClickTab = function(tab, fromLink) {

            getProjectDetails();

            navService.setCurrentTab(tab);
            if (fromLink === undefined || !fromLink) {
                $rootScope.$emit("toggleSideBar");
            }

            dismissSnackbar();

            switch (tab) {
                case 'summary':
                    $state.go(STATE_PROJECT_HOME);
                    refreshSummaryPage();
                    if ($scope.fromCreateProject && projectSaved) {
                        $scope.fromCreateProject = null;
                        projectStateService.setFromCreateProject(null);
                    }
                    return;
                case 'all_files':
                    $scope.selectedTab = 'allFiles';
                    projectService.getProject(projectStateService.getProjectId(), function(data) {
                        if (data.statusCode == 200 && data.results.detail) {
                            projectStateService.setTokenGroupName(data.results.detail.tokenGroupName);
                            $state.go(STATE_PROJECT_FILES, {tenantId: data.results.detail.tokenGroupName}, {reload: true});
                        }
                    });
                    $scope.refreshFilePage(rootFolder);
                    return;
                case 'users':
                    $state.go(STATE_PROJECT_USERS);
                    $scope.refreshUsersPage();
                    return;
                case 'all_projects':
                    initSettingsService.reloadSettings(function success() {
                            $rootScope.$emit("reloadSettings");
                        }, function error() {                    
                    });
                    projectStateService.setProjectId(-1);
                    $state.go(STATE_PROJECT_SUMMARY);
                    return;
                case 'configuration':
                    $state.go(STATE_PROJECT_CONFIGURATION_INFO);
                    $scope.configChanged = false;
                    return;
                case 'classification':
                    projectService.getProject(projectStateService.getProjectId(), function(data) {
                        if (data.statusCode == 200 && data.results.detail) {
                            projectStateService.setTokenGroupName(data.results.detail.tokenGroupName);
                            $state.go(STATE_PROJECT_CONFIGURATION_CLASSIFICATION, {tenantId: data.results.detail.tokenGroupName});
                        }
                    });
                    $scope.configChanged = false;
                    navService.setCurrentTab('configuration');
                    return;
                case 'preference':
                    $state.go(STATE_PROJECT_CONFIGURATION_PREFERENCE);
                    $scope.configChanged = false;
                    navService.setCurrentTab('configuration');
                    return;
                case 'accessPermission':
                    $state.go(STATE_PROJECT_CONFIGURATION_ACCESS_PERMISSIONS);
                    $scope.configChanged = false;
                    navService.setCurrentTab('configuration');
                    return;
                case 'policies':
                    projectService.getProject(projectStateService.getProjectId(), function(data) {
                        if (data.statusCode == 200 && data.results.detail) {
                            projectStateService.setTokenGroupName(data.results.detail.tokenGroupName);
                            $state.go(STATE_PROJECT_POLICIES_LIST, {tenantId: data.results.detail.tokenGroupName}, {reload: true});
                        }
                    });
                    return;
                case 'download_rmc':
                    navService.setIsInAllFilesPage(false);
                    initSettingsService.downloadRMC();
                    return;
            }
        }

        $scope.goToLanding = function() {
            $state.go(STATE_LANDING);
        }

        $scope.validateEmail = function(id) {
            $scope.createPrjEmailInvalid = validateEmail(id, $scope) && $scope.currentIds.length > 0;
        }

        var callopenModal = function(projectController) {
            $uibModal.open({
                animation: true,
                size: 'lg',
                windowClass: 'app-modal-window-activity',
                templateUrl: 'ui/app/Home/Projects/partials/inviteUserToProject.html',
                controller: ['$uibModalInstance', '$scope', '$stateParams', 'serviceProviderService', 'projectService',
                    function($uibModalInstance, $scope, $stateParams, $serviceProviderService, projectService) {
                        $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        var subscope = projectController;
                        $scope.project = projectController.project;
                        $scope.optional = false;
                        $scope.inviteUserLoading = false;
                        $scope.invitationMsg = $scope.project.invitationMsg;
                        $scope.validateEmail = function(id) {
                            validateEmail(id, $scope);
                        }
                        $scope.invite = function() {
                            $scope.currentIds = $("#mailShareTags").tagit("assignedTags");
                            var inviteMessage = $("#messagebox").val();
                            var params = {
                                "parameters": {
                                    "emails": $scope.currentIds,
                                    "invitationMsg": $scope.invitationMsg
                                }
                            };
                            $scope.inviteUserLoading = true;
                            projectService.inviteUsersToProject(params, projectStateService.getProjectId(), function(data) {
                                dismissSnackbar();
                                $scope.inviteUserLoading = false;
                                var messages;
                                var isSuccess = false;
                                if (data != null && data.statusCode == 200) {
                                    var result = data.results;
                                    var alreadyInvitedList = result.alreadyInvited;
                                    var nowInvited = result.nowInvited;
                                    var alreadyMembers = result.alreadyMembers;
                                    messages = [];
                                    isSuccess = true;
                                    if (nowInvited.length > 0) {
                                        messages.push($filter('translate')('project.invitation.success.message1') + constructEmailList(nowInvited) + $filter('translate')('project.invitation.success.message2'));
                                    }
                                    if (alreadyInvitedList.length > 0) {
                                        messages.push($filter('translate')('project.invitation.already.invited') + constructEmailList(alreadyInvitedList));
                                    }
                                    if (alreadyMembers.length > 0) {
                                        messages.push($filter('translate')('project.invitation.already.member') + constructEmailList(alreadyMembers));
                                    }
                                    projectController.refreshUsersPage();
                                } else {
                                    messages = $filter('translate')('project.invitation.failed.message');
                                }
                                $uibModalInstance.dismiss('cancel');
                                showSnackbar({
                                    isSuccess: isSuccess,
                                    messages: messages
                                });
                            });
                        }

                    }
                ]
            });
        }

        var constructEmailList = function(emailList) {
            var emailstring = "";
            for (var i = 0; i < emailList.length; i++) {
                if (i == 0) {
                    emailstring = emailList[i];
                } else {
                    emailstring += ", " + emailList[i];
                }
            }
            return emailstring;
        }

        $scope.openInvitationModal = function() {
            callopenModal($scope);
        }

        var callUserDetailModal = function(member, projectController) {
            $uibModal.open({
                animation: true,
                size: 'md',
                windowClass: 'modal-window-project-member',
                templateUrl: 'ui/app/Home/Projects/partials/memberInfoDetail.html',
                controller: ['$uibModalInstance', '$scope', '$stateParams', 'serviceProviderService', 'projectService', '$cookies',
                    function($uibModalInstance, $scope, $stateParams, $serviceProviderService, projectService, $cookies) {
                        $scope.deleteUserLoading = false;
                        $scope.modalLoading = true;
                        $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.member = member;
                        $scope.timeBeforeYesterDay = function(time) {
                            var yesterday = new Date();
                            yesterday.setDate(yesterday.getDate() - 1);
                            var timeFormatted = yesterday.getTime();
                            if (time < timeFormatted) {
                                return true;
                            }
                            return false;
                        }
                        $scope.isLoggedInUserAdmin = function() {
                            return projectController.project.ownedByMe;
                        }
                        $scope.isMemberAdmin = function() {
                            var project = projectController.project;
                            return $scope.member.userId === project.owner.userId;
                        }
                        $scope.deleteUser = function() {
                            var params = {
                                "parameters": {
                                    "memberId": $scope.member.userId
                                }
                            };

                            dialogService.confirm({
                                msg: $filter('translate')('projects.users.delete.warning'),
                                ok: function() {
                                    $scope.deleteUserLoading = true;
                                    projectService.removeMember(projectStateService.getProjectId(), params, function(data) {
                                        if (data.statusCode == 204) {
                                            projectController.refreshUsersPage();
                                            dismissSnackbar();
                                            $uibModalInstance.dismiss('cancel');
                                            showSnackbar({
                                                messages: $filter('translate')('project.member.remove.success', {
                                                    member: $scope.member.displayName
                                                }),
                                                isSuccess: true
                                            });
                                        }
                                    });
                                },
                                cancel: function() {}
                            });
                        }
                        projectService.getMemberDetails(projectStateService.getProjectId(), member.userId, null, function(data) {
                            $scope.modalLoading = false;
                            if (data.statusCode == 200 && data.results.detail) {
                                var detail = data.results.detail;
                                $scope.inviterDisplayName = detail.inviterDisplayName;
                            } else if (data.statusCode == 400) {
                                $state.go(STATE_PROJECT_UNAUTHORIZED);
                            } else {
                                $scope.errorMessage = "project.error.general";
                            }
                        });
                    }
                ]
            });
        }

        var callPendingInvitaionModal = function(pendingMember, projectController) {
            $uibModal.open({
                animation: true,
                size: 'md',
                windowClass: 'modal-window-project-member',
                templateUrl: 'ui/app/Home/Projects/partials/managePendingInvitation.html',
                controller: ['$uibModalInstance', '$scope', '$stateParams', 'serviceProviderService', 'projectService', '$cookies',
                    function($uibModalInstance, $scope, $stateParams, $serviceProviderService, projectService, $cookies) {
                        $scope.invitationLoading = false;
                        $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.member = pendingMember;
                        $scope.timeBeforeYesterDay = function(time) {
                            var yesterday = new Date();
                            yesterday.setDate(yesterday.getDate() - 1);
                            var timeFormatted = yesterday.getTime();
                            if (time < timeFormatted) {
                                return true;
                            }
                            return false;
                        }
                        $scope.isLoggedInUserAdmin = function() {
                            return projectController.project.ownedByMe;
                        }
                        $scope.revokeInvitation = function() {
                            var params = {
                                "parameters": {
                                    "invitationId": $scope.member.invitationId
                                }
                            };

                            dialogService.confirm({
                                msg: $filter('translate')('projects.invitation.revoke.warning'),
                                ok: function() {
                                    $scope.invitationLoading = true;
                                    dismissSnackbar();
                                    projectService.revokeInvitation(params, function(data) {
                                        var message;
                                        var isSuccess = false;
                                        switch (data.statusCode) {
                                            case 200:
                                                isSuccess = true;
                                                message = $filter('translate')('project.invitation.revoke.success');
                                                break;
                                            case 4001:
                                                message = $filter('translate')('projects.invitation.revoke.error.expired');
                                                break;
                                            case 4002:
                                                message = $filter('translate')('projects.invitation.revoke.error.declined');
                                                break;
                                            case 4005:
                                                message = $filter('translate')('projects.invitation.revoke.error.accepted');
                                                break;
                                            case 4006:
                                                message = $filter('translate')('projects.invitation.revoke.error.revoked');
                                                break;
                                            default:
                                                message = $filter('translate')('projects.invitation.revoke.error.default');
                                                break;
                                        }
                                        projectController.refreshUsersPage();
                                        $uibModalInstance.dismiss('cancel');
                                        showSnackbar({
                                            isSuccess: isSuccess,
                                            messages: message
                                        });
                                    });
                                },
                                cancel: function() {}
                            });
                        }
                        $scope.resendInvitation = function() {
                            $scope.invitationLoading = true;
                            var params = {
                                "parameters": {
                                    "invitationId": $scope.member.invitationId
                                }
                            };
                            dismissSnackbar();
                            projectService.sendReminder(params, function(data) {
                                var message;
                                var isSuccess = false;
                                switch (data.statusCode) {
                                    case 200:
                                        isSuccess = true;
                                        message = $filter('translate')('project.invitation.resend.success');
                                        break;
                                    case 4001:
                                        message = $filter('translate')('projects.invitation.resend.error.expired');
                                        break;
                                    case 4002:
                                        message = $filter('translate')('projects.invitation.resend.error.declined');
                                        break;
                                    case 4005:
                                        message = $filter('translate')('projects.invitation.resend.error.accepted');
                                        break;
                                    case 4006:
                                        message = $filter('translate')('projects.invitation.resend.error.revoked');
                                        break;
                                    default:
                                        message = $filter('translate')('projects.invitation.resend.error.default');
                                        break;
                                }
                                projectController.refreshUsersPage();
                                $uibModalInstance.dismiss('cancel');
                                showSnackbar({
                                    isSuccess: isSuccess,
                                    messages: message
                                });
                            });
                        }
                    }
                ]
            });
        }

        $scope.memberInfo = function(member) {
            callUserDetailModal(member, $scope);
        }

        $scope.managePendingInvitation = function(pendingMember) {
            callPendingInvitaionModal(pendingMember, $scope);
        }

        $scope.MenuClickedMode = false;

        $scope.toggleMenuMode = function() {
            $scope.MenuClickedMode = !$scope.MenuClickedMode;
        }

        $scope.toggleSearch = function() {
            $scope.showSort = false;
            $scope.showSearch = true;
            $scope.showRepoTitle = false;
        }

        $scope.toggleSort = function() {
            $scope.showSort = true;
            $scope.showSearch = false;
            $scope.showRepoTitle = false;
        }

        $scope.closeFileListOptions = function() {
            $scope.showSearch = false;
            $scope.showSort = false;
            $scope.showRepoTitle = true;
        }

        $scope.isActiveTab = function(tab) {
            return navService.getCurrentTab() == tab;
        }

        $scope.dismissProjectCreationMessage = function() {
            $scope.projectCreated = false;
        }

        var getProjectDetails = function() {

            if (!projectStateService.getProjectId() || isNaN(projectStateService.getProjectId())) {
                $state.go(STATE_PROJECT_UNAUTHORIZED);
                return;
            }

            $scope.isLoadingDesc = true;
            projectService.getProject(projectStateService.getProjectId(), function(data) {
                $scope.isLoadingDesc = false;
                if (data.statusCode == 200 && data.results.detail) {
                    $scope.project = data.results.detail;
                    $scope.updateParams.projectName = $scope.project.name;
                    $scope.updateParams.projectDescription = $scope.project.description;
                    $scope.updateParams.invitationMsg = $scope.project.invitationMsg;
                    projectStateService.setTokenGroupName($scope.project.tokenGroupName);
                } else if (data.statusCode == 400) {
                    $state.go(STATE_PROJECT_UNAUTHORIZED);
                } else {
                    $scope.errorMessage = "project.error.general";
                }
            });
        }

        $scope.getPolicyCount = function() {
            var url = RMS_CONTEXT_NAME + "/rs/policy/" + projectStateService.getTokenGroupName() + "/policies";
            networkService.get(url, getJsonHeaders(), function(data){
                if(data.statusCode === 200) {
                    $rootScope.totalPolicies = data.results.policies.length;
                } else {
                    $rootScope.totalPolicies = 0;
                }
            });
        }

        var refreshSummaryPage = function() {
            $scope.members = [];
            getRecentFiles();
            getMembers();
            projectService.getProject(projectStateService.getProjectId(), function(data) {
                if (data.statusCode == 200 && data.results.detail) {
                    projectStateService.setTokenGroupName(data.results.detail.tokenGroupName);
                    if((($rootScope.saasMode && $scope.project && $scope.project.ownedByMe) ||
                        (!$rootScope.saasMode && (($scope.project && $scope.project.ownedByMe) || $rootScope.isSystemAdmin || $rootScope.isTenantAdmin || $rootScope.isProjectAdmin)))) {
                        $scope.getPolicyCount();
                    }
                }
            });
        }

        var getMembers = function(queryParams) {
            var project = {
                id: projectStateService.getProjectId()
            };
            var orderByArray = null;
            if ($state.current.name == STATE_PROJECT_HOME) {
                orderByArray = ["-creationTime"];
            } else {
                orderByArray = $scope.selectedProjectSort;
            }
            var params = queryParams ? queryParams : {
                page: PAGE,
                size: maxNumOfMemberToShow * $scope.pageOffset,
                orderBy: orderByArray,
                searchString: $scope.searchMemberString
            };
            var url = "/rs/project/" + project.id + '/members';
            $scope.isLoadingMembers = true;
            projectService.getMemberList(project, url, params, function(project, data) {
                $scope.isLoadingMembers = false;
                if (data.statusCode == 200 && data.results.detail != null) {
                    $rootScope.totalMembers = data.results.detail.totalMembers;
                    if (data.results.detail.members.length + ($scope.pageOffset - 1) * maxNumOfMemberToShow < $rootScope.totalMembers) {
                        $scope.showMoreMembers = true;
                    } else {
                        $scope.showMoreMembers = false;
                    }
                    if ($scope.pageOffset > 1 || !$scope.searchActivated) {
                        $scope.members = $scope.members.concat(data.results.detail.members);
                    } else {
                        $scope.members = data.results.detail.members;
                    }
                } else if (data.statusCode == 400) {
                    $state.go(STATE_PROJECT_UNAUTHORIZED);
                } else {
                    $scope.errorMessage = "project.error.member";
                }
            });
        }

        var getPendingMembers = function(queryParams) {
            var project = {
                id: projectStateService.getProjectId()
            };
            var params = queryParams ? queryParams : {
                page: PAGE,
                size: maxNumOfMemberToShow * $scope.pendingPageOffset,
                orderBy: $scope.selectedProjectSort,
                searchString: $scope.searchMemberString
            };
            $scope.isLoadingMembers = true;
            var url = "/rs/project/" + project.id + '/invitation/pending';
            projectService.getMemberList(project, url, params, function(project, data) {
                $scope.isLoadingMembers = false;
                if (data.statusCode == 200 && data.results.pendingList != null) {
                    $scope.totalInvitations = data.results.pendingList.totalInvitations;
                    if (data.results.pendingList.invitations.length + ($scope.pendingPageOffset - 1) * maxNumOfMemberToShow < $scope.totalInvitations) {
                        $scope.showMorePendingMembers = true;
                    } else {
                        $scope.showMorePendingMembers = false;
                    }
                    if ($scope.pendingPageOffset > 1 || !$scope.searchActivated) {
                        $scope.pendingMembers = $scope.pendingMembers.concat(data.results.pendingList.invitations);
                    } else {
                        $scope.pendingMembers = data.results.pendingList.invitations;
                    }
                } else if (data.statusCode == 400) {
                    $state.go(STATE_PROJECT_UNAUTHORIZED);
                } else {
                    $scope.errorMessage = "project.error.member";
                }
            });
        }

        $scope.loadMoreMembers = function() {
            $scope.pageOffset = $scope.pageOffset + 1;
            var orderByArray = null;
            if ($state.current.name == STATE_PROJECT_HOME) {
                orderByArray = ["-creationTime"];
            } else {
                orderByArray = $scope.selectedProjectSort;
            }
            var params = {
                page: $scope.pageOffset,
                size: maxNumOfMemberToShow,
                orderBy: orderByArray,
                searchString: $scope.searchMemberString
            };
            getMembers(params);
        }

        $scope.loadMorePendingMembers = function() {
            $scope.pendingPageOffset = $scope.pendingPageOffset + 1;
            var params = {
                page: $scope.pendingPageOffset,
                size: maxNumOfMemberToShow,
                orderBy: $scope.selectedProjectSort,
                searchString: $scope.searchMemberString
            };
            getPendingMembers(params);
        }

        $scope.sortOptions = [{
                'lookupCode': ['-folder', '-lastModified'],
                'description': 'last.modified'
            },
            {
                'lookupCode': ['-folder', 'lastModified'],
                'description': 'first.modified'
            },
            {
                'lookupCode': ['-folder', 'name'],
                'description': 'filename.ascending'
            },
            {
                'lookupCode': ['-folder', '-name'],
                'description': 'filename.descending'
            },
            {
                'lookupCode': ['-folder', 'size', 'name'],
                'description': 'file.size.ascending'
            },
            {
                'lookupCode': ['-folder', '-size', 'name'],
                'description': 'file.size.descending'
            }
        ];

        $scope.sortProjectOptions = [{
                'lookupCode': ['displayName'],
                'description': 'username.ascending'
            },
            {
                'lookupCode': ['-displayName'],
                'description': 'username.descending'
            },
            {
                'lookupCode': ['creationTime'],
                'description': 'joinTime.ascending'
            },
            {
                'lookupCode': ['-creationTime'],
                'description': 'joinTime.descending'
            }
        ];
        $scope.selectedProjectSort = $scope.sortProjectOptions[0].lookupCode;
        $scope.selectedSort = $scope.sortOptions[0].lookupCode;

        var getRecentFiles = function() {
            var projectId = projectStateService.getProjectId();
            var params = {
                page: 1,
                size: maxNumOfFileToShow,
                orderBy: $scope.sortOptions[0].lookupCode[1]
            };
            $scope.isLoadingFiles = true;
            projectService.getProjectFiles(projectId, params, function(data) {
                $scope.isLoadingFiles = false;
                if (data.statusCode == 200 && data.results.detail != null) {
                    $scope.totalFiles = data.results.detail.totalFiles;
                    $scope.recentFiles = data.results.detail.files;
                } else if (data.statusCode == 400) {
                    $state.go(STATE_PROJECT_UNAUTHORIZED);
                } else {
                    $scope.errorMessage = "project.error.file";
                }
            });
        }

        var getProjectFiles = function(folder, params) {
            $scope.isLoading = true;
            $scope.currentFolder = folder;
            if($scope.selectedTab === 'allFiles'){
                getAllProjectList();
            }
            var queryParams = params ? params : {
                pathId: folder.pathId,
                orderBy: $scope.selectedSort,
                page: PAGE,
                size: SIZE * $scope.filePageOffset,
                searchString: $scope.searchString, 
                filter: $scope.selectedTab
            }
            if($scope.selectedTab === 'allShared'){
                queryParams.pathId = null;
            }
            projectService.getProjectFiles(projectStateService.getProjectId(), queryParams, setData);
        }

        $scope.loadMoreFiles = function() {
            $scope.filePageOffset = $scope.filePageOffset + 1;
            if ($scope.selectedTab === 'sharedWithProject') {
                getSharedWithProjectFiles();
            } else {
                var queryParams = {
                    pathId: $scope.currentFolder.pathId,
                    orderBy: $scope.selectedSort,
                    page: $scope.filePageOffset,
                    size: SIZE,
                    searchString: $scope.searchString
                }
                getProjectFiles($scope.currentFolder, queryParams);
            }
        }


        //repeated code from fielistController. needs to be refactored
        $scope.createFolderModal = function() {
            $scope.creatingFolder = true;
            $timeout(function() {
                document.getElementById("newFolderTextBox").focus();
            });
        }

        $scope.exitCreateFolderModal = function() {
            $scope.creatingFolder = false;
        }

        $scope.createFolder = function() {
            if ($scope.isLoading) {
                return;
            }
            $scope.isLoading = true;
            var newFolderName = document.getElementById("newFolderTextBox").value.trim();
            if (newFolderName.length == 0) {
                handleCreateFolderError($filter('translate')('create_folder.name.empty'));
                return;
            }
            if (newFolderName.length > 127) {
                handleCreateFolderError($filter('translate')('create_folder.name.long'));
                return;
            }
            if (!/^[\u00C0-\u1FFF\u2C00-\uD7FF\w- ]*$/.test(newFolderName)) {
                handleCreateFolderError($filter('translate')('create_folder.name.incorrect_format'));
                return;
            }

            for (var i = 0; i < $scope.contents.length; i++) {
                if ($scope.contents[i].folder) {
                    if ($scope.contents[i].name.toLowerCase() === newFolderName.toLowerCase()) {
                        handleCreateFolderError($filter('translate')('create_folder.name.existedErr'));
                        return;
                    }
                }
            }
            var path = {
                "parameters": {
                    "parentPathId": $scope.currentFolder.pathId,
                    "name": newFolderName
                }
            };

            projectService.createFolder(path, projectStateService.getProjectId(), function(data) {
                if (data.statusCode == 200 && data.results.entry != null && data.results.entry.pathId != null) {
                    $scope.refreshFilePage($scope.currentFolder);
                    showSnackbar({
                        isSuccess: true,
                        messages: $filter('translate')('create_folder.success', {
                            folderName: newFolderName
                        })
                    });
                } else if (data.statusCode == 404) {
                    var errormsg = $filter('translate')('project.folder.not.found');
                    $scope.onClickFile({
                        folder: true,
                        pathId: '/',
                        pathDisplay: '/'
                    });
                    handleCreateFolderError(errormsg);
                } else {
                    var errormsg = $filter('translate')('create_folder.fail', {
                        folderName: newFolderName
                    });
                    handleCreateFolderError(errormsg);
                }
                $scope.creatingFolder = false;
                $scope.isLoading = false;
            });

        }

        var handleCreateFolderError = function(errMsg) {
            $scope.isLoading = false;
            $scope.creatingFolder = false;
            showSnackbar({
                isSuccess: false,
                messages: errMsg
            });
        };

        var setData = function(data) {
            if (data.statusCode == 400) {
                $state.go(STATE_PROJECT_UNAUTHORIZED);
            }
            if (data.statusCode == 404) {
                $scope.onClickFile({
                    folder: true,
                    pathId: '/',
                    pathDisplay: '/'
                });
                $scope.isLoading = false;
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('project.folder.not.found')
                });
                return;
            }
            if (data.results) {
                $scope.usage = data.results.usage;
                $scope.quota = data.results.quota;
                $scope.emptyFolderExists = true;
                if (data.results.detail.totalFiles > 0) {
                    $scope.showMoreFiles = true;
                    if (data.results.detail.files.length < SIZE || data.results.detail.totalFiles == SIZE) {
                        $scope.showMoreFiles = false;
                    }
                    if ($scope.selectedTab === "allShared") {
                        for (var i in data.results.detail.files) {
                            var sharedWithProjectNames = "";  
                            var maxDisplay = 2;
                            var idx = 0;
                            for (var j in data.results.detail.files[i].shareWithProject) {
                                // var sharedProjectName = data.results.detail.files[i].shareWithProject[j].name;
                                var sharedProjectName = data.results.detail.files[i].shareWithProjectName[j];
                                if (idx < maxDisplay) {
                                    if (sharedWithProjectNames.length > 0) {
                                        sharedWithProjectNames = sharedWithProjectNames + ", ";
                                    }
                                    sharedWithProjectNames = sharedWithProjectNames + sharedProjectName;
                                }
                                ++idx;
                            }
                            if (idx > maxDisplay) {
                                sharedWithProjectNames += ' and ';
                                sharedWithProjectNames += (idx - maxDisplay);
                                if(idx - maxDisplay === 1){
                                    sharedWithProjectNames += ' other ';
                                } else {
                                    sharedWithProjectNames += ' others ';
                                }
                            }
                            data.results.detail.files[i].sharedWithProjectNames = sharedWithProjectNames;
                        }
                    }
                    $scope.emptyFolderExists = false;
                    $scope.contents = $scope.contents.concat(data.results.detail.files);
                }
            } else {
                $scope.contents = [];
                $scope.emptyFolderExists = true;
            }
            $scope.isLoading = false;
            buildBreadCrumbs();
            $scope.scrollToTop = true;
        }

        $scope.$watch("usage", function() {
            $scope.myProjectWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.quota) + '%';
        });

        $scope.$watch(function() {
            return $scope.usage > $scope.quota;
        }, function(newValue) {
            $scope.storageExceeded = newValue;
        });

        var buildBreadCrumbs = function() {
            var folder = $scope.currentFolder;
            if (folder.pathId === '/') {
                $scope.breadCrumbsContent = [];
                $scope.parentFolder = null;
            } else {
                var backOrRefresh = false;
                for (var i = 0; i < $scope.breadCrumbsContent.length; i++) {
                    var breadCrumbEntry = $scope.breadCrumbsContent[i];
                    if (breadCrumbEntry.pathId == folder.pathId) {
                        $scope.breadCrumbsContent = $scope.breadCrumbsContent.slice(0, i + 1);
                        backOrRefresh = true;
                        break;
                    }
                }
                if (!backOrRefresh) {
                    if ($scope.searchActivated) {
                        $scope.breadCrumbsContent = [];
                        var breadCrumbsContents = getBreadCrumbsContentForSearch(folder);
                        for (var i = 0; i < breadCrumbsContents.length; i++) {
                            var breadCrumbEntry = breadCrumbsContents[i];
                            $scope.breadCrumbsContent.push(breadCrumbEntry);
                        }
                    } else {
                        $scope.breadCrumbsContent.push(folder);
                    }
                }
                if ($scope.breadCrumbsContent.length > 1) {
                    $scope.parentFolder = $scope.breadCrumbsContent[$scope.breadCrumbsContent.length - 2];
                } else {
                    $scope.parentFolder = rootFolder;
                }
            }
        }

        var getBreadCrumbsContentForSearch = function(folder) {
            var breadCrumbsContents = [];
            var folderPaths = folder.pathDisplay.split('/');
            var folderPathIds = folder.pathId.split('/');
            var folderPathEndPos = 0;
            var folderPathIdEndPos = 0;
            for (var i = 1; i < folderPaths.length; i++) {
                folderPathEndPos += folderPaths[i].length + 1;
                folderPathIdEndPos += folderPathIds[i].length + 1;
                var path = folder.pathDisplay.substr(0, folderPathEndPos);
                var pathId = folder.pathId.substr(0, folderPathIdEndPos + 1);
                breadCrumbsContents.push({
                    pathDisplay: path,
                    pathId: pathId,
                    name: folderPaths[i],
                    folder: true
                });
            }
            return breadCrumbsContents;
        }

        $scope.onClickFile = function(file) {
            if (file.folder) {
                $scope.searchActivated = false;
                $scope.searchString = "";
                if (file.pathId != $scope.currentFolder.pathId) {
                    $scope.filePageOffset = 1;
                }
                $scope.refreshFilePage(file);
            } else {
                $scope.isLoading = true;
                var settings = initSettingsService.getSettings();
                if (!settings.userPreferences.disablePromptProjFileDownload) {
                    initSettingsService.reloadSettings(function success() {
                        showFile(file, initSettingsService.getSettings());
                    }, function error() {
                        showFile(file, settings);
                    });
                } else {
                    showFile(file, settings);
                }
            }
        }

        var showFile = function(file, settings) {
            var showFileParams = $.param({
                pathId: file.pathId,
                pathDisplay: file.pathDisplay,
                lastModifiedDate: !file.lastModified ? 0 : file.lastModified,
                userName: settings.userName,
                offset: new Date().getTimezoneOffset(),
                tenantName: settings.tenantName,
                projectId: projectStateService.getProjectId(),
                promptDownload: !settings.userPreferences.disablePromptProjFileDownload
            });
            projectService.showFile(file, showFileParams, openViewer)
        }

        $scope.$on("projectTagList", function(event, result) {
            $scope.projectTagList = result.data;
        });

        $scope.addProject = function() {
            $scope.createProjectLoading = true;
            $scope.currentIds = $("#projectInviteTags").tagit("assignedTags");
            var project = {
                "parameters": {
                    "projectName": $scope.projectName,
                    "projectDescription": $scope.projectDescription,
                    "emails": $scope.currentIds,
                    "invitationMsg": $scope.invitationMsg,
                    "projectTags": $scope.projectTagList
                }
            };

            projectService.createProject(project, function(data) {
                $scope.createProjectLoading = false;
                if (data.statusCode == 200 && data.results.projectId != null && !isNaN(data.results.projectId)) {
                    projectStateService.setTokenGroupName(data.results.membership.tokenGroupName);
                    $state.go(STATE_PROJECT_CONFIGURATION_CLASSIFICATION, {
                        projectId: data.results.projectId,
                        fromCreateProject: true,
                        tenantId: data.results.membership.tokenGroupName
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: data.message
                    });
                }
            });
        }

        $scope.proceed = function() {
            $scope.projectTagsStep = true;
        }

        $scope.cancel = function() {
            $scope.projectTagsStep = false;
        }

        $scope.goBack = function() {
            if ($scope.fromState) {
                $state.go($scope.fromState, $scope.fromParams);
            } else {
                $state.go(STATE_LANDING);
            }
        }

        $scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
            if(to.name === STATE_CREATE_PROJECT) {
                $scope.fromState = from.name;
                $scope.fromParams = fromParams;
            }
            if(to.name === STATE_PROJECT_CONFIGURATION_INFO) {
                $scope.configTabId = 0;
            } else if(to.name === STATE_PROJECT_CONFIGURATION_CLASSIFICATION) {
                $scope.configTabId = 1;
            } else if(to.name === STATE_PROJECT_CONFIGURATION_PREFERENCE) {
                $scope.configTabId = 2;
            } else if(to.name === STATE_PROJECT_CONFIGURATION_ACCESS_PERMISSIONS) {
                $scope.configTabId = 3;
            }
        });

        $scope.configureProject = function() {
            $scope.configureProjectLoading = true;
            var project = {
                "parameters": {
                    "projectName": $scope.updateParams.projectName,
                    "projectDescription": $scope.updateParams.projectDescription,
                    "invitationMsg": $scope.updateParams.invitationMsg
                }
            };

            projectService.updateProject(project, $scope.project.id, function(data) {
                $scope.configureProjectLoading = false;
                if (data.statusCode == 200) {
                    $scope.project.name = data.results.detail.name;
                    $scope.project.description = data.results.detail.description;
                    $scope.totalFiles = data.results.detail.totalFiles;
                    $rootScope.totalMembers = data.results.detail.totalMembers;
                    
                    if($scope.fromCreateProject) {
                        $state.go(STATE_PROJECT_CONFIGURATION_CLASSIFICATION, {tenantId: data.results.detail.tenantId});
                    } else {
                        showSnackbar({
                            isSuccess: true,
                            messages: $filter('translate')('project.update.success')
                        });
                    }
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('project.update.error')
                    });
                }
                $scope.configChanged = false;
            });
        }

        var getHeaders = function() {
            return {
                'Content-Type': 'application/json; charset=utf-8',
                'userId': $cookies.get('userId'),
                'ticket': $cookies.get('ticket'),
                'clientId': $cookies.get('clientId'),
                'platformId': $cookies.get('platformId')
            };
        }

        $scope.onClickShare = function(file) {
            $scope.selectedFile = file;
            $scope.isLoading = false;
            resetSelectedProject();

            if ($scope.projectsAll === null || $scope.projectsAll.length == 0) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('no.projects.to.share.with')
                });
                return;
            }

            shareDialogService.shareFile({
                file: $scope.selectedFile,
                rights: $scope.data.results.fileInfo.rights,
                owner: $scope.data.results.fileInfo.owner,
                nxl: $scope.data.results.fileInfo.nxl,
                operation: "share",
                startDate: $scope.data.results.fileInfo.expiry.startDate,
                endDate: $scope.data.results.fileInfo.expiry.endDate,
                protectionType: $scope.data.results.fileInfo.protectionType, 
                tags: $scope.data.results.fileInfo.tags,
                projectId : projectStateService.getProjectId() ,
                isSharedFromProject : true ,
                sharedToProjectsList : $scope.projectsAll
            });
            $scope.toggleMenuMode();
        }

        var resetSelectedProject = function() {
            for (var i in  $scope.projectsAll ) {
                $scope.projectsAll[i].selected = false;
            }
        }

        var getAllProjectList = function() {
            projectService.getAllProjectList(function (data) {
                if (data) {
                    $scope.projectsAll = [];
                    for (var i = 0; i < data.detail.length; i++) {
                        var project = data.detail[i];
                        project.selected = false;
                        if (project.id != projectStateService.getProjectId()) {
                            $scope.projectsAll.push(project);
                        }
                    }
                }
            });
        }

        $scope.viewSharedFileDetails = function(file) {
            $scope.isLoading = true;
            $scope.duid = file.duid;
            $scope.filePathId = file.pathId;
            resetSelectedProject();
            $scope.shareWithProject = file.shareWithProject;
            $scope.sharedWithProjectNames = file.sharedWithProjectNames;
            var params = {
                "parameters": {
                    "pathId": file.pathId
                }
            };
            projectService.getFileDetails(params, projectStateService.getProjectId(), function(data){
                $scope.isLoading = false;
                data.results.fileInfo.shared = data.isShared;
                data.results.fileInfo.deleted = false; 
                
                $scope.data = data;
                if(data.statusCode == 200) {
                    shareDialogService.viewSharedFileDetails({
                        duid: $scope.duid,
                        pathId: $scope.filePathId,
                        file: data.results.fileInfo,
                        fromSpace: "PROJECTSPACE",
                        isSharedFromProject: true,
                        projectId : projectStateService.getProjectId() ,
                        sharedToProjectsList : $scope.projectsAll,
                        shareWithProject : $scope.shareWithProject,
                        sharedWithProjectNames : $scope.sharedWithProjectNames
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: data.message
                    });
                }
                
            });
        }

        $rootScope.$on("refreshProjectSharedFileList", function(event, args) {
            $scope.refreshFilePage($scope.currentFolder)
        })

        $scope.toggleTabs = function(value) {
            $scope.filePageOffset = 1;
            if ($scope.selectedTab !== value) {
                $scope.recentFiles = [];
                $scope.selectedTab = value;
                dismissSnackbar();
                if($scope.selectedTab === 'sharedWithProject' ){
                    $scope.sortOptions = projectService.getSortOptions(true);
                    $scope.selectedSort = $scope.sortOptions[0].lookupCode;
                    getSharedWithProjectFiles();
                } else {
                    $scope.sortOptions = projectService.getSortOptions(false);
                    $scope.selectedSort = $scope.sortOptions[0].lookupCode;
                    $scope.refreshFilePage(rootFolder);
                }
            }
        }

        var getSharedWithProjectFiles = function() {
            $scope.isLoading = true;
            $scope.showMore = true;
            $scope.projectId = projectStateService.getProjectId();
            var params = {
                page: $scope.filePageOffset,
                size: SIZE,
                orderBy : $scope.selectedSort,
                fileName : $scope.searchString,
                filterOptions : $scope.selectedFilter,
                fromSpace : '1',
                spaceId : projectStateService.getProjectId()
            };
            shareFileService.getSharedWithMeFiles(params, loadFiles);
        }

        var loadFiles = function(data) {
            if ($scope.selectedTab === "sharedWithProject") {
                if (data.results.detail.totalFiles > 0) {
                    $scope.showMoreFiles = true;
                    if (data.results.detail.files.length < SIZE || data.results.detail.totalFiles == SIZE) {
                        $scope.showMoreFiles = false;
                    }
                }
                $scope.recentFiles = $scope.recentFiles.concat(data.results.detail.files);
            }
            $scope.isLoading = false;
        }
        
        $scope.onClickDownload = function(file) {
            var settings = initSettingsService.getSettings();
            if (!settings.userPreferences.disablePromptProjFileDownload) {
                initSettingsService.reloadSettings(function success() {
                    var prompt = !initSettingsService.getSettings().userPreferences.disablePromptProjFileDownload;
                    downloadFile(file, prompt);
                }, function error() {
                    downloadFile(file, true);
                });
            } else {
                downloadFile(file, false);
            }
        }

        $scope.onClickDecrypt = function(file) {
            projectService.decryptProjectFile(projectStateService.getProjectId(), file.pathId);
        }

        var downloadFile = function(file, prompt) {
            if (prompt) {
                dialogService.confirm({
                    msg: $filter('translate')('project.file.download.prompt'),
                    ok: function(data) {
                        if (data.checkboxModel) {
                            var params = {
                                parameters: {
                                    preferences: {
                                        disablePromptProjFileDownload: true
                                    }
                                }
                            };
                            networkService.post(RMS_CONTEXT_NAME + "/rs/usr/profile", JSON.stringify(params), getHeaders(), function(data) {
                                if (data != null && data.statusCode == 200) {
                                    initSettingsService.getSettings().userPreferences.disablePromptProjFileDownload = true;
                                }
                            });
                        }
                        projectService.downloadProjectFile(projectStateService.getProjectId(), file.pathId);
                    },
                    cancel: function() {},
                    showCheckbox: $filter('translate')('dont.show.again'),
                });
            } else {
                projectService.downloadProjectFile(projectStateService.getProjectId(), file.pathId);
            }
        }

        var openViewer = function(data) {
            $scope.isLoading = false;
            var redirectUrl = data.viewerUrl;
            openSecurePopup(redirectUrl);
        }

        var getRepoList = function() {
            $scope.repoList = [];

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
        }

        getRepoList();

        $scope.uploadFileModal = function(selectedFile) {
                var folder = $state.current.name == STATE_PROJECT_HOME ? rootFolder : $scope.currentFolder;
                var parameters = {
                    repoList: $scope.repoList,
                    mydrive: $scope.mydrive,
                    operation: "uploadProjectFile",
                    header: $filter('translate')('widget.upload.project.file.label') + " " + $scope.project.name,
                    project: $scope.project,
                    currentFolder: folder,
                    toProjectSpace: true
                };
                protectWidgetService.protectFileModal(parameters, selectedFile, function() {
                    if ($state.current.name == STATE_PROJECT_HOME) {
                        getRecentFiles();
                    } else {
                        $scope.refreshFilePage(parameters.currentFolder);
                    }
                });
        }

        $scope.onClickClassify = function(file) {
            $scope.isLoading = true;
            var params = {
                "parameters": {
                    "pathId": file.pathId
                }
            };
            projectService.getFileDetails(params, projectStateService.getProjectId(), openClassifyWindow);
        }

        $scope.toggleAllMenuMode = function(pathIdParam) {
            for (var i = 0; i < $scope.contents.length; i++) {
                if ($scope.contents[i].pathId!=pathIdParam) {
                    $scope.contents[i].isMenuClicked=false;
                } else {
                    $scope.contents[i].isMenuClicked=!$scope.contents[i].isMenuClicked;
                }
            }
        }
        $scope.getPermissions = function(pathId, isFolder) {
            $scope.toggleAllMenuMode(pathId);
            $scope.selectedFileId = pathId;
            if(isFolder) {
                return;
            }
            $scope.isLoading = true;
            var params = {
                "parameters": {
                    "pathId": pathId
                }
            };
            projectService.getFileDetails(params, projectStateService.getProjectId(), function(data){
                $scope.isLoading = false;
                var error;
                $scope.data = data;
                if(data.statusCode == 200) {
                    $scope.rights = data.results.fileInfo.rights;
                    $scope.protectionType = data.results.fileInfo.protectionType;
                } else if (data.statusCode == 404) {
                    error = $filter('translate')('project.file.not.found');
                } else {
                    error = data.message;
                }
                if (error) {
                    showSnackbar({
                        isSuccess: false,
                        messages: error
                    });
                }
            });
        }

        $scope.onClickInfo = function() {
            openInfoWindow($scope.data);
        }

        $scope.onClickDelete = function(file) {
            var fileTypeStr = file.folder ? 'delete.folder.confirmation' : 'delete.file.confirmation';
            dialogService.confirm({
                msg: $filter('translate')(fileTypeStr),
                ok: function() {
                    $scope.isLoading = true;
                    $scope.selectedFile = file;
                    var path = {
                        "parameters": {
                            "pathId": file.pathId
                        }
                    };
                    projectService.deleteFilesFolders(path, projectStateService.getProjectId(), function(data) {
                        var isSuccess = false;
                        var message;
                        if (data.statusCode == 200 && data.results.pathId != null && data.results.name != null) {
                            $scope.refreshFilePage($scope.currentFolder);
                            isSuccess = true;
                            message = $filter('translate')('folder.file.delete.success1', {
                                fileType: (file.folder ? "Folder" : "File")
                            }) + file.name + $filter('translate')('folder.file.delete.success2');
                        } else if (data.statusCode == 404) {
                            if (file.folder) {
                                message = $filter('translate')('project.folder.not.found');
                            } else {
                                message = $filter('translate')('project.file.not.found');
                            }
                        } else {
                            message = $filter('translate')('folder.file.delete.error', {
                                fileType: (file.folder ? "folder" : "file")
                            }) + file.name + $filter('translate')('ending');
                        }
                        $scope.isLoading = false;
                        showSnackbar({
                            isSuccess: isSuccess,
                            messages: message
                        });
                    });
                }
            });
        }

        $scope.search = function() {
            $scope.filePageOffset = 1;
            if ($scope.searchString.length == 0) {
                dismissSnackbar();
                if ($scope.canResetSearchResults) {
                    $scope.refreshFilePage($scope.currentFolder);
                    $scope.canResetSearchResults = false;
                }
                $scope.searchActivated = false;
                return;
            } else {
                $scope.searchActivated = true;
                $scope.canResetSearchResults = true;
                $scope.contents = [];
            }
            $scope.isLoading = true;
            
            if ($scope.selectedTab === 'sharedWithProject') {
                $scope.recentFiles = [];
                var params = {
                    page: PAGE,
                    size: SIZE * $scope.filePageOffset,
                    orderBy: $scope.selectedSort,
                    fileName: $scope.searchString,
                    filterOptions: $scope.selectedFilter,
                    fromSpace: '1',
                    spaceId: projectStateService.getProjectId()
                };
                shareFileService.getSharedWithMeFiles(params, loadFiles);
            } else {
                var queryParams = {
                    pathId: $scope.currentFolder.pathId,
                    orderBy: $scope.selectedSort,
                    page: $scope.filePageOffset,
                    size: SIZE,
                    searchString: $scope.searchString,
                    filter: $scope.selectedTab
                }
                if ($scope.selectedTab === 'allShared') {
                    queryParams.pathId = null;
                }
                projectService.getProjectFiles(projectStateService.getProjectId(), queryParams, function (data) {
                    if (data.statusCode == 400) {
                        $state.go(STATE_PROJECT_UNAUTHORIZED);
                    }
                    if (data.results && data.results.detail.totalFiles > 0) {
                        $scope.showMoreFiles = true;
                        if (data.results.detail.files.length < SIZE || data.results.detail.totalFiles == SIZE) {
                            $scope.showMoreFiles = false;
                        }
                        $scope.contents = data.results.detail.files;
                        dismissSnackbar();
                    } else {
                        $scope.contents = [];
                        $scope.noSearchResult = true;
                    }
                    $scope.isLoading = false;
                });
            }
        }

        $scope.searchMember = function() {
            dismissSnackbar();
            if ($scope.searchMemberString.length == 0) {
                if ($scope.searchActivated === true) {
                    $scope.members = [];
                    $scope.pendingMembers = [];
                    getMembers();
                    getPendingMembers();
                    $scope.searchActivated = false;
                }
                return;
            }
            $scope.pageOffset = 1;
            $scope.pendingPageOffset = 1;
            $scope.members = [];
            $scope.pendingMembers = [];
            $scope.searchActivated = true;
            getMembers();
            getPendingMembers();

        }

        var openInfoWindow = function(data) {
            var error;
            if (data.statusCode == 200) {
                dialogService.info({
                    isProject: true,
                    tags: [],
                    rights: data.results.fileInfo.rights,
                    endDate: data.results.fileInfo.expiry.endDate,
                    startDate: data.results.fileInfo.expiry.startDate,
                    owner: data.results.fileInfo.owner,
                    nxl: data.results.fileInfo.nxl,
                    fileName: data.results.fileInfo.name,
                    fileSize: data.results.fileInfo.size,
                    fileType: data.results.fileInfo.fileType,
                    repoName: '',
                    lastModifiedTime: data.results.fileInfo.lastModified,
                    path: data.results.fileInfo.pathDisplay,
                    tags: data.results.fileInfo.tags,
                    protectionType: data.results.fileInfo.protectionType,
                    shareWithProjects: data.results.fileInfo.shareWithProjects,
                    fromTab: $scope.selectedTab
                });
            } else if (data.statusCode == 404) {
                error = $filter('translate')('project.file.not.found');
            } else {
                error = data.message;
            }
            $scope.toggleMenuMode();
            if (error) {
                showSnackbar({
                    isSuccess: false,
                    messages: error
                });
            }
        }

        var openClassifyWindow = function(data) {
            $scope.isLoading = false;
            var error;
            if (data.statusCode == 200) {
                dialogService.getClassificationDetails({
                    fileName: data.results.fileInfo.name,
                    path: data.results.fileInfo.pathDisplay,
                    tags: data.results.fileInfo.tags
                });
            } else if (data.statusCode == 404) {
                error = $filter('translate')('project.file.not.found');
            } else {
                error = data.message;
            }
            $scope.toggleMenuMode();
            if (error) {
                showSnackbar({
                    isSuccess: false,
                    messages: error
                });
            }
        }

        var checkInvitationCookie = function() {
            var projectName = $cookies.get("projectName");
            if (projectName) {
                $scope.projectName = projectName;
                $cookies.remove('projectName', {
                    path: '/'
                });
                $cookies.remove('projectName', {
                    path: '/'
                });
                showSnackbar({
                    isSuccess: true,
                    messages: $filter('translate')('project.invitation.accept.success', {
                        projectName: $scope.projectName
                    })
                });
            }
        }

        $scope.clearSearch = function() {
            $scope.searchString = "";
            dismissSnackbar();
            $scope.noSearchResult = false;
            $scope.searchActivated = false;
            $scope.filePageOffset = 1;
            $scope.refreshFilePage(rootFolder);
        }
        $scope.clearMemberSearch = function() {
            $scope.searchMemberString = "";
            $scope.pageOffset = 1;
            $scope.pendingPageOffset = 1;
            $scope.searchActivated = false;
            dismissSnackbar();
            $scope.refreshUsersPage();
        }

        $scope.update = function () {
            $scope.contents = [];
            $scope.isLoading = true;

            $scope.recentFiles = [];
            if ($scope.selectedTab === 'sharedWithProject') {
                var params = {
                    page: PAGE,
                    size: SIZE * $scope.filePageOffset,
                    orderBy: $scope.selectedSort,
                    fileName: $scope.searchString,
                    filterOptions: $scope.selectedFilter,
                    fromSpace: '1',
                    spaceId: projectStateService.getProjectId()
                };
                shareFileService.getSharedWithMeFiles(params, loadFiles);
            } else {

                var queryParams = {
                    pathId: $scope.currentFolder.pathId,
                    orderBy: $scope.selectedSort,
                    page: PAGE,
                    size: SIZE * $scope.filePageOffset,
                    searchString: $scope.searchString,
                    filter: $scope.selectedTab
                }
                if ($scope.selectedTab === 'allShared') {
                    queryParams.pathId = null;
                }
                projectService.getProjectFiles($stateParams.projectId, queryParams, setData);
            }
        }

        $scope.refreshFilePage = function(folder) {
            if (!folder.uploadProjectFileFail) {
                dismissSnackbar();
            }
            $scope.contents = [];
            getProjectFiles(folder);
        }

        $scope.refreshUsersPage = function() {
            getProjectDetails();
            $scope.members = [];
            $scope.pendingMembers = [];
            $scope.showMoreMembers = true;
            $scope.showMorePendingMembers = true;
            getMembers();
            getPendingMembers();

        }

        $scope.$watch('fileupload', function(newValue, oldValue) {
            if (newValue && $scope.fileupload) {
                $scope.uploadFileModal($scope.fileupload);
            }
        });
        init();

        $scope.viewFileActivity = function(file) {
            var params = {};
            params.userId = window.readCookie("userId");
            params.ticket = window.readCookie("ticket");
            params.duid = file.duid;
            params.file = file
            params.start = 0;
            params.count = 50;

            shareDialogService.viewSharedFileActivity(params);
            $scope.toggleMenuMode();
        }

        $scope.fileIconName = function(eachFile) {
            eachFile.protectedFile = true;
            return repositoryService.getIconName(eachFile);
        }

        $scope.changeConfig = function() {
            $scope.configChanged = true;
        }
    }
]);

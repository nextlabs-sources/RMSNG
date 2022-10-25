mainApp.factory('protectWidgetService', ['$uibModal', '$rootScope', '$state', '$filter', 'dialogService', 'workSpaceService', 'projectService', 'networkService', 'initSettingsService', 'uploadFileService', 'Upload', 'repositoryService', 'navService', 'digitalRightsExpiryService', '$controller',
    function($uibModal, $rootScope, $state, $filter, dialogService, workSpaceService, projectService, networkService, initSettingsService, uploadFileService, Upload, repositoryService, navService, digitalRightsExpiryService, $controller) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var protectFileModal = function(parameter, selectedFiles, callback) {
            $uibModal.open({
                animation: true,
                windowClass: 'app-modal-landing-widget',
                templateUrl: 'ui/app/Home/Repositories/partials/protectWidgetTemplate.html',
                controllerAs: 'modal',
                controller: ['$uibModalInstance', '$scope', '$state', 'Upload', '$timeout', 'uploadFileService', 'repositoryService', 'sharedWorkspaceService', '$controller', 'userPreferenceService', '$uibModal', 'RightsProtectionMethodService',
                    function($uibModalInstance, $scope, $state, Upload, $timeout, uploadFileService, repositoryService, sharedWorkspaceService, $controller, userPreferenceService, $uibModal, RightsProtectionMethodService) {
                        $scope.projectId = parameter.project ? parameter.project.id : undefined;
                        var init = function () {
                            userPreferenceService.getPreference(function (data) {
                                if (data.statusCode == 200) {
                                    if (data.results != undefined) {
                                        $scope.watermarkStr = data.results.watermark;
                                        $controller('watermarkController',{$scope: $scope});
                                        $scope.showWatermarkResult($scope.watermarkStr);
                                        if (data.results.expiry) {
                                            $scope.expiryJson = data.results.expiry;
                                            $scope.expiryStr = digitalRightsExpiryService.getExpiryStr($scope.expiryJson);
                                        }
                                        return;
                                    }
                                }
                            });
                            $controller('classificationSelectionController',{$scope: $scope});
                        };
                        init();
                        var MAX_FILE_SIZE = 157286400;
                        var fileName;
                        var userIdFromCookie = window.readCookie('userId');
                        var ticketFromCookie = window.readCookie('ticket');
                        var clientId = window.readCookie('clientId');
                        var platformId = window.readCookie('platformId');
                        $scope.protectUsing = {};
                        $scope.protectUsing.mode = "rights";
                        $scope.repoList = parameter.repoList;
                        $scope.mydrive = parameter.mydrive;
                        $scope.operation = parameter.operation;
                        $scope.project = parameter.project;
                        $scope.selectedSort = ['-isFolder', '-lastModifiedTime'];
                        $scope.callback = callback;
                        $scope.files = selectedFiles;
                        $scope.filePristine = true;
                        $scope.expiryInfo = true;
                        $scope.toProjectSpace = parameter.toProjectSpace;
                        $scope.currentFolder = parameter.currentFolder;
                        $scope.fileChosen = false;
                        $scope.mailPristine = true;
                        $scope.doApply = true;
                        $scope.collapsed = {};
                        $scope.comment = {};
                        $scope.header = parameter.header;
                        $scope.showWatermark = false;
                        $scope.editWatermark = false;
                        $scope.editExpiry = false;
                        $scope.tenantId = parameter.tenantId;
                        $scope.isError = false;
                        $scope.errorFadeOut = false;
                        $scope.errorTimer = {};
                        // In place protection is not developed for BOX yet
                        // NOTE: SHAREPOINT_ONLINE here is used in the shareDialogService as well.
                        // Implementations of SHAREPOINT_ONLINE in dialogService and uploadDialogService are deprecated code
                        var inPlaceProtectionEnabledRepos = [
                            'GOOGLE_DRIVE',
                            'ONE_DRIVE',
                            'SHAREPOINT_ONLINE',
                            'DROPBOX'
                        ];
                        RightsProtectionMethodService.getAdhocRights(function(data) {
                            if(data.statusCode == 200) {
                                $scope.isAdhocRightEnabled = data.extra["ADHOC_ENABLED"];
                            } else {
                                showSnackbar({
                                     isSuccess: false,
                                     messages: $filter('translate')('adhoc.rights.enable.failure')
                                });
                                $scope.isAdhocRightEnabled = true;
                            }
                            if($scope.isAdhocRightEnabled) {
                                $scope.protectUsing.mode = "rights";
                            } else {
                                $scope.protectUsing.mode = "classification";
                            }
                        });

                        $scope.showError = function(message) {
                            if($scope.isError) {
                                $timeout.cancel($scope.errorTimer);
                                $scope.errorFadeOut = true;
                                $timeout(function(){
                                    $scope.errorFadeOut = false;
                                    $scope.error = message;
                                    $scope.errorTimer = $timeout(function() {
                                        $scope.hideError();
                                    },5000);
                                },200);
                            } else {
                                $scope.isError = true;
                                $scope.error = message;
                                $scope.errorTimer = $timeout(function() {
                                    $scope.hideError();
                                },5000);
                            }
                        };

                        $scope.hideError = function() {
                            $timeout.cancel($scope.errorTimer);
                            if($scope.isError) {
                                $scope.errorFadeOut = true;
                                $timeout(function(){
                                    $scope.errorFadeOut = false;
                                    $scope.isError = false;
                                    $scope.error = "";
                                },200);
                            }
                        };

                        $scope.toggleRightType = function(mode) {
                            if(mode == "rights") {
                                $scope.protectUsing.mode = "rights";
                                $timeout(function(){
                                    $scope.showWatermarkResult($scope.watermarkStr);
                                })
                            } else if(mode == "classification") {
                                $scope.protectUsing.mode = "classification";
                            }
                        }

                        $scope.openEdit= function (option) {
                            $scope.showEditWatermarkModal = true;
                            $uibModal.open({
                                animation: true,
                                scope: $scope,
                                windowClass: 'quick-fade-out',
                                templateUrl: 'ui/app/Home/SharedFiles/partials/editWatermark.html',
                                controller: ['$uibModalInstance', '$scope', '$rootScope', '$state', '$filter', '$timeout', 'navService', '$controller',
                                    function($uibModalInstance, $scope, $rootScope, $state, $filter, $timeout, navService, $controller) {
                                        if (option === 'watermark') {
                                            $scope.isEditWatermark = true;
                                            var watermarkScope = $controller('watermarkController',{$scope: $scope});
                                        } else {
                                            $scope.isEditWatermark = false;
                                            var digitalRightsExpiryScope = $controller('digitalRightsExpiryDateController', {$scope: $scope});
                                        }
                                        $scope.cancel = function () {
                                            $uibModalInstance.dismiss('cancel');
                                        }
                                        $scope.save = function () {
                                            if ($scope.isEditWatermark) {
                                                if ($scope.addWatermarkStr().length > 50) {
                                                    return;
                                                }
                                                $scope.$parent.watermarkStr = $scope.addWatermarkStr();
                                                $scope.showWatermarkResult($scope.$parent.watermarkStr);
                                            } else {
                                                $scope.$parent.expiryJson = $scope.addExpiry();
                                                $scope.$parent.expiryStr = digitalRightsExpiryService.getExpiryStr($scope.expiryJson);
                                            }
                                            $uibModalInstance.dismiss('cancel');
                                        }

                                        $scope.$on('onExpiryTypeChange',function(event,result){
                                            $scope.expiryType = result.data;
                                        });

                                        $scope.$on('onExpiryYearChange',function(event,result){
                                            $scope.expiry.year = result.data;
                                        });

                                        $scope.$on('onExpiryMonthChange',function(event,result){
                                            $scope.expiry.month = result.data;
                                        });

                                        $scope.$on('onExpiryWeekChange',function(event,result){
                                            $scope.expiry.week = result.data;
                                        });

                                        $scope.$on('onExpiryDayChange',function(event,result){
                                            $scope.expiry.day = result.data;
                                        });

                                        $scope.$on('onDatePickerMilliSecChange',function(event, result){
                                            $scope.datePickerMilliSec = result.data;
                                        });

                                        $scope.$on('onStartDatePickerMilliSecChange',function(event, result){
                                            $scope.startDatePickerMilliSec = result.data;
                                        });

                                        $scope.$on('onEndDatePickerMilliSecChange',function(event, result){
                                            $scope.endDatePickerMilliSec = result.data;
                                        });
                                    }
                                ]
                            });
                        };

                        $scope.$watch('rights.length',function(newValue, oldValue){
                            $scope.showWatermark = $scope.rights.indexOf('WATERMARK') >= 0 ? true : false;
                            if (!$scope.showWatermark) {
                                $scope.editWatermark = false;
                            } else if (!$scope.watermarkScope) {
                                $scope.watermarkScope = $controller('watermarkController',{$scope: $scope});
                            }
                            $scope.isLoading = false;
                        });

                        $scope.$on('modal.closing', function(event, reason, closed) {
                            if ($scope.isLoading) {
                                event.preventDefault();
                            }
                        });

                        $scope.contentRightsAvailable = dialogService.getContentRights();
                        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
                        $scope.effectRightsAvailable = dialogService.getEffectRights();
                        $scope.hiddenSectionRights = dialogService.getHiddenSectionRights();
                        $scope.showOptions = true;
                        $scope.rights = ['VIEW'];
                        $scope.mobile = jscd.mobile;

                        $scope.listFoldersAndFilter = function(filter) {
                            $scope.hideError();
                            $scope.repoContents = [];
                            if($scope.srcRepo.providerClass === 'APPLICATION') {
                                var params = {
                                    path: $scope.srcFolder.path
                                };
                                if(filter === 'nxl') {
                                    params.hideFiles = filter;
                                }
                                $scope.isLoadingFileList = true;
                                sharedWorkspaceService.getFiles(params, $scope.srcRepo.repoId, function(data) {
                                    $scope.isLoadingFileList = false;
                                    if(data.statusCode === 200) {
                                        $scope.repoContents = data.results.detail;
                                    } else {
                                        $scope.showError(data.message);
                                    }
                                    $scope.buildBreadCrumbs();
                                });
                            } else {
                                var repoDetails = $.param({
                                    repoId: $scope.srcRepo.repoId,
                                    path: $scope.srcFolder.usePathId ? $scope.srcFolder.pathId : $scope.srcFolder.path,
                                    filter: filter
                                });
                                $scope.isLoadingFileList = true;
                                repositoryService.getFilesWithPath(repoDetails, function(data) {
                                    $scope.isLoadingFileList = false;
                                    if (!data.result) {
                                        $scope.showError(data.messages[0]);
                                    } else if (data.repoId === $scope.srcRepo.repoId) {
                                        $scope.repoContents = data.content && data.content.name === "Root" ? data.content.children : data.content;
                                    }
                                    $scope.buildBreadCrumbs();
                                });
                            }
                        };

                        $scope.chooseSrcRepo = function(repo) {
                            $scope.collapsed.status = false;
                            $scope.srcRepo = repo;
                            $scope.srcIsLocal = false;                            
                            $scope.srcFolder = {
                                pathId: '/',
                                path: '/',
                                repoId: repo.repoId
                            };
                            $scope.hideError();
                            $scope.exploreFolder($scope.srcFolder);
                        }

                        $scope.chooseSrcLocal = function() {
                            $scope.srcRepo = {};
                            $scope.srcIsLocal = true;
                            $scope.fileChosen = false;
                            $scope.hideError();
                            $scope.collapsed.status = false;
                        }

                        $scope.hideRepo = function (repo) {
                            return repo.providerClass === 'APPLICATION' &&
                                    ($scope.operation === 'share' || $scope.operation === 'uploadProjectFile' || $scope.operation === 'uploadWorkspaceFile');
                        };

                        $scope.init = function() {
                            $scope.chooseSrcLocal();
                        }
                        $scope.exploreFolder = function(folder) {
                            $scope.srcFolder = folder;
                            $scope.isNxl = false;
                            $scope.fileChosen = false;
                            if ($scope.operation === 'protect') {
                                $scope.listFoldersAndFilter("nxl");
                            } else if ($scope.operation === 'share') {
                                $scope.listFoldersAndFilter("nxl");
                            } else if ($scope.operation === 'uploadProjectFile') {
                                $scope.listFoldersAndFilter("nxl");
                            } else if ($scope.operation === 'uploadWorkspaceFile') {
                                $scope.listFoldersAndFilter("nxl");
                            }
                        }

                        $scope.buildBreadCrumbs = function() {
                            var folder = $scope.srcFolder;
                            if (folder.pathId === '/') {
                                $scope.breadCrumbsContent = [];
                                $scope.parentFolder = null;
                            } else {
                                if ($scope.breadCrumbsContent) {
                                    var backOrRefresh = false;
                                    for (var i = 0; i < $scope.breadCrumbsContent.length; i++) {
                                        var breadCrumbEntry = $scope.breadCrumbsContent[i];
                                        if (breadCrumbEntry.pathId === folder.pathId) {
                                            $scope.breadCrumbsContent = $scope.breadCrumbsContent.slice(0, i + 1);
                                            backOrRefresh = true;
                                            break;
                                        }
                                    }
                                    if (!backOrRefresh) {
                                        $scope.breadCrumbsContent.push(folder);
                                    }
                                    if ($scope.breadCrumbsContent.length > 1) {
                                        $scope.parentFolder = $scope.breadCrumbsContent[$scope.breadCrumbsContent.length - 2];
                                    } else {
                                        $scope.parentFolder = {
                                            isFolder: true,
                                            pathId: '/',
                                            path: '/'
                                        };
                                    }
                                }
                            }
                        };

                        $scope.onClickFile = function(item) {
                            if (item.isFolder) {
                                $scope.exploreFolder(item);
                            } else {
                                $scope.srcFile = item;
                                $scope.fileChosen = true;
                                $scope.isNxl = $scope.srcFile.name.endsWith('.nxl');
                                $scope.saveFileLocationConfirmed = true;
                                $scope.protectInPlaceAllowed = false;
                                $scope.saveFileLocation = "myvault";

                                if($scope.operation == "protect" && inPlaceProtectionEnabledRepos.indexOf($scope.srcRepo.type) > -1){
                                    $scope.protectInPlaceAllowed = true;
                                    $scope.saveFileLocation = "inplace";
                                }
                            }
                        }

                        $scope.fileIconName = function(file) {
                            return repositoryService.getIconName(file);
                        }

                        $scope.init();

                        $scope.checkNxl = function() {
                            return $scope.files.name.endsWith('.nxl');
                        };

                        $scope.resetRights = function() {
                            $scope.rights = ['VIEW'];
                        };

                        $scope.resetClassifications = function() {
                            $scope.getClassificationProfile();
                        };

                        var resetFields = function(){
                            $scope.hideError();
                            $scope.resetRights();
                            $scope.resetClassifications();
                            if(($scope.operation === 'uploadProjectFile' || $scope.operation === 'uploadWorkspaceFile' || $scope.saveFileLocation == "inplace") && !$scope.isAdhocRightEnabled) {
                                $scope.protectUsing.mode = "classification"; // classification is selected by default here if user is protecting file outside myvault and adhoc isnt enabled by admin
                            } else {
                                $scope.protectUsing.mode = "rights";
                            }
                        }
                        
                        var checkLengthOfFileName = function(length) {
                            if (length > 128) {
                                $scope.isLoading = false;
                                return true;
                            }
                        }
                        var checkFile = function() {
                            $scope.fileChosen = false;
                            if ($scope.files.type === "" && $scope.files.name.indexOf(".") == -1) {
                                $scope.filePristine = true;
                                $scope.showError($filter('translate')('upload.file.is.folder'));
                            } else if ($scope.files.size > MAX_FILE_SIZE) {
                                $scope.filePristine = true;
                                $scope.showError($filter('translate')('upload.file.maxallowedsize'));
                            } else if (repositoryService.checkInvalidCharacters($scope.files.name)) {
                                $scope.filePristine = true;
                                $scope.showError($filter('translate')('file.upload.invalidfilename'));
                            } else if ($scope.files.size == 0) {
                                $scope.filePristine = true;
                                $scope.showError($filter('translate')('upload.file.empty'));
                            } else if ($scope.operation === 'protect' && $scope.checkNxl()) {
                                $scope.showError($filter('translate')('widget.protect.file.error.nxl.protected'));
                            } else if ($scope.operation === 'uploadProjectFile' && checkLengthOfFileName($scope.files.name.length)) {     
                                $scope.showError($filter('translate')('upload.protected.file.nameLength.tooLong') + $scope.files.name + $filter('translate')('upload.file.nameLength.tooLong2'));
                            } else if ($scope.operation === 'uploadWorkspaceFile' && checkLengthOfFileName($scope.files.name.length)) {     
                                $scope.showError($filter('translate')('upload.protected.file.nameLength.tooLong') + $scope.files.name + $filter('translate')('upload.file.nameLength.tooLong2'));
                            } else {
                                $scope.filePristine = false;
                                $scope.isNxl = $scope.checkNxl();
                                resetFields();
                                $scope.fileChosen = true;
                                $scope.saveFileLocationConfirmed = true;
                                $scope.protectInPlaceAllowed = false;
                                $scope.saveFileLocation = "myvault";    
                            }
                        }

                        if ($scope.files) {
                            checkFile();
                        }

                        $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        };

                        $scope.toggleSelection = function(id) {
                            var idx = $scope.rights.indexOf(id);
                            if (idx > -1) {
                                $scope.rights.splice(idx, 1);
                            } else {
                                $scope.rights.push(id);
                            }
                        };

                        $scope.validateEmail = function(id) {
                            validateEmail(id, $scope) && $scope.currentIds.length > 0;
                        }

                        $scope.$watch('files', function(newValue, oldValue) {
                            if (newValue != oldValue && (oldValue || $scope.filePristine)) {
                                if (newValue == null) {
                                    $scope.files = oldValue;
                                } else {
                                    checkFile();
                                }
                            }
                        });

                        $scope.$watch('protectUsing.mode', function(newValue, oldValue){
                            if(newValue != oldValue) {
                                $scope.hideError();
                            }
                        });

                        $scope.proceed = function() {
                            $scope.hideError();
                            // encryptable (boolean) false currently refers to the file being a Google native file e.g. gdoc
                            if(!$scope.srcIsLocal && !$scope.isNxl && $scope.srcFile.encryptable != undefined && !$scope.srcFile.encryptable) {
                                $scope.fileChosen = false;
                                $scope.showError($filter('translate')('protect.file.not-allowed.google-native-file'));
                                return;
                            }
                            
                            if ($scope.operation === 'share' && $scope.isNxl) {
                                $scope.fileChosen = false;
                                var startDate;
                                var endDate;
                                if ($scope.srcIsLocal) {
                                    checkRights($scope.files, function success(data) {
                                        resetFields();
                                        $scope.rights = data.rights;
                                        $scope.isOwner = data.isOwner;
                                        $scope.protectionType = data.protectionType;
                                        $scope.fileConfirmed = true;
                                        if(data.validity.relativeDay) {
                                            endDate = digitalRightsExpiryService.calculateRelativeEndDate(data.validity.relativeDay.year, data.validity.relativeDay.month, data.validity.relativeDay.week, data.validity.relativeDay.day);
                                        } else {
                                            startDate = data.validity.startDate;
                                            endDate = data.validity.endDate;
                                        }
                                        digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                    }, function error(code) {
                                        $scope.showError($filter('translate')(code));
                                    }, function loading(data) {
                                        $scope.isLoading = data;
                                    })
                                } else {
                                    $scope.checkShareRights($scope.srcFile, function success(data) {
                                        resetFields();
                                        $scope.rights = data.rights;
                                        $scope.isOwner = data.isOwner;
                                        $scope.fileConfirmed = true;
                                        $scope.protectionType = data.protectionType;
                                        if(data.expiry.relativeDay) {
                                            endDate = digitalRightsExpiryService.calculateRelativeEndDate(data.expiry.relativeDay.year, data.expiry.relativeDay.month, data.expiry.relativeDay.week, data.expiry.relativeDay.day);
                                        } else {
                                            startDate = data.expiry.startDate;
                                            endDate = data.expiry.endDate;
                                        }
                                        digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                    });
                                }
                            } else if($scope.operation === 'uploadProjectFile'){
                                if($scope.srcFile!= undefined && checkLengthOfFileName($scope.srcFile.name.length)) {
                                    $scope.fileChosen = false;
                                    $scope.showError($filter('translate')('upload.protected.file.nameLength.tooLong') + $scope.srcFile.name + $filter('translate')('upload.file.nameLength.tooLong2'));
                                    return;
                                } else if($scope.srcFile!= undefined && $scope.srcFile.fileSize == 0) {
                                    $scope.fileChosen = false;
                                    $scope.showError($filter('translate')('upload.file.empty'));
                                    return;
                                }
                                $scope.fileConfirmed = true;
                                resetFields();
                            } else if($scope.operation === 'uploadWorkspaceFile'){
                                if($scope.srcFile!= undefined && checkLengthOfFileName($scope.srcFile.name.length)) {
                                    $scope.fileChosen = false;
                                    $scope.showError($filter('translate')('upload.protected.file.nameLength.tooLong') + $scope.srcFile.name + $filter('translate')('upload.file.nameLength.tooLong2'));
                                    return;
                                } else if($scope.srcFile!= undefined && $scope.srcFile.fileSize == 0) {
                                    $scope.fileChosen = false;
                                    $scope.showError($filter('translate')('upload.file.empty'));
                                    return;
                                }
                                $scope.fileConfirmed = true;
                                resetFields();
                            } else {
                                if($scope.srcFile!= undefined && $scope.srcFile.fileSize == 0) {
                                    $scope.fileChosen = false;
                                    $scope.showError($filter('translate')('upload.file.empty'));
                                    return;
                                }
                                $scope.fileConfirmed = true;
                                resetFields();
                            }
                        }

                        var workspaceConfirmCall = function() {
                            $scope.uploadWorkspaceFile();
                        }

                        var projectConfirmCall = function() {
                            $scope.uploadProjectFile();
                        }

                        var inPlaceProtectConfirmCall = function(data) {
                            $scope.protect();
                        }

                        var myVaultConfirmCall = function() { 
                            if($scope.operation==='share'){
                                $scope.share();
                            } else if ($scope.operation==='protect') {
                                $scope.protect();
                            }
                        }

                        $scope.checkIfWorkspaceFileExistsThenUpload = function(){
                            $scope.userConfirmedFileOverwrite = false;
                            var pathId = getFilePathIdForRepoSelected();
                            $scope.isLoading = true; 
                            workSpaceService.checkIfFilePathExists(pathId, function(data){
                                $scope.isLoading = false;
                                showUserConfirmDialog('workspace', data , workspaceConfirmCall);
                            });
                        }

                        $scope.checkIfProjectFileExistsThenUpload = function(){
                            $scope.userConfirmedFileOverwrite = false;
                            var pathId = getFilePathIdForRepoSelected();
                            $scope.isLoading = true;
                            projectService.checkIfFilePathExists(pathId, $scope.projectId, function(data){
                                $scope.isLoading = false;
                                showUserConfirmDialog('project', data , projectConfirmCall);
                            });
                        }

                        $scope.checkIfInPlaceFileExistsThenProtect = function() {
                            $scope.userConfirmedFileOverwrite = false;
                            $scope.isLoading = true;

                            if($scope.srcRepo.providerClass === 'APPLICATION') {
                                var payload = {
                                    "parameters": {
                                        "path": $scope.srcFile.path + ".nxl"
                                    }
                                };
                                sharedWorkspaceService.checkIfFileExists(payload, $scope.srcRepo.repoId, function(data) {
                                    $scope.isLoading = false;
                                    showUserConfirmDialog('inplace', data, inPlaceProtectConfirmCall);
                                });
                            } else {
                                var searchDetails = $.param({
                                    repoId: $scope.srcRepo.repoId,
                                    searchString: $scope.srcFile.name + ".nxl"
                                });
                                repositoryService.getSearchResults(searchDetails, function(data) {
                                    var fileExists = false;
    
                                    if(data.result === true && data.content.length > 0) {
                                        var searchResults = data.content;
                                        for(var i = 0; i < searchResults.length; i++) {
                                            if (searchResults[i].path == $scope.srcFile.path + ".nxl") {
                                                fileExists = true;
                                                break;
                                            }
                                        }
                                    }
    
                                    if(fileExists) {
                                        dialogService.confirm({
                                            msg:  $filter('translate')('inplace.file.upload.exists') + $scope.srcRepo.name + "."
                                                + $filter('translate')('file.replace.alert.confirm'),
                                            ok: function(data) {
                                                $scope.userConfirmedFileOverwrite = true;
                                                inPlaceProtectConfirmCall();
                                            },
                                            cancel: function() {$scope.isLoading = false;},
                                        });
                                    } else {
                                        inPlaceProtectConfirmCall();
                                    }

                                    $scope.isLoading = false;
                                });
                            }
                        };

                        $scope.checkMyVaultFileExistsThenShareFile = function() {
                            $scope.userConfirmedFileOverwrite = false;
                            var pathId;
                            if ($scope.srcIsLocal) {
                                pathId = $scope.files.name;
                            } else {
                                pathId =$scope.srcFile.name;
                            }
                           
                            $scope.isLoading = true;
                            repositoryService.checkIfMyVaultFilePathExists("/nxl_myvault_nxl/" + pathId, function(data){
                                $scope.isLoading = false;
                                showUserConfirmDialog('myVault', data , myVaultConfirmCall);
                            });
                        }

                        var showUserConfirmDialog = function(space, data, method){
                            $scope.hideError();
                            var message = "";

                            switch (space) {
                                case "workspace":
                                    message =  $filter('translate')('workspace.file.upload.exists')+ $filter('translate')('file.replace.alert.confirm'); 
                                    break;
                                case "project":
                                    message =  $filter('translate')('project.file.upload.exists')+ $filter('translate')('file.overwrite.alert.confirmation');
                                    break;
                                case "myVault": 
                                    message =  $filter('translate')('myvault.file.upload.exists')+ $filter('translate')('file.overwrite.alert.confirmation');
                                    break;
                                case "inplace":
                                    message = $filter('translate')('inplace.file.upload.exists') + $scope.srcRepo.name + "." + $filter('translate')('file.replace.alert.confirm');
                                    break;
                            }

                            if(data.statusCode == 200) {
                                if(data.results.fileExists) {
                                    dialogService.confirm({
                                        msg: message ,
                                        ok: function(data) {
                                            $scope.userConfirmedFileOverwrite = true;
                                            method();
                                        },
                                        cancel: function() {},
                                    });
                                } else {
                                    method();
                                }
                            } else {
                                $scope.showError(data.message);
                            }
                        };

                        var getFilePathIdForRepoSelected = function() {
                            var pathId ; 
                            if ($scope.srcIsLocal) {
                                pathId = $scope.currentFolder.pathId + $scope.files.name;
                            } else {
                                pathId = $scope.currentFolder.pathId + $scope.srcFile.name;
                            }
                            return pathId; 
                        }
                    
                        $scope.uploadProjectFile = function(){
                            $scope.hideError();
                            if($scope.protectUsing.mode === 'classification') {
                                $scope.buildSelectedClassifications();
                                if(!$scope.validClassificationChoice && !$scope.isNxl) {
                                    $scope.showError($filter('translate')('widget.protect.file.error.category.mandatory'));
                                    return;
                                }
                                var tags = $scope.classifications;
                            }
                            $uibModalInstance.dismiss('cancel');
                            $scope.isLoading = true;
                            if ($scope.disableUpload) {
                                $scope.isLoading = false;
                                return;
                            }
                            $scope.disableUpload = true;
                            var url = CONTEXT_PATH + '/rs/project/' + $scope.projectId + '/upload';
                            var expiry = $scope.expiryJson;
                            if(expiry.relativeDay){
                                expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                            }
                            if ($scope.srcIsLocal) {
                                fileName = $scope.files.name;
                                var uploader = Upload.upload({
                                    url: url,
                                    headers: {
                                        'userId': userIdFromCookie,
                                        'ticket': ticketFromCookie,
                                        'clientId': clientId,
                                        'platformId': platformId
                                    },
                                    data: {
                                        file: $scope.files,
                                        "API-input": JSON.stringify({
                                            'parameters': {
                                                'name': $scope.files.name,
                                                'parentPathId': $scope.currentFolder.pathId,
                                                'rightsJSON': $scope.rights,
                                                'tags' : tags,
                                                'watermark' : $scope.watermarkStr,
                                                'expiry' : expiry,
                                                'userConfirmedFileOverwrite' : $scope.userConfirmedFileOverwrite
                                            }
                                        })
                                    }
                                });
                            } else {
                                var map = {"repoId":$scope.srcRepo.repoId,"pathId":$scope.srcFile.pathId,"pathDisplay":$scope.srcFile.path};
                                fileName = $scope.srcFile.name;
                                var uploader = Upload.upload({
                                    url: url,
                                    headers: {
                                        'userId': userIdFromCookie,
                                        'ticket': ticketFromCookie,
                                        'clientId': clientId,
                                        'platformId': platformId
                                    },
                                    data: {
                                        "API-input": JSON.stringify({
                                            'parameters': {
                                                'name': $scope.srcFile.name,
                                                'parentPathId': $scope.currentFolder.pathId,
                                                'rightsJSON': $scope.rights,
                                                'tags' : tags,
                                                'source': map,
                                                'watermark' : $scope.watermarkStr,
                                                'expiry' : expiry,
                                                'userConfirmedFileOverwrite' : $scope.userConfirmedFileOverwrite
                                            }
                                        })
                                    }
                                });
                            }
                            uploadFileService.setCloseStatus(false);
                            uploadFileService.setMinimizeStatus(jscd.mobile && uploadFileService.getMinimizeStatus());
                            if(!$scope.isNxl){
                                $scope.uploadedFileName = fileName + new Date().getTime();
                            }
                            var appendedFileName = getShortName(fileName, 48);
                            var hoverFileName = fileName;
                            $scope.uploadedFilePath = $scope.currentFolder.filePath;
                            uploadFileService.setUploadingStatus(true);
                            uploadFileService.setUploadedStatus(false);
                            uploadFileService.getUploadFileList().push({
                                "displayFileName": appendedFileName,
                                "fileName": $scope.uploadedFileName,
                                "hoverFileName": hoverFileName,
                                "filePath": $scope.uploadedFilePath,
                                "repoId": $scope.projectId,
                                "fileUploading": uploadFileService.getUploadingStatus(),
                                "fileUploaded": uploadFileService.getUploadedStatus(),
                                "percentUploaded": 0
                            });
                            var uploadFinish = function(index, error, response) {
                                var message;
                                var isSuccess = false;
                                dismissSnackbar();
                                if (error) {
                                    if (response.data.statusCode === 5001) {
                                        message = $filter('translate')('project.file.upload.validate.failed');
                                    } else if(response.data.statusCode === 5003){
                                        message = $filter('translate')('project.file.upload.tenant.mismatch');
                                    } else if(response.data.statusCode === 5006){
                                        message = $filter('translate')('project.file.upload.invalid.version');
                                    } else if(response.data.statusCode === 5007){
                                        message = $filter('translate')('project.file.upload.duid.mismatch');
                                    } else if(response.data.statusCode === 5008){
                                    	message = $filter('translate')('project.file.upload.invalid.metadata');
                                    } else if (response.data.statusCode === 404) {                                    
                                        message = $filter('translate')('project.folder.not.found');
                                    } else if (response.data.statusCode === 4001) {
                                        message = $filter('translate')('project.file.upload.exists');
                                    } else if (response.data.statusCode === 4002) {
                                        message = $filter('translate')('project.file.upload.overwrite.error');
                                    } else if (response.data.statusCode === 4003) {
                                        message = $filter('translate')('project.file.upload.failed.expiry');
                                    } else if(response.data.statusCode === 5009){
                                        message = $filter('translate')('project.file.duid.exists');
                                    } else {
                                        message = $filter('translate')('project.file.upload.failed') + fileName + $filter('translate')('ending');
                                    }
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(false);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": $filter('translate')('file.upload.fail') + appendedFileName + $filter('translate')('ending'),
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.projectId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "uploadFailed": true,
                                        "error": message,
                                        "percentUploaded": 0
                                    };
                                } else {
                                    callback($scope.currentFolder);
                                    uploadFileService.removeRedundantMessage($scope.uploadedFileName);
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(true);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": appendedFileName,
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.projectId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "percentUploaded": 100
                                    };
                                    isSuccess = true;
                                    message = $filter('translate')('project.file.upload.success1') + response.data.results.entry.name + $filter('translate')('project.file.upload.success2');
                                }
                                showSnackbar({
                                    isSuccess: isSuccess,
                                    messages: message
                                });
                            }

                            uploader.then(function(response) {
                                    var fileName = $scope.srcIsLocal ? $scope.files.name : $scope.srcFile.name;
                                    $scope.isLoading = false;
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName, $scope.uploadedFilePath, $scope.projectId, uploadFileService.getUploadFileList());
                                    var error = response.data.statusCode !== 200;
                                    uploadFinish(index, error, response);
                                    if (error) {
                                        $scope.showError($filter('translate')('file.upload.fail') + fileName + $filter('translate')('ending'));
                                        $scope.success = false;
                                        $scope.disableUpload = false;
                                    } else {
                                        $scope.success = true;
                                        $scope.protectedFileName = response.data.results.entry.name;
                                        $rootScope.$emit("refreshSharedFileList");
                                        $rootScope.$emit("updateMyDriveUsage");
                                    }
                                }, errorHandler,
                                function(evt) {
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName, $scope.uploadedFilePath, $scope.projectId, uploadFileService.getUploadFileList());
                                    if (evt.total == 0) {
                                        $scope.isLoading = false;
                                        $scope.showError($filter('translate')('protect.file.failure'));
                                        uploadFinish(index, $filter('translate')('upload.file.failure'), null);
                                    } else {
                                        var progressPercentage = parseInt(90.0 * evt.loaded / evt.total);
                                        var uploadEntry = uploadFileService.getUploadFileList()[index];
                                        uploadEntry.percentUploaded = progressPercentage;
                                    }
                                }
                            );              
                        }
                        $scope.uploadWorkspaceFile = function(){
                            $scope.hideError();
                            if($scope.protectUsing.mode=='rights') {
                                var rights = JSON.stringify($scope.rights);
                            } else {
                                $scope.buildSelectedClassifications();
                                if(!$scope.validClassificationChoice && !$scope.isNxl) {
                                    $scope.showError($filter('translate')('widget.protect.file.error.category.mandatory'));
                                    return;
                                }
                                var tags = $scope.classifications;
                            }
                            $uibModalInstance.dismiss('cancel');
                            $scope.isLoading = true;
                            if ($scope.disableUpload) {
                                $scope.isLoading = false;
                                return;
                            }
                            $scope.disableUpload = true;
                            var url = CONTEXT_PATH + '/rs/enterprisews/file';
                            var expiry = $scope.expiryJson;
                            if(expiry.relativeDay){
                                expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                            }
                            if ($scope.srcIsLocal) {
                                fileName = $scope.files.name;
                                var uploader = Upload.upload({
                                    url: url,
                                    headers: {
                                        'userId': userIdFromCookie,
                                        'ticket': ticketFromCookie,
                                        'clientId': clientId,
                                        'platformId': platformId
                                    },
                                    data: {
                                        file: $scope.files,
                                        "API-input": JSON.stringify({
                                            'parameters': {
                                                'name': $scope.files.name,
                                                'parentPathId': $scope.currentFolder.pathId,
                                                'rightsJSON': rights,
                                                'tags' : tags,
                                                'watermark' : $scope.watermarkStr,
                                                'expiry' : JSON.stringify(expiry), 
                                                'userConfirmedFileOverwrite' : $scope.userConfirmedFileOverwrite
                                            }
                                        })
                                    }
                                });
                            } else {
                                var map = {"repoId":$scope.srcRepo.repoId,"pathId":$scope.srcFile.pathId,"pathDisplay":$scope.srcFile.path};
                                fileName = $scope.srcFile.name;
                                var uploader = Upload.upload({
                                    url: url,
                                    headers: {
                                        'userId': userIdFromCookie,
                                        'ticket': ticketFromCookie,
                                        'clientId': clientId,
                                        'platformId': platformId
                                    },
                                    data: {
                                        "API-input": JSON.stringify({
                                            'parameters': {
                                                'name': $scope.srcFile.name,
                                                'parentPathId': $scope.currentFolder.pathId,
                                                'rightsJSON': rights,
                                                'tags' : tags,
                                                'source': map,
                                                'watermark' : $scope.watermarkStr,
                                                'expiry' : JSON.stringify(expiry), 
                                                'userConfirmedFileOverwrite': $scope.userConfirmedFileOverwrite
                                            }
                                        })
                                    }
                                });
                            }
                            uploadFileService.setCloseStatus(false);
                            uploadFileService.setMinimizeStatus(jscd.mobile && uploadFileService.getMinimizeStatus());
                            if(!$scope.isNxl){
                                $scope.uploadedFileName = fileName + new Date().getTime();
                            }
                            var appendedFileName = getShortName(fileName, 48);
                            var hoverFileName = fileName;
                            $scope.uploadedFilePath = $scope.currentFolder.filePath;
                            uploadFileService.setUploadingStatus(true);
                            uploadFileService.setUploadedStatus(false);
                            uploadFileService.getUploadFileList().push({
                                "displayFileName": appendedFileName,
                                "fileName": $scope.uploadedFileName,
                                "hoverFileName": hoverFileName,
                                "filePath": $scope.uploadedFilePath,
                                "repoId": $scope.tenantId,
                                "fileUploading": uploadFileService.getUploadingStatus(),
                                "fileUploaded": uploadFileService.getUploadedStatus(),
                                "percentUploaded": 0
                            });
                            var uploadFinish = function(index, error, response) {
                                var message;
                                var isSuccess = false;
                                dismissSnackbar();
                                if (error) {
                                    if (response.data.statusCode === 5001) {
                                        message = $filter('translate')('workspace.file.upload.validate.failed');
                                    } else if(response.data.statusCode === 5003){
                                        message = $filter('translate')('workspace.file.upload.tenant.mismatch');
                                    } else if(response.data.statusCode === 5006){
                                        message = $filter('translate')('workspace.file.upload.invalid.version');
                                    } else if(response.data.statusCode === 5007){
                                        message = $filter('translate')('workspace.file.upload.duid.mismatch');
                                    } else if(response.data.statusCode === 5008){
                                    	message = $filter('translate')('workspace.file.upload.invalid.metadata');
                                    } else if (response.data.statusCode === 404) {                                    
                                        message = $filter('translate')('workspace.folder.not.found');
                                    } else if (response.data.statusCode === 4001) {
                                        message = $filter('translate')('workspace.file.upload.exists');
                                    } else if (response.data.statusCode === 4002) {
                                        message = $filter('translate')('workspace.file.upload.overwrite.error');
                                    } else if (response.data.statusCode === 4003) {
                                        message = $filter('translate')('workspace.file.upload.failed.expiry');
                                    } else if(response.data.statusCode === 5009){
                                        message = $filter('translate')('workspace.file.duid.exists');
                                    } else {
                                        message = $filter('translate')('workspace.file.upload.failed') + fileName + $filter('translate')('ending');
                                    }
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(false);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": $filter('translate')('file.upload.fail') + appendedFileName + $filter('translate')('ending'),
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.tenantId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "uploadFailed": true,
                                        "error": message,
                                        "percentUploaded": 0
                                    };
                                } else {
                                    callback($scope.currentFolder);
                                    uploadFileService.removeRedundantMessage($scope.uploadedFileName);
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(true);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": appendedFileName,
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.tenantId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "percentUploaded": 100
                                    };
                                    isSuccess = true;
                                    message = $filter('translate')('workspace.file.upload.success1') + response.data.results.entry.name + $filter('translate')('workspace.file.upload.success2');
                                }
                                showSnackbar({
                                    isSuccess: isSuccess,
                                    messages: message
                                });
                            }

                            uploader.then(function(response) {
                                    var fileName = $scope.srcIsLocal ? $scope.files.name : $scope.srcFile.name;
                                    $scope.isLoading = false;
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName, $scope.uploadedFilePath, $scope.tenantId, uploadFileService.getUploadFileList());
                                    var error = response.data.statusCode !== 200;
                                    uploadFinish(index, error, response);
                                    if (error) {
                                        $scope.showError($filter('translate')('file.upload.fail') + fileName + $filter('translate')('ending'));
                                        $scope.success = false;
                                        $scope.disableUpload = false;
                                    } else {
                                        $scope.success = true;
                                        $scope.protectedFileName = response.data.results.entry.name;
                                        $rootScope.$emit("refreshSharedFileList");
                                        $rootScope.$emit("updateMyDriveUsage");
                                    }
                                }, errorHandler,
                                function(evt) {
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName, $scope.uploadedFilePath, $scope.tenantId, uploadFileService.getUploadFileList());
                                    if (evt.total == 0) {
                                        $scope.isLoading = false;
                                        $scope.showError($filter('translate')('protect.file.failure'));
                                        uploadFinish(index, $filter('translate')('upload.file.failure'), null);
                                    } else {
                                        var progressPercentage = parseInt(90.0 * evt.loaded / evt.total);
                                        var uploadEntry = uploadFileService.getUploadFileList()[index];
                                        uploadEntry.percentUploaded = progressPercentage;
                                    }
                                }
                            );              
                        }

                        $scope.protect = function() {
                            $scope.isLoading = true;
                            if ($scope.srcIsLocal) {
                                var expiry = $scope.expiryJson;
                                var startDate;
                                var endDate;
                                if(expiry.relativeDay){
                                    endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    expiry.endDate = endDate;
                                } else {
                                    startDate = expiry.startDate;
                                    endDate = expiry.endDate;
                                }
                                var data = {
                                    file: $scope.files,
                                    rightsJSON: JSON.stringify($scope.rights),
                                    expiry: JSON.stringify(expiry),
                                    userConfirmedFileOverwrite: $scope.userConfirmedFileOverwrite
                                };
                                if ($scope.showWatermark) {
                                    data.watermark = $scope.watermarkStr;
                                }
                                var uploader = Upload.upload({
                                    url: RMS_CONTEXT_NAME + "/RMSViewer/UploadAndProtect",
                                    data: data
                                });
                                uploader.then(function(response) {
                                        $scope.isLoading = false;
                                        var error = response.data.error;
                                        if (error) {
                                            $scope.showError(error);
                                        } else {
                                            $scope.success = true;
                                            $scope.protectedFileName = response.data.name;
                                            $rootScope.$emit("refreshSharedFileList");
                                            $rootScope.$emit("updateMyDriveUsage");
                                            digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                            $scope.protectionType = response.data.protectionType;
                                        }
                                    }, errorHandler,
                                    function(evt) {
                                        if (evt.total == 0) {
                                            $scope.isLoading = false;
                                            $scope.showError($filter('translate')('protect.file.failure'));
                                        }
                                    }
                                );
                            } else if ($scope.saveFileLocation == "inplace") {
                                var expiry = $scope.expiryJson;
                                var startDate;
                                var endDate;
    
                                if(expiry.relativeDay){
                                    endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    expiry.endDate = endDate;
                                } else {
                                    startDate = expiry.startDate;
                                    endDate = expiry.endDate;
                                }
                                var params = {
                                    "filePath": $scope.srcFile.path,
                                    "filePathId": $scope.srcFile.pathId,
                                    "repoId": $scope.srcRepo.repoId,
                                    "repoName": $scope.srcRepo.name,
                                    "fileName": $scope.srcFile.name,
                                    "filePathDisplay": $scope.srcFile.path,
                                    "expiry": JSON.stringify(expiry),
                                    "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite
                                };
                                if ($scope.showWatermark) {
                                    params.watermark = $scope.watermarkStr;
                                }

                                if($scope.protectUsing.mode=='rights') {
                                    params.rightsGranted = $scope.rights;
                                    $scope.protectionType = 0;
                                } else {
                                    $scope.buildSelectedClassifications();
                                    if(!$scope.validClassificationChoice && !$scope.isNxl) {
                                        $scope.showError($filter('translate')('widget.protect.file.error.category.mandatory'));
                                        $scope.isLoading = false;
                                        return;
                                    }
                                    params.tags = JSON.stringify($scope.classifications);
                                    $scope.protectionType = 1;
                                }

                                if($scope.srcRepo.providerClass === 'APPLICATION') {
                                    if($scope.protectUsing.mode === 'rights') {
                                        params = {
                                            "parameters" : {
                                                "path": $scope.srcFile.path,
                                                "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite,
                                                "protectionType": $scope.protectionType,
                                                "rights": $scope.rights,
                                                "expiry": expiry,
                                                "watermark": ""
                                            }
                                        };

                                        if ($scope.showWatermark) {
                                            params.parameters.watermark = $scope.watermarkStr;
                                        }        
                                    } else {
                                        params = {
                                            "parameters": {
                                                "path": $scope.srcFile.path,
                                                "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite,
                                                "protectionType": $scope.protectionType,
                                                "tags": $scope.classifications
                                            }
                                        };
                                    }
                                    sharedWorkspaceService.protectFileInPlace(params, $scope.srcRepo.repoId, function(data) {
                                        if(data.statusCode === 200) {
                                            var payload = {
                                                "parameters": {
                                                    "path": data.results.entry.path
                                                }
                                            };
                                    
                                            sharedWorkspaceService.getFileMetadata(payload, $scope.srcRepo.repoId, function(response) {
                                                if(response.statusCode === 200) {
                                                    $scope.protectedFileName = response.results.fileInfo.name;

                                                    $scope.tagsExist = false;
                                                    $scope.tags = {};
                                                    $scope.rights = response.results.fileInfo.rights;
                                                    if(response.results.fileInfo.protectionType === 1) {
                                                        $scope.tagsExist = true;
                                                        $scope.tags = response.results.fileInfo.tags;
                                                    }
                                                    if(!expiry.relativeDay) {
                                                        $scope.startDate = response.results.fileInfo.expiry.startDate;
                                                    }
                                                    $scope.endDate = response.results.fileInfo.expiry.endDate;
                                                    digitalRightsExpiryService.addExpiryInfo($scope.startDate, $scope.endDate);
                                                    $scope.success = true;        
                                                } else {
                                                    var error = response.message;
                                                    if(!error) {
                                                        error = $filter('translate')('view.file.info.error');
                                                    }
                                                    $scope.showError(error);
                                                }
                                                $rootScope.$emit("refreshApplicationRepoFileList");
                                                $scope.isLoading = false;
                                            });
                                        } else {
                                            $scope.showError(data.message);
                                            $rootScope.$emit("refreshApplicationRepoFileList");
                                            $scope.isLoading = false;
                                        }
                                    });
                                } else {
                                    networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ProtectAndUploadInPlace", params, null, function(data) {
                                        if (data.error && data.error.length > 0) {
                                            $scope.showError(data.error);
                                            $scope.isLoading = false;
                                        } else {
                                            digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                            $scope.protectedFileName = data.name;
                                            $rootScope.$emit("refreshPersonalRepoFileList");
    
                                            var fileParams = {
                                                "repoId": data.repoId,
                                                "pathId": data.filePath,
                                                "path": data.filePathDisplay
                                            }
    
                                            repositoryService.getRepositoryFileDetails(fileParams, function(detailsData) {
                                                if (detailsData.result == false) {
                                                    $scope.showError(detailsData.message);
                                                } else {
                                                    if(detailsData.tags && Object.keys(detailsData.tags).length) {
                                                        $scope.tagsExist = true;
                                                        $scope.tags = detailsData.tags;
                                                    } else {
                                                        $scope.tagsExist = false;
                                                        $scope.tags = {};
                                                    }
                                                    $scope.rights = detailsData.rights;
                                                    $scope.success = true;
                                                }
                                                $scope.isLoading = false;
                                            });
                                        }
                                    });
                                }
                            } else {
                                var expiry = $scope.expiryJson;
                                var startDate;
                                var endDate;
                                if(expiry.relativeDay){
                                    endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    expiry.endDate = endDate;
                                } else {
                                    startDate = expiry.startDate;
                                    endDate = expiry.endDate;
                                }
                                var params = {
                                    "rightsGranted": $scope.rights,
                                    "filePath": $scope.srcFile.path,
                                    "filePathId": $scope.srcFile.pathId,
                                    "repoId": $scope.srcRepo.repoId,
                                    "repoName": $scope.srcRepo.name,
                                    "fileName": $scope.srcFile.name,
                                    "filePathDisplay": $scope.srcFile.path,
                                    "expiry": JSON.stringify(expiry),
                                    "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite
                                };
                                if ($scope.showWatermark) {
                                    params.watermark = $scope.watermarkStr;
                                }
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ProtectAndUpload", params, null, function(data) {
                                    $scope.isLoading = false;
                                     if (data.error.length > 0) {
                                         $scope.showError(data.error);
                                     } else {
                                         digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                         $scope.success = true;
                                         $scope.protectedFileName = data.name;
                                         $rootScope.$emit("refreshSharedFileList");
                                         $rootScope.$emit("updateMyDriveUsage");
                                         $scope.protectionType = data.protectionType;
                                     }
                                });
                            }
                        }

                        $scope.share = function() {
                            $scope.isLoading = true;
                            $scope.recipients = $("#shareWithModal").val().replace(/ /g, '')
                            if ($scope.srcIsLocal) {
                                var data = {
                                    file: $scope.files,
                                    rightsJSON: JSON.stringify($scope.rights),
                                    shareWith: JSON.stringify($scope.currentIds),
                                    comment: $scope.comment.text, 
                                    userConfirmedFileOverwrite: $scope.userConfirmedFileOverwrite
                                };
                                if (!$scope.isNxl) {
                                    var expiry = $scope.expiryJson;
                                    if (expiry.relativeDay) {
                                        expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    }
                                    data.expiry = JSON.stringify(expiry);
                                    if ($scope.showWatermark) {
                                        data.watermark = $scope.watermarkStr;
                                    }
                                }
                                var uploader = Upload.upload({
                                    url: RMS_CONTEXT_NAME + "/RMSViewer/ProtectAndShare",
                                    data: data
                                });
                                uploader.then(function(response) {
                                        $scope.handleShareResponse(response.data);
                                    }, errorHandler,
                                    function(evt) {
                                        if (evt.total == 0) {
                                            $scope.isLoading = false;
                                            $scope.showError($filter('translate')('share.file.failure'));
                                        }
                                    }
                                );
                            } else {
                                var params = {
                                    "shareWith": $scope.recipients,
                                    "rightsGranted": $scope.rights,
                                    "filePath": $scope.srcFile.path,
                                    "filePathId": $scope.srcFile.pathId,
                                    "repoId": $scope.srcRepo.repoId,
                                    "repoName": $scope.srcRepo.name,
                                    "fileName": $scope.srcFile.name,
                                    "filePathDisplay": $scope.srcFile.path,
                                    "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite
                                };
                                if (!$scope.isNxl) {
                                    var expiry = $scope.expiryJson;
                                    if (expiry.relativeDay) {
                                        expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    }
                                    params.expiry = JSON.stringify(expiry);
                                    if ($scope.showWatermark) {
                                        params.watermark = $scope.watermarkStr;
                                    }
                                }
                                if ($scope.comment.text && $scope.comment.text.length > 0) {
                                    params.comment = $scope.comment.text
                                }
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ShareFile", params, null, function(data) {
                                    $scope.handleShareResponse(data);
                                });
                            }
                        }

                        $scope.handleShareResponse = function(response) {
                            $scope.isLoading = false;
                            if (!response.result) {
                                $scope.showError(response.messages[0]);
                                return;
                            } else {
                                $scope.comment = {};
                                $scope.fileLink = response.sharedLink;
                                $scope.duid = response.duid;
                                $scope.filePathId = response.filePathId;
                                $scope.protectedFileName = response.fileName;
                                $scope.protectionType = response.protectionType;
                                var startDate;
                                var endDate;
                                if(response.validity.relativeDay) {
                                    endDate = digitalRightsExpiryService.calculateRelativeEndDate(response.validity.relativeDay.year, response.validity.relativeDay.month, response.validity.relativeDay.week, response.validity.relativeDay.day);
                                } else {
                                    startDate = response.validity.startDate;
                                    endDate = response.validity.endDate;
                                }
                                digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                $scope.success = true;
                                $scope.formattedRecipients = $scope.recipients.split(',').join(', ');
                                if ($scope.isNxl) {
                                    $scope.message1 = response.messages[0];
                                    $scope.message2 = response.messages[1];
                                }
                                $rootScope.$emit("refreshSharedFileList");
                                $rootScope.$emit("updateMyDriveUsage");
                            }
                        }

                        $scope.goToMyVault = function() {
                            if ($state.current.name !== STATE_SHARED_FILES) {
                                $state.go(STATE_SHARED_FILES);
                                navService.setCurrentTab('shared_files');
                            } else {
                                $uibModalInstance.dismiss('cancel');
                            }
                        }

                        $scope.goToProtectedFileRepo = function () {
                            if(navService.getCurrentTab() != $scope.srcRepo.repoId ) {
                                navService.setIsInAllFilesPage(false);
                                navService.setCurrentTab($scope.srcRepo.repoId);
                                var params = {
                                    repoId: $scope.srcRepo.repoId,
                                    repoName: $scope.srcRepo.name,
                                    repoType: $scope.srcRepo.type
                                };
                                $state.go(STATE_REPOSITORIES, params);
                            } else {
                                $uibModalInstance.dismiss('cancel');
                            }
                        }

                        $scope.checkShareRights = function(file, successCallback) {
                            var params = $.param({
                                repoId: file.repoId,
                                filePath: file.pathId,
                                filePathDisplay: file.path,
                                fileName: file.name
                            });
                            $scope.isLoading = true;
                            var headers = {
                                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
                            };
                            networkService.post(RMS_CONTEXT_NAME + "/RMSViewer/CheckSharedRight", params, headers, function(data) {
                                $scope.isLoading = false;
                                if (data.statusCode == 200) {
                                    var rights = data.results.r;
                                    var owner = data.results.o;
                                    var nxl = data.results.nxl;
                                    var expiry = data.results.expiry;
                                    var protectionType = data.results.protectionType;
                                    if (owner || rights.indexOf('SHARE') >= 0) {
                                        var result = {};
                                        result["rights"] = rights;
                                        result["isOwner"] = owner;
                                        result["expiry"] = expiry;
                                        result["protectionType"] = protectionType;
                                        successCallback(result);
                                    } else {
                                        $scope.showError($filter('translate')('share.file.unauthorized.reshare'));
                                    }
                                } else {
                                    $scope.showError(data.message);
                                }
                            });
                        }
                    }
                ]
            });
        };

        var checkRights = function(file, successCallback, errorCallback, loadingCallback) {
            if (window.File && window.FileReader && window.FileList && window.Blob && FileReader.prototype.readAsArrayBuffer && FileReader.prototype.readAsBinaryString) {
                file = file.slice(0, 16384);
            }
            var url = RMS_CONTEXT_NAME + "/RMSViewer/GetNXLFileInfo";
            var rightsUploader = Upload.upload({
                url: url,
                data: {
                    file: file,
                }
            });
            rightsUploader.then(function(response) {
                loadingCallback(false);
                handleCheckShareRightsResponse(response.data, successCallback, errorCallback);
            }, errorHandler);
        }

        var handleCheckShareRightsResponse = function(data, successCallback, errorCallback) {
            var result = {};
            if (!data.success || !data.metadata) {
                errorCallback('managelocalfile.rights.error.label');
                return;
            } else {
                if (data.metadata.isTenantMembershipInvalid) {
                    errorCallback('share.file.tenant.mismatch');
                    return;
                }
                var rightsFromFile = [];
                if (!data.metadata.rights) {
                    data.metadata.rights = {};
                }
                for (var i = 0; i < data.metadata.rights.length; ++i) {
                    var right = data.metadata.rights[i];
                    if (rightsFromFile.indexOf(right) == -1) {
                        rightsFromFile.push(right);
                    }
                }
                result = data.metadata;
                result["rights"] = rightsFromFile;
                result["protectionType"] = data.metadata.protectionType;
            }
            if (data.metadata.isRevoked) {
                if (data.metadata.isOwner) {
                    errorCallback('managelocalfile.rights.revoke.label', result);
                } else {
                    errorCallback('managelocalfile.rights.unauthorized.label', result);
                }
            } else {
                if (!data.metadata.isOwner && !data.metadata.allowedToShare) {
                    errorCallback('managelocalfile.rights.unauthorized.label', result);
                } else {
                    successCallback(result);
                }
            }
        }

        return {
            protectFileModal: protectFileModal,
            checkRights: checkRights
        }
    }
]);
mainApp.factory('shareDialogService', ['$uibModal', '$rootScope', '$filter', 'networkService', 'repositoryService', 'initSettingsService', 'dialogService', 'shareFileService', 'projectService', '$controller', 'digitalRightsExpiryService', 'serviceProviderService', 'sharedWorkspaceService',
    function($uibModal, $rootScope, $filter, networkService, repositoryService, initSettingsService, dialogService, shareFileService, projectService, $controller, digitalRightsExpiryService, serviceProviderService, sharedWorkspaceService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var shareFile = function(parameters, callback) {
            var file = parameters.file;
            var operation = parameters.operation;
            var rights = parameters.rights;
            var tags = parameters.tags;
            var projectId = parameters.projectId;
            var isSharedFromProject = parameters.isSharedFromProject;
            var allProjectList = parameters.sharedToProjectsList;
            var tagsExist = tags && Object.keys(tags).length > 0 ? true : false;
            var owner = parameters.owner;
            var nxl = parameters.nxl;
            var closeRHCallback = parameters.rhcallback;
            var protectionType = parameters.protectionType;
            RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
            $uibModal.open({
                animation: true,
                templateUrl: parameters.fromViewer ? '/viewer/ui/app/Home/SharedFiles/partials/shareFileTemplate.html' : 'ui/app/Home/SharedFiles/partials/shareFileTemplate.html',
                windowClass: 'app-modal-window app-modal-window-mobile modal fade share-dialog',
                controller: ['$uibModalInstance', '$uibModalStack', '$scope', '$rootScope', '$state', '$filter', '$timeout', 'navService', 'userPreferenceService', 'RightsProtectionMethodService',
                    function($uibModalInstance, $uibModalStack, $scope, $rootScope, $state, $filter, $timeout, navService, userPreferenceService, RightsProtectionMethodService) {
                        $uibModalStack.dismissAll();
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
                                            $scope.expiryInfo = true;
                                        }
                                        return;
                                    }
                                }
                            });
                            $controller('classificationSelectionController',{$scope: $scope});
                        };
                        init();
                        $scope.showWatermark = false;
                        $scope.fromViewer = parameters.fromViewer;
                        $scope.startDate = parameters.startDate;
                        $scope.endDate = parameters.endDate;
                        $scope.editWatermark = false;
                        $scope.editExpiry = false;
                        $scope.showAdvanceOptions = false;
                        $scope.owner = owner;
                        $scope.shareSuccess = false;
                        $scope.nxl = nxl;
                        $scope.rights = rights;
                        $scope.operation = operation;
                        $scope.file = file;
                        $scope.doApply = true;
                        $scope.mode = "link";
                        $scope.steward = $scope.owner;
                        $scope.optional = false;
                        $scope.isMyVault = $state.current.name == STATE_SHARED_FILES;
                        $scope.tags = tags;
                        $scope.tagsExist = tagsExist;
                        $scope.sharedFromProjectId =  projectId;
                        $scope.isSharedFromProject = isSharedFromProject;
                        $scope.allProjectList = allProjectList;
                        $scope.isReshare = false; 
                        $scope.comment = {};
                        $scope.protectUsing = {};
                        $scope.protectUsing.mode = "rights";
                        $scope.protectionType = protectionType;
                        $scope.protectInPlaceAllowed = false;
                        $scope.saveFileLocation = "myvault";
                        $scope.saveFileLocationConfirmed = true;
                        // In place protection is not developed for BOX yet
                        var inPlaceProtectionEnabledRepos = [
                            'GOOGLE_DRIVE',
                            'ONE_DRIVE',
                            'SHAREPOINT_ONLINE',
                            'DROPBOX'
                        ];
                        $scope.currentRepoClass = "";
                        var repositoryClassMapping = {};

                        if($scope.nxl) {
                            digitalRightsExpiryService.addExpiryInfo($scope.startDate, $scope.endDate);
                        }

                        // block to get repo classes
                        serviceProviderService.getSupportedServiceProviders(function(data) {
                            if(data.statusCode === 200) {
                                repositoryClassMapping = data.results.supportedProvidersMap;
                                if(repositoryClassMapping[$scope.file.repoType]) {
                                    $scope.currentRepoClass = repositoryClassMapping[$scope.file.repoType].providerClass;
                                } else {
                                    $scope.currentRepoClass = 'PERSONAL';
                                }
                            } else {
                                showSnackbar({
                                    isSuccess: false,
                                    messages: data.message
                                });
                            }
                        });
                        // block ends

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
                            if($scope.isAdhocRightEnabled || $scope.saveFileLocation == "myvault") {
                                $scope.protectUsing.mode = "rights";
                            } else {
                                $scope.protectUsing.mode = "classification";
                            }
                        });

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

                        $scope.resetRights = function() {
                            $scope.rightsGranted = [];
                            $scope.rightsGranted['VIEW'] = true;
                        };

                        $scope.resetClassifications = function() {
                            $scope.getClassificationProfile();
                        };

                        // In-Place Protection block immediately below
                        if($scope.operation == "protect" && inPlaceProtectionEnabledRepos.indexOf($scope.file.repoType) > -1){
                            $scope.protectInPlaceAllowed = true;
                            $scope.saveFileLocation = "inplace";
                        }

                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                            if ($scope.shareSuccess) {
                                $rootScope.$emit("refreshSharedFileList");
                            }
                            if (closeRHCallback) {
                                closeRHCallback();
                            }
                        };

                        $(window).off('click');

                        $scope.validateEmail = function(id) {
                            validateEmail(id, $scope);
                        }

                        $scope.toggleSelection = function(rightId) {
                            $scope.rightsGranted[rightId] = !$scope.rightsGranted[rightId];
                        }

                        $scope.linkAvailable = false;

                        $scope.$watch('rightsGranted["WATERMARK"]',function(newValue, oldValue){
                            $scope.showWatermark = $scope.rightsGranted.hasOwnProperty('WATERMARK') ? $scope.rightsGranted["WATERMARK"] : false;
                        });

                        $scope.openEdit= function (option) {
                            $scope.showEditWatermarkModal = true;
                            $scope.relativeExpiryError = false;
                            $uibModal.open({
                                animation: true,
                                scope: $scope,
                                windowClass: 'padding-top-52 quick-fade-out',
                                templateUrl: parameters.fromViewer ? '/viewer/ui/app/Home/SharedFiles/partials/editWatermark.html' : 'ui/app/Home/SharedFiles/partials/editWatermark.html',
                                controller: ['$uibModalInstance', '$scope', '$rootScope', '$state', '$filter', '$timeout', 'navService', '$controller',
                                    function($uibModalInstance, $scope, $rootScope, $state, $filter, $timeout, navService, $controller) {
                                        if (option === 'watermark') {
                                            $scope.isEditWatermark = true;
                                            var watermarkScope = $controller('watermarkController',{$scope: $scope});
                                            if (parameters.fromViewer) {
                                                enableShiftCtrl();
                                            }
                                        } else {
                                            $scope.isEditWatermark = false;
                                            var digitalRightsExpiryScope = $controller('digitalRightsExpiryDateController', {$scope: $scope});
                                        }
                                        $scope.cancel = function () {
                                            $uibModalInstance.dismiss('cancel');
                                        }
                                        $scope.save = function () {
                                            if ($scope.isEditWatermark) {
                                                if ($scope.addWatermarkStr().trim().length == 0 || $scope.addWatermarkStr().length > 50) {
                                                    return;
                                                }
                                                $scope.$parent.watermarkStr = $scope.addWatermarkStr();
                                                $scope.showWatermarkResult($scope.$parent.watermarkStr);
                                            } else {
                                                $scope.$parent.expiryJson = $scope.addExpiry();
                                                $scope.$parent.expiryStr = digitalRightsExpiryService.getExpiryStr($scope.expiryJson);
                                                if ($scope.relativeExpiryZero()) {
                                                    $scope.relativeExpiryError = true;
                                                    return;
                                                }
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
                            }).result.then(function () {
                                if(parameters.fromViewer) {
                                    disableShiftCtrl();
                                }
                            }, function () {
                                if(parameters.fromViewer) {
                                    disableShiftCtrl();
                                }
                            });
                        };

                        function handleSuccess(response) {
                            $scope.isLoading = false;
                            $scope.comment = {};
                            $scope.messageStatus = response.result == true ? 2 : 1;
                            $scope.message = response.messages[0];
                            if(response.validity != undefined) {
                                $scope.startDate = response.validity.startDate;
                            }
                            if(response.validity != undefined) {
                                if(response.validity.relativeDay) {
                                    $scope.endDate = digitalRightsExpiryService.calculateRelativeEndDate(response.validity.relativeDay.year, response.validity.relativeDay.month, response.validity.relativeDay.week, response.validity.relativeDay.day);
                                } else {
                                    $scope.endDate = response.validity.endDate; 
                                }
                            }
                            if (nxl) {
                                $scope.message2 = response.messages[1];
                            } else {
                                $scope.messageLink = response.messages[1];
                            }

                            if($scope.isSharedFromProject){
                                var messageNewSharedForProjectSuccess = ""; 
                                var messageAlreadySharedProjectSuccess = ""; 
                                for (var i in response.newSharedProjects) {
                                   var sharedProjectId = response.newSharedProjects[i];
                                   for(var j in allProjectList){
                                        if(allProjectList[j].id == sharedProjectId ){
                                            if(messageNewSharedForProjectSuccess.length > 0 ){
                                                messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + ", "
                                            }
                                            messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + allProjectList[j].name ; 
                                            break; 
                                        }
                                    }
                                }
                                if(messageNewSharedForProjectSuccess.length > 0 ){
                                    $scope.message = "The protected file is successfully shared with: " + messageNewSharedForProjectSuccess;
                                }

                                for (var i in response.alreadySharedProjects) {
                                    var sharedProjectId = response.alreadySharedProjects[i];
                                    for(var j in allProjectList){
                                         if(allProjectList[j].id == sharedProjectId ){
                                             if(messageAlreadySharedProjectSuccess.length > 0 ){
                                                messageAlreadySharedProjectSuccess = messageAlreadySharedProjectSuccess + ", "
                                             }
                                             messageAlreadySharedProjectSuccess = messageAlreadySharedProjectSuccess + allProjectList[j].name ; 
                                             break; 
                                         }
                                     }
                                 }

                                 if(messageAlreadySharedProjectSuccess.length > 0 ){
                                    $scope.message2 = "The file had already been shared with : " + messageAlreadySharedProjectSuccess;
                                }

                                $rootScope.$emit("refreshProjectSharedFileList");
                            }
                            
                            $scope.shareSuccess = response.result;
                            if (!$scope.shareSuccess && response.statusCode === 404) {
                                $rootScope.$emit("refreshSharedFileList");
                                $rootScope.$emit("refreshPersonalRepoFileList");
                                $scope.forbidden = true;
                            }
                            digitalRightsExpiryService.addExpiryInfo($scope.startDate, $scope.endDate);
                            $scope.rights = $scope.rightsGrantedArr; // to show summary
                            $scope.rightsGrantedArr = [];
                            if (response.sharedLink) {
                                $scope.fileLink = response.sharedLink;
                                $scope.linkAvailable = true;
                            }
                            if (response.duid) {
                                $scope.duid = response.duid;
                            }
                            if (response.filePathId) {
                                $scope.filePathId = response.filePathId;
                            }
                            $scope.protectionType = response.protectionType;
                        };

                        $scope.rightsGranted = [];
                        $scope.optionalRightsGranted = [];
                        $scope.rightsGrantedArr = [];

                        $scope.toggleAdvanceOptions = function() {
                            $scope.showAdvanceOptions = !$scope.showAdvanceOptions;
                        }

                        $scope.checkFileExistsThenShareFile = function() {
                            $scope.userConfirmedFileOverwrite = false;
                            var pathId =  file.name;
                            if(pathId.endsWith(".nxl")){
                                $scope.submitShareFile();
                                return;
                            }
                            
                            $scope.isLoading = true;
                            repositoryService.checkIfMyVaultFilePathExists("/nxl_myvault_nxl/" + pathId, function(data){
                                $scope.isLoading = false;
                                if(data.statusCode == 200 && data.results.fileExists == true) {
                                    dialogService.confirm({
                                        msg:  $filter('translate')('myvault.file.upload.exists')
                                        	+ $filter('translate')('file.overwrite.alert.confirmation'),
                                        ok: function(data) {
                                            $scope.userConfirmedFileOverwrite = true;
                                            $scope.submitShareFile();
                                        },
                                        cancel: function() {},
                                        fromViewer: $scope.fromViewer
                                    });
                                } else {
                                    $scope.submitShareFile();
                                }
                            });
                        }

                        $scope.checkIfInPlaceFileExistsThenProtect = function() {
                            $scope.userConfirmedFileOverwrite = false;
                            $scope.isLoading = true;
                            $scope.messageStatus = 0;
                            $scope.message = "";

                            if($scope.currentRepoClass === 'APPLICATION') {
                                var payload = {
                                    "parameters": {
                                        "path": file.path + ".nxl"
                                    }
                                };
                                sharedWorkspaceService.checkIfFileExists(payload, file.repoId, function(data) {
                                    if(data.statusCode === 200) {
                                        if(data.results.fileExists) {
                                            $scope.isLoading = false;
                                            dialogService.confirm({
                                                msg:  $filter('translate')('inplace.file.upload.exists') + file.repoName + "."
                                                    + $filter('translate')('file.replace.alert.confirm'),
                                                ok: function(data) {
                                                    $scope.userConfirmedFileOverwrite = true;
                                                    $scope.submitShareFile();
                                                },
                                                cancel: function() {},
                                                fromViewer: $scope.fromViewer
                                            });    
                                        } else {
                                            $scope.submitShareFile();
                                        }
                                    } else {
                                        $scope.messageStatus = 1;
                                        $scope.message = data.message;
                                        $scope.isLoading = false;
                                    }
                                });
                            } else {
                                var searchDetails = $.param({
                                    repoId: file.repoId,
                                    searchString: file.name + ".nxl"
                                });
    
                                repositoryService.getSearchResults(searchDetails, function(data) {
                                    var fileExists = false;
    
                                    if(data.result === true && data.content.length > 0) {
                                        var searchResults = data.content;
                                        for(var i = 0; i < searchResults.length; i++) {
                                            if (searchResults[i].path == file.path + ".nxl") {
                                                fileExists = true;
                                                break;
                                            }
                                        }
                                    }

                                    if(fileExists) {
                                        $scope.isLoading = false;
                                        dialogService.confirm({
                                            msg:  $filter('translate')('inplace.file.upload.exists') + file.repoName + "."
                                                + $filter('translate')('file.replace.alert.confirm'),
                                            ok: function(data) {
                                                $scope.userConfirmedFileOverwrite = true;
                                                $scope.submitShareFile();
                                            },
                                            cancel: function() {},
                                            fromViewer: $scope.fromViewer
                                        });
                                    } else {
                                        $scope.submitShareFile();
                                    }
                                });
                            }
                        }

                        $scope.submitShareFile = function() {
                            var expiry;
                            $scope.isLoading = true;

                            var shareWith;
                            if ($("#shareWith").val()) {
                                shareWith = $("#shareWith").val().replace(/ /g, '')
                            }
                            var params = {
                                "shareWith": shareWith,
                                "filePath": file.path,
                                "filePathId": file.pathId,
                                "repoId": file.repoId,
                                "repoName": file.repoName,
                                "fileName": file.name,
                                "filePathDisplay": file.path,
                                "userConfirmedFileOverwrite" : $scope.userConfirmedFileOverwrite
                            };

                            if($scope.protectUsing.mode =='rights') {
                                if (!$scope.nxl) {
                                    // Add granted rights into an array
                                    $scope.rightsGranted['VIEW'] = true;
                                    $scope.rightsGrantedArr = [];
                                    for (var key in $scope.rightsGranted) {
                                        if ($scope.rightsGranted[key] === true) {
                                            $scope.rightsGrantedArr.push(key);
                                        }
                                    }
                                } else {
                                    $scope.rightsGrantedArr = $scope.rights;
                                }
                                params.rightsGranted = $scope.rightsGrantedArr;
                                $scope.protectionType = 0;
                            } else {
                                $scope.buildSelectedClassifications();
                                if(!$scope.validClassificationChoice && !$scope.nxl) {
                                    $scope.messageStatus = 1;
                                    $scope.message = $filter('translate')('widget.protect.file.error.category.mandatory');
                                    $scope.isLoading = false;
                                    return;
                                }
                                params.tags = JSON.stringify($scope.classifications);
                                $scope.protectionType = 1;
                            }

                            if (!$scope.nxl) {
                                expiry = $scope.expiryJson;
                                if (expiry.relativeDay) {
                                    expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                }
                                params.expiry = JSON.stringify(expiry);
                                if ($scope.showWatermark) {
                                    params.watermark = $scope.watermarkStr;
                                }
                            }
                            if ($scope.comment.text && $scope.comment.text.length > 0) {
                                params.comment = $scope.comment.text;
                            }
                            if (operation == "share") {
                                params.isFileAttached = $scope.mode === "attachment";
                                
                                 if($scope.isSharedFromProject){
                                    params.fromSpace = "PROJECTSPACE";
                                    params.fromSpaceId = $scope.sharedFromProjectId;
                                    var shareWithProjectIds = [];
                                    for (var i = 0; i < $scope.allProjectList.length; i++) {
                                        if ($scope.allProjectList[i].selected === true) {
                                            shareWithProjectIds.push($scope.allProjectList[i].id);
                                        }
                                    }
                                    params.shareWithProject = shareWithProjectIds
                                 }
                               
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ShareFile", params, null, function(data) {
                                    handleSuccess(data);
                                    if ($scope.attachment != 1 && callback && typeof(callback) == "function") {
                                        callback(data);
                                    }
                                });
                            } else if (operation == "protect" && $scope.saveFileLocation == "inplace") {
                                if($scope.currentRepoClass === 'APPLICATION') {
                                    if($scope.protectUsing.mode === 'rights') {
                                        params = {
                                            "parameters" : {
                                                "path": file.path,
                                                "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite,
                                                "protectionType": $scope.protectionType,
                                                "rights": $scope.rightsGrantedArr,
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
                                                "path": file.path,
                                                "userConfirmedFileOverwrite": $scope.userConfirmedFileOverwrite,
                                                "protectionType": $scope.protectionType,
                                                "tags": $scope.classifications
                                            }
                                        };
                                    }
                                    sharedWorkspaceService.protectFileInPlace(params, file.repoId, function(data) {
                                        if(data.statusCode === 200) {
                                            var payload = {
                                                "parameters": {
                                                    "path": data.results.entry.path
                                                }
                                            };

                                            sharedWorkspaceService.getFileMetadata(payload, file.repoId, function(response) {
                                                if(response.statusCode === 200) {
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

                                                    $scope.messageStatus = 2;
                                                    $scope.message = $filter('translate')('rights.protected.file.uploaded1') + response.results.fileInfo.name + $filter('translate')('rights.protected.file.uploaded.inplace') + file.repoName + '.';
                                                    $scope.shareSuccess = true;
                                                } else {
                                                    var error = response.message;
                                                    if(!error) {
                                                        error = $filter('translate')('view.file.info.error');
                                                    }
                                                    $scope.messageStatus = 1;
                                                    $scope.message = error;
                                                }
                                                $rootScope.$emit("refreshApplicationRepoFileList");
                                                $scope.isLoading = false;
                                            });
                                        } else {
                                            $scope.messageStatus = 1;
                                            $scope.message = data.message;
                                            if (data.statusCode === 404) {
                                                $scope.forbidden = true;
                                            }
                                            $rootScope.$emit("refreshApplicationRepoFileList");
                                            $scope.isLoading = false;
                                        }
                                    });
                                } else {
                                    networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ProtectAndUploadInPlace", params, null, function(data) {
                                        if (data.error && data.error.length > 0) {
                                            $scope.messageStatus = 1;
                                            $scope.message = data.error;
                                            if (data.statusCode === 404) {
                                                $rootScope.$emit("refreshSharedFileList");
                                                $rootScope.$emit("refreshPersonalRepoFileList");
                                                $scope.forbidden = true;
                                            }
                                            $scope.isLoading = false;
                                        } else {
                                            var fileParams = {
                                                "repoId": data.repoId,
                                                "pathId": data.filePath,
                                                "path": data.filePathDisplay
                                            };
    
                                            repositoryService.getRepositoryFileDetails(fileParams, function(detailsData) {
                                                if (detailsData.result == false) {
                                                    $scope.messageStatus = 1;
                                                    $scope.message = detailsData.message;
                                                    if (detailsData.statusCode === 404) {
                                                        $rootScope.$emit("refreshSharedFileList");
                                                        $rootScope.$emit("refreshPersonalRepoFileList");
                                                        $scope.forbidden = true;
                                                    }
                                                } else {
                                                    digitalRightsExpiryService.addExpiryInfo(detailsData.validity.startDate, detailsData.validity.endDate);
                                                    if(detailsData.tags && Object.keys(detailsData.tags).length) {
                                                        $scope.tagsExist = true;
                                                        $scope.tags = detailsData.tags;
                                                    } else {
                                                        $scope.tagsExist = false;
                                                        $scope.tags = {};
                                                    }
                                                    $scope.rights = detailsData.rights;
        
                                                    $scope.messageStatus = 2;
                                                    $scope.message = $filter('translate')('rights.protected.file.uploaded1') + data.name + $filter('translate')('rights.protected.file.uploaded.inplace') + file.repoName + '.';
                                                    $scope.shareSuccess = true;
                                                }
                                                $rootScope.$emit("refreshPersonalRepoFileList");
                                                $scope.isLoading = false;
                                            });
                                        }
                                        if (callback && typeof(callback) == "function") {
                                            callback(data);
                                        }
                                    });
                                }
                            } else if (operation == "protect") {
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ProtectAndUpload", params, null, function(data) {
                                    $scope.isLoading = false;
                                     if (data.error.length > 0) {
                                        $scope.messageStatus = 1;
                                        $scope.message = data.error;
                                        if (data.statusCode === 404) {
                                            $rootScope.$emit("refreshSharedFileList");
                                            $rootScope.$emit("refreshPersonalRepoFileList");
                                            $scope.forbidden = true;
                                        }
                                    } else {
                                        if(expiry.relativeDay) {
                                            $scope.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                        } else {
                                            $scope.startDate = expiry.startDate;
                                            $scope.endDate = expiry.endDate;
                                        }
                                        digitalRightsExpiryService.addExpiryInfo($scope.startDate, $scope.endDate);
                                        $scope.shareSuccess = true;
                                        $scope.rights = $scope.rightsGrantedArr;
                                        $scope.messageStatus = 2;
                                        $scope.message = $filter('translate')('rights.protected.file.uploaded1') + data.name + $filter('translate')('rights.protected.file.uploaded2');
                                        $scope.protectionType = data.protectionType;
                                    }
                                    if (callback && typeof(callback) == "function") {
                                        callback(data);
                                    }
                                });
                            } else if (operation == "shareLocal") {
                                params.isFileAttached = $scope.mode === "attachment";
								params.efsId = file.efsId;
								params.originalName = file.name;
								params.rightsJSON = JSON.stringify($scope.rightsGrantedArr);
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/ShareFile", params, parameters.header, function(data) {
                                    handleSuccess(data);
                                    if ($scope.attachment != 1 && callback && typeof(callback) == "function") {
                                        callback(data);
                                    }
                                });
                            } else if (operation == "protectLocal") {
                                params.efsId = file.efsId;
                                params.originalName = file.name;
                                params.rightsJSON = JSON.stringify($scope.rightsGrantedArr);
                                networkService.postAsFormData(RMS_CONTEXT_NAME + "/RMSViewer/UploadAndProtect", params, parameters.header, function(data) {
                                    $scope.isLoading = false;
                                    if (data.error.length > 0) {
                                        $scope.messageStatus = 1;
                                        $scope.message = data.error;
                                        if (data.statusCode === 404) {
                                            $rootScope.$emit("refreshSharedFileList");
                                            $rootScope.$emit("refreshPersonalRepoFileList");
                                            $scope.forbidden = true;
                                        }
                                    } else {
                                        $scope.shareSuccess = true;
                                        $scope.rights = $scope.rightsGrantedArr;
                                        $scope.messageStatus = 2;
                                        $scope.message = $filter('translate')('rights.protected.file.uploaded1') + data.name + $filter('translate')('rights.protected.file.uploaded2');
                                        $scope.protectionType = data.protectionType;
                                        var expiry = JSON.parse(params.expiry);
                                        if (expiry) {
                                            $scope.startDate = expiry.startDate;
                                            if (expiry.relativeDay) {
                                                $scope.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                            } else {
                                                $scope.endDate = expiry.endDate;
                                            }
                                        }
                                        digitalRightsExpiryService.addExpiryInfo($scope.startDate, $scope.endDate);
                                    }
                                    if (callback && typeof(callback) == "function") {
                                        callback(data);
                                    }
                                });
                            }

                        };

                        $scope.contentRightsAvailable = dialogService.getContentRights();
                        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
                        $scope.effectRightsAvailable = dialogService.getEffectRights();

                        $scope.dt = null;

                        $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
                        $scope.format = $scope.formats[0];
                        $scope.altInputFormats = ['M!/d!/yyyy'];

                        $scope.popup1 = {
                            opened: false
                        };

                        $scope.popup1.opened = false;
                        $scope.open1 = function() {
                            $scope.popup1.opened = true;
                        };

                        $scope.dateOptions = {

                        };

                        $scope.mailPristine = true;
                        $scope.checkMailPristine = function() {
                            return $scope.mailPristine;
                        }

                        $scope.checkNoProjectSelected = function() {
                            var noProjectSelected = true;
                            for(var j in allProjectList){
                                if(allProjectList[j].selected === true){
                                    noProjectSelected = false; 
                                    break;
                                }
                            }
                            return noProjectSelected;
                        }

                        $scope.selectText = function() {
                            $("#fileLinkId").focus();
                            $("#fileLinkId").select();
                        }

                        $scope.copyLinkClicked = false;
                        $scope.clickAfterCopy = function() {
                            if ($scope.timeoutId) {
                                clearTimeout($scope.timeoutId);
                            }
                            $scope.copyLinkClicked = true;
                            $scope.timeoutId = setTimeout(function() {
                                $timeout(function() {
                                    clearTimeout($scope.timeoutId);
                                    $scope.timeoutId = false;
                                    $scope.copyLinkClicked = false;
                                });
                            }, 2000);
                        };

                        $scope.manageShareFile = function() {
                            $uibModalInstance.dismiss('cancel');
                            shareFileService.getSharedFileDetails({
                                duid: $scope.duid,
                                pathId: $scope.filePathId
                            }, function(data) {
                                if (data.statusCode == 200) {
                                    $scope.isLoading = false;
                                    viewSharedFileDetails({
                                        duid: $scope.duid,
                                        file: data.results.detail
                                    });
                                } else {
                                    dialogService.displayError({
                                        msg: $filter('translate')('manage.file.error')
                                    });
                                }
                            });
                        }

                        $scope.onClickMyVault = function() {
                            navService.setCurrentTab('shared_files');
                            navService.setIsInAllFilesPage(false);
                            $state.go(STATE_SHARED_FILES);
                            $uibModalInstance.dismiss('cancel');
                        }

                        $scope.onClickRepo = function() {
                            if(navService.getCurrentTab() != $scope.file.repoId) {
                                navService.setCurrentTab($scope.file.repoId);
                                navService.setIsInAllFilesPage(false);
                                var params = {
                                    repoId: $scope.file.repoId,
                                    repoName: $scope.file.repoName,
                                    repoType: $scope.file.repoType
                                };
                                $state.go(STATE_REPOSITORIES, params);
                            }
                            $uibModalInstance.dismiss('cancel');
                        }
                    }
                ]
            });
        };

        var reshareFile = function(parameters, callback) {
            var transactionId = parameters.transactionId;
            var transactionCode = parameters.transactionCode;
            var sharedOn = parameters.sharedOn;
            var rights = parameters.rights;
            var file = parameters.file;
			var closeRHCallback = parameters.rhcallback;
            var startDate = parameters.startDate;
            var endDate = parameters.endDate;
            var protectionType = parameters.protectionType;
            var projectId = parameters.projectId;
            var isSharedFromProject = parameters.isSharedFromProject;
            var allProjectList = parameters.sharedToProjectsList;
            var tags = parameters.tags;
            var tagsExist = tags && Object.keys(tags).length > 0 ? true : false;
            $uibModal.open({
                animation: true,
                templateUrl:  parameters.fromViewer ? '/viewer/ui/app/Home/SharedFiles/partials/reshareFileTemplate.html' : 'ui/app/Home/SharedFiles/partials/reshareFileTemplate.html',
                windowClass: 'app-modal-window app-modal-window-mobile modal fade',
                controller: ['$uibModalInstance', '$uibModalStack', '$scope', '$rootScope', '$state', '$filter', '$timeout', 'navService',
                    function($uibModalInstance, $uibModalStack, $scope, $rootScope, $state, $filter, $timeout, navService) {
                        $uibModalStack.dismissAll();
                        $scope.shareSuccess = false;
                        $scope.nxl = true;
                        $scope.rights = rights;
                        $scope.sharedOn = sharedOn;
                        $scope.doApply = parameters.doApply === false ? false : true;
                        $scope.operation = "share";
                        $scope.filename = file.name;
                        $scope.file = file;
                        $scope.isLoading = false;
                        $scope.comment = {};
                        $scope.expiryInfo = true;
                        digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                        $scope.protectionType = protectionType;
                        $scope.sharedFromProjectId =  projectId;
                        $scope.isSharedFromProject = isSharedFromProject;
                        $scope.allProjectList = allProjectList;
                        $scope.isReshare = true; 
                        $scope.tags = tags;
                        $scope.tagsExist = tagsExist;
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
							if (closeRHCallback) {
                                closeRHCallback();
                            }
                        };

                        $scope.validateEmail = function(id) {
                            validateEmail(id, $scope);
                        }

                        function displayMessage(msg, isSuccess) {
                            $scope.isLoading = false;
                            $scope.messageStatus = isSuccess ? 2 : 1;
                            $scope.message = msg;
                        };

                        function handleSuccess(response) {
                            $scope.isLoading = false;
                            if (response.statusCode == 200) {
                                $scope.comment = {};
                                var added = response.results.newSharedList;
                                var message = "";
                                if (added && added.length > 0) {
                                    message = $filter('translate')('share.success_email') + added.join(', ') + ". ";
                                }
                                var alreadyList = response.results.alreadySharedList;
                                if (alreadyList && alreadyList.length > 0) {
                                    $scope.message2 = $filter('translate')('share.already_sent_email', {
                                        receipients: alreadyList.join(', ')
                                    });
                                }

                                if($scope.isSharedFromProject){
                                    var messageNewSharedForProjectSuccess = ""; 
                                    var messageAlreadySharedProjectSuccess = ""; 
                                    for (var i in added) {
                                       var sharedProjectId = added[i];
                                       for(var j in allProjectList){
                                            if(allProjectList[j].id == sharedProjectId ){
                                                if(messageNewSharedForProjectSuccess.length > 0 ){
                                                    messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + ", "
                                                }
                                                messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + allProjectList[j].name ; 
                                                break; 
                                            }
                                        }
                                    }
                                    if(messageNewSharedForProjectSuccess.length > 0 ){
                                        message =  "The protected file is successfully shared with: " + messageNewSharedForProjectSuccess;
                                    }
    
                                    for (var i in alreadyList) {
                                        var sharedProjectId = alreadyList[i];
                                        for(var j in allProjectList){
                                             if(allProjectList[j].id == sharedProjectId ){
                                                 if(messageAlreadySharedProjectSuccess.length > 0 ){
                                                    messageAlreadySharedProjectSuccess = messageAlreadySharedProjectSuccess + ", "
                                                 }
                                                 messageAlreadySharedProjectSuccess = messageAlreadySharedProjectSuccess + allProjectList[j].name ; 
                                                 break; 
                                             }
                                         }
                                     }
    
                                     if(messageAlreadySharedProjectSuccess.length > 0 ){
                                        $scope.message2 = "The file had already been shared with : " + messageAlreadySharedProjectSuccess;
                                    }
    
                                }

                                $scope.shareSuccess = true;
                                $scope.protectionType = response.results.protectionType;
                                displayMessage(message, true);
                                digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                            } else if (response.statusCode == 403) {
                                var message = $filter('translate')('share.file.unauthorized.reshare');
                                $scope.shareSuccess = false;
                                displayMessage(message, false);
                            } else if (response.statusCode == 4001) {
                                var message = $filter('translate')('share.file.revoked');
                                $scope.shareSuccess = false;
                                displayMessage(message, false);
                            } else {
                                var message = $filter('translate')('share.error');
                                $scope.shareSuccess = false;
                                displayMessage(message, false);
                            }
                        };

                        $scope.rightsGranted = [];
                        $scope.optionalRightsGranted = [];

                        $scope.submitShareFile = function() {
                            $scope.isLoading = true;
                            var shareWith;
                            if ($("#shareWith").val()) {
                                shareWith = $("#shareWith").val().replace(/ /g, '')
                            }
                            var params = {
                                "parameters": {
                                    "transactionId": transactionId,
                                    "transactionCode": transactionCode,
                                    "shareWith": shareWith
                                }
                            };

                            if($scope.isSharedFromProject){
                               
                                params.parameters.spaceId = $scope.sharedFromProjectId;
                                var shareWithProjectIds = [];
                                for (var i = 0; i < $scope.allProjectList.length; i++) {
                                    if ($scope.allProjectList[i].selected === true) {
                                        var projectRecipient  = { 
                                            "projectId" : $scope.allProjectList[i].id
                                        }
                                        shareWithProjectIds.push(projectRecipient);
                                    }
                                }
                                params.parameters.recipients = shareWithProjectIds
                             }

                            if ($scope.comment.text && $scope.comment.text.length > 0) {
                                params.parameters.comment = $scope.comment.text;
                            }
                            shareFileService.reshareFile(params, function(data) {
                                handleSuccess(data);
                            })
                        };

                        $scope.contentRightsAvailable = dialogService.getContentRights();
                        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
                        $scope.effectRightsAvailable = dialogService.getEffectRights();

                        $scope.mailPristine = true;
                        $scope.checkMailPristine = function() {
                            return $scope.mailPristine;
                        }

                        $scope.checkNoProjectSelected = function() {
                            var noProjectSelected = true;
                            for(var j in allProjectList){
                                if(allProjectList[j].selected === true){
                                    noProjectSelected = false; 
                                    break;
                                }
                            }
                            return noProjectSelected;
                        }
                    }
                ]
            });
        };

        var viewSharedFileDetails = function(parameters, callback) {
            var file = parameters.file;
            var duid = parameters.duid;
            var pathId = parameters.pathId;
            var revoked = file.revoked;
            var deleted = file.deleted;
            var shared = file.shared;
            var displayName = file.name;
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/SharedFiles/partials/sharedFileDetailsTemplate.html',
                windowClass: 'app-modal-window app-modal-window-mobile modal fade',
                controller: ['$uibModalInstance', '$scope', '$filter', '$timeout', 'digitalRightsExpiryService',
                    function($uibModalInstance, $scope, $filter, $timeout, digitalRightsExpiryService) {
                        $scope.file = parameters.file;
                        $scope.firstLoad = true;
                        $scope.duid = duid;
                        $scope.pathId = pathId;
                        $scope.expiryInfo = true;
                        
                        if (!parameters.isSharedFromProject) {
                            $scope.origRecipients = parameters.file.recipients;
                            $scope.isAttachment = file.isAttachment;
                            $scope.fileLink = file.fileLink;
                            $scope.sharedWithArr = file.recipients;                            
                            var startDate = file.validity.startDate;
                            var endDate = file.validity.endDate;
                        }
                        if (parameters.isSharedFromProject) {
                            var arrayOfProjectIds = parameters.shareWithProject;
                            $scope.origProjectRecipients = arrayOfProjectIds;
                            $scope.sharedWithArr = [];
                            $scope.rights = parameters.file.rights;
                            $scope.tags = parameters.file.tags;
                            $scope.projectId = parameters.projectId;
                            $scope.isSharedFromProject = parameters.isSharedFromProject;
                            $scope.allProjectList = parameters.sharedToProjectsList;
                            $scope.pathId = parameters.file.pathId.lastIndexOf('/') == 0 ? "/" : parameters.file.pathId.substring(0, parameters.file.pathId.lastIndexOf('/') + 1);
                            $scope.fileName = parameters.file.name;
                            $scope.tagsExist = $scope.tags && Object.keys($scope.tags).length > 0 ? true : false;
                            for (var i in $scope.origProjectRecipients) {
                                var sharedProjectId = $scope.origProjectRecipients[i];
                                for (var j in $scope.allProjectList) {
                                    if ($scope.allProjectList[j].id == sharedProjectId) {
                                        $scope.sharedWithArr.push($scope.allProjectList[j].name);
                                        $scope.allProjectList[j].selected = true;
                                        break;
                                    }
                                }
                            }
                            var startDate = file.expiry.startDate;
                            var endDate = file.expiry.endDate;
                        }

                        digitalRightsExpiryService.addExpiryInfo(startDate, endDate);

                        $scope.sharedWithProjectUpdated = function (project) {
                            var currentProjectIds = [];
                            for (var j in $scope.allProjectList) {
                                if ($scope.allProjectList[j].selected === true) {
                                    currentProjectIds.push($scope.allProjectList[j].id);
                                }
                            }
                            if (currentProjectIds !== undefined) {
                                currentProjectIds.sort();
                            }
                            var userProjectRecipients = [];
                            // this block is to only return those project ids that this user is a member of
                            $scope.origProjectRecipients.forEach(function(projectId) {
                                for(var i = 0; i < $scope.allProjectList.length; i++) {
                                    if($scope.allProjectList[i].id == projectId) {
                                        userProjectRecipients.push(projectId);
                                        break;
                                    }
                                }
                            });
                            return !angular.equals(currentProjectIds, userProjectRecipients.sort());
                        }

                        $scope.updateProjectRecipientList = function () {
                            $scope.isLoading = true;
                            $scope.message = "";
                            $scope.message2 = "";
                            $scope.messageStatus = 0;

                            var currentProjectIds = [];
                            var allMemberProjectIds = [];
                            for (var j in $scope.allProjectList) {
                                if ($scope.allProjectList[j].selected === true) {
                                    currentProjectIds.push($scope.allProjectList[j].id);
                                }
                                allMemberProjectIds.push($scope.allProjectList[j].id);
                            }
                            if (currentProjectIds !== undefined) {
                                currentProjectIds.sort();
                            }
                            if ($scope.origProjectRecipients !== undefined) {
                                $scope.origProjectRecipients.sort();
                            }
                            var projectsWithoutAccess = $($scope.origProjectRecipients).not(allMemberProjectIds).get();
                            $scope.addedRecipients = $(currentProjectIds).not($scope.origProjectRecipients).get();
                            $scope.removedRecipients = $($scope.origProjectRecipients).not(currentProjectIds).not(projectsWithoutAccess).get();

                            if ($scope.addedRecipients.length > 0 || $scope.removedRecipients.length > 0) {
                                var addedRecipients = [];
                                $scope.addedRecipients.forEach(function (recipient) {
                                    addedRecipients.push({
                                        projectId: recipient
                                    });
                                });
                                var removedRecipients = [];
                                $scope.removedRecipients.forEach(function (recipient) {
                                    removedRecipients.push({
                                        projectId: recipient
                                    });
                                });

                                var params = {
                                    duid: $scope.duid,
                                    addedRecipients: addedRecipients,
                                    removedRecipients: removedRecipients
                                };

                                shareFileService.updateSharedFile(params, function (data) {
                                    if (data.statusCode == 200) {
                                        $scope.comment = {};
                                        var message = "";
                                        var messageNewSharedForProjectSuccess = "";
                                        var messageRevokedProjectSuccess = "";
                                        for (var i in data.results.newRecipients) {
                                            var sharedProjectId = data.results.newRecipients[i];
                                            for (var j in $scope.allProjectList) {
                                                if ($scope.allProjectList[j].id == sharedProjectId) {
                                                    if (messageNewSharedForProjectSuccess.length > 0) {
                                                        messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + ", "
                                                    }
                                                    messageNewSharedForProjectSuccess = messageNewSharedForProjectSuccess + $scope.allProjectList[j].name;
                                                    break;
                                                }
                                            }
                                        }

                                        for (var i in data.results.removedRecipients) {
                                            var removedProjectId = data.results.removedRecipients[i];
                                            for (var j in $scope.allProjectList) {
                                                if ($scope.allProjectList[j].id == removedProjectId) {
                                                    if (messageRevokedProjectSuccess.length > 0) {
                                                        messageRevokedProjectSuccess = messageRevokedProjectSuccess + ", "
                                                    }
                                                    messageRevokedProjectSuccess = messageRevokedProjectSuccess + $scope.allProjectList[j].name;
                                                    break;
                                                }
                                            }
                                        }

                                        if (messageNewSharedForProjectSuccess.length > 0) {
                                            message += "The protected file is successfully shared with " + messageNewSharedForProjectSuccess + ". ";
                                        }

                                        if (messageRevokedProjectSuccess.length > 0) {
                                            message += $filter('translate')('unshare.success.with') + messageRevokedProjectSuccess + ". ";;
                                        }

                                        displayMessage(message, true);
                                        $rootScope.$emit("refreshProjectSharedFileList");
                                    } else if (data.statusCode == 4001) {
                                        displayMessage($filter('translate')('managelocalfile.rights.revoke.label'), false);
                                        $scope.revoked = true;
                                    } else if (data.statusCode == 4003) {
                                        displayMessage($filter('translate')('share.error.expired'), false);
                                    } else if (data.statusCode == 403) {
                                        displayMessage($filter('translate')('share.file.unauthorized.reshare'), false);
                                    } else {
                                        displayMessage($filter('translate')('share.error'), false);
                                    }
                                    $scope.updateOrigProjectRecipients();
                                });
                            }
                        }

                        $scope.updateOrigProjectRecipients = function () {
                            $scope.isLoading = true;
                            var queryParams = {
                                pathId: $scope.pathId,
                                searchString: $scope.fileName
                            }
                            projectService.getProjectFiles($scope.projectId, queryParams, function (data) {
                                $scope.isLoading = false;
                                if (data.statusCode == 200) {
                                    $scope.origProjectRecipients = data.results.detail.files[0].shareWithProject;
                                }
                                if ($scope.origProjectRecipients !== undefined) {
                                    $scope.origProjectRecipients.sort();
                                }
                            });
                        }

                        $scope.displayName = displayName;
                        $scope.revoked = revoked;
                        $scope.deleted = deleted;
                        $scope.shared = shared;
                        
                        $scope.doApply = parameters.doApply === false ? false : true;
                        $scope.optional = true;
                        $scope.comment = {};
                        if ($scope.fileLink != null && $scope.fileLink != undefined) {
                            $scope.linkAvailable = true;
                        }
                        angular.element(document).ready(function() {
                            $scope.firstLoad = false;
                        });
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.selectText = function() {
                            $("#fileLinkId").focus();
                            $("#fileLinkId").select();
                        }

                        $scope.copyLinkClicked = false;
                        $scope.clickAfterCopy = function() {
                            if ($scope.timeoutId) {
                                clearTimeout($scope.timeoutId);
                            }
                            $scope.copyLinkClicked = true;
                            $scope.timeoutId = setTimeout(function() {
                                $timeout(function() {
                                    clearTimeout($scope.timeoutId);
                                    $scope.timeoutId = false;
                                    $scope.copyLinkClicked = false;
                                });
                            }, 2000);
                        };


                        $scope.sharedWithUpdated = function() {
                            var currentIds = $("#mailIdTags").tagit("assignedTags");
                            for (var i = 0; i < currentIds.length; i++) {
                                currentIds[i] = currentIds[i].toLowerCase();
                            }
                            currentIds.sort();
                            $scope.origRecipients.sort();
                            return !angular.equals(currentIds, $scope.origRecipients);
                        }

                        $scope.sharedWithAddedNew = function() {
                            var currentIds = $("#mailIdTags").tagit("assignedTags");
                            for (var i = 0; i < currentIds.length; i++) {
                                currentIds[i] = currentIds[i].toLowerCase();
                            }
                            return !currentIds.every(function(val) {
                                return $scope.origRecipients.indexOf(val) >= 0
                            });
                        }

                        function displayMessage(msg, isSuccess) {
                            $scope.isLoading = false;
                            $scope.messageStatus = isSuccess ? 2 : 1;
                            $scope.message = msg;
                        };

                        $scope.rightsGranted = [];
                        $scope.rightsGrantedArr = [];
                        var rightsArr = file.rights;
                        for (var i = 0; i < rightsArr.length; i++) {
                            $scope.rightsGranted[rightsArr[i]] = true;
                        }

                        $scope.revoke = function(fromSpaceId) {
                            var allMemberProjectIds = [];
                            for (var j in $scope.allProjectList) {
                                allMemberProjectIds.push($scope.allProjectList[j].id);
                            }
                            var projectsWithoutAccess = $($scope.origProjectRecipients).not(allMemberProjectIds).get();
                            var message = "";
                            if (projectsWithoutAccess.length > 0) {
                                message =  '<p>' + $filter('translate')('share.file.revoke.member.project.part1') + '<br>'
                                + $filter('translate')('share.file.revoke.member.project.share-count') + '<strong>' + projectsWithoutAccess.length + '</strong></p><p>'
                                + $filter('translate')('share.file.revoke.member.project.part2') + '</p>';
                            } else {
                                message =  '<p>' + $filter('translate')('share.file.revoke.member.project.part1') + '</p><p>'
                                + $filter('translate')('share.file.revoke.member.project.part2') + '</p>';
                            }
                            
                            dialogService.confirm({
                                newTemplate: true,
                                msg: message,
                                ok: function() {
                                    shareFileService.revokeFile($scope.duid, function(data) {
                                        if (data.statusCode == 200) {
                                            displayMessage($filter('translate')('share.file.revoked'), true);
                                            $scope.revoked = true;
                                            if (fromSpaceId == "PROJECTSPACE") {
                                                $rootScope.$emit("refreshProjectSharedFileList");
                                            } else {
                                                $rootScope.$emit("refreshSharedFileList");
                                            }
                                        } else if (data.statusCode == 403) {
                                            displayMessage($filter('translate')('share.file.unauthorized.revoke'), false);
                                        } else if (data.statusCode == 304) {
                                            displayMessage($filter('translate')('share.file.already.revoked'), false);
                                        } else {
                                            displayMessage($filter('translate')('manage.file.error'), false);
                                        }
                                    });
                                },
                                cancel: function() {
                                    $scope.isLoading = false;
                                }
                            });
                        }

                        $scope.validateEmail = function(id) {
                            validateEmail(id, $scope);
                        }

                        $scope.updateRecipientList = function() {
                            $scope.isLoading = true;
                            $scope.message = "";
                            $scope.message2 = "";
                            $scope.messageStatus = 0;
                            for (var q = 0; q < $scope.origRecipients.length; q++) {
                                $scope.origRecipients[q] = $scope.origRecipients[q].toLowerCase();
                            }
                            $scope.origRecipients.sort();
                            var shareWithArray = [];
                            if ($("#shareWith").val()) {
                                shareWithArray = $("#shareWith").val().replace(/ /g, '').split(',');
                                for (var q = 0; q < shareWithArray.length; q++) {
                                    shareWithArray[q] = shareWithArray[q].toLowerCase();
                                }
                                shareWithArray.sort();
                            }
                            $scope.addedRecipients = $(shareWithArray).not($scope.origRecipients).get();
                            $scope.removedRecipients = $($scope.origRecipients).not(shareWithArray).get();

                            if ($scope.addedRecipients.length > 0 || $scope.removedRecipients.length > 0) {
                                var addedRecipients = [];
                                $scope.addedRecipients.forEach(function(recipient) {
                                    addedRecipients.push({
                                        email: recipient
                                    });
                                });
                                var removedRecipients = [];
                                $scope.removedRecipients.forEach(function(recipient) {
                                    removedRecipients.push({
                                        email: recipient
                                    });
                                });

                                var params = {
                                    duid: $scope.duid,
                                    addedRecipients: addedRecipients,
                                    removedRecipients: removedRecipients
                                };
                                if ($scope.comment.text && $scope.comment.text.length > 0) {
                                    params.comment = $scope.comment.text;
                                }
                                shareFileService.updateSharedFile(params, function(data) {
                                    if (data.statusCode == 200) {
                                        $scope.comment = {};
                                        var added = data.results.newRecipients;
                                        var removed = data.results.removedRecipients;
                                        var message = "";
                                        if (added && added.length > 0) {
                                            message += $filter('translate')('share.success_email') + added.join(', ') + ". ";
                                        }
                                        if (removed && removed.length > 0) {
                                            message += $filter('translate')('unshare.success.with') + removed.join(', ') + ". ";
                                        }
                                        if (!message) {
                                            message = $filter('translate')('share.already_sent_email', {
                                                receipients: $scope.addedRecipients.join(', ')
                                            });
                                        }
                                        displayMessage(message, true);
                                        $rootScope.$emit("refreshSharedFileList");
                                    } else if (data.statusCode == 4001) {
                                        displayMessage($filter('translate')('managelocalfile.rights.revoke.label'), false);
                                        $scope.revoked = true;
                                    } else if (data.statusCode == 4003) {
                                        displayMessage($filter('translate')('share.error.expired'), false);
                                    } else if (data.statusCode == 403) {
                                    	displayMessage($filter('translate')('share.file.unauthorized.reshare'), false);
                                    } else {
                                        displayMessage($filter('translate')('share.error'), false);
                                    }
                                    $scope.updateOrigRecipients();
                                });
                            }
                        }

                        $scope.updateOrigRecipients = function() {
                            $scope.isLoading = true;
                            shareFileService.getSharedFileDetails({
                                duid: $scope.duid,
                                pathId: $scope.pathId
                            }, function(data) {
                                $scope.isLoading = false;
                                if (data.statusCode == 200) {
                                    $scope.origRecipients = data.results.detail.recipients;
                                } else {
                                    for (var b = 0; b < $scope.removedRecipients.length; b++) {
                                        var index = $scope.origRecipients.indexOf($scope.removedRecipients[b]);
                                        $scope.origRecipients.splice(index, 1);
                                    }
                                    for (var a = 0; a < $scope.addedRecipients.length; a++) {
                                        $scope.origRecipients.push($scope.addedRecipients[a]);
                                    }
                                }
                                $scope.origRecipients.sort();
                            });
                        }

                        $scope.contentRightsAvailable = dialogService.getContentRights();
                        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
                        $scope.effectRightsAvailable = dialogService.getEffectRights();

                        $scope.dt = null;

                        $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
                        $scope.format = $scope.formats[0];
                        $scope.altInputFormats = ['M!/d!/yyyy'];

                        $scope.popup1 = {
                            opened: false
                        };

                        $scope.popup1.opened = false;
                        $scope.open1 = function() {
                            $scope.popup1.opened = true;
                        };

                        $scope.dateOptions = {

                        };

                        $scope.mailPristine = true;
                        $scope.checkMailPristine = function() {
                            return $scope.mailPristine;
                        }

                        $scope.checkNoProjectSelected = function() {
                            var noProjectSelected = true;
                            for(var j in allProjectList){
                                if(allProjectList[j].selected === true){
                                    noProjectSelected = false; 
                                    break;
                                }
                            }
                            return noProjectSelected;
                        }

                        $scope.selectText = function() {
                            $("#fileLinkId").focus();
                            $("#fileLinkId").select();
                        }

                    }
                ]
            });
        };

        var viewSharedFileActivity = function(parameter, successCallback, errorCallback) {
            $uibModal.open({
                animation: true,
                size: 'lg',
                windowClass: 'app-modal-window-activity',
                templateUrl: 'ui/app/Home/SharedFiles/partials/sharedFileActivityTemplate.html',
                controller: ['$uibModalInstance', '$scope', '$filter', function($uibModalInstance, $scope, $filter) {
                    $scope.currentPage = 0;
                    $scope.orderBy = "accessTime";
                    $scope.orderByReverse = true;
                    $scope.pageItems = 10;
                    $scope.name = parameter.file.name;
                    $scope.searchOptions = [{
                            value: "default",
                            field: $filter('translate')('share.file.activity.search.select')
                        },
                        {
                            value: "email",
                            field: $filter('translate')('share.file.user')
                        },
                        {
                            value: "operation",
                            field: $filter('translate')('share.file.operation')
                        },
                        {
                            value: "deviceId",
                            field: $filter('translate')('share.file.deviceId')
                        }
                    ];

                    $scope.searchData = {};
                    $scope.searchData.field = "default";
                    $scope.searchData.value = "";
                    $scope.columnFilter = {};
                    $scope.isLoading = true;
                    $scope.error = false;
                    $scope.errorMessage = "";
                    $scope.canResetSearchResults = false;

                    $scope.onServerSideItemsRequested = function(currentPage, pageItems, orderBy, orderByReverse) {
                        $scope.isLoading = true;
                        $scope.error = false;
                        $scope.errorMessage = "";
                        var params = {};
                        params.duid = parameter.duid;
                        params.userId = parameter.userId;
                        params.ticket = parameter.ticket;
                        params.start = currentPage * pageItems;
                        params.count = pageItems;
                        params.orderBy = orderBy;
                        params.orderByReverse = orderByReverse;
                        params.searchField = $scope.searchData.field;
                        params.searchText = $scope.searchData.value;

                        shareFileService.getSharedFileActivityLog(params, handleActivityResponse);
                    };

                    $scope.clearSearch = function() {
                        $scope.searchData.value = "";
                        $scope.doSearch(true);
                    }

                    $scope.$watch('searchData.field', function (newValue, oldValue) {
                        $scope.searchData.value = "";
                    });

                    $scope.doSearch = function(hideEmptySearchMessage) {
                        if ($scope.searchData.field && $scope.searchData.field != "default" && $scope.searchData.value && $.trim($scope.searchData.value) != "") {
                            $scope.canResetSearchResults = true;
                            $scope.columnFilter = {};
                            $scope.columnFilter[$scope.searchData.field] = $.trim($scope.searchData.value);
                        } else {
                            if ($scope.canResetSearchResults) {
                                $scope.canResetSearchResults = false;
                                $scope.columnFilter = {};
                            } else {
                                if (!hideEmptySearchMessage) {
                                    $scope.error = true;
                                    $scope.errorMessage = $filter('translate')('share.file.activity.search.select.validation.message');
                                }
                            }
                        }
                    };

                    var handleActivityResponse = function(data) {
                        $scope.isLoading = false;
                        if (data.statusCode == 200) {
                            $scope.totalItems = data.results.totalCount;
                            $scope.items = data.results.data.logRecords;
                            return;
                        } else if (data.statusCode == 404) {
                            $scope.error = true;
                            $scope.errorMessage = $filter('translate')('share.file.activity.file.not.found');
                            return;
                        } else {
                            $scope.error = true;
                            $scope.errorMessage = $filter('translate')('share.file.activity.search.error');
                            return;
                        }
                    };

                    $scope.ok = function() {
                        $uibModalInstance.dismiss('cancel');
                    };
                }]
            });
        };

        var enableShiftCtrl = function () {
            shortcut.remove('Ctrl');
            shortcut.remove('Shift');
        }

        var disableShiftCtrl = function () {
            shortcut.add("Ctrl", ctrlKeyPressed, {
                'disable_in_input': true
            });
            shortcut.add("Shift", splKeyPressed, {
                'disable_in_input': true
            });
        }

        return {
            shareFile: shareFile,
            reshareFile: reshareFile,
            viewSharedFileDetails: viewSharedFileDetails,
            viewSharedFileActivity: viewSharedFileActivity
        }
    }
]);

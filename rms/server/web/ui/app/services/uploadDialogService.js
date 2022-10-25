mainApp.factory('uploadDialogService', ['$uibModal', '$rootScope', '$filter', 'dialogService', 'networkService', 'initSettingsService', 'uploadFileService', 'digitalRightsExpiryService', 'repositoryService', 
    function($uibModal, $rootScope, $filter, dialogService, networkService, initSettingsService, uploadFileService, digitalRightsExpiryService, repositoryService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var uploadFileModal = function(parameter, selectedFiles, successCallback, fileListControllerScope) {
            $uibModal.open({
                animation: true,
                size: 'lg',
                windowClass: 'app-modal-window-activity',
                templateUrl: 'ui/app/Home/Repositories/partials/uploadfileToRepository.html',
                controller: ['$uibModalInstance', '$scope', 'Upload', '$timeout', '$stateParams', 'uploadFileService', '$controller', 'userPreferenceService', '$uibModal', 'repositoryService',
                    function($uibModalInstance, $scope, Upload, $timeout, $stateParams, uploadFileService, $controller, userPreferenceService, $uibModal, repositoryService) {
                        var init = function () {
                            userPreferenceService.getPreference(function (data) {
                                if (data.statusCode == 200) {
                                    if (data.results != undefined) {
                                        $scope.watermarkStr = data.results.watermark;
                                        if (data.results.expiry) {
                                            $scope.expiryJson = data.results.expiry;
                                            $scope.expiryStr = digitalRightsExpiryService.getExpiryStr($scope.expiryJson);
                                        }
                                        return;
                                    }
                                }
                            });
                        };
                        init();
                        var MAX_FILE_SIZE = 157286400;
                        $scope.editWatermark = false;
                        $scope.showWatermark = false;
                        $scope.editExpiry = false;                       
                        $scope.uploadTitle = $stateParams.repoName ?
                            $filter('translate')('upload.file.with.repo.name', {
                                repoName: $stateParams.repoName
                            }) :
                            $filter('translate')('upload.file.repository');
                        $scope.files = selectedFiles;
                        $scope.filePristine = true;
                        $scope.fileChosen = false;
                        $scope.protect = true;
                        $scope.currentFolder = parameter.currentFolder;
                        $scope.error = "";
                        $scope.uploadedFileName = "";
                        $scope.contentRightsAvailable = dialogService.getContentRights();
                        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
                        $scope.effectRightsAvailable = dialogService.getEffectRights();
                        $scope.rights = ['VIEW'];
                        $scope.disableUpload = fileListControllerScope.isLoading;
                        $scope.isPageRefreshing = function() {
                            return fileListControllerScope.isLoading;
                        }

                        $scope.$watch('isPageRefreshing()', function(newValue) {
                            $scope.disableUpload = newValue;
                        })
                        $scope.checkNxl = function() {
                            return $scope.files.name.endsWith('.nxl');
                        };
                        $scope.resetRights = function() {
                            $scope.rights = ['VIEW'];
                            init();
                        };

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
                            } else {
                                if (!$scope.watermarkScope) {
                                    $scope.watermarkScope = $controller('watermarkController',{$scope: $scope});
                                }
                                $scope.showWatermarkResult($scope.watermarkStr);
                            }
                            $scope.isLoading = false;
                        });

                        var checkFile = function() {
                            $scope.isNxl = $scope.checkNxl();
                            if ($scope.files.type === "" && $scope.files.name.indexOf(".") == -1) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.file.is.folder');
                            } else if ($scope.files.size > MAX_FILE_SIZE) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.file.maxallowedsize');
                            } else if (repositoryService.checkInvalidCharacters($scope.files.name)) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('file.upload.invalidfilename');                                                        
                            } else if ($scope.files.size == 0) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.file.empty');
                            } else if ($scope.isNxl) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.file.is.nxt-file');
                            } else {
                                $scope.filePristine = false;
                                $scope.fileExist = false;
                                $scope.error = "";
                                $scope.resetRights();
                            }

                            $scope.fileChosen = true;
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
                        $scope.$watch('files', function(newValue, oldValue) {
                            if (newValue != oldValue && (oldValue || $scope.filePristine)) {
                                if (newValue == null) {
                                    $scope.files = oldValue;
                                } else {
                                    checkFile();
                                }
                            }
                        });
                        $scope.upload = function(protect) {
                            if ($scope.disableUpload) {
                                return;
                            }
                            if (fileListControllerScope.repoType === 'SHAREPOINT_ONLINE' && $scope.currentFolder.pathId === '/') {
                                $scope.error = $filter('translate')('upload.file.sharepoint.root');
                                return;
                            }
                            $scope.disableUpload = true;
                            $scope.protect = protect;
                            var objectNameList = {};
                            for (var i = fileListControllerScope.repoContents.length - 1; i >= 0; i--) {
                                objectNameList[fileListControllerScope.repoContents[i]['name'].toLowerCase()] = 1;
                            }
                            $scope.fileExist = false;
                            if (objectNameList[$scope.files.name.toLowerCase()]) {
                                $scope.fileExist = true;
                            }
                            if ($scope.protect) {
                                checkMyVaultFileExistsThenUploadFile($scope.files.name)
                            } else {
                                checkMyDriveFileExistsThenUploadFile($scope.files.name);
                            }
                        }

                        var checkMyVaultFileExistsThenUploadFile = function(fileName) {
                            if($scope.fileExist){
                                showConfirmMyVaultFileOverwriteDialog(fileName);
                                return;
                            }
                            $scope.userConfirmedFileOverwrite = false;
                            if(fileName.endsWith(".nxl")){
                                $scope.normalUpload(fileName + ".nxl", JSON.stringify($scope.rights));
                                return;
                            }
                           
                            repositoryService.checkIfMyVaultFilePathExists("/nxl_myvault_nxl/" + fileName, function(data){
                                $scope.isLoading = false;
                                if(data.statusCode == 200 && data.results.fileExists == true) {
                                    showConfirmMyVaultFileOverwriteDialog(fileName);
                                } else {
                                    $scope.normalUpload(fileName + ".nxl", JSON.stringify($scope.rights));
                                }
                            });
                        }
                        
                        var checkMyDriveFileExistsThenUploadFile = function(fileName) {
                            if($scope.fileExist){
                                showConfirmMyDriveFileOverwriteDialog(fileName);
                                return;
                            }
                            $scope.userConfirmedFileOverwrite = false;
                            var pathId = $scope.currentFolder.path + fileName;
                            if(pathId.endsWith(".nxl")){
                                $scope.normalUpload(fileName);
                                return;
                            }
                            $scope.isLoading = true;
                            repositoryService.checkIfMyVaultFilePathExists(pathId, function(data){
                                $scope.isLoading = false;
                                if(data.statusCode == 200 && data.results.fileExists == true) {
                                    showConfirmMyDriveFileOverwriteDialog(fileName)
                                } else {
                                    $scope.normalUpload(fileName);
                                }
                            });
                        }

                        var showConfirmMyVaultFileOverwriteDialog = function (fileName) {
                            dialogService.confirm({
                                msg:  $filter('translate')('myvault.file.upload.exists') + 
                                      $filter('translate')('file.overwrite.alert.confirmation'),
                                ok: function() {
                                    $scope.userConfirmedFileOverwrite = true;
                                    $scope.normalUpload(fileName + ".nxl", JSON.stringify($scope.rights));
                                },
                                cancel: function() {
                                    $scope.disableUpload = false;
                                },
                            });
                        }
                        
                        var showConfirmMyDriveFileOverwriteDialog = function (fileName) {
                            dialogService.confirm({
                                msg:  $filter('translate')('mydrive.file.upload.exists') + 
                                      $filter('translate')('file.replace.alert.confirm'),
                                ok: function(data) {
                                    $scope.userConfirmedFileOverwrite = true;
                                    $scope.normalUpload(fileName);
                                },
                                cancel: function() {
                                    $scope.disableUpload = false;
                                },
                            });
                        }

                        $scope.normalUpload = function(fileName, rights) {
                            if (fileName.length > 128) {
                                if (!fileName.endsWith('.nxl')) {
                                    $scope.error = $filter('translate')('upload.file.nameLength.tooLong1') + fileName +
                                        $filter('translate')('upload.file.nameLength.tooLong2');
                                } else {
                                    $scope.error = $filter('translate')('upload.protected.file.nameLength.tooLong') + fileName +
                                        $filter('translate')('upload.file.nameLength.tooLong2');
                                }
                                $scope.disableUpload = false;
                                return;
                            }
                            uploadFileService.setCloseStatus(false);
                            uploadFileService.setMinimizeStatus(jscd.mobile && uploadFileService.getMinimizeStatus());
                            var userIdFromCookie = window.readCookie("userId");
                            var ticketFromCookie = window.readCookie("ticket");
                            var clientId = window.readCookie("clientId");
                            if (userIdFromCookie == undefined || ticketFromCookie == undefined || clientId == undefined) {
                                window.location = initSettingsService.getTimeOutUrl();
                                return;
                            }
                            $uibModalInstance.dismiss('cancel');
                            $scope.uploadedFileName = fileName;
                            var appendedFileName;
                            var hoverFileName = getOriginalFileName(fileName, $scope.files.name);
                            url = CONTEXT_PATH + "/RMSViewer/UploadFile";
                            var folder = $scope.currentFolder;
                            $scope.uploadedFilePath = $scope.currentFolder.path;
                            var fileStillUploading = uploadFileService.fileStillUploading(hoverFileName, $scope.uploadedFilePath, $scope.uploadedRepoId, uploadFileService.getUploadFileList());
                            if (fileStillUploading) {
                                appendedFileName = getShortName($scope.files.name, 20);
                                uploadFileService.getUploadFileList().push({
                                    "displayFileName": appendedFileName + " " + $filter('translate')('file.uploading'),
                                    "fileName": $scope.uploadedFileName + " " + $filter('translate')('file.uploading'),
                                    "hoverFileName": hoverFileName,
                                    "filePath": $scope.uploadedFilePath,
                                    "repoId": $scope.uploadedRepoId,
                                    "fileUploading": false,
                                    "fileUploaded": false,
                                    "percentUploaded": 0
                                });
                                return;
                            } else {
                                uploadFileService.setUploadingStatus(true);
                                uploadFileService.setUploadedStatus(false);
                                appendedFileName = getShortName($scope.files.name, 48);
                                uploadFileService.getUploadFileList().push({
                                    "displayFileName": appendedFileName,
                                    "fileName": $scope.uploadedFileName,
                                    "hoverFileName": hoverFileName,
                                    "filePath": $scope.uploadedFilePath,
                                    "repoId": $scope.uploadedRepoId,
                                    "fileUploading": uploadFileService.getUploadingStatus(),
                                    "fileUploaded": uploadFileService.getUploadedStatus(),
                                    "percentUploaded": 0
                                });
                            }
                            var uploadFinish = function(index, error, response) {
                                fileListControllerScope.isLoading = false;
                                fileListControllerScope.isUploadMessage = true;
                                var hoverFileName = response != null && response.data != null ?
                                    getOriginalFileName(response.data.name, $scope.files.name) : $scope.files.name;
                                $scope.resetRights();
                                var isSuccess = false;
                                var message;
                                var onClickCallback;
                                if (error == $filter('translate')('file.upload.fail.vault')) {
                                    appendedFileName = getShortName($scope.files.name, 48);
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(true);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": appendedFileName,
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.uploadedRepoId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "percentUploaded": 100
                                    };
                                    if ($scope.uploadedFileName.endsWith('.nxl') && !$scope.files.name.endsWith('.nxl')) {
                                        message = $filter('translate')('rights.protected.file.uploaded.failed1') + $scope.uploadedFileName + $filter('translate')('rights.protected.file.uploaded.failed2');
                                    }
                                    successCallback($scope.currentFolder);
                                } else if (error) {
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(false);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": $filter('translate')('file.upload.fail') + $scope.files.name + $filter('translate')('ending'),
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.uploadedRepoId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "uploadFailed": true,
                                        "error": error,
                                        "percentUploaded": 0
                                    };
                                    message = error;
                                } else {
                                    uploadFileService.removeRedundantMessage($scope.uploadedFileName);
                                    uploadFileService.setUploadingStatus(false);
                                    uploadFileService.setUploadedStatus(true);
                                    appendedFileName = getShortName($scope.files.name, 48);
                                    uploadFileService.getUploadFileList()[index] = {
                                        "displayFileName": appendedFileName,
                                        "fileName": $scope.uploadedFileName,
                                        "hoverFileName": hoverFileName,
                                        "filePath": $scope.uploadedFilePath,
                                        "repoId": $scope.uploadedRepoId,
                                        "fileUploading": uploadFileService.getUploadingStatus(),
                                        "fileUploaded": uploadFileService.getUploadedStatus(),
                                        "percentUploaded": 100
                                    };
                                    if ($scope.uploadedFileName.endsWith('.nxl')) {
                                        if ($scope.files.name.endsWith('.nxl')) {
                                            message = $filter('translate')('File') + $scope.files.name + $filter('translate')('upload.protected.file.uploaded');
                                            isSuccess = true;
                                        } else {
                                            message = [];
                                            message.push($filter('translate')('File') + $scope.files.name + $filter('translate')('upload.protected.file.uploaded') + ' ' +
                                                $filter('translate')('rights.protected.file.uploaded1') + $scope.uploadedFileName + $filter('translate')('rights.protected.file.uploaded2'));
                                            message.push('<div id="link" class="manage-profile-functions"">' + $filter('translate')('managelocalfile.message.myvault.link') + '</div>');
                                            isSuccess = true;
                                            onClickCallback = fileListControllerScope.onClickMyVault;
                                        }
                                    } else {
                                        message = $filter('translate')('File') + $scope.files.name + $filter('translate')('upload.protected.file.uploaded');
                                        isSuccess = true;
                                    }
                                    if (response.data.usage) {
                                        fileListControllerScope.usage = Number(response.data.usage);
                                    }
                                    if (response.data.myVaultUsage) {
                                        fileListControllerScope.myVaultUsage = Number(response.data.myVaultUsage);
                                    }
                                    successCallback($scope.currentFolder);
                                }
                                if (message) {
                                    showSnackbar({
                                        isSuccess: isSuccess,
                                        messages: message,
                                        linkCallback: onClickCallback
                                    });
                                }
                            };
                            var data = {
                                file: $scope.files,
                                repoId: folder.repoId ? folder.repoId : $stateParams.repoId,
                                filePathId: folder.pathId,
                                filePathDisplay: folder.path,
                                rightsJSON: rights,
                                conflictFileName: $scope.uploadedFileName, 
                                userConfirmedFileOverwrite : $scope.userConfirmedFileOverwrite
                            };

                            if (!$scope.nxl) {
                                var expiry = $scope.expiryJson;
                                if (expiry.relativeDay) {
                                    expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day)
                                }
                                data.expiry = JSON.stringify(expiry);
                                if ($scope.showWatermark) {
                                    data.watermark = $scope.watermarkStr;
                                }
                            }

                            var uploader = Upload.upload({
                                url: url,
                                data: data
                            });
                            uploader.then(function(response) {
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName, $scope.uploadedFilePath, $scope.uploadedRepoId, uploadFileService.getUploadFileList());
                                    var error = response.data.error;
                                    uploadFinish(index, error, response);
                                },
                                errorHandler,
                                function(evt) {
                                    var index = uploadFileService.getIndexOfUploadedFileName($scope.uploadedFileName,
                                        $scope.uploadedFilePath, $scope.uploadedRepoId, uploadFileService.getUploadFileList());
                                    /*
                                    do not show 100% because even if client has uploaded file completely, 
                                    we have to wait until RMS uploaded same file to repository
                                    */
                                    if (evt.total == 0) {
                                        uploadFinish(index, $filter('translate')('upload.file.failure'), null);
                                    } else {
                                        var progressPercentage = parseInt(90.0 * evt.loaded / evt.total);
                                        var uploadEntry = uploadFileService.getUploadFileList()[index];
                                        uploadEntry.percentUploaded = progressPercentage;
                                    }
                                }
                            );
                        }
                    }
                ]
            });
        };

        var uploadProfilePictureModal = function(callingScope) {
            $uibModal.open({
                animation: true,
                size: 'lg',
                windowClass: 'app-modal-window-activity',
                templateUrl: 'ui/app/Home/Repositories/partials/uploadProfilePicture.html',
                controller: ['$uibModalInstance', '$scope', '$cookies',
                    function($uibModalInstance, $scope, $cookies) {
                        var MAX_FILE_SIZE = 250000;
                        $scope.files = "";
                        $scope.filePristine = true;
                        $scope.fileChosen = false;
                        $scope.error = "";

                        var checkFile = function() {
                            if (!$scope.files.name.toLowerCase().endsWith('.jpg') && !$scope.files.name.toLowerCase().endsWith('.jpeg') && !$scope.files.name.toLowerCase().endsWith('.png') &&
                                !$scope.files.name.toLowerCase().endsWith('.bmp')) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.profile.picture.wrongExtension');
                            } else if ($scope.files.size > MAX_FILE_SIZE) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.profile.picture.maxallowedsize');
                            } else if ($scope.files.size == 0) {
                                $scope.filePristine = true;
                                $scope.error = $filter('translate')('upload.file.empty');
                            } else {
                                $scope.filePristine = false;
                                $scope.error = "";
                            }

                            $scope.fileChosen = true;
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
                        $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        var uploadProfilePictureHelper = function(url) {
                            var jsonHeaders = {
                                'Content-Type': 'application/json; charset=utf-8'
                            };

                            userId = $cookies.get('userId');
                            ticket = $cookies.get('ticket');
                            usrDetails = {};
                            usrDetails["userId"] = parseInt(userId);
                            usrDetails["ticket"] = ticket;
                            var preferences = new Object();
                            preferences["profile_picture"] = url;
                            usrDetails["preferences"] = preferences;
                            params = {};
                            params["parameters"] = usrDetails;

                            networkService.post(RMS_CONTEXT_NAME + "/rs/usr/profile", JSON.stringify(params), jsonHeaders, function(data) {
                                $scope.editingDisplayNameLoading = false;
                                if (data != null && data.statusCode == 200) {
                                    callingScope.profilePictureUrl = url;
                                    callingScope.hasProfilePicture = true;
                                    callingScope.handleSuccess("Profile picture successfully updated.");
                                } else {
                                    callingScope.handleError("Error occurred while updating profile picture.");
                                }
                                $uibModalInstance.dismiss('cancel');
                            });

                        }
                        $scope.uploadProfilePicture = function() {
                            callingScope.isLoading = true;
                            if ($scope.files) {
                                var FR = new FileReader();
                                FR.onload = function(e) {
                                    if (e.target.result) {
                                        uploadProfilePictureHelper(e.target.result);
                                    } else {
                                        callingScope.handleError("Error occurred while updating profile picture.");
                                    }
                                };
                                FR.readAsDataURL($scope.files);
                            }
                        }
                    }
                ]
            });
        };

        return {
            uploadFileModal: uploadFileModal,
            uploadProfilePictureModal: uploadProfilePictureModal
        }
    }
]);
mainApp.controller('manageLocalFileController', ['$scope', '$rootScope', '$state', 'networkService', 'dialogService', 'shareDialogService', '$location',
    'repositoryService', '$filter', 'serviceProviderService', 'Upload', 'initSettingsService', 'shareFileService', 'navService', 'protectWidgetService', '$controller', 'userPreferenceService', '$uibModal', 'digitalRightsExpiryService',
    function($scope, $rootScope, $state, networkService, dialogService, shareDialogService, $location,
        repositoryService, $filter, serviceProviderService, Upload, initSettingsService, shareFileService, navService, protectWidgetService, $controller, userPreferenceService, $uibModal, digitalRightsExpiryService) {
        var defaultWatermark;
        var defaultExpiryJson;
        var defaultExpiryStr;
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        $controller('watermarkController',{$scope: $scope});
        $scope.resetPreference = function () {
            $scope.watermarkStr = defaultWatermark;
            $scope.showWatermarkResult($scope.watermarkStr);
            $scope.expiryJson = defaultExpiryJson;
            $scope.expiryStr = defaultExpiryStr;
        }
        var init = function () {
            userPreferenceService.getPreference(function (data) {
                if (data.statusCode == 200) {
                    if (data.results != undefined) {
                        defaultWatermark = data.results.watermark;
                        if (data.results.expiry) {
                            defaultExpiryJson = data.results.expiry;
                            defaultExpiryStr = digitalRightsExpiryService.getExpiryStr(defaultExpiryJson);
                        }
                        $scope.resetPreference();
                        return;
                    }
                }
            });
        };
        init();
        $scope.files;
        $scope.fileChosen = false;
        $scope.doApply = true;
        $scope.manageOption = 'protect';
        $scope.filePristine = true;
        $scope.mailPristine = true;
        $scope.isFileNXL = false;
        $scope.allowedToShare = true;
        $scope.rightsFound = true;
        $scope.error = '';
        $scope.successMessage = '';
        $scope.successMessage2 = '';
        $scope.fileButtonName = $filter('translate')('managelocalfile.file.button.browse');
        $scope.rights = ['VIEW'];
        $scope.rightsModel = {};
        $scope.attachment = 0;
        $scope.optional = false;
        $scope.isLoading = false;
        $scope.showFileTypesHelp = false;
        $scope.comment = {};
        $scope.expiryInfo = true;
        $scope.showWatermark = false;
        $scope.editWatermark = false;
        $scope.editExpiry = false;

        var MAX_FILE_SIZE = 157286400;
        var settings = initSettingsService.getSettings();
        var rightShare = 'SHARE';

        $scope.manageButtonName = $filter('translate')('managelocalfile.manage.button.protect');
        $scope.lastStepText = $filter('translate')('managelocalfile.view.label');

        $scope.contentRightsAvailable = dialogService.getContentRights();
        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
        $scope.effectRightsAvailable = dialogService.getEffectRights();

        var resetRights = function() {
            for (var i = 0; i < $scope.contentRightsAvailable.length; ++i) {
                var key = $scope.contentRightsAvailable[i].id;
                $scope.rightsModel[key] = false;
            }
            for (var i = 0; i < $scope.collaborationRightsAvailable.length; ++i) {
                var key = $scope.collaborationRightsAvailable[i].id;
                $scope.rightsModel[key] = false;
            }
            for (var i = 0; i < $scope.effectRightsAvailable.length; ++i) {
                var key = $scope.effectRightsAvailable[i].id;
                $scope.rightsModel[key] = false;
            }

            $scope.rightsModel['VIEW'] = true;
        }

        var resetTags = function() {
            $scope.mailPristine = true;
            $("#mailShareTags").tagit("removeAll");
        }

        resetRights();

        $scope.checkNXL = function() {
            return $scope.isFileNXL;
        }

        $scope.canShare = function() {
            return $scope.allowedToShare;
        }

        $scope.switchSelection = function() {
            checkFile({
                keepCurrentOption: true
            });
        }

        var checkFile = function(params) {
            params = !params ? {} : params;
            $scope.isLoading = true;
            $scope.successMessage = '';
            $scope.successMessage2 = '';
            $scope.fileChosen = false;
            $scope.rightsFound = false;
            $scope.finished = false;
            if ($scope.files.name.length > 128) {
                $scope.isLoading = false;
                $scope.error = !$scope.files.name.endsWith('.nxl') ? $filter('translate')('upload.file.nameLength.tooLong1') + $scope.files.name + $filter('translate')('upload.file.nameLength.tooLong2') :
                    $filter('translate')('upload.protected.file.nameLength.tooLong') + $scope.files.name + $filter('translate')('upload.file.nameLength.tooLong2');
                return;
            }
            if ($scope.files.type === "" && $scope.files.name.indexOf(".") == -1) {
                $scope.error = $filter('translate')('upload.file.is.folder');
                $scope.isLoading = false;
            } else if ($scope.files.size == 0) {
                $scope.error = $filter('translate')('upload.file.empty');
                $scope.isLoading = false;
            } else if (repositoryService.checkInvalidCharacters($scope.files.name)) {
                $scope.error = $filter('translate')('file.upload.invalidfilename');
                $scope.isLoading = false;                
            } else if ($scope.files.size > MAX_FILE_SIZE) {
                $scope.error = $filter('translate')('upload.file.maxallowedsize');
                $scope.isLoading = false;
            } else {
                $scope.filePristine = false;
                resetRights();
                resetTags();
                $scope.mailPristine = true;
                $scope.rights = ['VIEW'];
                $scope.error = '';
                $scope.fileButtonName = $filter('translate')('managelocalfile.file.button.change');
                if (endsWith($scope.files.name, ".nxl")) {
                    $scope.isFileNXL = true;
                    $scope.manageOption = params.keepCurrentOption ? $scope.manageOption : 'protectAndShare';
                    if (!$scope.files.size) {
                        $scope.isLoading = false;
                        $scope.error = $filter('translate')('managelocalfile.rights.file.error');
                        $scope.successMessage = '';
                        return;
                    }
                } else {
                    $scope.isFileNXL = false;
                    $scope.manageOption = params.keepCurrentOption ? $scope.manageOption : 'protect';
                    $scope.isOwner = true;
                }
                $scope.fileChosen = true;
                $scope.rightsFound = true;
                $scope.allowedToShare = true;
                $scope.isLoading = false;
            }
        }

        $scope.$watch('files', function(newValue, oldValue) {
            if (newValue != oldValue && (oldValue || $scope.filePristine)) {
                if (newValue == null) {
                    $scope.files = oldValue;
                } else {
                    $scope.comment = {};
                    $scope.optional = true;
                    checkFile();
                    $scope.optional = false;
                }
            }
        });

        function checkRights() {
            protectWidgetService.checkRights($scope.files, function success(data) {
                $scope.rights = data.rights;
                $scope.isOwner = data.isOwner;
                $scope.allowedToShare = true;
                $scope.validity = data.validity;
                $scope.error = "";
                checkIfFileExistsThenUpload();
            }, function error(code, data) {
                $scope.successMessage = '';
                $scope.successMessage2 = '';
                $scope.allowedToShare = false;
                $scope.error = $filter('translate')(code);
            }, function loading(data) {
                $scope.isLoading = data;
            })
        }

        function readCredentials() {
            var userIdFromCookie = window.readCookie("userId");
            var ticketFromCookie = window.readCookie("ticket");
            var clientId = window.readCookie("clientId");
            var platformId = window.readCookie("platformId");
            if (userIdFromCookie == undefined || ticketFromCookie == undefined || clientId == undefined) {
                window.location = CONTEXT_PATH + '/timeout';
                return undefined;
            }
            var result = {};
            result['userId'] = userIdFromCookie;
            result['ticket'] = ticketFromCookie;
            result['clientId'] = clientId;
            result['platformId'] = platformId;
            return result;
        }

        $scope.manageFile = function() {
            if ($scope.isFileNXL && $scope.manageOption == 'protectAndShare') {
                checkRights();
            } else {
                if ($scope.manageOption == 'view') {
                    uploaded();
                } else {
                    checkIfFileExistsThenUpload();
                }
            }
        }
        function checkIfFileExistsThenUpload() {
            $scope.userConfirmedFileOverwrite = false;
            var pathId= $scope.files.name;

            $scope.isLoading = true;
            repositoryService.checkIfMyVaultFilePathExists("/nxl_myvault_nxl/" + pathId, function(data){
                $scope.isLoading = false;
                if(data.statusCode == 200 && data.results.fileExists == true) {
                    dialogService.confirm({
                        msg:  $filter('translate')('myvault.file.upload.exists') 
                            + $filter('translate')('file.overwrite.alert.confirmation'),
                        ok: function(data) {
                            $scope.userConfirmedFileOverwrite = true;
                            uploaded();
                        },
                        cancel: function() {},
                    });
                } else {
                    uploaded();
                }
            });
        }

        function uploaded() {
            if ($scope.files) {
                var cookie = readCredentials();
                if (cookie == undefined) {
                    return;
                }
                $scope.isLoading = true;
                var url;
                if ($scope.manageOption == 'view') {
                    url = VIEWER_URL + "/RMSViewer/UploadAndView";
                } else if ($scope.manageOption == 'protect') {
                    url = CONTEXT_PATH + "/RMSViewer/UploadAndProtect";
                } else if ($scope.manageOption == 'protectAndShare') {
                    url = CONTEXT_PATH + "/RMSViewer/ProtectAndShare";
                } else {
                    return;
                }
                var data = {
                    file: $scope.files,
                    userName: settings.userName,
                    offset: new Date().getTimezoneOffset(),
                    tenantId: settings.tenantId,
                    uid: cookie.userId,
                    ticket: cookie.ticket,
                    clientId: cookie.clientId,
                    tenantName: settings.tenantName,
                    rightsJSON: JSON.stringify($scope.rights),
                    shareWith: JSON.stringify($scope.currentIds),
                    userConfirmedFileOverwrite: $scope.userConfirmedFileOverwrite
                };
                if ($scope.comment.text && $scope.comment.text.length > 0) {
                    data.comment = $scope.comment.text;
                }
                if ($scope.manageOption != 'view') {
                    if(!$scope.isFileNXL) {
                        var expiry = $scope.expiryJson;
                        if (expiry.relativeDay) {
                            expiry.endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day)
                        }
                        data.expiry = JSON.stringify(expiry);
                    }
                    if ($scope.showWatermark) {
                        data.watermark = $scope.watermarkStr;
                    }
                }
                var uploader = Upload.upload({
                    url: url,
                    data: data
                });
                $scope.finished = false;
                uploader.then(function(response) {
                        $scope.isLoading = false;
                        $scope.finished = true;
                        var url = response.data.viewerUrl;
                        if ($scope.manageOption != 'protectAndShare') {
                            var error = response.data.error;
                            if (error) {
                                $scope.error = error;
                                $scope.successMessage = '';
                                $scope.successMessage2 = '';
                                if (response.data.statusCode == 5009) {
                                    $scope.showFileTypesHelp = true;
                                }
                            } else {
                                $scope.comment = {};
                                $scope.error = '';
                                if ($scope.manageOption == 'protect') {
                                    var expiry = $scope.expiryJson;
                                    var startDate;
                                    var endDate;
                                    if(expiry.relativeDay){
                                        endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    } else {
                                        startDate = expiry.startDate;
                                        endDate = expiry.endDate;
                                    }
                                    $scope.successMessage = $filter('translate')('rights.protected.file.uploaded1') + response.data.name + $filter('translate')('rights.protected.file.uploaded2');
                                    $scope.successLink = $filter('translate')('managelocalfile.message.myvault.link');
                                    digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                    $scope.resetPreference();
                                    $scope.protectionType = response.data.protectionType;
                                } else if ($scope.manageOption == 'view') {
                                    $scope.successMessage = $filter('translate')('managelocalfile.message.view.success');
                                    openSecurePopup(url);
                                }
                            }
                        } else {
                            if (response.data.result == true) {
                                $scope.duid = response.data.duid;
                                $scope.filePathId = response.data.filePathId;
                                $scope.error = '';
                                var startDate;
                                var endDate;
                                var expiry;
                                if(!$scope.isFileNXL) {
                                    expiry = $scope.expiryJson;
                                    if(expiry.relativeDay){
                                        endDate = digitalRightsExpiryService.calculateRelativeEndDate(expiry.relativeDay.year, expiry.relativeDay.month, expiry.relativeDay.week, expiry.relativeDay.day);
                                    } else {
                                        startDate = expiry.startDate;
                                        endDate = expiry.endDate;
                                    }
                                } else {
                                    if($scope.validity.relativeDay){
                                        endDate = digitalRightsExpiryService.calculateRelativeEndDate($scope.validity.relativeDay.year, $scope.validity.relativeDay.month, $scope.validity.relativeDay.week, $scope.validity.relativeDay.day);
                                    } else {
                                        startDate = $scope.validity.startDate;
                                        endDate = $scope.validity.endDate;
                                    }
                                }
                                digitalRightsExpiryService.addExpiryInfo(startDate, endDate);
                                $scope.resetPreference();
                                $scope.successMessage = response.data.messages[0];
                                if (!$scope.isFileNXL) {
                                    $scope.successLink = response.data.messages[1];
                                } else if (response.data.messages.length > 1) {
                                    $scope.successMessage2 = response.data.messages[1];
                                }
                                $scope.protectionType = response.data.protectionType;
                            } else {
                                $scope.error = response.data.messages[0];
                                $scope.successMessage = '';
                                $scope.successMessage2 = '';
                            }
                        }
                    }, errorHandler,
                    function(evt) {
                        if (evt.total == 0) {
                            $scope.isLoading = false;
                            $scope.finished = true;
                            $scope.successMessage = '';
                            $scope.successMessage2 = '';
                            if ($scope.manageOption == 'view') {
                                $scope.error = $filter('translate')('view.file.failure');
                            } else if ($scope.manageOption == 'protect') {
                                $scope.error = $filter('translate')('protect.file.failure');
                            } else if ($scope.manageOption == 'protectAndShare') {
                                $scope.error = $filter('translate')('share.file.failure');
                            } else {
                                $scope.error = $filter('translate')('upload.file.failure');
                            }
                        }
                    }
                );
            }
        }

        $scope.$watch('manageOption', function(newValue, oldValue) {
            if (newValue != oldValue) {
                if ($scope.manageOption == 'view') {
                    $scope.manageButtonName = $filter('translate')('managelocalfile.manage.button.view');
                    $scope.lastStepText = $filter('translate')('managelocalfile.view.label');
                } else if ($scope.manageOption == 'protect') {
                    $scope.manageButtonName = $filter('translate')('managelocalfile.manage.button.protect');
                    $scope.lastStepText = $filter('translate')('managelocalfile.protect.label');
                } else if ($scope.manageOption == 'protectAndShare') {
                    if ($scope.isFileNXL) {
                        $scope.lastStepText = $filter('translate')('managelocalfile.protect.and.share.nxl.label');
                    } else {
                        $scope.lastStepText = $filter('translate')('managelocalfile.protect.and.share.label');
                    }
                    $scope.manageButtonName = $filter('translate')('managelocalfile.manage.button.protect.and.share');
                    $scope.attachment = 0;
                }
                $scope.resetPreference();
            }
        });

        function endsWith(str, suffix) {
            return str.slice(-suffix.length) === suffix
        }

        $scope.addRight = function(right) {
            var index = $scope.rights.indexOf(right);

            if (index == -1) {
                $scope.rights.push(right);
            }
        }

        $scope.toggleSelection = function toggleSelection(id) {
            var idx = $scope.rights.indexOf(id);
            // is currently selected
            if (idx > -1) {
                $scope.rights.splice(idx, 1);
            }
            // is newly selected
            else {
                $scope.rights.push(id);
            }
        };

        $scope.showRights = function() {
            return $scope.manageOption == 'protect' || $scope.manageOption == 'protectAndShare';
        }

        $scope.showRecipients = function() {
            return $scope.fileChosen && $scope.manageOption == 'protectAndShare';
        }

        $scope.getLastStepText = function() {
            return $scope.lastStepText;
        }

        $scope.getManageButtonName = function() {
            return $scope.manageButtonName;
        }

        $scope.checkMailPristine = function() {
            return $scope.mailPristine && $scope.manageOption == 'protectAndShare';
        }

        $scope.validateEmail = function(id) {
            validateEmail(id, $scope);
        }

        $scope.viewSharedFileDetails = function() {
            resetTags();
            shareFileService.getSharedFileDetails({
                duid: $scope.duid,
                pathId: $scope.filePathId
            }, function(data) {
                if (data.statusCode == 200) {
                    $scope.isLoading = false;
                    shareDialogService.viewSharedFileDetails({
                        duid: $scope.duid,
                        pathId: $scope.filePathId,
                        file: data.results.detail
                    });
                } else {
                    dialogService.displayError({
                        msg: $filter('translate')('manage.file.error')
                    });
                }
            });
        }

        $scope.reset = function() {
            $scope.fileChosen = false;
            $scope.filePristine = true;
            $scope.fileButtonName = $filter('translate')('managelocalfile.file.button.browse');
            $scope.successMessage = '';
            $scope.finished = false;
        }

        $scope.onClickMyVault = function() {
            navService.setCurrentTab('shared_files');
            navService.setIsInAllFilesPage(false);
            $state.go(STATE_SHARED_FILES);
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
                                $scope.showWatermarkResult($scope.watermarkStr);
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
    }
]);

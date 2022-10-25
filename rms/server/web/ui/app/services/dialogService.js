/*
  This is a shared service for app level config settings
*/
mainApp.factory('dialogService', ['$uibModal', '$rootScope', '$filter', '$state', 'serviceProviderService', 'repositoryService', 'networkService', 'initSettingsService',
    function($uibModal, $rootScope, $filter, $state, serviceProviderService, repositoryService, networkService, initSettingsService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var contentRightsAvailable = [{
                "id": "VIEW",
                "name": "View"
            },
            {
                "id": "PRINT",
                "name": "Print"
            }
        ];

        var collaborationRightsAvailable = [{
                "id": "SHARE",
                "name": "ReShare"
            },
            {
                "id": "DOWNLOAD",
                "name": "Save As"
            },
            {
                "id": "EDIT",
                "name": "Edit"
            }
        ];

        var hiddenSectionRights = [{
            "id": "DECRYPT",
            "name": "Extract"
        }];

        var effectRightsAvailable = [{
            "id": "WATERMARK",
            "name": "Watermark"
        }];

        var getContentRights = function() {
            return contentRightsAvailable;
        };

        var getCollaborationRights = function() {
            return collaborationRightsAvailable;
        };

        var getHiddenSectionRights = function() {
            return hiddenSectionRights;
        }

        var getEffectRights = function() {
            return effectRightsAvailable;
        };

        var confirm = function(parameter) {
            var title = parameter.title;
            var msg = parameter.msg;
            var ok = parameter.ok;
            var cancel = parameter.cancel;
            var selectedFile = parameter.selectedFile;
            var showCheckbox = parameter.showCheckbox;
            var setAttribute = parameter.setAttribute;
            var templateUrl = "";

            if (parameter.fromViewer) {
                templateUrl = '/viewer/ui/app/Home/SharedFiles/partials/dialog-confirm.html';
            } else if (parameter.newTemplate) {
                templateUrl = 'ui/app/templates/dialog-confirm-new.html';
            } else {
                templateUrl = 'ui/app/templates/dialog-confirm.html';
            }

            $uibModal.open({
                backdrop: 'static',
                keyboard: false,
                animation: true,
                windowClass: parameter.fromViewer ?  'app-modal-confirm-window' : '',
                templateUrl: templateUrl,
                controller: ['$uibModalInstance', '$scope', function($uibModalInstance, $scope) {
                    $scope.title = title;
                    $scope.msg = msg;
                    $scope.showCheckbox = showCheckbox;
                    $scope.setAttribute = setAttribute;
                    $scope.checkboxModel = false;
                    $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                            if ($scope.showCheckbox != undefined && $scope.showCheckbox.length > 0) {
                                var data = {
                                    checkboxModel: $scope.checkboxModel
                                };
                                ok && ok(data, selectedFile);
                            } else if (selectedFile) {
                                ok && ok(selectedFile);
                            } else {
                                ok && ok();
                            }
                        },
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                            cancel && cancel();
                        };
                }]
            });
        };
        var displayError = function(parameter) {
            var title = parameter.title;
            var msg = parameter.msg;
            var ok = parameter.ok;
            $uibModal.open({
                animation: true,
                backdrop: 'static',
                keyboard: false,
                templateUrl: 'ui/app/templates/dialog-error.html',
                controller: ['$uibModalInstance', '$scope', function($uibModalInstance, $scope) {
                    $scope.title = title;
                    $scope.msg = msg;
                    $scope.ok = function() {
                        $uibModalInstance.dismiss('cancel');
                        ok && ok();
                    };
                }]
            });
        };
        var info = function(parameter) {
            var ok = parameter.ok;
            var isProject = parameter.isProject;
            var isWorkspace = parameter.isWorkspace;
            var startDate = parameter.startDate;
            var endDate = parameter.endDate;
            var cancel = parameter.cancel;
            var tags = parameter.tags;
            var tagsExist = tags && Object.keys(tags).length > 0 ? true : false;
            var rights = parameter.rights;
            var nxl = parameter.nxl;
            var owner = parameter.owner;
            var shared = parameter.shared;
            var protectionType = parameter.protectionType;
            var fromTab = parameter.fromTab ? parameter.fromTab : "";
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/Repositories/partials/fileInfoTemplate.html',
                windowClass: 'app-modal-window',
                controller: ['$uibModalInstance', '$scope', function($uibModalInstance, $scope) {
                    $scope.tags = tags;
                    $scope.tagsExist = tagsExist;
                    $scope.rights = rights;
                    $scope.owner = owner;
                    $scope.nxl = nxl;
                    $scope.fileDetails = parameter;
                    $scope.shared = shared;
                    $scope.isProjectFile = isProject;
                    $scope.isWorkspaceFile = isWorkspace;
                    $scope.expiryInfo = true;
                    $scope.protectionType = protectionType;
                    $scope.selectedTab = "fileInfo";
                    var fullDateFormat = "dddd, mmmm d, yyyy";
                    if(startDate == undefined && endDate == undefined) {
                        $scope.expiration = $filter('translate')('never.expire');
                    } else if(startDate == undefined && endDate != undefined) {
                        $scope.expiration = $filter('translate')('until') + dateFormat(new Date(endDate), fullDateFormat);
                    } else if(startDate != undefined && endDate == undefined) {
                        $scope.expiration = $filter('translate')('from') + dateFormat(new Date(startDate), fullDateFormat);
                    } else {
                        $scope.expiration = dateFormat(new Date(startDate), fullDateFormat) + " - " + dateFormat(new Date(endDate), fullDateFormat);
                    }

                    $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                            ok && ok();
                    },
                    $scope.cancel = function() {
                        $uibModalInstance.dismiss('cancel');
                        cancel && cancel();
                    };

                    var byProjectName = function(a,b) {
                        if(a.name < b.name) { return -1; }
                        if(a.name > b.name) { return 1; }
                        return 0;
                    }

                    if (fromTab == "allShared" && $scope.isProjectFile) {
                        $scope.isSharedFromProjectTab = true;
                        $scope.shareWithProjects = parameter.shareWithProjects.sort(byProjectName);
                    } else {
                        $scope.isSharedFromProjectTab = false;
                    }

                    $scope.toggleTab = function(tab) {
                        $scope.selectedTab = tab;
                    };
                }]
            });
        };


        var getClassificationDetails = function(parameter) {
            var ok = parameter.ok;
            var cancel = parameter.cancel;
            var tags = parameter.tags;
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/Repositories/partials/modifyRightsTemplate.html',
                windowClass: 'app-modal-window',
                controller: ['$uibModalInstance', '$scope', function($uibModalInstance, $scope) {
                    $scope.tags = tags;
                    $scope.fileDetails = parameter;
                    $scope.ok = function() {
                        $uibModalInstance.dismiss('cancel');
                        ok && ok();
                    };
                    $scope.cancel = function() {
                        $uibModalInstance.dismiss('cancel');
                        cancel && cancel();
                    };
                }]
            });
        }

        var addServiceProviderConfiguration = function(parameters) {
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/settings/partials/addServiceProvider.html',
                windowClass: 'app-modal-window app-modal-window-mobile configure-service-provider',
                controller: ['$uibModalInstance', '$scope', 'serviceProviderService', '$filter',
                    function($uibModalInstance, $scope, serviceProviderService, $filter) {
                        // Init default data
                        $scope.serviceProvider = parameters.serviceProvider;
                        $scope.providerName = parameters.providerName;
                        $scope.showAppSecret = true;
                        $scope.redirectUrl = window.location.origin + '/rms';
                        $scope.formData = {
                            displayName: '',
                            appId: '',
                            appSecret: '',
                            redirectUrl: $scope.redirectUrl,
                            directoryId: '',
                            siteUrl: '',
                            driveName: '',
                            enabled: true,
                        };

                        $scope.errorMessage = '';

                        // This block sets the default selected provider class to the first unconfigured provider class when the dialog opens
                        // APPLICATION providerClass can be configured infinitely
                        for (var i = 0; i < $scope.serviceProvider.supportedClasses.length; i++) {
                            var providerClass = $scope.serviceProvider.supportedClasses[i];
                            if (providerClass !== 'APPLICATION' && !$scope.serviceProvider[providerClass].configured || providerClass == 'APPLICATION') {
                                $scope.selectedProviderClass = providerClass;
                                break;
                            }
                        }

                        // Setting up the help info
                        $scope.appIdInfo = $filter('translate')('sp.app_id.info', {serviceProvider: $scope.serviceProvider.displayName});
                        $scope.appSecretInfo = $filter('translate')('sp.app_secret.info', {serviceProvider: $scope.serviceProvider.displayName});
                        $scope.displayNameInfo = $filter('translate')('sp.display_name.info');
                        // TODO get proper copy from Irene for this toggle's info message
                        $scope.appEnableToggleInfo = $filter('translate')('If enabled, users will be able to connect to this repository.');

                        // PUBLIC METHODS BELOW

                        $scope.switchProviderClass = function(providerClass) {
                            if($scope.selectedProviderClass != providerClass) {
                                $scope.selectedProviderClass = providerClass;
                                $scope.formData = freshForm($scope.redirectUrl);
                                $scope.errorMessage = "";
                            }
                        };

                        $scope.save = function() {
                            if($scope.configurationForm.$pristine || $scope.configurationForm.$invalid) {
                                return;
                            }

                            $scope.isLoading = true;
                            $scope.errorMessage = "";

                            var attributes = {};
                            if($scope.selectedProviderClass === 'PERSONAL') {
                                attributes = {
                                    "APP_ID": $scope.formData.appId,
                                    "APP_SECRET": $scope.formData.appSecret,
                                    "REDIRECT_URL": $scope.formData.redirectUrl
                                };
                            } else {
                                attributes = {
                                    "DISPLAY_NAME": $scope.formData.displayName,
                                    "APP_ID": $scope.formData.appId,
                                    "APP_SECRET": $scope.formData.appSecret,
                                    "APP_TENANT_ID": $scope.formData.directoryId,
                                    "SITE_URL": $scope.formData.siteUrl,
                                    "DRIVE_NAME": $scope.formData.driveName
                                };
                            }                

                            var payload = {
                                "parameters": {
                                    "serviceProvider": {
                                        "tenantId": readCookie('tenantId'),
                                        "providerType": $scope.serviceProvider[$scope.selectedProviderClass].type,
                                        "attributes": attributes
                                    }
                                }
                            };

                            serviceProviderService.addServiceProviderConfiguration(payload, function(response) {
                                if(response.statusCode === 200) {
                                    serviceProviderService.setMessageParams(true, true, response.message);
                                    $rootScope.$emit('reloadSettings');
                                    $uibModalInstance.close(true);
                                } else {
                                    $scope.errorMessage = response.message;
                                }
                                $scope.isLoading = false;
                            });
                        };

                        $scope.cancel = function() {
                            $scope.messageStatus = 1;
                            $uibModalInstance.dismiss('cancel');
                        };

                        $scope.setFormDirty = function() {
                            $scope.configurationForm.$setDirty();
                        };

                        // PRIVATE METHODS BELOW (PURE FUNCTIONS RECOMMENDED)

                        var freshForm = function(redirectUrl) {
                            var form = {
                                displayName: "",
                                appId: "",
                                appSecret: "",
                                redirectUrl: redirectUrl,
                                directoryId: "",
                                enabled: true, 
                            };
                            return form;
                        };
                    }
                ]
            });
        };

        var editServiceProviderConfiguration = function(parameters) {
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/settings/partials/editServiceProvider.html',
                windowClass: 'app-modal-window app-modal-window-mobile configure-service-provider',
                controller: ['$uibModalInstance', '$scope', 'serviceProviderService', '$filter',
                    function($uibModalInstance, $scope, serviceProviderService, $filter) {
                        // Init default data
                        $scope.serviceProvider = parameters.serviceProvider;

                        $scope.providerName = $scope.serviceProvider.provider;
                        $scope.showAppSecret = false;
                        $scope.formData = {
                            displayName: $scope.serviceProvider.attributes.DISPLAY_NAME,
                            appId: $scope.serviceProvider.attributes.APP_ID,
                            appSecret: $scope.serviceProvider.attributes.APP_SECRET,
                            redirectUrl: $scope.serviceProvider.attributes.REDIRECT_URL,
                            directoryId: $scope.serviceProvider.attributes.APP_TENANT_ID,
                            siteUrl: $scope.serviceProvider.attributes.SITE_URL,
                            driveName: $scope.serviceProvider.attributes.DRIVE_NAME,
                            enabled: true, // TODO: hardcoded true until ready
                        };

                        $scope.errorMessage = "";                        

                        // Setting up the help info
                        $scope.appIdInfo = $filter('translate')('sp.app_id.info', {serviceProvider: $scope.serviceProvider.provider});
                        $scope.appSecretInfo = $filter('translate')('sp.app_secret.info', {serviceProvider: $scope.serviceProvider.provider});
                        $scope.displayNameInfo = $filter('translate')('sp.display_name.info');
                        // TODO get proper copy from Irene for this toggle's info message
                        $scope.appEnableToggleInfo = $filter('translate')('If enabled, users will be able to connect to this repository.');

                        // PUBLIC METHODS BELOW
                        $scope.update = function() {
                            if($scope.configurationForm.$pristine || $scope.configurationForm.$invalid) {
                                return;
                            }

                            $scope.isLoading = true;
                            $scope.errorMessage = "";

                            var attributes = {};
                            if($scope.serviceProvider.providerClass === 'PERSONAL') {
                                attributes = {
                                    "APP_ID": $scope.formData.appId,
                                    "APP_SECRET": $scope.formData.appSecret,
                                    "REDIRECT_URL": $scope.formData.redirectUrl
                                };
                            } else {
                                attributes = {
                                    "DISPLAY_NAME": $scope.formData.displayName,
                                    "APP_ID": $scope.formData.appId,
                                    "APP_SECRET": $scope.formData.appSecret,
                                    "APP_TENANT_ID": $scope.formData.directoryId,
                                    "SITE_URL": $scope.formData.siteUrl,
                                    "DRIVE_NAME": $scope.formData.driveName
                                };
                            }

                            var payload = {
                                "parameters": {
                                    "serviceProvider": {
                                        "id": $scope.serviceProvider.id,
                                        "tenantId": readCookie('tenantId'),
                                        "providerType": $scope.serviceProvider.providerType,
                                        "attributes": attributes
                                    }
                                }
                            };

                            serviceProviderService.updateServiceProviderConfiguration(payload, function(response) {
                                if(response.statusCode === 200) {
                                    serviceProviderService.setMessageParams(true, true, response.message);
                                    $rootScope.$emit('reloadSettings');
                                    $uibModalInstance.close(true);
                                } else {
                                    $scope.errorMessage = response.message;
                                }
                                $scope.isLoading = false;
                            });
                        };

                        $scope.showAppSecretField = function() {
                            $scope.showAppSecret = true;
                        };

                        $scope.cancel = function() {
                            $scope.messageStatus = 1;
                            $uibModalInstance.dismiss('cancel');
                        };

                        $scope.delete = function() {
                            $scope.isLoading = true;
                            $scope.errorMessage = "";

                            confirm({
                                msg: $filter('translate')('service_provider.delete.confirm'),
                                ok: function() {
                                    var payload = {
                                        "parameters": {
                                            "id": $scope.serviceProvider.id
                                        }
                                    };
                
                                    serviceProviderService.deleteServiceProviderConfiguration(payload, function(data) {
                                        if (data.statusCode === 204) {
                                            serviceProviderService.setMessageParams(true, true, data.message);
                                            $rootScope.$emit("reloadSettings");
                                            $uibModalInstance.close();
                                        } else {
                                            serviceProviderService.setMessageParams(true, false, data.message);
                                        }
                                        $scope.isLoading = false;
                                    });
                                },
                                cancel: function() {
                                    $scope.isLoading = false;
                                }
                            });
                        };

                        $scope.setFormDirty = function() {
                            $scope.configurationForm.$setDirty();
                        };
                    }
                ]
            });
        };

        var editRepository = function(parameters, successCallback, errorCallback) {

            var REPO_NAME_MAX_LENGTH = 40;

            $uibModal.open({
                animation: true,
                // template: msg,
                templateUrl: 'ui/app/Home/Repositories/partials/editRepository.html',
                windowClass: 'app-modal-window',
                controller: ['$uibModalInstance', '$scope', '$filter', function($uibModalInstance, $scope, $filter) {
                    $scope.repository = angular.copy(parameters.repository);
                    $scope.repoNmaeOrigin = parameters.repository.repoName;
                    $scope.nameNotChanged = true;
                    $scope.repoNameShortened = repositoryService.getShortRepoName($scope.repository.repoName, REPO_NAME_MAX_LENGTH);
                    $scope.cancel = function() {
                        $uibModalInstance.dismiss('cancel');
                    };

                    $scope.updateRepository = function(repository) {
                        $scope.isLoading = true;
                        repositoryService.updateRepository(repository, $scope.repository.repoName, function(data) {
                            $scope.isLoading = false;
                            if (successCallback && typeof(successCallback) == "function") {
                                $uibModalInstance.dismiss('cancel');
                                repositoryService.setMessageParams(true, data.result, data.message);
                                successCallback(data);
                            }
                        }, function(response) {
                            $scope.isLoading = false;
                            if (errorCallback && typeof(errorCallback) == "function") {
                                repositoryService.setMessageParams(true, false, $filter('translate')('managerepo.update.error'));
                                errorCallback(response);
                            }
                        });
                    };

                    $scope.$watch('repository.repoName', function(newValue) {
                        $scope.nameNotChanged = newValue === $scope.repoNmaeOrigin;
                    })

                    $scope.deleteRepository = function(repository) {
                        confirm({
                            msg: $filter('translate')('managerepo.delete.confirm'),
                            ok: function() {
                                repositoryService.removeRepository(repository.repoId, function(data) {
                                    if (successCallback && typeof(successCallback) == "function") {
                                        $uibModalInstance.dismiss('cancel');
                                        repositoryService.setMessageParams(true, data.result, data.message);
                                        successCallback(data);
                                    }
                                });
                            },
                            cancel: function() {}
                        });
                    };
                }]
            });
        };

        var addRepository = function(parameters, callback) {
            var params = {};
            var confirmationMsg = "";
            var allowedServiceProvidersList = [];

            var allowedServiceProvidersMap = parameters.allowedRepository;
            for (var value in allowedServiceProvidersMap) {
                var allowedServiceProvider = {
                    key: "",
                    serviceProvider: ""
                };
                allowedServiceProvider.key = value;
                allowedServiceProvider.serviceProvider = allowedServiceProvidersMap[value];
                allowedServiceProvidersList.push(allowedServiceProvider);
            }
            var addFailed = false;
            var addRepositoryModal = $uibModal.open({
                animation: true,
                // template: msg,
                templateUrl: 'ui/app/Home/Repositories/partials/addRepository.html',
                windowClass: 'app-modal-window',
                controller: ['$uibModalInstance', '$scope', '$filter', function($uibModalInstance, $scope, $filter) {
                    $scope.change = function(serviceProviderId) {
                        $scope.displayName = "";
                        $scope.sitesUrl = "";
                        $scope.button = serviceProviderId;
                        $scope.selectedServiceProvider = allowedServiceProvidersMap[serviceProviderId];
                    };
                    $scope.displayName = "";
                    $scope.selectedServiceProvider = allowedServiceProvidersList[0].serviceProvider;
                    $scope.button = allowedServiceProvidersList[0].key;
                    $scope.change(allowedServiceProvidersList[0].key);
                    $scope.sitesUrl = "";
                    $scope.allowedServiceProvidersList = allowedServiceProvidersList;
                    $scope.cancel = function() {
                        $uibModalInstance.dismiss('cancel');
                    };
                    $scope.connectRepository = function() {
                        params.repoName = $scope.displayName;
                        if ($scope.button == 'GOOGLE_DRIVE') {
                            params.repoType = $scope.button;
                            confirmationMsg = $filter('translate')('managerepo.redirect.google');
                        } else if ($scope.button == 'DROPBOX') {
                            params.repoType = $scope.button;
                            confirmationMsg = $filter('translate')('managerepo.redirect.dropbox');
                        } else if ($scope.button == 'ONE_DRIVE') {
                            params.repoType = $scope.button;
                            confirmationMsg = $filter('translate')('managerepo.redirect.onedrive');
                        } else if ($scope.button == 'BOX') {
                          params.repoType = $scope.button;
                          confirmationMsg = $filter('translate')('managerepo.redirect.box');
                        }
                        confirm({
                            msg: confirmationMsg,
                            ok: function() {
                                repositoryService.addRepository(params, function(data) {
                                    $scope.isLoading = false;
                                    $uibModalInstance.dismiss('cancel');
                                    addFailed = !data.result;
                                    repositoryService.setMessageParams(true, data.result, data.message);
                                });
                            },
                            cancel: function() {}
                        });
                    };
                }]
            });

            addRepositoryModal.result.then(function() {
                if (parameters.fromState != null && parameters.fromState != 'undefined' && addFailed == false) {
                    $state.go(parameters.fromState);
                }
            }, function() {
                if (parameters.fromState != null && parameters.fromState != 'undefined' && addFailed == false) {
                    $state.go(parameters.fromState);
                }
            });
        };
        var submitFeedBack = function(parameter, successCallback, errorCallback) {
            var params = {};
            $uibModal.open({
                animation: true,
                // template: msg,
                templateUrl: 'ui/app/Home/Profile/feedback.html',
                windowClass: 'app-modal-window',
                controller: ['$uibModalInstance', '$scope', 'Upload', '$timeout', function($uibModalInstance, $scope, Upload, $timeout) {
                    var initData = initSettingsService.getSettings();
                    $scope.supportedAttachmentFormats = [""];
                    $scope.feedbackType = ["Feedback", "Report Issue"];
                    $scope.feedbackSelectedType = "Feedback";
                    $scope.ok = function() {
                            $uibModalInstance.dismiss('cancel');
                        },
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                    $scope.changeType = function(type) {
                        $scope.feedbackSelectedType = type;
                    }
                    $scope.submitFeedback = function(form) {
                        params.feedbackType = $scope.feedbackSelectedType;
                        params.summary = $scope.summary;
                        params.description = $scope.description;
                        $scope.isLoading = true;
                        var uploader = Upload.upload({
                            url: "RMSViewer/SubmitFeedback",
                            data: {
                                file: params.file
                            },
                            params: {
                                feedbackType: params.feedbackType,
                                feedbackSummary: params.summary,
                                feedbackDescription: params.description
                            }
                        });
                        uploader.then(function(response) {
                            $scope.isLoading = false;
                            if (response.data.result) {
                                $scope.messageStatus = 2;
                            } else {
                                $scope.messageStatus = 1;
                            }
                            $scope.message = response.data.message;
                            $scope.resetFeedbackForm(form);
                        }, errorHandler);
                    }
                    $scope.resetFeedbackForm = function(form) {
                        $scope.summary = "";
                        $scope.description = "";
                        $scope.attachmentName = "";
                        $scope.isFileAttached = false;
                        params.file = null;
                        form.$setUntouched();
                    }
                    $scope.$watch('files', function() {
                        $scope.upload($scope.files);
                    });
                    $scope.upload = function(file, errFiles) {
                        if (file) {
                            $scope.fileSizeBig = false;
                            $scope.unsupportedtype = false;
                            $scope.attachmentName = "";
                            $scope.isFileAttached = false;
                            if (file.size > 2000000) {
                                $scope.fileSizeBig = true;
                                return;
                            }
                            if (!($.inArray("." + file.name.toLowerCase().split('.').pop(), $scope.supportedAttachmentFormats) >= 0)) {
                                $scope.unsupportedtype = true;
                                return;
                            }
                            params.file = file;
                            $scope.attachmentName = file.name;
                            if (file.name.length > 40) {
                                var fileName = file.name.substring(0, 40) + "..."
                                $scope.attachmentName = fileName;
                            }
                            $scope.isFileAttached = true;
                        }
                    }
                    $scope.removeFile = function() {
                        $scope.attachmentName = "";
                        $scope.isFileAttached = false;
                        params.file = null;
                    }
                }]
            });
        };


        var displayProfile = function() {
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/Profile/profile.html',
                windowClass: 'app-modal-window-centered',
                controller: 'manageProfileController'
            });
        }

        function setTooltip(data, $scope, $filter) {

            if (data.settings.providerType === "ONE_DRIVE") {
                $scope.appIdInfo = $filter('translate')('onedrive.app_id.info');
                $scope.appSecretInfo = $filter('translate')('onedrive.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('onedrive.redirect_url.info');
            } else if (data.settings.providerType === "GOOGLE_DRIVE") {
                $scope.appIdInfo = $filter('translate')('googledrive.app_id.info');
                $scope.appSecretInfo = $filter('translate')('googledrive.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('googledrive.redirect_url.info');
            } else if (data.settings.providerType === "DROPBOX") {
                $scope.appIdInfo = $filter('translate')('dropbox.app_id.info');
                $scope.appSecretInfo = $filter('translate')('dropbox.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('dropbox.redirect_url.info');
            } else if (data.settings.providerType === "BOX") {
                $scope.appIdInfo = $filter('translate')('box.app_id.info');
                $scope.appSecretInfo = $filter('translate')('box.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('box.redirect_url.info');
            } else if (data.settings.providerType === "SHAREPOINT_ONLINE") {
                $scope.appIdInfo = $filter('translate')('spol.app_id.info');
                $scope.appSecretInfo = $filter('translate')('spol.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('spol.redirect_url.info');
            } else if (data.settings.providerType === "SHAREPOINT_ONLINE_CROSSLAUNCH") {
                $scope.appIdInfo = $filter('translate')('spol_cl.app_id.info');
                $scope.appSecretInfo = $filter('translate')('spol_cl.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('spol_cl.redirect_url.info');
                $scope.appDisplayMenu = $filter('translate')('spol_cl.app_display_menu.info');
            } else if (data.settings.providerType === "SHAREPOINT_CROSSLAUNCH") {
                $scope.appIdInfo = $filter('translate')('sp_cl.app_id.info');
                $scope.appSecretInfo = $filter('translate')('sp_cl.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('sp_cl.redirect_url.info');
                $scope.appDisplayMenu = $filter('translate')('sp_cl.app_display_menu.info');
            }
        }


        function setTooltip(data, $scope, $filter) {

            if (data.settings.providerType === "ONE_DRIVE") {
                $scope.appIdInfo = $filter('translate')('onedrive.app_id.info');
                $scope.appSecretInfo = $filter('translate')('onedrive.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('onedrive.redirect_url.info');
            } else if (data.settings.providerType === "GOOGLE_DRIVE") {
                $scope.appIdInfo = $filter('translate')('googledrive.app_id.info');
                $scope.appSecretInfo = $filter('translate')('googledrive.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('googledrive.redirect_url.info');
            } else if (data.settings.providerType === "DROPBOX") {
                $scope.appIdInfo = $filter('translate')('dropbox.app_id.info');
                $scope.appSecretInfo = $filter('translate')('dropbox.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('dropbox.redirect_url.info');
            } else if (data.settings.providerType === "BOX") {
                $scope.appIdInfo = $filter('translate')('box.app_id.info');
                $scope.appSecretInfo = $filter('translate')('box.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('box.redirect_url.info');
            } else if (data.settings.providerType === "SHAREPOINT_ONLINE") {
                $scope.appIdInfo = $filter('translate')('spol.app_id.info');
                $scope.appSecretInfo = $filter('translate')('spol.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('spol.redirect_url.info');
            } else if (data.settings.providerType === "SHAREPOINT_ONLINE_CROSSLAUNCH") {
                $scope.appIdInfo = $filter('translate')('spol_cl.app_id.info');
                $scope.appSecretInfo = $filter('translate')('spol_cl.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('spol_cl.redirect_url.info');
                $scope.appDisplayMenu = $filter('translate')('spol_cl.app_display_menu.info');
            } else if (data.settings.providerType === "SHAREPOINT_CROSSLAUNCH") {
                $scope.appIdInfo = $filter('translate')('sp_cl.app_id.info');
                $scope.appSecretInfo = $filter('translate')('sp_cl.app_secret.info');
                $scope.appRedirectUrl = $filter('translate')('sp_cl.redirect_url.info');
                $scope.appDisplayMenu = $filter('translate')('sp_cl.app_display_menu.info');
            }
        }

        var addIDPDialog = function (parameters, saveCallback) {
            var allowedIDPList = parameters.allowedIDPs;
            var selectedIDP = parameters.selectedIDP;
            var selectedIDPId = parameters.id;
            var selectedIDPType = parameters.type;
            var selectedIDPName = parameters.name;
            $uibModal.open({
                animation: true,
                templateUrl: 'ui/app/Home/settings/partials/addIDP.html',
                windowClass: 'app-modal-window app-modal-window-mobile',
                controller: ['$uibModalInstance', '$scope', '$filter',
                    function($uibModalInstance, $scope, $filter) {
                        $scope.idpFormData = {};
                        if (allowedIDPList && allowedIDPList.length > 0) {
                            $scope.allowedIDPList = allowedIDPList;
                            $scope.isAdd = true;
                        } else {
                            $scope.selectedIDP = selectedIDP;
                            $scope.isAdd = false;
                            $scope.idpFormData[selectedIDPName] = JSON.parse(JSON.stringify($scope.selectedIDP));
                            $scope.selectedIDP.name = selectedIDPName;
                        }
                        $scope.change = function(index) {
                            if (!$scope.isAdd) {
                                return;
                            }
                            $scope.selectedIDP = parameters.allowedIDPs[index];
                            $scope.count = {
                                count: $scope.selectedIDP.count
                            };
                            $scope.idpFormData[$scope.selectedIDP.name] = {};
                        };
                        $scope.cancel = function() {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.save = function () {
                            $scope.isLoading = true;
							
                            // Clear the signup url if approval is disabled
                            if($scope.selectedIDP.name == 'Azure') {
                            	if(!$scope.idpFormData.Azure.enableApproval) {
                                    $scope.idpFormData.Azure.signupUrl = '';
                            	}
                            } else if($scope.selectedIDP.name == 'Facebook') {
                            	if(!$scope.idpFormData.Facebook.enableApproval) {
                                    $scope.idpFormData.Facebook.signupUrl = '';
                            	}
                            } else if($scope.selectedIDP.name == 'Google') {
                            	if(!$scope.idpFormData.Google.enableApproval) {
                                    $scope.idpFormData.Google.signupUrl = '';	
                            	}                            	
                            }
							
                            if ($scope.isAdd) {
                                var params = {
                                    parameters: {
                                        idp: {
                                            type: $scope.selectedIDP.type,
                                            attributes: JSON.stringify($scope.idpFormData[$scope.selectedIDP.name])
                                        }
                                    }
                                };
                                networkService.put(RMS_CONTEXT_NAME + "/rs/idp/" + window.readCookie("ltId"), params, getJsonHeaders(), function (data) {
                                    if (data.statusCode == 200) {
                                        $scope.selectedIDP = {};
                                        $scope.messageStatus = 2;
                                        $scope.message = $filter('translate')('idp.add.success');
                                        saveCallback();
                                    } else {
                                        $scope.messageStatus = 1;
                                        $scope.message = $filter('translate')('idp.add.error');
                                    }
                                    $scope.isLoading = false;
                                });
                            } else {
                                var params = {
                                    parameters: {
                                        idp: {
                                            id: selectedIDPId,
                                            type: selectedIDPType,
                                            attributes: JSON.stringify($scope.idpFormData[selectedIDPName])
                                        }
                                    }
                                };
                                networkService.post(RMS_CONTEXT_NAME + "/rs/idp/" + window.readCookie("ltId"), params, getJsonHeaders(), function (data) {
                                    if (data.statusCode == 200) {
                                        $scope.messageStatus = 2;
                                        $scope.message = $filter('translate')('idp.edit.success');
                                        saveCallback();
                                    } else {
                                        $scope.messageStatus = 1;
                                        $scope.message = $filter('translate')('idp.edit.error');
                                    }
                                    $scope.isLoading = false;
                                });
                            }
                        };
                    }
                ]
            });
        };

        return {
            getContentRights: getContentRights,
            getCollaborationRights: getCollaborationRights,
            getEffectRights: getEffectRights,
            getClassificationDetails: getClassificationDetails,
            confirm: confirm,
            displayError: displayError,
            info: info,
            // repositoryConfig: repositoryConfig,
            addServiceProviderConfiguration: addServiceProviderConfiguration,
            editServiceProviderConfiguration: editServiceProviderConfiguration,
            editRepository: editRepository,
            addRepository: addRepository,
            submitFeedBack: submitFeedBack,
            displayProfile: displayProfile,
            addIDPDialog: addIDPDialog,
            getHiddenSectionRights: getHiddenSectionRights
        }
    }
]);
mainApp.controller('sharedFileListController', ['$scope', '$state', 'networkService', 'dialogService', 'shareDialogService', '$location', '$filter', '$rootScope',
    'initSettingsService', 'serviceProviderService', 'shareFileService', 'repositoryService', 'projectService', 'protectWidgetService',
    function($scope, $state, networkService, dialogService, shareDialogService, $location, $filter, $rootScope,
        initSettingsService, serviceProviderService, shareFileService, repositoryService, projectService, protectWidgetService) {

        var PAGE = 1;
        var SIZE = 10;

        $scope.menuClickedMode = false;
        $scope.page = PAGE;
        $scope.showSearch = false;
        $scope.showShareFilter = false;
        $scope.showSort = false;
        $scope.searchActivated = false;
        $scope.showMore = true;
        $scope.emptyFileList = false;
        $scope.expiryInfo = false;
        $scope.allProjectList = [];
        var isSearchCalled = false;
        var parentFolderId = "/nxl_myvault_nxl/";
        $scope.repoList = [];
        $scope.showSharedWithMe = STATE_SHARED_ACTIVE_FILES === $state.current.name ? true : false;

        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var params = {};
        $scope.messages = [];

        var refreshSharedFileList = function(page) {
            $scope.isLoading = true;
            $scope.showMore = true;
            params.page = PAGE;
            params.size = SIZE * $scope.page;
            params.orderBy = $scope.selectedSort;
            params.fileName = $scope.searchString;
            params.filterOptions = $scope.selectedFilter;

            if ($scope.showSharedWithMe) {
                shareFileService.getSharedWithMeFiles(params, setData);
            } else {
                if ($state.current.name === STATE_SHARED_FILES) {
                    repositoryService.getMyDriveUsage(function(data) {
                        $scope.usage = data.results.usage;
                        $scope.quota = data.results.quota;
                        $scope.myVaultUsage = data.results.myVaultUsage;
                    });
                }
                shareFileService.getAllSharedFiles(params, setData);
            }
        }
        var getRepoList = function() {
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

        $scope.$watch("usage", function() {
            $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.quota) + '%';
            $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.quota) + '%';
        });

        $scope.$watch("myVaultUsage", function() {
            $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage, $scope.quota) + '%';
            $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.quota) + '%';
        });
        
        var truncateSharedWithList = function(contents) {
            contents.forEach(function(file) {
                if (file.hasOwnProperty("sharedWith") && file.sharedWith.length > 0) {
                    var sharedWithString = '';
                    var idx=0;
                    var maxDisplay=2;
                    for (var i=0; i<file.sharedWith.length; i++) {
                        sharedWithString += file.sharedWith[i];
                        if (idx < maxDisplay - 1 && file.sharedWith.length > 1) {
                            sharedWithString += ', ';
                        }
                        if (++idx >= maxDisplay) {
                            break;
                        }
                    }
                    if (file.sharedWith.length > maxDisplay) {
                        sharedWithString += ' and ';
                        sharedWithString += (file.sharedWith.length - maxDisplay);                            
                        sharedWithString += ' others ';
                    }                    
                    file.sharedWith = sharedWithString;
                }
            });
        }

        var setData = function(data) {
            var contents = data.results.detail.files;
            if (contents.length == 0) {
                if (!$scope.showSharedWithMe) {
                    $scope.emptyFileList = true;
                    $scope.showWidgetIfEmpty = true;

                    if ($scope.selectedFilter === $scope.filterOptions[1].lookupCode) {
                        $scope.isActiveFilter = true;
                    } else if ($scope.selectedFilter === $scope.filterOptions[2].lookupCode || $scope.selectedFilter === $scope.filterOptions[0].lookupCode) {
                        $scope.isActiveFilter = false;
                    } else if ($scope.selectedFilter === $scope.filterOptions[3].lookupCode) {
                        $scope.showWidgetIfEmpty = false;
                        $scope.isDeletedFilter = true;
                    } else if ($scope.selectedFilter === $scope.filterOptions[4].lookupCode) {
                        $scope.showWidgetIfEmpty = false;
                        $scope.isDeletedFilter = false;
                    }
                }

                if (isSearchCalled) {
                    if ($scope.searchString && $.trim($scope.searchString) != "") {
                        $scope.emptyFileList = false; // set as false in order to prevent both messages to appear at the same time
                    }
                    isSearchCalled = false;
                }
            } else {
                isSearchCalled = false; // set as false because we do not want "no search" result appearing when one selects options: protected, active share, all share. Instead respective no results msg should be displayed
                $scope.emptyFileList = false;
            }
            if (contents.length < SIZE) {
                $scope.showMore = false;
            }
            truncateSharedWithList(contents);
            if (!$scope.showSharedWithMe) {
                contents.forEach(function(file) {
                    if (file.revoked && !file.deleted) {
                        file.title = $filter('translate')('share.file.revoked');
                    } else if (file.deleted) {
                        file.title = $filter('translate')('share.file.delete.title');
                    }
                });
            }
            $scope.repoContents = contents;
            $scope.isLoading = false;
        }
        var loadFiles = function(data) {
            var contents = data.results.detail.files;
            if (contents.length == 0 || contents.length < SIZE) {
                $scope.showMore = false;
            }
            truncateSharedWithList(contents);
            $scope.repoContents = $scope.repoContents.concat(contents);
            $scope.isLoading = false;
        }

        $scope.onClickMenu = function(file) {
            $scope.selectedFileId = file.pathId ? file.pathId : file.duid;
            $scope.toggleMenuMode();
        }

        $scope.showMenu = function(file) {
            $scope.selectedFileId = file.pathId ? file.pathId : file.duid;
            $scope.menuClickedMode = true;
        }

        $scope.hideMenu = function(file) {
            $scope.selectedFileId = file.pathId ? file.pathId : file.duid;
            $scope.menuClickedMode = false;
        }

        $scope.toggleMenuMode = function() {
            $scope.menuClickedMode = !$scope.menuClickedMode;
        }

        $scope.clearSearch = function() {
            $scope.searchString = "";
            this.$parent.searchString = "";
            $scope.search();
            $scope.searchActivated = false;
        }

        $scope.loadMoreSharedFiles = function() {
            $scope.isLoading = true;
            $scope.page = $scope.page + 1;
            params.page = $scope.page;
            params.size = SIZE;
            params.sFields = $scope.selectedSort;
            params.sString = $scope.searchString;
            params.filterOptions = $scope.selectedFilter;
            if ($scope.showSharedWithMe) {
                shareFileService.getSharedWithMeFiles(params, loadFiles);
            } else {
                shareFileService.getAllSharedFiles(params, loadFiles);
            }
        }
        $scope.sortOptions = shareFileService.getSortOptions($scope.showSharedWithMe);
        $scope.filterOptions = [{
                'lookupCode': 'allFiles',
                'description': 'myvault.filter.allFiles'
            },
            {
                'lookupCode': 'activeTransaction',
                'description': 'myvault.filter.activeTransaction'
            },
            {
                'lookupCode': 'protected',
                'description': 'myvault.filter.protected'
            },
            {
                'lookupCode': 'deleted',
                'description': 'myvault.filter.deleted'
            },
            {
                'lookupCode': 'revoked',
                'description': 'myvault.filter.revoked'
            }
        ];
        if ($state.current.name === STATE_DELETED_FILES) {
            $scope.selectedFilter = $scope.filterOptions[3].lookupCode;
        } else if ($state.current.name === STATE_SHARED_ACTIVE_FILES) {
            $scope.selectedFilter = $scope.filterOptions[1].lookupCode;
        } else {
            $scope.selectedFilter = $scope.filterOptions[0].lookupCode;
        }
        $scope.selectedSort = $scope.sortOptions[0].lookupCode;

        $scope.showAllServices = true;
        $scope.showTree = false;
        $scope.searchString = null;
        params.page = $scope.page;
        params.size = SIZE;
        params.sFields = $scope.selectedSort;
        params.filterOptions = $scope.selectedFilter;
        refreshSharedFileList();
        if ($state.current.name === STATE_SHARED_FILES || $state.current.name === STATE_SHARED_ACTIVE_FILES) {
            getRepoList();
        }

        $scope.viewFileDetails = function(file) {
            $scope.isLoading = true;
            $scope.duid = file.duid;
            $scope.filePathId = file.pathId;
            shareFileService.getSharedFileDetails(file, function(data) {
                $scope.isLoading = false;
                if (data.statusCode == 200) {
                    shareDialogService.viewSharedFileDetails({
                        duid: $scope.duid,
                        pathId: $scope.filePathId,
                        file: data.results.detail
                    });
                } else {
                    showSnackbar({
                        messages: $filter('translate')('manage.file.error'),
                        isSuccess: false
                    });
                }
            });
            $scope.toggleMenuMode();
        }

        $scope.onClickShare = function(file) {
            $scope.isLoading = true;
            $scope.duid = file.duid;
            shareFileService.getSharedFileDetails(file, function(data) {
                $scope.isLoading = false;
                if (data.statusCode == 200) {
                    var result = data.results.detail;
                    var myVaultFile = {
                        name: file.name,
                        path: file.pathDisplay,
                        pathId: file.pathId,
                        repoId: file.repoId,
                        repoName: file.repoName
                    };
                    shareDialogService.shareFile({
                        file: myVaultFile,
                        rights: result.rights,
                        owner: true,
                        nxl: true,
                        operation: "share",
                        startDate: result.validity.startDate,
                        endDate: result.validity.endDate,
                        protectionType: result.protectionType,
                    }, refreshSharedFileList);
                } else {
                    showSnackbar({
                        messages: $filter('translate')('manage.file.error'),
                        isSuccess: false
                    });
                }
            });
            $scope.toggleMenuMode();
        }

        $scope.toggleAllMenuMode = function(file) {
            for (var i = 0; i < $scope.recentFiles.length; i++) {
                if ($scope.recentFiles[i].duid!=file.duid) {
                    $scope.recentFiles[i].isMenuClicked=false;
                } else {
                    $scope.recentFiles[i].isMenuClicked=!$scope.recentFiles[i].isMenuClicked;
                }
            }
        }

        $scope.onClickMenuGetPermissions = function(file, spaceId, fromSpaceId) {
            $scope.selectedFileId = file.duid;
            $scope.isLoading = true;
            $scope.toggleAllMenuMode(file);
            shareFileService.getSharedWithMeFileDetails(file, spaceId , function(data){
                $scope.isLoading = false;
                var error;
                $scope.data = data;
                if(data.statusCode == 200) {
                    $scope.rights = data.results.detail.rights;
                    $scope.protectionType = data.results.detail.protectionType;
                    
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

        $scope.onClickSharedWithMeFileInfo = function(file, spaceId, fromSpaceId) {
            $scope.isLoading = true;
            shareFileService.getSharedWithMeFileDetails(file, spaceId, function(data){
                $scope.isLoading = false;
                var isProject ;
                if(fromSpaceId == "PROJECTSPACE"){ 
                    isProject = true;
                }
                if(data.statusCode === 200 && data.results.detail) {
                    metadata = data.results.detail;
                    dialogService.info({
                        rights: metadata.rights,
                        startDate: metadata.validity.startDate,
                        endDate: metadata.validity.endDate,
                        owner: metadata.isOwner,
                        nxl: true,
                        shared: true,
                        fileName: metadata.name,
                        fileSize: file.size,
                        fileType: metadata.fileType,
                        tags: metadata.tags,
                        lastModifiedTime: metadata.sharedDate,
                        protectionType: metadata.protectionType, 
                        isProject : isProject
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('operation.unauthorized')
                    });
                    return;
                }
            });
        }

        $scope.onClickNxlFileInfo = function(file) {
            $scope.isLoading = true;
            shareFileService.getSharedFileDetails(file, function(data) {
                $scope.isLoading = false;
                if (data.statusCode == 200) {
                    dialogService.info({
                        rights: file.rights,
                        owner: true,
                        nxl: true,
                        startDate: data.results.detail.validity.startDate,
                        endDate: data.results.detail.validity.endDate,
                        shared: file.shared,
                        fileName: file.name,
                        fileSize: file.size,
                        fileType: file.fileType,
                        tags: {},
                        repoName: file.customMetadata.SourceRepoName,
                        lastModifiedTime: file.sharedOn,
                        path: file.customMetadata.SourceFilePathDisplay,
                        protectionType: data.results.detail.protectionType
                    });
                } else {
                    showSnackbar({
                        messages: $filter('translate')('manage.file.error'),
                        isSuccess: false
                    });
                }
            });
        }

        $scope.onClickReshare = function(file, spaceId, fromSpaceId) {
            $scope.isLoading = true;
            if (fromSpaceId == "PROJECTSPACE") {
                projectService.getAllProjectList(function (data) {
                    if (data) {
                        $scope.allProjectList = [];
                        for (var i = 0; i < data.detail.length; i++) {
                            var project = data.detail[i];
                            project.selected = false;
                            if (project.id != spaceId) {
                                $scope.allProjectList.push(project);
                            }
                        }
                    }
                });
            }
            shareFileService.reshareFile({
                "parameters": {
                    "transactionId": file.transactionId,
                    "transactionCode": file.transactionCode,
                    "spaceId" : spaceId,
                    "validateOnly": true
                }
            }, function(data) {
                $scope.isLoading = false;
                if(data.statusCode !==  200) {
                    var messageCode = data.statusCode === 4001 ? 'managelocalfile.rights.revoke.label' :'share.file.unauthorized.reshare';
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')(messageCode)
                    });
                } else {
                    shareFileService.getSharedWithMeFileDetails(file, spaceId,  function(data) {
                        var parameters = {
                            transactionId: file.transactionId,
                            transactionCode: file.transactionCode,
                            rights: file.rights,
                            sharedOn: file.sharedDate,
                            file: file,
                            startDate: data.results.detail.validity.startDate,
                            endDate: data.results.detail.validity.endDate,
                            protectionType: data.results.detail.protectionType
                        }; 
                        if(fromSpaceId == "PROJECTSPACE"){ 
                            parameters.projectId = spaceId;
                            parameters.isSharedFromProject = true;
                            parameters.sharedToProjectsList = $scope.allProjectList;
                            parameters.rights=  data.results.detail.rights;
                            parameters.tags=  data.results.detail.tags;
                            // fix protected on parameters.file.protectedOn 
                        }

                        shareDialogService.reshareFile(parameters , null);
                    });     
                }
            });
        }

        $scope.viewFileActivity = function(file) {
            var params = {};
            params.userId = window.readCookie("userId");
            params.ticket = window.readCookie("ticket");
            params.duid = file.duid;
            params.file = file;
            params.start = 0;
            params.count = 50;

            shareDialogService.viewSharedFileActivity(params);
            $scope.toggleMenuMode();
        }

        $scope.closeFileListOptions = function() {
            $scope.showSearch = false;
            $scope.showShareFilter = false;
            $scope.showSort = false;
            angular.element("#rms-repoTitle-id").show();
        }

        $scope.toggleSearch = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = true;
            $scope.showShareFilter = false;
            $scope.showSort = false;
        }

        $scope.toggleShareFilter = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = false;
            $scope.showShareFilter = true;
            $scope.showSort = false;
        }

        $scope.toggleSort = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = false;
            $scope.showShareFilter = false;
            $scope.showSort = true;
        }

        $scope.sortChanged = function(sort) {
            $scope.isLoading = true;
            dismissSnackbar();
            $scope.selectedSort = sort;
            refreshSharedFileList();
        }

        $scope.filterChanged = function(filter) {
            $scope.isLoading = true;
            dismissSnackbar();
            $scope.selectedFilter = filter;
            $scope.page = PAGE;
            refreshSharedFileList();
        }

        $scope.showRevoked = function() {
            $scope.isLoading = true;
            refreshSharedFileList();
        }

        $rootScope.$on("refreshSharedFileList", function(event, args) {
            refreshSharedFileList();
        });

        $scope.canResetSearchResults = false;
        $scope.search = function() {
            if (STATE_SHARED_ACTIVE_FILES === $state.current.name) {
                $scope.searchString = this.$parent.searchString;
            }
            if ($scope.searchString && $.trim($scope.searchString) != "") {
                $scope.canResetSearchResults = true;
            } else {
                if (!$scope.canResetSearchResults) {
                    return;
                }
                $scope.canResetSearchResults = false;
            }
            dismissSnackbar();
            $scope.isLoading = true;
            isSearchCalled = true;
            $scope.searchActivated = true;
            $scope.page = PAGE;
            refreshSharedFileList();
        }

        $scope.onClickDelete = function(file) {
            dialogService.confirm({
                msg: $filter('translate')('myvault.delete.file.confirmation'),
                ok: function() {
                    $scope.isLoading = true;
                    var params = {
                        "parameters": {
                            "pathId": file.pathId
                        }
                    };
                    var headers = {
                        'Content-Type': 'application/json; charset=utf-8',
                        'userId': window.readCookie("userId"),
                        'ticket': window.readCookie("ticket"),
                        'clientId': window.readCookie("clientId"),
                        'platformId': window.readCookie("platformId")
                    };
                    networkService.post("/rms/rs/myVault/" + file.duid + "/delete", JSON.stringify(params), headers, function(data) {
                        $scope.isLoading = false;
                        if (data.statusCode === 200) {
                            refreshSharedFileList();
                            if (!file.revoked && file.shared) {
                                $scope.msg = $filter('translate')('File') + file.name + $filter('translate')('myvault.delete.revoke.file.success');
                            } else {
                                $scope.msg = $filter('translate')('File') + file.name + $filter('translate')('myvault.delete.file.success');
                            }
                            showSnackbar({
                                isSuccess: true,
                                messages: $scope.msg,
                            });
                        } else {
                            showSnackbar({
                                isSuccess: false,
                                messages: $filter('translate')('myvault.delete.file.fail') + file.name + $filter('translate')('ending'),
                            });
                        }
                    });
                }
            });
        }

        $scope.onClickDecrypt = function(file, spaceId, fromSpaceId) {
            var url =   "/RMSViewer/DownloadSharedWithMeFile?transactionId=" + encodeURIComponent(file.transactionId) +
                        "&transactionCode=" + encodeURIComponent(file.transactionCode) ;
                var query = [];
                query.push('decrypt=' + "true");
                if (spaceId) {
                    query.push('spaceId=' + encodeURIComponent(spaceId));
                }
                if (query.length > 0) {
                    url = url + '&' + query.join('&');
                }
            window.open(RMS_CONTEXT_NAME + url);
        }



        $scope.onClickDownload = function(file, spaceId, fromSpaceId) {
            var url = ($scope.showSharedWithMe || spaceId)?
                "/RMSViewer/DownloadSharedWithMeFile?transactionId=" + encodeURIComponent(file.transactionId) +
                "&transactionCode=" + encodeURIComponent(file.transactionCode) :
                "/RMSViewer/DownloadFileFromMyVault?filePathId=" + encodeURIComponent(file.pathId);

                var query = [];
                if (spaceId) {
                    query.push('spaceId=' + encodeURIComponent(spaceId));
                }
                if (query.length > 0) {
                    url = url + '&' + query.join('&');
                }

            window.open(RMS_CONTEXT_NAME + url);
        }

        $scope.onClickView = function(file) {
            $scope.isLoading = true;
            var settings = initSettingsService.getSettings();
            var showFileParams = $.param({
                filePath: file.pathId,
                filePathDisplay: file.pathDisplay,
                repoId: file.repoId,
                userName: settings.userName,
                offset: new Date().getTimezoneOffset(),
                tenantName: settings.tenantName,
                /*source: "myVault",*/
                lastModifiedDate: file.sharedOn
            });
            repositoryService.showFile(file, showFileParams, openViewer);
        }

        $scope.onClickViewForSharedWithMe = function(file, spaceId, fromSpaceId) {
            $scope.isLoading = true;
            var settings = initSettingsService.getSettings();
            var showFileParams = $.param({
                d: file.transactionId,
                c: file.transactionCode,
                spaceId: spaceId,
                fromSpace : fromSpaceId,
                offset: new Date().getTimezoneOffset(),
                userName: settings.userName,
                tenantName: settings.tenantName,
                lastModifiedDate: file.sharedDate
            });
            repositoryService.showSharedFile(showFileParams, openViewer);
        }

        var openViewer = function(data) {
            $scope.isLoading = false;
            var redirectUrl = data.viewerUrl;
            openSecurePopup(redirectUrl);
        }

        $scope.sourceTag = function(file) {
            var key = null;
            var value = null;
            if (file.customMetadata != null) {
                if (file.customMetadata.SourceRepoName == "Local") {
                    key = $filter('translate')('share.file.sourceTag.local');
                } else {
                    key = file.customMetadata.SourceRepoName;
                }

                value = file.customMetadata.SourceFilePathDisplay;
                if (key != null && value != null) {
                    return key + " : " + value;
                } else {
                    return '';
                }
            }
            return '';
        }

        $scope.openProtectWidget = function(selectedFiles) {
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "protect",
                header: $filter('translate')('widget.protect.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles, refreshSharedFileList);
        }

        $scope.openShareWidget = function(selectedFiles) {
            var parameters = {
                repoList: $scope.repoList,
                mydrive: $scope.mydrive,
                operation: "share",
                header: $filter('translate')('widget.share.file.label')
            };
            protectWidgetService.protectFileModal(parameters, selectedFiles, refreshSharedFileList);
        }

        $scope.toggleShareTab = function(value) {
            if ($scope.showSharedWithMe !== value) {
                $scope.showSharedWithMe = value;
                dismissSnackbar();
                $scope.sortOptions = shareFileService.getSortOptions(value);
                $scope.selectedSort = $scope.sortOptions[0].lookupCode;
                $scope.page = PAGE;
                refreshSharedFileList();
            }
        }

        $scope.markFavorite = function(file) {
            $scope.isLoading = true;
            var jsonFile = {
                repoId: file.repoId,
                fileId: file.pathId,
                path: file.pathDisplay,
                parentFolderId: parentFolderId,
                fileSize: file.size,
                lastModifiedTime: file.sharedOn
            }
            repositoryService.markFileAsFavorite(jsonFile, function success() {
                $scope.isLoading = false;
                file.favorited = true;
                showSnackbar({
                    isSuccess: true,
                    messages: $filter('translate')('favorite.mark.success1') + file.name + $filter('translate')('favorite.mark.success2')
                });
            }, function error() {
                $scope.isLoading = false;
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('favorite.mark.error')
                });
            });
        }

        $scope.unmarkFavorite = function(file) {
            $scope.isLoading = true;
            var jsonFile = {
                repoId: file.repoId,
                fileId: file.pathId,
                path: file.pathDisplay,
                parentFolderId: parentFolderId,
                fileSize: file.size,
                lastModifiedTime: file.sharedOn
            }
            repositoryService.unmarkFileAsFavorite(jsonFile, function success() {
                $scope.isLoading = false;
                file.favorited = false;
                showSnackbar({
                    isSuccess: true,
                    messages: $filter('translate')('favorite.unmark.success1') + file.name + $filter('translate')('favorite.unmark.success2')
                });
            }, function error() {
                $scope.isLoading = false;
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('favorite.unmark.error')
                });
            })
        }

        $scope.fileIconName = function(eachFile) {
            if (!eachFile.revoked && !eachFile.deleted) {
                eachFile.protectedFile = true;
            }
            return repositoryService.getIconName(eachFile);
        }
    }
]);

mainApp.controller('workSpaceController', ['$scope', 'workSpaceService', 'repositoryService', 'serviceProviderService', '$timeout', '$filter', '$cookies', 'dialogService', 'initSettingsService', 'shareDialogService', 'protectWidgetService', '$state', 'networkService', function($scope, workSpaceService, repositoryService, serviceProviderService, $timeout, $filter, $cookies, dialogService, initSettingsService, shareDialogService, protectWidgetService, $state, networkService) {
    $scope.fileupload;
    $scope.searchActivated = false;
    $scope.contents = [];
    $scope.repoList = [];
    $scope.parentFolder = null;
    $scope.showMoreFiles = false;
    $scope.emptyFolderExists = true;
    $scope.searchString = "";
    $scope.showSearch = false;
    $scope.showSort = false;
    $scope.pageTitle = "WorkSpace";
    $scope.canResetSearchResults = false;
    $scope.tenantId = initSettingsService.getSettings().tenantId;
    $scope.currentFolder = {
        pathId: "/"
    }
    var SIZE = 10;
    var PAGE = 1;
    var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
    var DISPLAY_NAME_MAX_LENGTH = 20;
    var rootFolder = {
        pathId: "/",
        pathDisplay: "/",
        folder: true
    }
    $scope.filePageOffset = PAGE;

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
    $scope.selectedSort = $scope.sortOptions[0].lookupCode;

    $scope.$watch(function() {
        return $scope.usage > $scope.quota;
    }, function(newValue) {
        $scope.storageExceeded = newValue;
    });

    var getFiles = function() {
        var params = {
            page: 1,
            size: SIZE,
            orderBy: $scope.selectedSort,
            searchString: $scope.searchString,
            pathId: $scope.currentFolder.pathId
        };
        $scope.isLoading = true;
        workSpaceService.getFiles(params, function(data) {
            $scope.isLoading = false;
            if (data.statusCode == 200 && data.results.detail != null) {
                $scope.totalFiles = data.results.detail.totalFiles;
                if ($scope.totalFiles > SIZE) {
                    $scope.showMoreFiles = true;
                }
                $scope.contents = data.results.detail.files;
            } else {
                $scope.contents = [];
                $scope.errorMessage = "workspace.error.file";
            }
        });
        buildBreadCrumbs();
    }

    var getWorkSpaceFiles = function(folder, params) {
        $scope.isLoading = true;
        $scope.currentFolder = folder;
        var queryParams = params ? params : {
            pathId: folder.pathId,
            orderBy: $scope.selectedSort,
            page: PAGE,
            size: SIZE * $scope.filePageOffset,
            searchString: $scope.searchString
        }
        workSpaceService.getFiles(queryParams, setData);
    }

    var setData = function(data) {
        if (data.statusCode == 404) {
            $scope.onClickFile({
                folder: true,
                pathId: '/',
                pathDisplay: '/'
            });
            $scope.isLoading = false;
            showSnackbar({
                isSuccess: false,
                messages: $filter('translate')('workspace.folder.not.found')
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
    var getTotalFilesCount = function() {
        var params = {
            page: 1,
            size: SIZE,
            orderBy: $scope.selectedSort,
            searchString: $scope.searchString
        };
        $scope.isLoading = true;
        workSpaceService.getFiles(params, function(data) {
            $scope.isLoading = false;
            if (data.statusCode == 200 && data.results.detail != null) {
                $scope.num = data.results.detail.totalFiles;
            } else {
                $scope.errorMessage = "workspace.error.file";
            }
        });
    }

    $scope.exitCreateFolderModal = function() {
        $scope.creatingFolder = false;
    }

    var handleCreateFolderError = function(errMsg) {
        $scope.isLoading = false;
        $scope.creatingFolder = false;
        showSnackbar({
            isSuccess: false,
            messages: errMsg
        });
    };

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

        workSpaceService.createFolder(path, function(data) {
            if (data.statusCode == 200 && data.results.entry != null && data.results.entry.pathId != null) {
                $scope.refreshFilePage($scope.currentFolder);
                showSnackbar({
                    isSuccess: true,
                    messages: $filter('translate')('create_folder.success', {
                        folderName: newFolderName
                    })
                });
            } else if (data.statusCode == 404) {
                var errormsg = $filter('translate')('workspace.folder.not.found');
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

    $scope.createFolderModal = function() {
        $scope.creatingFolder = true;
        $timeout(function() {
            document.getElementById("newFolderTextBox").focus();
        });
    }


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


    $scope.loadMoreFiles = function() {
        $scope.filePageOffset = $scope.filePageOffset + 1;
        var queryParams = {
            pathId: $scope.currentFolder.pathId,
            orderBy: $scope.selectedSort,
            page: $scope.filePageOffset,
            size: SIZE,
            searchString: $scope.searchString
        }
        getWorkSpaceFiles($scope.currentFolder, queryParams);
    }

    $scope.fileIconName = function(eachFile) {
        eachFile.protectedFile = true;
        return repositoryService.getIconName(eachFile);
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
        var queryParams = {
            pathId: $scope.currentFolder.pathId,
            orderBy: $scope.selectedSort,
            page: $scope.filePageOffset,
            size: SIZE,
            searchString: $scope.searchString
        }
        workSpaceService.getFiles(queryParams, function(data) {
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

    $scope.clearSearch = function() {
        $scope.searchString = "";
        dismissSnackbar();
        $scope.noSearchResult = false;
        $scope.searchActivated = false;
        $scope.filePageOffset = 1;
        $scope.refreshFilePage(rootFolder);
    }

    $scope.toggleSearch = function() {
        $scope.showSort = false;
        $scope.showSearch = true;
    }

    $scope.refreshFilePage = function(folder) {
        dismissSnackbar();
        $scope.contents = [];
        getWorkSpaceFiles(folder);
    }

    $scope.closeFileListOptions = function() {
        $scope.showSearch = false;
        $scope.showSort = false;
    }

    $scope.toggleSort = function() {
        $scope.showSort = true;
        $scope.showSearch = false;
    }

    $scope.update = function() {
        $scope.contents = [];
        $scope.isLoading = true;
        var queryParams = {
            pathId: $scope.currentFolder.pathId,
            orderBy: $scope.selectedSort,
            page: PAGE,
            size: SIZE * $scope.filePageOffset,
            searchString: $scope.searchString
        }
        workSpaceService.getFiles(queryParams, setData);
    }

    var openViewer = function(data) {
        $scope.isLoading = false;
        var redirectUrl = data.viewerUrl;
        openSecurePopup(redirectUrl);
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
            if (!settings.userPreferences.disablePromptEWSFileDownload) {
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
            promptDownload: !settings.userPreferences.disablePromptEWSFileDownload
        });
        workSpaceService.showFile(file, showFileParams, openViewer)
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
    }

    getRepoList();

    $scope.uploadFileModal = function(selectedFile) {
        var settings = initSettingsService.getSettings();
        $scope.tenantId = settings.tenantId;
        var parameters = {
            repoList: $scope.repoList,
            mydrive: $scope.mydrive,
            operation: "uploadWorkspaceFile",
            header: $filter('translate')('widget.upload.workspace.file.label'),
            currentFolder: $scope.currentFolder,
            tenantId: settings.tenantId
        };

        protectWidgetService.protectFileModal(parameters, selectedFile, function() {
            getFiles();
        });
    }

    $scope.$watch("fileupload", function (newValue, oldValue) {
        if (newValue && $scope.fileupload) {
            $scope.uploadFileModal($scope.fileupload);
        }
    });
    
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
                workSpaceService.deleteFilesFolders(path, function(data) {
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
                            message = $filter('translate')('workspace.folder.not.found');
                        } else {
                            message = $filter('translate')('workspace.file.not.found');
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

    $scope.toggleMenuMode = function() {
        $scope.MenuClickedMode = !$scope.MenuClickedMode;
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

    $scope.onClickMenu = function(pathId, isFolder) {
        $scope.toggleAllMenuMode(pathId);
        $scope.getPermissions(pathId, isFolder);
    }

    $scope.getPermissions = function(pathId, isFolder) {
        $scope.selectedFileId = pathId;
        if (isFolder) {
            return;
        }
        $scope.isLoading = true;
        var params = {
            "parameters": {
                "pathId": pathId
            }
        };
        workSpaceService.getFileDetails(params, function(data) {
            $scope.isLoading = false;
            var error;
            $scope.data = data;
            if (data.statusCode == 200) {
                $scope.rights = data.results.fileInfo.rights;
                $scope.protectionType = data.results.fileInfo.protectionType;
            } else if (data.statusCode == 404) {
                error = $filter('translate')('workspace.file.not.found');
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


    var openInfoWindow = function(data) {
        var error;
        if (data.statusCode == 200) {
            dialogService.info({
                isWorkspace: true,
                rights: data.results.fileInfo.rights,
                endDate: data.results.fileInfo.expiry.endDate,
                startDate: data.results.fileInfo.expiry.startDate,
                owner: data.results.fileInfo.owner,
                nxl: data.results.fileInfo.nxl,
                fileName: data.results.fileInfo.name,
                fileSize: data.results.fileInfo.size,
                fileType: data.results.fileInfo.fileType,
                repoName: '', // TODO 
                lastModifiedTime: data.results.fileInfo.lastModified,
                path: data.results.fileInfo.pathDisplay,
                tags: data.results.fileInfo.tags,
                protectionType: data.results.fileInfo.protectionType
            });
        } else if (data.statusCode == 404) {
            error = $filter('translate')('workspace.file.not.found');
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

    $scope.onClickInfo = function() {
        openInfoWindow($scope.data);
    }

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
            error = $filter('translate')('workspace.file.not.found');
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

    $scope.onClickClassify = function(file) {
        $scope.isLoading = true;
        var params = {
            "parameters": {
                "pathId": file.pathId
            }
        };
        workSpaceService.getFileDetails(params, openClassifyWindow);
    }
    $scope.onClickDownload = function(file) {
        var settings = initSettingsService.getSettings();
        if (!settings.userPreferences.disablePromptEWSFileDownload) {
            initSettingsService.reloadSettings(function success() {
                var prompt = !initSettingsService.getSettings().userPreferences.disablePromptEWSFileDownload;
                downloadFile(file, prompt);
            }, function error() {
                downloadFile(file, true);
            });
        } else {
            downloadFile(file, false);
        }
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


    var downloadFile = function(file, prompt) {
        if (prompt) {
            dialogService.confirm({
                msg: $filter('translate')('workspace.file.download.prompt'),
                ok: function(data) {
                    if (data.checkboxModel) {
                        var params = {
                            parameters: {
                                preferences: {
                                    disablePromptEWSFileDownload: true
                                }
                            }
                        };
                        networkService.post(RMS_CONTEXT_NAME + "/rs/usr/profile", JSON.stringify(params), getHeaders(), function(data) {
                            if (data != null && data.statusCode == 200) {
                                initSettingsService.getSettings().userPreferences.disablePromptEWSFileDownload = true;
                            }
                        });
                    }
                    workSpaceService.downloadWorkspaceFile(file.pathId);
                },
                cancel: function() {},
                showCheckbox: $filter('translate')('dont.show.again'),
            });
        } else {
            workSpaceService.downloadWorkspaceFile(file.pathId);
        }
    }

    $scope.onClickMainMenu = function() {
        $state.go(STATE_WORKSPACE_FILES, {}, { reload: true });
        $scope.onClickFile({
            folder: true,
            pathId: '/',
            pathDisplay: '/'
        });
        return;
    }

    $scope.onClickDecrypt = function(file) {
        workSpaceService.decryptWorkspaceFile(file.pathId);
    }

    getFiles();
    getTotalFilesCount();

}]);
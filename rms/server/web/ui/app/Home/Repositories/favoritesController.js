mainApp.controller('favoritesController', ['$scope', '$state', '$stateParams', 'repositoryService', '$filter', 'initSettingsService', 'navService', '$cookies', 'shareDialogService', 'dialogService', 'networkService',
    function($scope, $state, $stateParams, repositoryService, $filter, initSettingsService, navService, $cookies, shareDialogService, dialogService, networkService) {
        initSettingsService.setRightPanelMinHeight();
        var PAGE = 1;
        var SIZE = 10;
        $scope.isLoading = false;
        $scope.favContents = [];
        $scope.searchActivated = false;
        $scope.page = PAGE;
        $scope.size = SIZE;
        $scope.searchString = '';
        $scope.menuClickedMode = false;
        $scope.loadMore = false;

        $scope.displayOperationMsg = false;
        $scope.isOperationSuccess = true;
        $scope.expiryInfo = false;

        $scope.showSearch = false;
        $scope.showSort = false;

        $scope.sortOptions = [{
                'lookupCode': ['-isFolder', '-lastModifiedTime'],
                'description': 'last.modified'
            },
            {
                'lookupCode': ['-isFolder', 'lastModifiedTime'],
                'description': 'first.modified'
            },
            {
                'lookupCode': ['-isFolder', 'name'],
                'description': 'filename.ascending'
            },
            {
                'lookupCode': ['-isFolder', '-name'],
                'description': 'filename.descending'
            },
            {
                'lookupCode': ['-isFolder', 'fileSize', 'name'],
                'description': 'file.size.ascending'
            },
            {
                'lookupCode': ['-isFolder', '-fileSize', 'name'],
                'description': 'file.size.descending'
            }
        ];
        $scope.selectedSort = $scope.sortOptions[0].lookupCode;

        $scope.onClickMenu = function(file) {
            $scope.selectedFileId = file.pathId;
            $scope.toggleMenuMode();
        }

        $scope.toggleMenuMode = function() {
            $scope.menuClickedMode = !$scope.menuClickedMode;
        }

        $scope.toggleSearch = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = true;
            $scope.showSort = false;
        }

        $scope.toggleSort = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = false;
            $scope.showSort = true;
        }

        $scope.closeFileListOptions = function() {
            if ($scope.searchActivated && $scope.searchString !== '') {
                $scope.clearSearch();
            }
            $scope.showSearch = false;
            $scope.showSort = false;
            angular.element("#rms-repoTitle-id").show();
        }

        $scope.showMenu = function(file) {
            $scope.selectedFileId = file.pathId;
            $scope.menuClickedMode = true;
        }

        $scope.hideMenu = function(file) {
            $scope.selectedFileId = file.pathId;
            $scope.menuClickedMode = false;
        }

        $scope.search = function() {
            $scope.favContents = [];
            if (!$scope.searchActivated) {
                $scope.page = 1;
                $scope.loadMore = false;
                $scope.searchActivated = true;
            }
            if ($scope.searchString === '') {
                $scope.refresh();
            } else {
                getFavoriteFiles($scope.page, $scope.size, $scope.selectedSort, $scope.searchString);
            }
        }

        $scope.clearSearch = function() {
            $scope.searchString = "";
            $scope.searchActivated = false;
            $scope.search();
        }

        $scope.fileIconName = function(eachFile) {
            return repositoryService.getIconName(eachFile);
        }

        var getFavoriteFiles = function(page, size, selectedSort, searchString) {
            $scope.isLoading = true;
            repositoryService.getFavoriteFiles(page, size, selectedSort, searchString, setData);
        }

        var setData = function(data) {
            var favFileList = data.results;
            if (favFileList && favFileList.length > 0) {
                $scope.loadMore = data.loadMore;
                $scope.favContents = $scope.favContents.concat(favFileList);
                if (!$scope.selectedFile) {
                    dismissSnackbar()
                }
            }
            $scope.size = SIZE;
            $scope.isLoading = false;
        }

        $scope.refreshCurrentPages = function() {
            $scope.favContents = [];
            var page = PAGE;
            $scope.size = $scope.page * SIZE;
            getFavoriteFiles(page, $scope.size, $scope.selectedSort, $scope.searchString);
        }

        $scope.refresh = function() {
            $scope.favContents = [];
            $scope.searchActivated = false;
            $scope.page = PAGE;
            $scope.size = SIZE;
            $scope.searchString = '';
            dismissSnackbar()
            getFavoriteFiles($scope.page, $scope.size, $scope.selectedSort, $scope.searchString);
        }

        $scope.onClickFileOp = function(file, operation) {
            $scope.isLoading = true;
            dismissSnackbar()
            $scope.selectedFile = file;
            var op;
            switch (operation) {
                case 'view':
                    op = $scope.operationView;
                    break;
                case 'getInfo':
                    op = $scope.operationGetInfo;
                    break;
                case 'protect':
                    op = $scope.operationProtect;
                    break;
                case 'share':
                    op = $scope.operationShare;
                    break;
                case 'download':
                    op = $scope.operationDownload;
                    break;
                case 'delete':
                    op = $scope.operationDelete;
                    break;
                case 'unmark':
                    $scope.unmarkFavorite(file);
                    return;
            }
            repositoryService.getFileDetails(file, op);
        }

        var checkExistence = function(data, getInfoOrShare) {
            if (data.hasOwnProperty('result') && data.result == false) {
                if (data.statusCode === 404 || data.statusCode === 4005) {
                    // Could not find file
                    var msg = $filter('translate')('favorite.file.not.exist');
                    dialogService.confirm({
                        msg: msg,
                        ok: $scope.unmarkFavorite,
                        selectedFile: $scope.selectedFile
                    });
                } else if (data.statusCode === 5003) {
                    // Not authorized to operate: file's revoked or user has been removed from recipient list or DUID is not in AllNXL table
                    if (getInfoOrShare && !data.owner) {
                        showSnackbar({
                            isSuccess: false,
                            messages: data.message
                        })
                    } else {
                        // allow download or delete operation
                        return true;
                    }
                } else if (data.statusCode === 5007) {
                    // Invalid NXL file
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('share.file.unsupported.nxl')
                    })
                    if (getInfoOrShare == null) {
                        // allow download and delete
                        return true;
                    }
                } else {
                    // Generic error msg
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('view.file.info.error')
                    })
                }
                return false;
            } else {
                if ((Object.keys(data.tags)) && ((Object.keys(data.tags).length % 2) == 1)) {
                    data.tags[" "] = " ";
                }
                $scope.selectedFile.duid = data.duid;
                $scope.selectedFile.tags = data.tags;
                $scope.selectedFile.rights = data.rights;
                $scope.selectedFile.owner = data.owner;
                $scope.selectedFile.nxl = data.nxl;
                $scope.selectedFile.fileName = $scope.selectedFile.name;
                $scope.selectedFile.revoked = data.revoked;
                return true;
            }
        }

        $scope.unmarkFavorite = function(file) {
            $scope.isLoading = true;
            repositoryService.unmarkFileAsFavorite(file, function success() {
                var msg = $filter('translate')('favorite.unmark.success1') + file.name + $filter('translate')('favorite.unmark.success2');
                showSnackbar({
                    isSuccess: true,
                    messages: msg
                });
                $scope.refreshCurrentPages();
            }, function error() {
                $scope.isLoading = false;
                var errMsg = $filter('translate')('favorite.unmark.error', {
                    fileName: file.name
                });
                showSnackbar({
                    isSuccess: false,
                    messages: errMsg
                });
            });
        }

        $scope.operationView = function(data) {
            $scope.isLoading = false;
            if (checkExistence(data, false)) {
                $scope.isLoading = true;
                var file = $scope.selectedFile;
                var lastModifiedTime = file.lastModifiedTime;
                if (!lastModifiedTime) {
                    lastModifiedTime = 0;
                }
                var settings = initSettingsService.getSettings();
                var showFileParams = $.param({
                    filePath: file.pathId,
                    filePathDisplay: file.path,
                    repoId: file.repoId,
                    lastModifiedDate: lastModifiedTime,
                    userName: settings.userName,
                    offset: new Date().getTimezoneOffset(),
                    tenantName: settings.tenantName,
                    repoName: file.repoName,
                    repoType: file.repoType
                });
                repositoryService.showFile(file, showFileParams, openViewer);
            }
            $scope.toggleMenuMode();
        }

        var openViewer = function(data) {
            $scope.isLoading = false;
            var redirectUrl = data.viewerUrl;
            openSecurePopup(redirectUrl);
        }

        $scope.operationGetInfo = function(data) {
            $scope.isLoading = false;
            if (checkExistence(data, 'getInfo')) {
                $scope.selectedFile.startDate = data.validity.startDate;
                $scope.selectedFile.endDate = data.validity.endDate;
                dialogService.info($scope.selectedFile);
            }
            $scope.toggleMenuMode();
        }

        $scope.operationProtect = function(data) {
            $scope.isLoading = false
            if (checkExistence(data)) {
                shareDialogService.shareFile({
                    file: $scope.selectedFile,
                    operation: "protect"
                });
            }
        }

        $scope.operationShare = function(data) {
            $scope.isLoading = false
            if (checkExistence(data, 'share')) {
                if ($scope.selectedFile.revoked) {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('share.file.revoked')
                    });
                } else if ($scope.selectedFile.owner || $scope.selectedFile.rights.indexOf('SHARE') >= 0) {
                    shareDialogService.shareFile({
                        file: $scope.selectedFile,
                        rights: $scope.selectedFile.rights,
                        owner: $scope.selectedFile.owner,
                        nxl: $scope.selectedFile.nxl,
                        operation: "share",
                        endDate: data.validity.endDate,
                        startDate: data.validity.startDate,
                        protectionType: data.protectionType,
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('share.file.unauthorized.reshare')
                    });
                }
            }
        }

        $scope.viewFileActivity = function(eachFile) {
            $scope.isLoading = true;
            repositoryService.getFileDetails(eachFile, function(data) {
                $scope.isLoading = false;
                if (data.hasOwnProperty('result') && data.result == false) {
                    showSnackbar({
                        isSuccess: false,
                        messages: data.message
                    });
                    if (data.statusCode === 404) {
                        refreshFolder();
                    }
                } else if (data.hasOwnProperty('owner') && data.owner == false) {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('operation.unauthorized')
                    });
                } else {
                    var params = {};
                    params.userId = window.readCookie("userId");
                    params.ticket = window.readCookie("ticket");
                    params.duid = data.duid;
                    params.file = data;
                    params.start = 0;
                    params.count = 50;
                    shareDialogService.viewSharedFileActivity(params);
                }
            });
            $scope.toggleMenuMode();
        }

        $scope.operationDownload = function(data) {
            $scope.isLoading = false;
            if (checkExistence(data)) {
                downloadRepoFile($scope.selectedFile.repoId, $scope.selectedFile.pathId, $scope.selectedFile.path);
            }
            $scope.toggleMenuMode();
        }

        $scope.operationDelete = function(data) {
            $scope.isLoading = false;
            if (checkExistence(data)) {
                if ($scope.selectedFile.fromMyVault) {
                    dialogService.confirm({
                        msg: $filter('translate')('myvault.delete.file.confirmation'),
                        ok: function() {
                            $scope.isLoading = true;
                            var params = {
                                "parameters": {
                                    "pathId": $scope.selectedFile.pathId
                                }
                            };
                            var headers = {
                                'Content-Type': 'application/json; charset=utf-8',
                                'userId': window.readCookie("userId"),
                                'ticket': window.readCookie("ticket"),
                                'clientId': window.readCookie("clientId"),
                                'platformId': window.readCookie("platformId")
                            };
                            networkService.post("/rms/rs/myVault/" + $scope.selectedFile.duid + "/delete", JSON.stringify(params), headers, function(data) {
                                $scope.isLoading = false;
                                if (data.statusCode === 200) {
                                    $scope.refreshCurrentPages();
                                    var message = '';
                                    if (!$scope.selectedFile.revoked) {
                                        message = $filter('translate')('File') + $scope.selectedFile.name + $filter('translate')('myvault.delete.revoke.file.success');
                                    } else {
                                        message = $filter('translate')('File') + $scope.selectedFile.name + $filter('translate')('myvault.delete.file.success');
                                    }
                                    showSnackbar({
                                        isSuccess: true,
                                        messages: message
                                    });
                                } else {
                                    showSnackbar({
                                        isSuccess: false,
                                        messages: $filter('translate')('myvault.delete.file.fail') + $scope.selectedFile.name + $filter('translate')('ending')
                                    });
                                }
                            });
                        }
                    });
                } else {
                    dialogService.confirm({
                        msg: $filter('translate')('delete.file.confirmation'),
                        ok: function() {
                            $scope.isLoading = true;
                            var file = $scope.selectedFile;
                            var params = $.param({
                                repoId: file.repoId,
                                filePath: file.pathId,
                                filePathDisplay: file.path,
                                fileName: file.name,
                                isFolder: false
                            });
                            var headers = {
                                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
                            };
                            networkService.post("/rms/RMSViewer/DeleteItem", params, headers, function(data) {
                                if (data.result == true) {
                                    $scope.refreshCurrentPages();
                                    showSnackbar({
                                        isSuccess: true,
                                        messages: msg
                                    });
                                } else {
                                    $scope.isLoading = false;
                                    showSnackbar({
                                        isSuccess: false,
                                        messages: msg
                                    });
                                }
                            });
                        }
                    });
                }
            }
            $scope.toggleMenuMode();
        }

        $scope.loadMoreFiles = function() {
            $scope.page = $scope.page + 1;
            getFavoriteFiles($scope.page, $scope.size, $scope.selectedSort, $scope.searchString)
        }

        getFavoriteFiles($scope.page, $scope.size, $scope.selectedSort, $scope.searchString);
    }
]);
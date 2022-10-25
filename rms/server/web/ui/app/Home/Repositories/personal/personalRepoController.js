mainApp.controller('personalRepoController', ['$scope', '$state', '$timeout', 'networkService', 'dialogService', '$location', '$stateParams',
    'repositoryService', '$filter', '$rootScope', 'initSettingsService', 'serviceProviderService', 'navService', '$cookies', 'Upload', 'uploadFileService', 'uploadDialogService',
    'shareDialogService',
    function($scope, $state, $timeout, networkService, dialogService, $location, $stateParams, repositoryService,
        $filter, $rootScope, initSettingsService, serviceProviderService, navService, $cookies, Upload, uploadFileService, uploadDialogService, shareDialogService) {

        initSettingsService.setRightPanelMinHeight();
        $scope.fileupload;
        $scope.invalidTokenRepoExists = false;
        $scope.scrollToTop = true;
        var PAGE_TITLE_MAX_LENGTH_DESKTOP = 40;
        var PAGE_TITLE_MAX_LENGTH_MOBILE = 20;
        $scope.expiryInfo = false;
        $scope.parentFolder = null;
        $scope.currentFolderName = null;
        $scope.rootFolder = {
            isFolder: true,
            pathId: '/',
            path: '/',
            fileId: '/'
        };

        var isRepoFirstTimeAdded = function() {
            if ($cookies.get("repoAddedFirstTime") != undefined) {
                if ($cookies.get("repoAddedFirstTime") === "true") {
                    var msg = $filter('translate')('repo.added');
                    showSnackbar({
                        isSuccess: true,
                        messages: $filter('translate')('repo.added'),
                    });
                }
                deleteCookie("repoAddedFirstTime");
            }
        }

        var getFiles = function(folder) {
            $scope.currentFolder = folder;
            $scope.MenuClickedMode = false;
            $scope.searchString = "";
            $scope.showTree = true;
            $scope.isLoading = true;
            if ($stateParams.all) {
                repositoryService.getAllFiles(null, setData);
            } else {
                $scope.repoType = folder.repoType ? folder.repoType : $stateParams.repoType;
                repoDetails = $.param({
                    repoId: folder.repoId ? folder.repoId : $stateParams.repoId,
                    repoType: $scope.repoType,
                    path: folder.usePathId ? folder.pathId : folder.path
                });

                if (folder.pathId == "/") {
                    $scope.currentFolderName = "";
                } else {
                    var indexOfPath = folder.path.lastIndexOf("/");
                    $scope.currentFolderName = folder.path.substring(++indexOfPath);
                }

                $scope.onClickFolder = false;
                $scope.showAllServices = false;
                $scope.searchActivated = false;
                navService.setIsInAllFilesPage(false);
                $scope.repoContents = [];
                repositoryService.getFilesWithPath(repoDetails, setData);
            }
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
                    if (breadCrumbEntry.pathId === folder.pathId) {
                        $scope.breadCrumbsContent = $scope.breadCrumbsContent.slice(0, i + 1);
                        backOrRefresh = true;
                        break;
                    }
                }
                if (!backOrRefresh) {
                    if (isSubFolder(folder) && $stateParams.inSearchContext) {
                        var breadCrumbsContents = getBreadCrumbsContentForSearch(folder);
                        for (var i = 0; i < breadCrumbsContents.length; i++) {
                            var breadCrumbEntry = breadCrumbsContents[i];
                            $scope.breadCrumbsContent.push(breadCrumbEntry);
                        }
                        $stateParams.inSearchContext = false;
                    } else {
                        $scope.breadCrumbsContent.push(folder);
                    }
                }
                if ($scope.breadCrumbsContent.length > 1) {
                    $scope.parentFolder = $scope.breadCrumbsContent[$scope.breadCrumbsContent.length - 2];
                } else {
                    $scope.parentFolder = $scope.rootFolder;
                }
            }
        };

        var isSubFolder = function(folder) {
            return folder.path.split('/').length >= 3;
        }

        var getBreadCrumbsContentForSearch = function(folder) {
            var breadCrumbsContents = [];
            var folderPaths = folder.path.split('/');
            var folderPathIds = folder.pathId.split('/');
            var folderPathEndPos = 0;
            var folderPathIdEndPos = 0;
            for (var i = 1; i < folderPaths.length; i++) {
                folderPathEndPos += folderPaths[i].length + 1;
                var pathId;
                var path = folder.path.substr(0, folderPathEndPos);
                if (folderPathIds.length >= 3) {
                    folderPathIdEndPos += folderPathIds[i].length + 1;
                    pathId = folder.pathId.substr(0, folderPathIdEndPos);
                } else {
                    if (folder.usePathId) {
                        pathId = "TempId" + Math.random().toString() + ":" + path;
                    } else {
                        pathId = i == (folderPaths.length - 1) ? folder.pathId : "TempId" + Math.random().toString();
                    }
                }
                if (folder.repoName === $filter('translate')('MyDrive')) {
                    path += '/';
                    pathId += '/';
                }
                breadCrumbsContents.push({
                    path: path,
                    pathId: pathId,
                    fileId: folder.fileId,
                    repoId: folder.repoId,
                    name: folderPaths[i],
                    isFolder: true,
                    usePathId: folder.usePathId,
                    repoName: folder.repoName
                });
            }
            return breadCrumbsContents;
        }

        $scope.quota = 0;
        $scope.usage = 0;
        $scope.myVaultUsage = 0;
        $scope.showAllServices = $stateParams.all;
        $scope.repoContents = [];
        $scope.breadCrumbsContent = [];
        $scope.isTreeOpen = false;
        $scope.canResetSearchResults = false;
        $scope.inMyDrive = function() {
            return initSettingsService.getSettings().inbuiltServiceProvider === $scope.repoType;
        }
        $scope.isFileInMyDrive = function(file) {
            return file.repoType === initSettingsService.getSettings().inbuiltServiceProvider;
        }
        $scope.inMyVaultFolder = function() {
            return $scope.inMyDrive() && $scope.currentFolder.path.indexOf($filter('translate')('repo.myDrive.myVault')) == 0;
        }
        $scope.isFileInMyVault = function(file) {
            return $scope.isFileInMyDrive(file) && file.path.indexOf($filter('translate')('repo.myDrive.myVault')) == 0;
        }
        $scope.isRepoReadOnly = function(file) {
            return file ? !$scope.isFileInMyDrive(file) : !$scope.inMyDrive();
        }

        $scope.$watch("usage", function() {
            $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage - $scope.myVaultUsage, $scope.quota) + '%';
            $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.usage) + '%';
            checkDriveExceeded();
        });

        $scope.$watch(function() {
            return $scope.usage > $scope.quota;
        }, function(newValue) {
            $scope.driveExceeded = newValue;
        });

        var checkDriveExceeded = function() {
            $scope.driveExceeded = $scope.usage > $scope.quota;
        }

        $scope.$watch("myVaultUsage", function() {
            $scope.myDriveWidth = repositoryService.getUsageBarWidth($scope.usage - $scope.myVaultUsage, $scope.quota) + '%';
            $scope.myVaultWidth = repositoryService.getUsageBarWidth($scope.myVaultUsage, $scope.usage) + '%';
            checkVaultExceeded();
        });

        var checkVaultExceeded = function() {
            $scope.vaultExceeded = $scope.usage > $scope.vaultQuota;
        }

        $scope.toggleTreeView = function() {
            setMinHeightToTree();
            var len = $scope.breadCrumbsContent.length;
            if (len > 0) {
                $scope.onClickFolder = true;
                var current_selection = angular.element("#rmstree").jstree("get_selected");
                angular.element("#rmstree").jstree("deselect_node", "#" + current_selection);
                angular.element("#rmstree").jstree("select_node", "#" + $scope.breadCrumbsContent[len - 1].path);
                $scope.onClickFolder = false;
            }
            $scope.isTreeOpen = !$scope.isTreeOpen;
            return $scope.isTreeOpen;
        }

        $scope.repos = [];
        var allKeyword = "all";
        $scope.filterData = [];
        $scope.filterData[allKeyword] = true;
        $scope.filteredRepos = [allKeyword];
        var repositoryList = function() {
            repositoryService.getRepositoryList(function(data) {
                var repoList = data;
                for (var i = 0; i < repoList.length; i++) {
                    if (repoList[i].usage != null && repoList[i].quota != null) {
                        $scope.usage = repoList[i].usage;
                        $scope.quota = repoList[i].quota;
                        $scope.myVaultUsage = repoList[i].myVaultUsage != null ? repoList[i].myVaultUsage : 0;
                        $scope.vaultQuota = repoList[i].vaultQuota != null ? repoList[i].vaultQuota : 0;
                    }
                    $scope.repos.push({
                        repoObj: repoList[i]
                    });
                    $scope.filterData[repoList[i].repoId] = true;
                }
            });
        }
        repositoryList();

        var DISPLAY_NAME_MAX_LENGTH = 25;
        var repoNamesToString = function() {
            var selectedRepos = $scope.selectedRepos;
            var str = "";
            for (var i = 0; i < selectedRepos.length; i++) {
                str += selectedRepos[i];
                if (i != selectedRepos.length - 1) {
                    str += ", "
                }
            }
            str = repositoryService.getShortRepoName(str, DISPLAY_NAME_MAX_LENGTH);
            return str;
        }

        $scope.searchActivated = false;
        $scope.clearSearch = function() {
            $scope.searchString = "";
            $scope.search();
            $scope.searchActivated = false;
        }

        $scope.search = function() {
            if ($scope.searchString === "" && !$scope.canResetSearchResults) {
                return;
            }
            if ($scope.searchString === "") {
                $scope.canResetSearchResults = false;
                $scope.searchActivated = false;
                repositoryService.setSearchActivatedAlias(false);
                var folder = $scope.rootFolder;
                getFiles(folder);
                dismissSnackbar();
                return;
            } else {
                $scope.canResetSearchResults = true;
                $scope.isLoading = true;
                $scope.pageTitle = $filter('translate')('search');
                searchDetails = $.param({
                    repoId: $stateParams.repoId ? $stateParams.repoId : "",
                    searchString: $scope.searchString
                });
                repositoryService.getSearchResults(searchDetails, setData);
                $scope.searchActivated = true;
                navService.setIsInAllFilesPage(false);
            }
            $scope.pageTitleDesktopShortened = $scope.pageTitle;
            $scope.pageTitleMobileShortened = $scope.pageTitle;
        }
        $scope.filterRepo = function() {
            if ($scope.selectedReposValidationStatus) {
                if (!$scope.filterData[allKeyword]) {
                    appendingRepoNames();
                    $scope.displaySelectedRepos = repoNamesToString();
                } else {
                    $scope.selectedRepos = [];
                    $scope.displaySelectedRepos = "";
                }
                $scope.isOpen = false;
                $scope.filteredRepos = [];
                var filterKeys = Object.keys($scope.filterData);
                if ($scope.filterData[allKeyword]) {
                    $scope.filteredRepos.push(allKeyword);
                    return $scope.filteredRepos;
                }
                for (var i = 0; i < filterKeys.length; i++) {
                    if ($scope.filterData[filterKeys[i]]) {
                        $scope.filteredRepos.push(filterKeys[i]);
                    }
                }
            }
            return $scope.filteredRepos;
        }

        $scope.isFiltered = function(data) {
            return $scope.filteredRepos.indexOf(allKeyword) > -1 || $scope.filteredRepos.indexOf(String(data.repoId)) > -1;
        }

        $scope.selectedRepos = [];
        $scope.selectAllRepos = function() {
            var filterKeys = Object.keys($scope.filterData);
            for (var i = 0; i < filterKeys.length; i++) {
                if ($scope.filterData[allKeyword]) {
                    $scope.filterData[filterKeys[i]] = true;
                } else {
                    $scope.filterData[filterKeys[i]] = false;
                }
            }
            validateSelectedRepos();
        }

        $scope.clickCheckbox = function() {
            $scope.filterData[allKeyword] = false;
            validateSelectedRepos();
        }

        $scope.selectedReposValidationStatus = true;

        function validateSelectedRepos() {
            var filterKeys = Object.keys($scope.filterData);
            var count = 0;
            for (var i = 0; i < filterKeys.length; i++) {
                if (!$scope.filterData[filterKeys[i]]) {
                    count++;
                }
            }
            if (count == filterKeys.length) {
                $scope.selectedReposValidationStatus = false;
            } else {
                $scope.selectedReposValidationStatus = true;
            }
        }

        function appendingRepoNames() {
            $scope.selectedRepos = [];
            for (var i = 0; i < $scope.repos.length; i++) {
                var repoObj = $scope.repos[i].repoObj;
                if ($scope.filterData[repoObj.repoId]) {
                    $scope.selectedRepos.push(repoObj.repoName);
                }
            }
        }

        $scope.dropdownClicked = function($event) {
            if ($($event.target).attr('data-propagation') != 'true' || !$scope.selectedReposValidationStatus) {
                $event.stopPropagation();
            }
        };

        $scope.messages = [];

        $scope.authorizeRepo = function(data) {
            dialogService.confirm({
                msg: data.repoType === 'SHAREPOINT_ONPREMISE' ? $filter('translate')('err.sharepoint.onpremise.not.authorized') : $filter('translate')('err.repo.not.authorized'),
                ok: function() {
                    if (data.repoType === 'SHAREPOINT_ONPREMISE') {
                        initSettingsService.logout();
                    } else {
                        window.location.replace(data.redirectUrl + '?name=' + data.repoName);
                    }
                },
                cancel: function() {
                    deleteCookie('rpredirect');
                    navService.setCurrentTab(0);
                    navService.setIsInAllFilesPage(true);
                    $state.go(STATE_ALL_REPOSITORIES);
                }
            });
        }

        var setData = function(data) {
            if (data.messages && data.messages.length > 0) {
                showSnackbar({
                    messages: data.messages,
                    isSuccess: false
                });
            }
            $scope.isLoading = false;
            if (!$stateParams.all && !$scope.searchActivated && ($stateParams.repoId !== data.repoId)) {
                return;
            }
            isRepoFirstTimeAdded();
            if (!data.result && data.redirectUrl) {
                $scope.authorizeRepo(data);
            }
            $scope.emptyFolderExists = !$scope.searchActivated && !$stateParams.all && data.result && data.content.length == 0;
            $scope.repoContents = [];
            if (data.content && data.content.length != 0) {
                var contents = data.content.name == "Root" ? data.content.children : data.content;
                var index = 0;
                for (index = 0; index < contents.length; index++) {
                    contents[index]["index"] = index;
                }
                $scope.repoContents = contents;
            }

            if ($scope.searchActivated) {
                if (data.content && data.content.length > 0) {
                    dismissSnackbar();
                }
            } else {
                if (!$stateParams.all) {
                    $scope.pageTitle = data.repoName ? data.repoName : '';
                    $stateParams.repoType = data.repoType;
                    $stateParams.repoName = data.repoName;
                    $scope.repoType = $stateParams.repoType;
                    $scope.pageTitleDesktopShortened = repositoryService.getShortRepoName($scope.pageTitle, PAGE_TITLE_MAX_LENGTH_DESKTOP);
                    $scope.pageTitleMobileShortened = repositoryService.getShortRepoName($scope.pageTitle, PAGE_TITLE_MAX_LENGTH_MOBILE);
                } else {
                    $scope.pageTitle = $filter('translate')('all.files');
                    $scope.pageTitleDesktopShortened = $scope.pageTitle;
                    $scope.pageTitleMobileShortened = $scope.pageTitle;
                }
            }

            var invalidTokenRepos = data.invalidTokenRepos;
            if (invalidTokenRepos && Array.isArray(invalidTokenRepos) && invalidTokenRepos.length > 0) {
                $scope.invalidTokenRepoExists = true;
                var message = "";
                if (invalidTokenRepos.length == 1) {
                    message = "<center><p>" + $filter('translate')('err.repo.invalid.token') + "</p><p>";
                    message += constructInvalidRepoLink(invalidTokenRepos[0]);
                    message += "</p></center>";
                } else {
                    message = "<center><p>" + $filter('translate')('err.repos.invalid.token') + "</p><p>";
                    var repoList = "";
                    for (i = 0; i < invalidTokenRepos.length; i++) {
                        repoList += constructInvalidRepoLink(invalidTokenRepos[i]) + ", ";
                    }
                    message += repoList.substring(0, repoList.length - 2);
                    message += "</p></center>";
                }
                var tokenErrDiv = document.getElementById("invalid-token-repo-err-div");
                if (tokenErrDiv) {
                    tokenErrDiv.innerHTML = message;
                }
            }
            repositoryService.setSearchActivatedAlias($scope.searchActivated);
            buildBreadCrumbs();
            $scope.scrollToTop = true;
        }

        function constructInvalidRepoLink(repo) {
            var params = {
                repoId: repo.repoId,
                repoName: repo.repoName,
                repoType: repo.repoType
            };
            var redirectUrl = $state.href(STATE_REPOSITORIES, params, {
                absolute: false
            });
            return "<a class='invalid-token-repo-msg' href='" + redirectUrl + "'>" + repo.repoName + "</a>";
        }

        $scope.onClickFolder = false;
        $scope.onClickFile = function(folder) {
            $scope.cancelRepoFirstTimeAddedMessage();
            dismissSnackbar();
            if (!folder.isFolder) {
                $scope.isLoading = true;

                var lastModifiedTime = folder.lastModifiedTime;
                if (!lastModifiedTime) {
                    lastModifiedTime = 0;
                }

                var settings = initSettingsService.getSettings();
                var showFileParams = $.param({
                    filePath: folder.pathId,
                    filePathDisplay: folder.path,
                    repoId: folder.repoId,
                    lastModifiedDate: lastModifiedTime,
                    userName: settings.userName,
                    offset: new Date().getTimezoneOffset(),
                    tenantName: settings.tenantName,
                    repoName: folder.repoName,
                    repoType: folder.repoType
                });
                repositoryService.showFile(folder, showFileParams, openViewer);
            } else {
                $scope.onClickFolder = true;
                if (!navService.getIsInAllFilesPage() && !repositoryService.getSearchActivatedAlias()) {
                    var current_selection = angular.element("#rmstree").jstree("get_selected");
                    angular.element("#rmstree").jstree("deselect_node", "#" + current_selection);
                    angular.element("#rmstree").jstree("select_node", "#" + (folder.usePathId ? folder.pathId : folder.path));
                }
                if ($stateParams.all || $scope.searchActivated) {
                    var params = {
                        repoId: folder.repoId,
                        folder: folder,
                        inSearchContext: $scope.searchActivated
                    };
                    $state.go(STATE_REPOSITORIES, params, {
                        reload: STATE_HOME
                    });
                } else {
                    getFiles(folder);
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

        var getAllowedServiceProviders = function() {
            serviceProviderService.getAllowedServiceProviders(function(data) {
                for (var value in data) {
                    $scope.serviceProviderPresent = true;
                    break;
                }
                var initData = initSettingsService.getSettings();
                if (!$scope.serviceProviderPresent && initData.isAdmin) {
                    $scope.message = $filter('translate')('err.add.service.provider.admin');
                } else if (!$scope.serviceProviderPresent && !initData.isAdmin) {
                    $scope.message = $filter('translate')('err.add.service.provider.user');
                }
            });
        }
        getAllowedServiceProviders();

        // Executes with this controller on load
        if ($stateParams.folder) {
            navService.setCollapseStatus(true);
            navService.setCurrentTab($stateParams.folder.repoId);
            getFiles($stateParams.folder);
            $stateParams.folder = null;
        } else {
            getFiles($scope.rootFolder);
        }

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
            $scope.selectedIndex = file.index;
            $scope.toggleMenuMode();
        }

        $scope.showMenu = function(file) {
            $scope.selectedFileId = file.pathId;
            $scope.selectedIndex = file.index;
            $scope.MenuClickedMode = true;
        }

        $scope.hideMenu = function(file) {
            $scope.selectedFileId = file.pathId;
            $scope.selectedIndex = file.index;
            $scope.MenuClickedMode = false;
        }

        $scope.MenuClickedMode = false;

        $scope.toggleMenuMode = function() {
            $scope.MenuClickedMode = !$scope.MenuClickedMode;
        }
        $scope.onClickDownload = function(file) {
            downloadRepoFile(file.repoId, file.pathId, file.path);
        }
        $scope.onClickInfo = function(file) {
            $scope.isLoading = true;
            $scope.selectedFile = file;
            repositoryService.getRepositoryFileDetails(file, openInfoWindow);
        }
        $scope.onClickProtect = function(file) {
            // encryptable (boolean) false currently refers to the file being a Google native file e.g. gdoc
            if(!file.encryptable) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('protect.file.not-allowed.google-native-file')
                });
                return;
            }
            
            $scope.selectedFile = file;

            shareDialogService.shareFile({
                file: $scope.selectedFile,
                operation: "protect"
            });
        }

        $scope.markFavorite = function(file) {
            file.parentFolderId = $scope.currentFolder.fileId;
            $scope.isLoading = true;
            repositoryService.markFileAsFavorite(file, function success() {
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
            repositoryService.unmarkFileAsFavorite(file, function success() {
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
            });
        }

        $scope.onClickShare = function(file) {
            $scope.isLoading = true;
            $scope.selectedFile = file;
            var params = $.param({
                repoId: file.repoId,
                filePath: file.pathId,
                filePathDisplay: file.path,
                fileName: file.name
            });
            var headers = {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
            };
            networkService.post("/rms/RMSViewer/CheckSharedRight", params, headers, function(data) {
                var error;
                if (data.statusCode == 200) {
                    var rights = data.results.r;
                    var owner = data.results.o;
                    var nxl = data.results.nxl;
                    var expiry = data.results.expiry;
                    var protectionType = data.results.protectionType;
                    if (owner || rights.indexOf('SHARE') >= 0) {
                        shareFile(params, rights, owner, nxl, expiry, protectionType);
                    } else {
                        error = $filter('translate')('share.file.unauthorized.reshare');
                        $scope.isLoading = false;
                    }
                } else {
                    error = data.message;
                    $scope.isLoading = false;
                    if (data.statusCode === 404) {
                        refreshFolder();
                    }
                }
                if (error) {
                    showSnackbar({
                        isSuccess: false,
                        messages: error
                    });
                }
            });
        }

        $scope.onClickDelete = function(file) {
            var fileTypeStr = file.isFolder ? 'delete.folder.confirmation' : 'delete.file.confirmation';
            dialogService.confirm({
                msg: $filter('translate')(fileTypeStr),
                ok: function() {
                    $scope.isLoading = true;
                    $scope.selectedFile = file;
                    var params = $.param({
                        repoId: file.repoId,
                        filePath: file.pathId,
                        filePathDisplay: file.path,
                        fileName: file.name,
                        isFolder: file.isFolder
                    });
                    var headers = {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
                    };
                    networkService.post("/rms/RMSViewer/DeleteItem", params, headers, function(data) {
                        var fileType = file.isFolder ? "Folder" : "File";
                        var opMsg;
                        var isSuccess = false;
                        if (data.result == true) {
                            $scope.removeNode(file.usePathId ? file.pathId : file.path);
                            getFiles($scope.currentFolder);
                            if (data.usage) {
                                $scope.usage = Number(data.usage);
                            }
                            if (data.myVaultUsage) {
                                $scope.myVaultUsage = Number(data.myVaultUsage);
                            }
                            opMsg = $filter('translate')('folder.file.delete.success1', {
                                fileType: fileType
                            }) + file.name + $filter('translate')('folder.file.delete.success2');
                            isSuccess = true;
                        } else if (data.statusCode === 404) {
                            opMsg = data.message;
                            refreshFolder();
                        } else {
                            $scope.isLoading = false;
                            opMsg = $filter('translate')('folder.file.delete.failure', {
                                fileType: fileType
                            }) + file.name + $filter('translate')('ending');
                        }
                        showSnackbar({
                            isSuccess: isSuccess,
                            messages: opMsg
                        });
                    });
                }
            });
        }

        var shareFile = function(data, rights, owner, nxl, expiry, protectionType) {
            $scope.isLoading = false;
            shareDialogService.shareFile({
                file: $scope.selectedFile,
                rights: rights,
                owner: owner,
                nxl: nxl,
                operation: "share",
                startDate: expiry.startDate,
                endDate: expiry.endDate,
                protectionType: protectionType
            });
            $scope.toggleMenuMode();
        }

        var refreshFolder = function() {
            getFiles($scope.currentFolder);
            $scope.isLoading = false;
        }

        $rootScope.$on("refreshPersonalRepoFileList", function(event, args) {
            refreshFolder();
        });

        var openInfoWindow = function(data) {
            $scope.isLoading = false;
            if (data.hasOwnProperty('result') && data.result == false || !data) {
                var error = data.message;
                if(!error) {
                    error = $filter('translate')('view.file.info.error');
                }
                showSnackbar({
                    isSuccess: false,
                    messages: error
                });
                if (data.statusCode === 404) {
                    refreshFolder();
                }
            } else {
                dialogService.info({
                    tags: data.tags,
                    rights: data.rights,
                    endDate:data.validity.endDate,
                    startDate:data.validity.startDate,
                    owner: data.owner,
                    nxl: data.nxl,
                    protectionType: data.protectionType,
                    fileName: $scope.selectedFile.name,
                    fileSize: $scope.selectedFile.fileSize,
                    fileType: $scope.selectedFile.fileType,
                    repoName: $scope.selectedFile.repoName,
                    lastModifiedTime: $scope.selectedFile.lastModifiedTime,
                    path: $scope.selectedFile.path
                });
            }
            $scope.toggleMenuMode();
        }

        var openViewer = function(data) {
            $scope.isLoading = false;
            var redirectUrl = data.viewerUrl;
            openSecurePopup(redirectUrl);
        }

        function setMinHeightToTree() {
            var containerHeight = $("#rms-main-container").height();
            var fileListHeaderHeight = $("#fileListHeader").height();
            var footerHeight = initSettingsService.getFooterHeight();
            var minHeight = containerHeight - fileListHeaderHeight - footerHeight - 40;
            $("#resizable-tree").css("min-height", minHeight);
        }


        $scope.showSearch = false;
        $scope.showRepoFilter = false;
        $scope.showSort = false;
        $scope.toggleSearch = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = true;
            $scope.showRepoFilter = false;
            $scope.showSort = false;
        }

        $scope.closeFileListOptions = function() {
            $scope.showSearch = false;
            $scope.showRepoFilter = false;
            $scope.showSort = false;
            angular.element("#rms-repoTitle-id").show();
        }

        $scope.toggleRepoFilter = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = false;
            $scope.showRepoFilter = true;
            $scope.showSort = false;
        }

        $scope.toggleSort = function() {
            angular.element("#rms-repoTitle-id").hide();
            $scope.showSearch = false;
            $scope.showRepoFilter = false;
            $scope.showSort = true;
        }


        $scope.treeConfig = {
            core: {
                multiple: false,
                animation: true,
                error: function(error) {},
                check_callback: true,
                worker: true
            },
            version: 1,
            types: {
                "default": {
                    "icon": "glyphicon glyphicon-folder-close"
                }
            },
            plugins: ["types"]
        };

        $scope.applyModelChanges = function() {
            return true;
        };

        $scope.treeEventsObj = {
            'select_node': selectNode
        }
        $scope.treeData = [{
            id: '/',
            parent: '#',
            text: '/',
            state: {
                opened: true
            },
            data: {
                "pathId": "/",
                "path": "/",
                "usePathId": false
            },
            folderDetails: {
                "pathId": "/",
                "path": "/",
                "usePathId": false
            }
        }];

        $scope.cancelRepoFirstTimeAddedMessage = function() {
            $scope.displayRepoFirstTimeAdded = false;
        }

        function selectNode(event, repoContents) {
            if (!$scope.onClickFolder) {
                var folder = {};
                folder.isFolder = true;
                folder.pathId = repoContents.node.data.pathId;
                folder.path = repoContents.node.data.path;
                folder.fileId = repoContents.node.data.fileId;
                folder.repoName = repoContents.node.data.repoName;
                folder.usePathId = repoContents.node.data.usePathId;
                getFiles(folder);
            }
        }

        function convertToJSON(repoContents) {
            var result = [];
            var idx = 0;
            repoContents = Array.isArray(repoContents) ? repoContents : [repoContents];
            for (var i = 0; i < repoContents.length; i++) {
                var record = repoContents[i];
                if (record.isFolder === true) {
                    var folder = {};
                    if (record.usePathId) {
                        folder.id = record.pathId;
                        folder.parent = getParentNode(record.pathId);
                    } else {
                        folder.id = record.path;
                        folder.parent = getParentNode(record.path);
                    }

                    folder.text = record.name;
                    folder.state = {
                        opened: true
                    };
                    folder.data = {
                        "path": record.path,
                        "pathId": record.pathId,
                        "repoName": record.repoName,
                        "usePathId": record.usePathId
                    };
                    folder.folderDetails = {
                        "path": record.path,
                        "pathId": record.pathId,
                        "repoName": record.repoName,
                        "usePathId": record.usePathId
                    };
                    if (!!folder.id && !!folder.parent && !!folder.data) {
                        result[idx++] = folder;
                    }
                }
            }
            return result;
        }

        function getParentNode(pathId) {
            var elements = $scope.treeData;
            var parentPathId = pathId.substr(0, pathId.lastIndexOf('/'));
            parentPathId = $.trim(parentPathId).length == 0 ? '/' : parentPathId;
            for (var idx in elements) {
                if (elements[idx].id === parentPathId) {
                    return elements[idx];
                }
            }
            return null;
        }

        function pushNode(newElement) {
            var elements = $scope.treeData;
            var exists = false;
            for (var idx in elements) {
                var el = elements[idx];
                if (el.id === newElement.id) {
                    exists = true;
                }
            }
            if (!exists) {
                elements.push(newElement);
            }
        }

        $scope.uploadFileModal = function(selectedFiles) {
            if ($scope.showAllServices) {
                return;
            } else {
                var parameters = {
                    "currentFolder": $scope.currentFolder
                };
                uploadDialogService.uploadFileModal(parameters, selectedFiles, function(folder) {
                        if ($scope.currentFolder === folder) {
                            getFiles($scope.currentFolder);
                        }
                    },
                    $scope);
            }
        };

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
            if ($scope.pageTitle === $filter('translate')('MyDrive') &&
                $scope.currentFolder.path === $filter('translate')('mydrive.root.path') &&
                newFolderName.toLowerCase() === $filter('translate')('myvault.folder.name').toLowerCase()) {
                handleCreateFolderError($filter('translate')('create.folder.name.reserved', {
                    folder_name: newFolderName
                }));
                return;
            }
            if (!/^[\u00C0-\u1FFF\u2C00-\uD7FF\w- ]*$/.test(newFolderName)) {
                handleCreateFolderError($filter('translate')('create_folder.name.incorrect_format'));
                return;
            }

            for (var i = $scope.repoContents.length - 1; i >= 0; i--) {
                if ($scope.repoContents[i]['name'].toLowerCase() === newFolderName.toLowerCase()) {
                    handleCreateFolderError($filter('translate')('create_folder.name.existedErr'));
                    return;
                }
            }
            var url = CONTEXT_PATH + "/RMSViewer/CreateFolder";
            var headers = {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
            };
            var folder = $scope.currentFolder;
            var params = $.param({
                createFolderName: newFolderName,
                repoId: folder.repoId ? folder.repoId : $stateParams.repoId,
                filePathId: folder.pathId,
                filePathDisplay: folder.path
            });
            networkService.post(url, params, headers, function(response) {
                $scope.isLoading = false;
                $scope.creatingFolder = false;
                var error = response.error;
                var opMsg;
                var isSuccess = false;
                if (error) {
                    opMsg = $filter('translate')('create_folder.fail');
                } else {
                    getFiles($scope.currentFolder);
                    opMsg = $filter('translate')('create_folder.success', {
                        folderName: newFolderName
                    });
                    isSuccess = true;
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: opMsg
                });
            });
        };

        var handleCreateFolderError = function(errMsg) {
            $scope.isLoading = false;
            $scope.creatingFolder = false;
            showSnackbar({
                isSuccess: false,
                messages: errMsg
            });
        };

        $scope.$watch('fileupload', function(newValue, oldValue) {
            if (newValue && $scope.fileupload) {
                $scope.uploadFileModal($scope.fileupload);
            }
        });

        $scope.removeNode = function(filePathDisplay) {
            for (var i = 0; i < $scope.treeData.length; i++) {
                if ($scope.treeData[i].id == filePathDisplay) {
                    $scope.treeData.splice(i, 1);
                    break;
                }
            }
        }

        $scope.fileIconName = function(eachFile) {
            return repositoryService.getIconName(eachFile);
        }

        $scope.onClickMyVault = function() {
            navService.setCurrentTab('shared_files');
            navService.setIsInAllFilesPage(false);
            $state.go(STATE_SHARED_FILES);
        }
        dismissSnackbar();
    }
]);
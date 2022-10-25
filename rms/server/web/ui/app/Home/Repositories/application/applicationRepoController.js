mainApp.controller('applicationRepoController', ['$rootScope', '$scope', 'sharedWorkspaceService', 'workSpaceService', 'repositoryService', 'serviceProviderService', '$timeout', '$filter', '$cookies', 'dialogService', 'initSettingsService', 'shareDialogService', 'protectWidgetService', '$state', 'networkService', '$stateParams', function($rootScope, $scope, sharedWorkspaceService, workSpaceService, repositoryService, serviceProviderService, $timeout, $filter, $cookies, dialogService, initSettingsService, shareDialogService, protectWidgetService, $state, networkService, $stateParams) {
    $scope.searchActivated = false;
    $scope.thisRepo = {};
    $scope.contents = [];
    $scope.repoList = [];
    $scope.parentFolder = null;
    $scope.showMoreFiles = false;
    $scope.emptyFolderExists = false;
    $scope.searchString = "";
    $scope.showSearch = false;
    $scope.showSort = false;
    $scope.pageTitle = "";
    $scope.canResetSearchResults = false;
    $scope.tenantId = initSettingsService.getSettings().tenantId;
    $scope.currentFolder = {
        path: "/"
    };

    $scope.isMenuClicked = false;

    var SIZE = 10;
    var rootFolder = {
        pathId: "/",
        path: "/",
        folder: true
    };

    $scope.sortOptions = [
        {
            sortBy: {
                type: 'lastModifiedTime',
                order: 'dsc'
            },
            description: 'last.modified'
        },
        {
            sortBy: {
                type: 'lastModifiedTime',
                order: 'asc'
            },
            description: 'first.modified'
        },
        {
            sortBy: {
                type: 'name',
                order: 'asc'
            },
            description: 'filename.ascending'
        },
        {
            sortBy: {
                type: 'name',
                order: 'dsc'
            },
            description: 'filename.descending'
        },
        {
            sortBy: {
                type: 'fileSize',
                order: 'asc'
            },
            description: 'file.size.ascending'
        },
        {
            sortBy: {
                type: 'fileSize',
                order: 'dsc'
            },
            description: 'file.size.descending'
        }
    ];
    $scope.selectedSort = $scope.sortOptions[0].sortBy;

    var initRepo = function() {
        repositoryService.getRepositories(function(data) {
            var allRepositories = data.results.repoItems;
            var currentRepoId = $stateParams.repoId;
            $scope.thisRepo = {};
    
            for(var i = 0; i < allRepositories.length; i++) {
                var currRepo = allRepositories[i];
    
                if(currRepo.repoId === currentRepoId) {
                    $scope.thisRepo = currRepo;
                    break;
                }
            }
            getRepoList();
            getFiles();
        });    
    };

    var getFiles = function(folder) {
        $scope.isLoading = true;
        var params = {
            searchString: '',
            path: '/'
        };

        if(folder) {
            params.path = folder.path;
        }
        sharedWorkspaceService.getFiles(params, $scope.thisRepo.repoId, function(data) {
            if(data.statusCode === 200) {
                $scope.contents = data.results.detail;
                $scope.emptyFolderExists = !$scope.searchActivated && $scope.contents.length === 0;
                $scope.sort();
                dismissSnackbar();
            } else {
                $scope.contents = [];
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('error.repository.cannot-retrieve-files')
                });
            }
            $scope.isLoading = false;
        });

        buildBreadCrumbs();
        $scope.searchActivated = false;
        $scope.searchString = "";
        $scope.noSearchResult = false;
        $scope.scrollToTop = true;
    };

    var buildBreadCrumbs = function() {
        var folder = $scope.currentFolder;
        if (folder.path === '/') {
            $scope.breadCrumbsContent = [];
            $scope.parentFolder = null;
        } else {
            var backOrRefresh = false;
            for (var i = 0; i < $scope.breadCrumbsContent.length; i++) {
                var breadCrumbEntry = $scope.breadCrumbsContent[i];
                if (breadCrumbEntry.path == folder.path) {
                    $scope.breadCrumbsContent = $scope.breadCrumbsContent.slice(0, i + 1);
                    backOrRefresh = true;
                    break;
                }
            }
            if (!backOrRefresh) {
                if (isSubFolder(folder) && $scope.searchActivated) {
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
    };

    var isSubFolder = function(folder) {
        return folder.path.split('/').length >= 3;
    };

    var getBreadCrumbsContentForSearch = function(folder) {
        var breadCrumbsContents = [];
        var folderPaths = folder.path.split('/');
        var folderPathEndPos = 0;
        for (var i = 1; i < folderPaths.length; i++) {
            folderPathEndPos += folderPaths[i].length + 1;
            var path = folder.path.substr(0, folderPathEndPos);
            breadCrumbsContents.push({
                path: path,
                pathId: folder.pathId,
                name: folderPaths[i],
                isFolder: true
            });
        }
        return breadCrumbsContents;
    };

    $scope.fileIconName = function(eachFile) {
        return repositoryService.getIconName(eachFile);
    };

    $scope.search = function() {
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
        var params = {
            path: '/',
            searchString: $scope.searchString
        };

        sharedWorkspaceService.getFiles(params, $scope.thisRepo.repoId, function(data) {
            if(data.statusCode === 200) {
                $scope.contents = [];
                $scope.noSearchResult = true;

                if(data.results.detail.length > 0) {
                    $scope.contents = data.results.detail;
                    $scope.noSearchResult = false;
                    $scope.sort();
                }

                dismissSnackbar();
            } else {
                $scope.contents = [];
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('error.repository.cannot-retrieve-files')
                });
            }
            $scope.searchActivated = true;
            $scope.isLoading = false;
        });
    };

    $scope.clearSearch = function() {
        $scope.searchString = "";
        dismissSnackbar();
        $scope.noSearchResult = false;
        $scope.searchActivated = false;
        $scope.refreshFilePage(rootFolder);
    };
    
    // used in mobile header template
    $scope.toggleSearch = function() {
        $scope.showSort = false;
        $scope.showSearch = true;
    };

    $scope.closeFileListOptions = function() {
        $scope.showSearch = false;
        $scope.showSort = false;
    };

    $scope.toggleSort = function() {
        $scope.showSort = true;
        $scope.showSearch = false;
    };
    // end

    $scope.refreshFilePage = function(folder) {
        dismissSnackbar();
        $scope.contents = [];
        $scope.currentFolder = folder;
        getFiles(folder);
    };

    $rootScope.$on("refreshApplicationRepoFileList", function(event, args) {
        $scope.refreshFilePage($scope.currentFolder);
    });

    var openViewer = function(data) {
        $scope.isLoading = false;
        var redirectUrl = data.viewerUrl;
        openSecurePopup(redirectUrl);
    };

    $scope.onClickFile = function(file) {
        if (file.isFolder) {
            $scope.refreshFilePage(file);
        } else {
            $scope.isLoading = true;
            var settings = initSettingsService.getSettings();

            var lastModifiedTime = file.lastModifiedTime;
            if (!lastModifiedTime) {
                lastModifiedTime = 0;
            }

            var params = $.param({
                filePath: file.pathId,
                filePathDisplay: file.path,
                repoId: $scope.thisRepo.repoId,
                lastModifiedDate: lastModifiedTime,
                userName: settings.userName,
                offset: new Date().getTimezoneOffset(),
                tenantName: settings.tenantName,
                repoName: $scope.thisRepo.name,
                repoType: $scope.thisRepo.type
            });
            sharedWorkspaceService.showFile(params, openViewer);
        }
    };

    $scope.onClickProtect = function(file) {
        // adding additional details not present in the "file" variable
        file.repoId = $scope.thisRepo.repoId;
        file.repoType = $scope.thisRepo.type;
        file.repoName = $scope.thisRepo.name;

        shareDialogService.shareFile({
            file: file,
            operation: "protect"
        });
    };

    $scope.sort = function() {        
        $scope.contents.sort(function(a , b) {
            var sortByType = $scope.selectedSort.type;
            var aSortItem = a[sortByType];
            var bSortItem = b[sortByType];
            
            // for ascending order, orderInteger is 1, else -1
            var orderInteger = 1;
            if($scope.selectedSort.order === 'dsc') {
                orderInteger = -1;
            }

            // when sortByType is by fileSize, and both a and b are folders, then default to order by name in ascending order (no fileSize for folders)
            if(sortByType === 'fileSize' && a.isFolder && b.isFolder) {
                sortByType = 'name';
                orderInteger = 1;
            }

            // if a is a folder but b is a file then a is earlier in the array
            if(a.isFolder && !b.isFolder) {
                return -1;
            }

            // if a is a file but b is a folder then b is earlier in the array
            if(!a.isFolder && b.isFolder) {
                return 1;
            }

            // To change the name strings to uppercase to ignore upper and lowercase for sorting names
            if(sortByType === 'name') {
                aSortItem = a[sortByType].toUpperCase();
                bSortItem = b[sortByType].toUpperCase();
            }

            // if a is supposed to be earlier in the array than b, return -1
            if(aSortItem < bSortItem) {
                return -1 * orderInteger;
            }
            // if b is supposed to be earlier in the array than a, return 1
            if(aSortItem > bSortItem) {
                return 1 * orderInteger;
            }
            // if a and b are equivalent and orders should not be changed, return 0
            return 0;
        });
    };

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

    $scope.showMenu = function(pathId) {
        $scope.selectedFileId = pathId;
        $scope.isMenuClicked = true;
    };

    $scope.hideMenu = function() {
        $scope.isMenuClicked = false;
    };

    var openInfoWindow = function(data) {
        if (data.statusCode === 200) {
            dialogService.info({
                tags: data.results.fileInfo.tags,
                rights: data.results.fileInfo.rights,
                endDate: data.results.fileInfo.expiry.endDate,
                startDate: data.results.fileInfo.expiry.startDate,
                owner: data.results.fileInfo.owner,
                nxl: data.results.fileInfo.protectedFile,
                fileName: data.results.fileInfo.name,
                fileSize: data.results.fileInfo.size,
                fileType: data.results.fileInfo.fileType,
                lastModifiedTime: data.results.fileInfo.lastModified,
                path: data.results.fileInfo.path,
                protectionType: data.results.fileInfo.protectionType
            });
        } else {
            var error = data.message;
            if(!error) {
                error = $filter('translate')('view.file.info.error');
            }
            if(data.statusCode === 404) {
                $rootScope.$emit("refreshApplicationRepoFileList");
            }
            showSnackbar({
                isSuccess: false,
                messages: error
            });
        }
        $scope.isLoading = false;
    };

    $scope.onClickInfo = function(file) {
        $scope.isLoading = true;
        var payload = {
            "parameters": {
                "path": file.path
            }
        };

        sharedWorkspaceService.getFileMetadata(payload, $scope.thisRepo.repoId, function(data) {
            openInfoWindow(data);
        });
    };

    initRepo();
}]);
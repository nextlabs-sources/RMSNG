mainApp.factory('projectService', ['$http', '$filter', 'networkService', 'initSettingsService',
    function($http, $filter, networkService, initSettingsService) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var createProject = function(parameter, callback) {
            networkService.put(RMS_CONTEXT_NAME + "/rs/project", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var updateProject = function(parameter, projectId, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId, parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var createFolder = function(parameter, projectId, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + "/createFolder", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var modifyRights = function(parameter, projectId, callback) {
            networkService.put(RMS_CONTEXT_NAME + "/rs/project/" + projectId + "/file/classification", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var getProjectsCreated = function(queryParams, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/project";
            var query = [];
            var projectsCreatedUrl;
            if (queryParams) {
                if (queryParams.page) {
                    query.push('page=' + encodeURIComponent(queryParams.page));
                }
                if (queryParams.size) {
                    query.push('size=' + encodeURIComponent(queryParams.size));
                }
                if (queryParams.orderBy) {
                    query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
                }
            }
            if (query.length > 0) {
                projectsCreatedUrl = url + '?' + query.join('&') + '&ownedByMe=true';
            } else {
                projectsCreatedUrl = url + '?ownedByMe=true';
            }
            networkService.get(projectsCreatedUrl, getBasicHeaders(), function(data) {
                if (data != null && data.statusCode == 200) {
                    var projectList = data.results;
                    callback(projectList);
                } else {
                    callback();
                }
            });
        }

        var inviteUsersToProject = function(parameter, projectId, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + "/invite", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }

            });
        }

        var revokeInvitation = function(parameter, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/revokeInvite", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }

            });
        }

        var sendReminder = function(parameter, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/sendReminder", parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }

            });
        }

        var getProjectsInvited = function(queryParams, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/project";
            var query = [];
            var projectsInvitedUrl;
            if (queryParams) {
                if (queryParams.page) {
                    query.push('page=' + encodeURIComponent(queryParams.page));
                }
                if (queryParams.size) {
                    query.push('size=' + encodeURIComponent(queryParams.size));
                }
                if (queryParams.orderBy) {
                    query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
                }
            }
            if (query.length > 0) {
                projectsInvitedUrl = url + '?' + query.join('&') + '&ownedByMe=false';
            } else {
                projectsInvitedUrl = url + '?ownedByMe=false';
            }
            networkService.get(projectsInvitedUrl, getBasicHeaders(), function(data) {
                if (data != null && data.statusCode == 200) {
                    var projectList = data.results;
                    callback(projectList);
                } else {
                    callback();
                }
            });
        }

        var getProjectList = function(queryParams, projectsCreatedCallback, projectsInvitedCallback) {
            getProjectsCreated(queryParams, projectsCreatedCallback);
            getProjectsInvited(queryParams, projectsInvitedCallback);
        }

        var getProject = function(id, callback) {
            networkService.get(RMS_CONTEXT_NAME + "/rs/project/" + id, getBasicHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var getMemberList = function(project, url, queryParams, callback) {
            var url = RMS_CONTEXT_NAME + url;
            var query = [];
            if (queryParams) {
                if (queryParams.page) {
                    query.push('page=' + encodeURIComponent(queryParams.page));
                }
                if (queryParams.size) {
                    query.push('size=' + encodeURIComponent(queryParams.size));
                }
                if (queryParams.orderBy) {
                    query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
                }
                if (queryParams.picture) {
                    query.push('picture=' + encodeURIComponent(queryParams.picture));
                }
                if (queryParams.searchString) {
                    var searchFields = ["email", "name"];
                    query.push('q=' + searchFields + '&searchString=' + encodeURIComponent(queryParams.searchString));
                }
            }
            if (query.length > 0) {
                url = url + '?' + query.join('&');
            }
            networkService.get(url, getBasicHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(project, data);
                }
            })
        }

        var getMemberDetails = function(projectId, memberId, queryParams, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/member/' + memberId;
            var query = [];
            if (queryParams) {
                if (queryParams.picture) {
                    query.push('picture=' + encodeURIComponent(queryParams.picture));
                }
            }
            if (query.length > 0) {
                url = url + '?' + query.join('&');
            }
            networkService.get(url, getBasicHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            })
        }

        var removeMember = function(projectId, parameter, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/members/remove', parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var getProjectFiles = function(projectId, queryParams, callback) {
            var url = RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/files';
            var query = [];
            if (queryParams) {
                if (queryParams.page) {
                    query.push('page=' + encodeURIComponent(queryParams.page));
                }
                if (queryParams.size) {
                    query.push('size=' + encodeURIComponent(queryParams.size));
                }
                if (queryParams.orderBy) {
                    query.push('orderBy=' + encodeURIComponent(queryParams.orderBy));
                }
                if (queryParams.pathId) {
                    query.push('pathId=' + encodeURIComponent(queryParams.pathId));
                }
                if (queryParams.filter) {
                    query.push('filter=' + encodeURIComponent(queryParams.filter));
                }
                if (queryParams.searchString) {
                    var searchFields = ["name"];
                    query.push('q=' + searchFields + '&searchString=' + encodeURIComponent(queryParams.searchString));
                }
            }
            if (query.length > 0) {
                url = url + '?' + query.join('&');
            }
            networkService.get(url, getBasicHeaders(), function(data) {
                if (callback && typeof(callback) == 'function') {
                    callback(data);
                }
            })
        }

        var getFileDetails = function(parameter, projectId, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/file/metadata', parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var checkIfFilePathExists = function(pathId, projectId, callback) {

            if(!pathId.endsWith(".nxl")){
                pathId = pathId + ".nxl";
            }
            
            var parameter = {
                "parameters": {
                    "pathId": pathId
                }
            };

            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/file/checkIfExists', parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var deleteFilesFolders = function(parameter, projectId, callback) {
            networkService.post(RMS_CONTEXT_NAME + "/rs/project/" + projectId + '/delete', parameter, getJsonHeaders(), function(data) {
                if (callback && typeof(callback) == "function") {
                    callback(data);
                }
            });
        }

        var downloadProjectFile = function(projectId, filePath) {
            window.open(RMS_CONTEXT_NAME + "/RMSViewer/DownloadFileFromProject?pathId=" + encodeURIComponent(filePath) +
                "&projectId=" + encodeURIComponent(projectId));
        }

        var decryptProjectFile = function(projectId, filePath) {
            window.open(RMS_CONTEXT_NAME + "/RMSViewer/DownloadFileFromProject?pathId=" + encodeURIComponent(filePath) +
                "&projectId=" + encodeURIComponent(projectId) + "&decrypt=true");
        }

        var showFile = function(folder, params, callback) {
            networkService.post(VIEWER_URL + "/RMSViewer/ShowProjectFile", params, {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
            }, callback);
        };

        var getExpireDateText = function(trialEndTime) {
            var now = new Date();
            now.setHours(0);
            now.setMinutes(0);
            now.setSeconds(0);
            now.setMilliseconds(0);
            var trialEnd = new Date(trialEndTime);
            trialEnd.setHours(0);
            trialEnd.setMinutes(0);
            trialEnd.setSeconds(0);
            trialEnd.setMilliseconds(0);
            if (now.getTime() <= trialEnd.getTime()) {
                var remainDays = Math.floor((trialEnd.getTime() - now.getTime()) / 86400000);
                return $filter('translate')('project.trial.end', {
                    remainDays: remainDays
                });
            } else {
                return $filter('translate')('project.trial.expired');
            }
        }

        var getAllProjectList = function(callback) {
            var url = RMS_CONTEXT_NAME + "/rs/project";
            networkService.get(url, getBasicHeaders(), function(data) {
                if (data != null && data.statusCode == 200) {
                    var projectList = data.results;
                    callback(projectList);
                } else {
                    callback();
                }
            });
        }

        var getSortOptions = function(isSharedWithMe) {
            if (isSharedWithMe) {
                return [{
                        'lookupCode': '-sharedDate',
                        'description': 'last.modified'
                    },
                    {
                        'lookupCode': 'sharedDate',
                        'description': 'first.modified'
                    },
                    {
                        'lookupCode': 'name',
                        'description': 'filename.ascending'
                    },
                    {
                        'lookupCode': '-name',
                        'description': 'filename.descending'
                    },
                    {
                        'lookupCode': 'size',
                        'description': 'file.size.ascending'
                    },
                    {
                        'lookupCode': '-size',
                        'description': 'file.size.descending'
                    },
                    {
                        'lookupCode': '-sharedBy',
                        'description': 'sharedby.descending'
                    },
                    {
                        'lookupCode': 'sharedBy',
                        'description': 'sharedby.ascending'
                    }
                ];
            } else {
                    return [{
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
            }
        }

        return {
            createProject: createProject,
            updateProject: updateProject,
            createFolder: createFolder,
            getProjectsCreated: getProjectsCreated,
            getProjectsInvited: getProjectsInvited,
            getProject: getProject,
            getMemberList: getMemberList,
            getProjectFiles: getProjectFiles,
            getProjectList: getProjectList,
            getFileDetails: getFileDetails,
            deleteFilesFolders: deleteFilesFolders,
            downloadProjectFile: downloadProjectFile,
            inviteUsersToProject: inviteUsersToProject,
            showFile: showFile,
            getMemberDetails: getMemberDetails,
            removeMember: removeMember,
            revokeInvitation: revokeInvitation,
            sendReminder: sendReminder,
            getExpireDateText: getExpireDateText,
            getAllProjectList: getAllProjectList,
            decryptProjectFile: decryptProjectFile,
            getSortOptions: getSortOptions,
            checkIfFilePathExists: checkIfFilePathExists,
            modifyRights: modifyRights
        }
    }
]);
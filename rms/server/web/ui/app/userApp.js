var STATE_LANDING = "Root";
var STATE_HOME = "Home";
var STATE_REPOSITORIES = "Home.Repositories";
var STATE_MYSPACE = "Home.MySpace";
var STATE_ALL_REPOSITORIES = "Home.AllRepositories";
var STATE_MANAGE_REPOSITORIES = "Home.ManageRepositories";
var STATE_SHARED_FILES = "Home.SharedFiles";
var STATE_WORKSPACE_FILES = "Workspace"
var STATE_DELETED_FILES = "Home.DeletedFiles";
var STATE_SHARED_ACTIVE_FILES = "Home.ActiveShares";
var STATE_MANAGE_lOCAL_FILE = "Home.ManageLocalFile";
var STATE_VIEW_SHARED_FILE = "Home.ViewSharedFiles";
var STATE_FAVORITES = "Home.Favorites";
var STATE_SETTINGS = "Home.Settings";
var STATE_SYSTEM_SETTINGS = "Home.SystemSettings";
var STATE_DOWNLOAD_RMD = "Home.DownloadRMD";
var STATE_USER_PREFERENCE = "UserPreference";
var STATE_WELCOME_PAGE = "Home.Welcome";
var STATE_FEEDBACK_PAGE = "Home.Feedback";
var STATE_NETWORKERROR = "Home.NetworkError";
var STATE_PROJECT = "Project";
var STATE_CREATE_PROJECT = "Project.CreateProject";
var STATE_UPGRADE_PROJECT = "Project.UpgradeProject";
var STATE_PROJECT_INVITATION_EMAIL_MISMATCH = "Project.EmailMismatch";
var STATE_PROJECT_UNAUTHORIZED = "Project.Unauthorized";
var STATE_PROJECT_HOME = "Project.Home";
var STATE_PROJECT_FILES = "Project.Home.Files";
var STATE_PROJECT_USERS = "Project.Home.Users";
var STATE_PROJECT_POLICIES = "Project.Home.Policies";
var STATE_PROJECT_POLICIES_LIST = "Project.Home.Policies.List";
var STATE_PROJECT_POLICIES_CREATE = "Project.Home.Policies.Create";
var STATE_PROJECT_POLICIES_EDIT = "Project.Home.Policies.Edit";
var STATE_RIGHTS_HELP = "Help.UserDefinedRights";
var STATE_HELP_COMPANY_DEFINED_RIGHTS = "Help.CompanyDefinedRights";
var STATE_SUPPORTED_FILETYPES = "Supported.Files";
var STATE_PROJECT_SUMMARY = "Project.Summary";
var STATE_PROJECT_CONFIGURATION = "Project.Home.Configuration";
var STATE_PROJECT_CONFIGURATION_INFO = "Project.Home.Configuration.Info";
var STATE_PROJECT_CONFIGURATION_ACCESS_PERMISSIONS = "Project.Home.Configuration.AccessPermissions";
var STATE_PROJECT_CONFIGURATION_CLASSIFICATION = "Project.Home.Configuration.Classification";
var STATE_PROJECT_CONFIGURATION_PREFERENCE = "Project.Home.Configuration.Preference";

var userApp = angular.module('userApp', ['ui.router', 'ui.bootstrap', 'uiSwitch', 'ngSanitize', 'pascalprecht.translate', 'templates-main', 'ngJsTree', 'ngFileUpload', 'angularResizable', 'ngAnimate', 'ngCookies', 'ngclipboard', 'angular-md5', 'trNgGrid', 'dndLists']);

userApp.config(['$stateProvider', '$urlRouterProvider', '$translateProvider', '$httpProvider', '$uibTooltipProvider', function($stateProvider, $urlRouterProvider, $translateProvider, $httpProvider, $uibTooltipProvider) {

    $httpProvider.defaults.withCredentials = true;
    delete $httpProvider.defaults.headers.common['X-Requested-With'];

    $urlRouterProvider.otherwise('/home');
    $stateProvider
        .state(STATE_LANDING, {
            url: '/home',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/landingPage.html'
                }
            }
        })
        .state('Home', {
            url: '/personal',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/home.html'
                },
                'bannerView@Home': {
                    templateUrl: 'ui/app/Home/banner.html'
                },
                'menuView@Home': {
                    templateUrl: 'ui/app/Home/menu.html'
                },
                'contentView@Home': {
                    templateUrl: 'ui/app/Home/content.html'
                }
            }
        })
        .state(STATE_WELCOME_PAGE, {
            url: '/welcome',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/welcome.html'
                }
            }
        })
        .state(STATE_DOWNLOAD_RMD, {
            url: '/downloadRMD',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/downloadRMD.html'
                }
            }
        })
        .state(STATE_USER_PREFERENCE, {
            url: '/userPreference',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/userPreference.html'
                }
            }
        })
        .state(STATE_PROJECT, {
            url: '/projects',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/projects.html'
                }
            }
        })
        .state(STATE_CREATE_PROJECT, {
            url: '/createNewProject',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/createNewProject.html'
                }
            }
        })
        .state(STATE_PROJECT_SUMMARY, {
            url: '/allProjects',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/allProjects.html'
                },
                'bannerView@Project.Summary': {
                    templateUrl: 'ui/app/Home/Projects/banner.html'
                }
            }
        })
        .state(STATE_UPGRADE_PROJECT, {
            url: '/upgradeProject',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/upgradeProject.html'
                }
            }
        })
        .state(STATE_PROJECT_INVITATION_EMAIL_MISMATCH, {
            url: '/emailMismatch',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/emailMismatch.html'
                }
            }
        })
        .state(STATE_PROJECT_UNAUTHORIZED, {
            url: '/unauthorized',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/unauthorized.html'
                }
            }
        })
        .state(STATE_PROJECT_HOME, {
            url: '/:projectId',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Projects/home.html'
                },
                'bannerView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/banner.html'
                },
                'menuView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/menu.html'
                },
                'contentView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/content.html'
                },
                'mainView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/partials/summary.html'
                }
            }
        })
        .state(STATE_PROJECT_FILES, {
            url: '/files/:tenantId',
            views: {
                'mainView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/partials/files.html'
                }
            }
        })
        .state(STATE_PROJECT_USERS, {
            url: '/users',
            views: {
                'mainView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/partials/users.html'
                }
            }
        })
        .state(STATE_PROJECT_CONFIGURATION, {
            url: '/configuration',
            views: {
                'mainView@Project.Home': {
                    templateUrl: 'ui/app/Home/Projects/partials/config.html'
                }
            },
            params: {
                fromCreateProject: false
            }
        })
        .state(STATE_PROJECT_CONFIGURATION_INFO, {
            url: '/info',
            views: {
                'configView@Project.Home.Configuration': {
                    templateUrl: 'ui/app/Home/Projects/partials/configuration.html'
                }
            }
        })
        .state(STATE_PROJECT_CONFIGURATION_ACCESS_PERMISSIONS, {
            url: '/access',
            views: {
                'configView@Project.Home.Configuration': {
                    templateUrl: 'ui/app/Home/Projects/partials/accessPermissions.html'
                }
            }
        })
        .state(STATE_PROJECT_CONFIGURATION_CLASSIFICATION, {
            url: '/classification/:tenantId',
            views: {
                'configView@Project.Home.Configuration': {
                    templateUrl: 'ui/app/Home/Projects/partials/classification.html'
                }
            }
        })
        .state(STATE_PROJECT_CONFIGURATION_PREFERENCE, {
            url: '/preference',
            views: {
                'configView@Project.Home.Configuration': {
                    templateUrl: 'ui/app/Home/Projects/partials/preference.html'
                }
            }
        })
        .state(STATE_PROJECT_POLICIES, {
            url: '/policies/:tenantId',
            views: {
                'mainView@Project.Home': {
                    templateUrl: 'ui/app/Home/PolicyStudio/policyStudio.html'
                }
            }
        })
        .state(STATE_PROJECT_POLICIES_LIST, {
            url: '/list',
            views: {
                'policyView@Project.Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyList.html'
                }
            }
        })
        .state(STATE_PROJECT_POLICIES_CREATE, {
            url: '/create',
            views: {
                'policyView@Project.Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_PROJECT_POLICIES_EDIT, {
            url: '/edit/:policyId',
            views: {
                'policyView@Project.Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_REPOSITORIES, {
            url: '/repositories/:repoId',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Repositories/repository.html'
                }
            },
            params: {
                repoName: null,
                folder: null,
                repoType: null,
                inSearchContext: false
            }
        })
        .state(STATE_MYSPACE, {
            url: '/mySpace',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/mySpace.html'
                }
            }
        })
        .state(STATE_ALL_REPOSITORIES, {
            url: '/repositories',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Repositories/fileList.html'
                }
            },
            params: {
                all: true
            }
        })
        .state(STATE_FAVORITES, {
            url: '/favorites',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Repositories/favorites.html'
                }
            }
        })
        .state(STATE_MANAGE_REPOSITORIES, {
            url: '/manageRepositories',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Repositories/manageRepositories.html'
                }
            }
        })
        .state(STATE_SHARED_FILES, {
            url: '/sharedFiles',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/SharedFiles/sharedFileList.html'
                }
            }
        })
        .state(STATE_MANAGE_lOCAL_FILE, {
            url: '/manageLocalFile',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/LocalFiles/manageLocalFile.html'
                }
            }
        })
        .state(STATE_VIEW_SHARED_FILE, {
            url: '/viewSharedFile',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/SharedFiles/viewSharedFile.html'
                }
            }
        })
        .state(STATE_SETTINGS, {
            url: '/settings',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/settings.html'
                }
            }
        })
        .state(STATE_DELETED_FILES, {
            url: '/deletedFiles',
            views: {

                'mainView@Home': {
                    templateUrl: 'ui/app/Home/SharedFiles/DeletedFiles.html'
                }
            }
        })
        .state(STATE_SHARED_ACTIVE_FILES, {
            url: '/activeShares',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/SharedFiles/ActiveSharedFiles.html'
                }
            }
        })
        .state(STATE_SYSTEM_SETTINGS, {
            url: '/systemSettings',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/systemSettings.html'
                }
            }
        }).state(STATE_FEEDBACK_PAGE, {
            url: '/feedback',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Profile/feedback.html'
                }
            }
        }).state(STATE_NETWORKERROR, {
            url: '/networkError',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/networkError.html'
                }
            }
        })

    if (!initSettingsData.isHideWorkspace) {
        $stateProvider.state(STATE_WORKSPACE_FILES, {
            url: '/workspace',
            views: {
                '': {
                    templateUrl: 'ui/app/Home/Workspace/workspaceHome.html'
                },
                'mainView@Workspace': {
                    templateUrl: 'ui/app/Home/Workspace/workspaceFileList.html'
                },
                'contentView@Workspace': {
                    templateUrl: 'ui/app/Home/Workspace/workspaceContent.html'
                },
                'bannerView@Workspace': {
                    templateUrl: 'ui/app/Home/Workspace/workspaceBanner.html'
                },
                'menuView@Workspace': {
                    templateUrl: 'ui/app/Home/Workspace/workspaceMenu.html'
                }
            }
        });
    }

    $translateProvider.useLoader('customTranslateLoader');
    $translateProvider.preferredLanguage('en');
    // Enable escaping of HTML
    $translateProvider.useSanitizeValueStrategy('escape');
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

    $uibTooltipProvider.options({
        appendToBody: false
    });

}]);

userApp.factory('customTranslateLoader', ['$q', '$http', function ($q, $http) {
    return function (options) {
        var deferred = $q.defer();
        $http({
            method: 'GET',
            url: CONTEXT_PATH + '/ui/app/i18n/' + options.key + '.json',
            params: {
                v: VERSION
            }
        }).then(function successCallback(response) {
            //i18n_data is initialized in main.jsp
            i18n_data = response.data;
            return deferred.resolve(response.data);
        }, function errorCallback(response) {
            return deferred.reject(options.key);
        });
        return deferred.promise;
    };
}]);

userApp.run(['initSettingsService', '$rootScope', '$timeout', function (initSettingsService, $rootScope, $timeout) {
    //initSettingsData and CONTEXT_PATH are initialized in main.jsp
    initSettingsService.loadSettings(initSettingsData);
    initSettingsService.setRMSContextName(CONTEXT_PATH);
    $rootScope.$on('$viewContentLoaded', function (event, toState, toParams, fromState, fromParams) {
        $timeout(function () {
            initSettingsService.setRightPanelMinHeight();
        });
    });
    $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams) {
        dismissSnackbar();
    })
}]);

var STATE_ADMIN_LANDING = "Home";
var STATE_TENANT_ADMIN_LANDING = "Tenant";
var STATE_SETTINGS = "Home.Settings";
var STATE_TENANT_CONFIG_SERVICE_PROVIDERS = "TenantConfig.ServiceProviders";
var STATE_DOWNLOAD_RMD = "Home.DownloadRMD";
var STATE_USER_PREFERENCE = "UserPreference";
var STATE_TENANT_CONFIG = "TenantConfig";
var STATE_TENANT_CONFIG_SYSTEM = "TenantConfig.System";
var STATE_WELCOME_PAGE = "Home.Welcome";
var STATE_FEEDBACK_PAGE = "Home.Feedback";
var STATE_NETWORKERROR = "Home.NetworkError";
var STATE_ADMIN_UNAUTHORIZED = "Unauthorized";
var STATE_ADMIN_TENANT_CLASSIFICATION = "Home.Classification";
var STATE_TENANT_ADMIN_TENANT_CLASSIFICATION = "Tenant.Classification";
var STATE_ADMIN_TENANT_POLICIES = "Home.Policies";
var STATE_ADMIN_TENANT_POLICIES_LIST = "Home.Policies.List";
var STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES = "Tenant.ProjectPolicies";
var STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST = "Tenant.ProjectPolicies.List";
var STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE = "Tenant.ProjectPolicies.CREATE";
var STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT = "Tenant.ProjectPolicies.Edit";
var STATE_ADMIN_TENANT_POLICIES_CREATE = "Home.Policies.CREATE";
var STATE_ADMIN_TENANT_POLICIES_EDIT = "Home.Policies.Edit";
var STATE_TENANT_ADMIN_TENANT_POLICIES = "Tenant.Policies";
var STATE_TENANT_ADMIN_TENANT_POLICIES_LIST = "Tenant.Policies.List";
var STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE = "Tenant.Policies.CREATE";
var STATE_TENANT_ADMIN_TENANT_POLICIES_EDIT = "Tenant.Policies.Edit";
var STATE_ADMIN_TENANT_PROJECT_ADMIN_ASSIGN = "Tenant.ProjectAdminAssign";
var STATE_ADMIN_SYSTEM_CONFIGURATION = "Home.SystemSettings";
var STATE_ADMIN_TENANT_CONFIGURATION = "Tenant.TenantSettings";
var STATE_ADMIN_SELECT_USER_ATTRIBUTE = "Home.UserAttribute";
var STATE_TENANT_ADMIN_SELECT_USER_ATTRIBUTE = "Tenant.UserAttribute";
var STATE_ADMIN_TENANT_CONFIGURE_SERVICE_PROVIDER = "Tenant.ServiceProviders";
var STATE_ADMIN_TENANT_SYSTEM_CONFIGURATION = "Home.SystemSettings";
var STATE_ADMIN_TENANT = "Home.Tenant";
var STATE_ADMIN_TENANT_LIST = "Home.Tenant.LIST";
var STATE_ADMIN_TENANT_CREATE = "Home.Tenant.CREATE";
var STATE_ADMIN_TENANT_EDIT = "Home.Tenant.EDIT";
var STATE_ADMIN_TENANT_CONFIGURE_IDENTITY_PROVIDER = "Tenant.IdentityProviders";
var STATE_ADMIN_TENANT_MANAGE_PROJECT_TAGS = "Tenant.ProjectTags";
var STATE_MANAGE_USER = "Home.User";

var STATE_RIGHTS_HELP = "Help.UserDefinedRights";

var adminApp = angular.module('adminApp', ['ui.router', 'ui.bootstrap', 'uiSwitch', 'ngSanitize', 'pascalprecht.translate', 'templates-main', 'ngJsTree', 'ngFileUpload', 'angularResizable', 'ngAnimate', 'ngCookies', 'ngclipboard', 'angular-md5', 'trNgGrid', 'dndLists']);

adminApp.config(['$stateProvider', '$urlRouterProvider', '$translateProvider', '$httpProvider', '$uibTooltipProvider', function($stateProvider, $urlRouterProvider, $translateProvider, $httpProvider, $uibTooltipProvider) {

    $httpProvider.defaults.withCredentials = true;
    delete $httpProvider.defaults.headers.common['X-Requested-With'];

    $urlRouterProvider.otherwise('/home');
    $stateProvider
        .state(STATE_ADMIN_LANDING, {
            url: '/home',
            views: {
                '': {
                    templateUrl: 'ui/app/Admin/adminLandingPage.html'
                },
                'menuView@Home': {
                    templateUrl: 'ui/app/Admin/Super/partials/superAdminLandingMenu.html'
                },
                'contentView@Home': {
                    templateUrl: 'ui/app/Home/content.html'
                },
                'mainView@Home': {
                    templateUrl: 'ui/app/Admin/Super/partials/superAdminDashboard.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_LANDING, {
            url: '/tenant',
            views: {
                '' : {
                    templateUrl: 'ui/app/Admin/adminHome.html'
                },
                'menuView@Tenant': {
                    templateUrl: 'ui/app/Admin/adminMenu.html'
                },
                'contentView@Tenant': {
                    templateUrl: 'ui/app/Home/content.html'
                },
                'bannerView@Tenant' : {
                    templateUrl: 'ui/app/Admin/adminBanner.html'
                }
            }
        })
        .state(STATE_ADMIN_UNAUTHORIZED, {
            url: '/unauthorized',
            views: {
                '': {
                    templateUrl: 'ui/app/Admin/unauthorized.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_CLASSIFICATION, {
            url: '/classification/:tenantId',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Projects/partials/adminClassification.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_TENANT_CLASSIFICATION, {
            url: '/classification/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Home/Projects/partials/classification.html'
                }
            }
        })
        .state(STATE_ADMIN_SYSTEM_CONFIGURATION, {
            url: '/systemConfiguration',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/settings.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_CONFIGURATION, {
            url: '/tenantConfiguration/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Home/settings.html'
                }
            }
        })
        .state(STATE_ADMIN_SELECT_USER_ATTRIBUTE, {
            url: '/userAttribute/:tenantId',
            views: {
                'mainView@Home' : {
                    templateUrl: 'ui/app/Admin/Common/adminSelectUserAttribute.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_SELECT_USER_ATTRIBUTE, {
            url: '/userAttribute/:tenantId',
            views: {
                'mainView@Tenant' : {
                    templateUrl: 'ui/app/Admin/Common/selectUserAttribute.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_PROJECT_ADMIN_ASSIGN, {
            url: '/projectAdmins/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Admin/Tenant/AssignProjAdmins.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_MANAGE_PROJECT_TAGS, {
            url: '/projectTags/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Admin/Tenant/projectTags.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_CONFIGURE_SERVICE_PROVIDER, {
            url: '/serviceProviders/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Admin/Tenant/serviceProviders.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_CONFIGURE_IDENTITY_PROVIDER, {
            url: '/identityProviders/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Admin/Tenant/identityProviders.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_POLICIES, {
            url: '/policies/:tenantId',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/PolicyStudio/adminPolicyStudio.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_POLICIES_LIST, {
            url: '/list',
            views: {
                'policyView@Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/adminPolicyList.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_POLICIES_CREATE, {
            url: '/create',
            views: {
                'policyView@Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES, {
            url: '/projectPolicies/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Home/PolicyStudio/adminProjectPolicyStudio.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST, {
            url: '/list',
            views: {
                'policyView@Tenant.ProjectPolicies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/adminProjectPolicyList.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT, {
            url: '/edit/:policyId',
            views: {
                'policyView@Tenant.ProjectPolicies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/projectPolicyDetails.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE, {
            url: '/create',
            views: {
                'policyView@Tenant.ProjectPolicies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/projectPolicyDetails.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_POLICIES_EDIT, {
            url: '/edit/:policyId',
            views: {
                'policyView@Home.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_TENANT_POLICIES, {
            url: '/policies/:tenantId',
            views: {
                'mainView@Tenant': {
                    templateUrl: 'ui/app/Home/PolicyStudio/policyStudio.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_TENANT_POLICIES_LIST, {
            url: '/list',
            views: {
                'policyView@Tenant.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyList.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE, {
            url: '/create',
            views: {
                'policyView@Tenant.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_TENANT_ADMIN_TENANT_POLICIES_EDIT, {
            url: '/edit/:policyId',
            views: {
                'policyView@Tenant.Policies': {
                    templateUrl: 'ui/app/Home/PolicyStudio/partials/policyDetails.html'
                }
            }
        })
        .state(STATE_NETWORKERROR, {
            url: '/networkError',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/networkError.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT, {
            url: '/tenant',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Home/Tenant/tenant.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_LIST, {
            url: '/list',
            params: {
                message: null
            },
            views: {
                'tenantView@Home.Tenant': {
                    templateUrl: 'ui/app/Home/Tenant/partials/tenantList.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_CREATE, {
            url: '/create',
            views: {
                'tenantView@Home.Tenant': {
                    templateUrl: 'ui/app/Home/Tenant/partials/addTenant.html'
                }
            }
        })
        .state(STATE_ADMIN_TENANT_EDIT, {
            url: '/edit',
            params: {
                tenant: null
            },
            views: {
                'tenantView@Home.Tenant': {
                    templateUrl: 'ui/app/Home/Tenant/partials/addTenant.html'
                }
            }
        })
        .state(STATE_MANAGE_USER, {
            url: '/user',
            views: {
                'mainView@Home': {
                    templateUrl: 'ui/app/Admin/Super/manageUser.html'
                }
            }
        });


    $translateProvider.useLoader('customTranslateLoader');
    $translateProvider.preferredLanguage('en');
    // Enable escaping of HTML
    $translateProvider.useSanitizeValueStrategy('escape');
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

    $uibTooltipProvider.options({
        appendToBody: false
    });

}]);

adminApp.factory('customTranslateLoader', ['$q', '$http', function($q, $http) {
    return function(options) {
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

adminApp.run(['initSettingsService', '$rootScope', '$timeout', function(initSettingsService, $rootScope, $timeout) {
    //initSettingsData and CONTEXT_PATH are initialized in main.jsp
    initSettingsService.loadSettings(initSettingsData);
    initSettingsService.setRMSContextName(CONTEXT_PATH);
    $rootScope.$on('$viewContentLoaded', function(event, toState, toParams, fromState, fromParams) {
        $timeout(function() {
            initSettingsService.setRightPanelMinHeight();
        });
    });
    $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams) {
        dismissSnackbar();
    })
}]);

var mainApp;
if (APPNAME === 'admin') {
    mainApp = adminApp;
} else {
    mainApp = userApp;
}

var mainApp = angular.module('mainApp', ['ui.router', 'ui.bootstrap', 'ngSanitize', 'pascalprecht.translate', 'ngJsTree', 'ngAnimate', 'ngCookies', 'ngclipboard', 'trNgGrid']);
var CONTEXT_PATH = "/rms";
var STATE_SHARED_FILES = "Home.SharedFiles";

mainApp.config(['$stateProvider','$translateProvider', '$httpProvider', function($stateProvider, $translateProvider, $httpProvider, $uibTooltipProvider) {
    $translateProvider.useLoader('customTranslateLoader');
    $translateProvider.preferredLanguage('en');
    // Enable escaping of HTML
    $translateProvider.useSanitizeValueStrategy('escape');
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

}]);

mainApp.factory('customTranslateLoader', ['$q', '$http', function($q, $http) {
    return function(options) {
        var deferred = $q.defer();
        $http({
            method: 'GET',
            url:  '/viewer/ui/app/i18n/rms_' + options.key + '.json',
            params: {
                v: VERSION
            }
        }).then(function successCallback(response) {
            i18n_data = response.data;
            return deferred.resolve(response.data);
        }, function errorCallback(response) {
            return deferred.reject(options.key);
        });
        return deferred.promise;
    };
}]);

mainApp.run(['initSettingsService', '$rootScope', '$timeout', function(initSettingsService, $rootScope, $timeout) {
    initSettingsService.loadSettings({'rmDownloadUrls':""});
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
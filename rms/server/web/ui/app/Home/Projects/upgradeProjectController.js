mainApp.controller('upgradeProjectController', ['$scope', '$rootScope', '$anchorScroll', '$state', 'initSettingsService', function($scope, $rootScope, $anchorScroll, $state, initSettingsService) {
    $scope.termsUrl = PROJECT_TERMS_URL;
    $scope.privatePolicyUrl = PROJECT_PRIVACY_URL;
    $scope.goToCreateProject = function() {
        $state.go(STATE_CREATE_PROJECT);
    }
    var stateParams;
    var rmsContextName = initSettingsService.getRMSContextName();

    $scope.goBack = function() {
        if ($scope.fromState && $scope.fromState !== STATE_CREATE_PROJECT) {
            $state.go($scope.fromState, stateParams);
        } else {
            $state.go(STATE_LANDING);
        }
    }

    $scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
        $scope.fromState = from.name;
        stateParams = fromParams;
    });

    $scope.projectHelp = function() {
        url = rmsContextName + '/help_users/index.html';
        openNewWindow(url, 'myWindow');
    }

    $anchorScroll();
}]);
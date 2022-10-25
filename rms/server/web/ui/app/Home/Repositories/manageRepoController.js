mainApp.controller('manageRepoController', ['$scope', '$rootScope', '$state', 'networkService', 'dialogService', '$location', 'repositoryService', '$filter', 'serviceProviderService', function($scope, $rootScope, $state, networkService, dialogService, $location, repositoryService, $filter, serviceProviderService) {
    $scope.isLoading = false;
    $scope.message = "";
    var urlParameters = $location.search();
    if (urlParameters.success || urlParameters.error) {
        $scope.isSuccess = urlParameters.success ? true : false;
        $scope.message = urlParameters.success ? urlParameters.success : urlParameters.error;
        repositoryService.setMessageParams(true, $scope.isSuccess, $scope.message);
    } else {
        repositoryService.setMessageParams(false, false, null);
    }

    var getAllowedServiceProviders = function() {
        dismissSnackbar();
        $scope.isLoading = true;
        $scope.serviceProviderPresent = false;
        $scope.allowedServiceProviders = {};

        serviceProviderService.getConfiguredServiceProviders(function(configuredResponse) {
            if(configuredResponse.statusCode === 200) {
                var configuredServiceProviderArray = configuredResponse.results.configuredServiceProviderSettingList;

                serviceProviderService.getSupportedServiceProviders(function(supportedResponse){
                    if(supportedResponse.statusCode === 200) {
                        var supportedServiceProviders = supportedResponse.results.supportedProvidersMap;

                        configuredServiceProviderArray.forEach(function(providerType) {
                            if(supportedServiceProviders[providerType].providerClass === 'PERSONAL') {
                                $scope.allowedServiceProviders[providerType] = supportedServiceProviders[providerType];
                            }
                        });
                    } else {
                        showSnackbar({
                            messages: supportedResponse.message,
                            isSuccess: false
                        });
                    }
                    $scope.serviceProviderPresent = Object.keys($scope.allowedServiceProviders).length > 0;

                    $scope.isLoading = false;
                    if ($scope.addRepositoryPop) {
                        $scope.addRepository(STATE_LANDING);
                    }        
                });
            } else {
                showSnackbar({
                    messages: configuredResponse.message,
                    isSuccess: false
                });
            }
        });
    };

    var getRepositories = function() {
        $scope.isLoading = true;
        $scope.repositories = {};
        $scope.hasPersonalRepository = false;
        $scope.hasSharedRepository = false;
        repositoryService.getManagedRepositories(function(data) {
            $scope.repositories = data;
            for (repository in $scope.repositories) {
                if ($scope.repositories[repository].isShared) {
                    $scope.hasSharedRepository = true;
                } else {
                    $scope.hasPersonalRepository = true;
                }
            }
            $scope.isLoading = false;
        });
    }

    var refreshRepoListIfNecessary = function(data) {
        if (data.result) {
            getRepositories();
            $rootScope.$broadcast("refreshRepoList", {});
        }
    }

    $scope.editRepository = function(repository) {
        dialogService.editRepository({
            "repository": repository
        }, function(data) {
            refreshRepoListIfNecessary(data);
        });
    }
    var edit = function(data) {
        getRepositories();
    }

    $scope.addRepository = function(fromState) {
        dialogService.addRepository({
            "allowedRepository": $scope.allowedServiceProviders,
            "fromState": fromState
        }, function(data) {
            refreshRepoListIfNecessary(data);
        });
    }

    $scope.getMessage = function() {
        return repositoryService.getMessage();
    }

    $scope.shouldDisplay = function() {
        return repositoryService.shouldDisplay();
    }

    $scope.isSuccess = function() {
        return repositoryService.isSuccess();
    }

    $scope.dismissMessage = function() {
        repositoryService.setMessageParams(false, false, null);
    }

    $scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
        $scope.addRepositoryPop = from.name === STATE_LANDING && to.name === STATE_MANAGE_REPOSITORIES;
    });

    getRepositories();
    getAllowedServiceProviders();
}]);
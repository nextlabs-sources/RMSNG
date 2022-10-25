mainApp.controller('serviceProvidersController', ['$scope', '$rootScope', '$state', 'networkService', 'dialogService', 'settingsService', 'serviceProviderService', '$location', '$filter',
    function($scope, $rootScope, $state, networkService, dialogService, settingsService, serviceProviderService, $location, $filter) {
        $scope.isLoading = false;
        $rootScope.$on("reloadSettings", function() {
            $scope.loadSettings();
        });
        $scope.loadSettings = function() {
            $scope.isLoading = true;

            serviceProviderService.getSupportedServiceProviders(function(supportedResponse) {
                var supportedServiceProviders = supportedResponse.results.supportedProvidersMap;
                $scope.serviceProviders = restructureServiceProviderData(supportedServiceProviders);

                serviceProviderService.getConfiguredServiceProvidersAsAdmin(function(configuredResponse) {
                    $scope.configuredServiceProviders = configuredResponse.results.configuredServiceProviderSettingList;

                    $scope.configuredServiceProviders.forEach(function(configuration) {
                        var providerClass = supportedServiceProviders[configuration.providerType].providerClass;
                        var provider = supportedServiceProviders[configuration.providerType].provider;
                        configuration.providerClass = providerClass;
                        configuration.provider = provider;

                        $scope.serviceProviders[provider].hasAtLeastOneConfigured = true;
                        $scope.serviceProviders[provider][providerClass].configured = true;
                        $scope.serviceProviders[provider][providerClass].configuration = configuration;
                    });

                    Object.keys($scope.serviceProviders).forEach(function(provider) {
                        var serviceProvider = $scope.serviceProviders[provider];
                        var supportedClasses = serviceProvider.supportedClasses;
                        $scope.serviceProviders[provider].fullyConfigured = true;
                        if($scope.serviceProviders[provider].hasAtLeastOneConfigured === undefined) {
                            $scope.serviceProviders[provider].hasAtLeastOneConfigured = false;
                        }

                        for (var i = 0; i < supportedClasses.length; i++) {
                            var supportedClass = supportedClasses[i];
                            if(!serviceProvider[supportedClass].configured) {
                                $scope.serviceProviders[provider].fullyConfigured = false;
                                break;
                            }
                        }
                    });

                    $scope.isLoading = false;
                });
            });

            // This function consumes the response from the api and produces a new data structure for service providers
            var restructureServiceProviderData = function(response) {
                var newServiceProviderObject = {};
                Object.keys(response).forEach(function(providerType) {
                    var currentServiceProviderObject = response[providerType];
                    var restructuredServiceProviderObject = {
                        name: currentServiceProviderObject.name,
                        type: providerType,
                        configured: false,
                        configuration: {}
                    };

                    if(newServiceProviderObject[currentServiceProviderObject.provider] === undefined) {
                        newServiceProviderObject[currentServiceProviderObject.provider] = {};
                    }

                    if(newServiceProviderObject[currentServiceProviderObject.provider].supportedClasses === undefined) {
                        newServiceProviderObject[currentServiceProviderObject.provider].supportedClasses = [];
                    }

                    newServiceProviderObject[currentServiceProviderObject.provider].displayName = currentServiceProviderObject.name;
                    newServiceProviderObject[currentServiceProviderObject.provider].supportedClasses.push(currentServiceProviderObject.providerClass);
                    newServiceProviderObject[currentServiceProviderObject.provider][currentServiceProviderObject.providerClass] = restructuredServiceProviderObject;
                });

                return newServiceProviderObject;
            };
        };

        $scope.addServiceProviderConfiguration = function(serviceProvider, providerName) {
            $scope.dismissMessage();
            dialogService.addServiceProviderConfiguration({
                serviceProvider: serviceProvider,
                providerName: providerName,
            });
        };

        $scope.editServiceProviderConfiguration = function(serviceProvider) {
            $scope.dismissMessage();
            dialogService.editServiceProviderConfiguration({
                serviceProvider: serviceProvider,
            });
        };

        $scope.isConfigurable = function(serviceProvider) {
            if (!serviceProvider || serviceProvider.supportedClasses && serviceProvider.supportedClasses.length == 0) {
                return false;
            }

            var hasApplicationAccount = serviceProvider.supportedClasses.includes('APPLICATION');
            return hasApplicationAccount || !hasApplicationAccount && !serviceProvider.fullyConfigured;
        };

        $scope.getMessage = function() {
            return serviceProviderService.getMessage();
        }

        $scope.shouldDisplay = function() {
            return serviceProviderService.shouldDisplay();
        }

        $scope.isSuccess = function() {
            return serviceProviderService.isSuccess();
        }

        $scope.dismissMessage = function() {
            serviceProviderService.setMessageParams(false, false, null);
        }

        $scope.dismissMessage();
        $scope.loadSettings();
    }
]);
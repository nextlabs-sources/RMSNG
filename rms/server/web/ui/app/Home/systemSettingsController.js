mainApp.controller('systemSettingsController', ['$scope', '$rootScope', '$filter', '$state', 'networkService', '$location', 'settingsService',
    function($scope, $rootScope, $filter, $state, networkService, $location, settingsService) {
        var configurations = null;
        //Load the data from server
        $scope.isLoading = false;
        $scope.cachedConfigurations = {};

        $scope.dismissStatus = function() {
            $scope.messageStatus = 0;
        }

        $scope.resetDataTypes = function() {
            if ($scope.configurations.SMTP_PASSWORD && $scope.configurations.SMTP_PASSWORD.length > 0) {
                $scope.configurations.showSMTPPassword = false;
            } else {
                $scope.configurations.showSMTPPassword = true;
            }

            $scope.messageStatus = 0;

            if ($.parseJSON($scope.configurations.SMTP_AUTH)) {
                $scope.configurations.SMTP_AUTH = true;
            } else {
                $scope.configurations.SMTP_AUTH = false;
            }

            if ($.parseJSON($scope.configurations.SMTP_ENABLE_TTLS)) {
                $scope.configurations.SMTP_ENABLE_TTLS = true;
            } else {
                $scope.configurations.SMTP_ENABLE_TTLS = false;
            }
            if ($.parseJSON($scope.configurations.DISABLE_FEEDBACK_MAIL)) {
                $scope.configurations.DISABLE_FEEDBACK_MAIL = true;
            } else {
                $scope.configurations.DISABLE_FEEDBACK_MAIL = false;
            }
        }

        var loadConfigurationSetting = function() {
            settingsService.getSystemSettings(function(data) {
                $scope.configurations = data;
                $scope.cachedConfigurations = angular.copy(data);
                $scope.resetDataTypes();
            });
        }

        var isValidInput = function(data) {
            if (data) {
                return true;
            }
            return false;
        }

        var isValidNumber = function(data) {
            data = data.trim();
            if (!isNaN(data)) {
                var input = parseInt(data);
                if (input > 0) {
                    return true;
                }
            }
            return false;
        }

        var isSMTPAuthValid = function(auth, username, password) {
            if (!auth) {
                return true;
            }
            if (isValidInput(username) && isValidInput(password)) {
                return true;
            }
            return false;
        }

        $scope.scrollTo = function(id) {
            var pos = $("#" + id).position();
            $(".container-fluid.rms-container-fluid").scrollTop(pos.top);
            $(".container-fluid.rms-container-fluid").scrollLeft(pos.left);
        };

        $scope.doReset = function() {
            $scope.configurations = angular.copy($scope.cachedConfigurations);
            $scope.resetDataTypes();
        };

        $scope.doSave = function() {
            $scope.isLoading = true;
            var params = {
                "SMTP_HOST": ($scope.configurations.SMTP_HOST) ? $scope.configurations.SMTP_HOST : "",
                "SMTP_PORT": ($scope.configurations.SMTP_PORT) ? $scope.configurations.SMTP_PORT : "",
                "SMTP_AUTH": ($scope.configurations.SMTP_AUTH) ? $scope.configurations.SMTP_AUTH : false,
                "SMTP_USER_NAME": ($scope.configurations.SMTP_USER_NAME) ? $scope.configurations.SMTP_USER_NAME : "",
                "SMTP_PASSWORD": ($scope.configurations.SMTP_PASSWORD) ? $scope.configurations.SMTP_PASSWORD : "",
                "SMTP_ENABLE_TTLS": ($scope.configurations.SMTP_PASSWORD) ? $scope.configurations.SMTP_ENABLE_TTLS : false,
                "DISABLE_FEEDBACK_MAIL": ($scope.configurations.DISABLE_FEEDBACK_MAIL) ? $scope.configurations.DISABLE_FEEDBACK_MAIL : false,
                "SUPPORT_EMAILID": ($scope.configurations.SUPPORT_EMAILID) ? $scope.configurations.SUPPORT_EMAILID : "",
                "RMC_TOKEN_EXPIRATION_TIME": ($scope.configurations.RMC_TOKEN_EXPIRATION_TIME) ? $scope.configurations.RMC_TOKEN_EXPIRATION_TIME : ""
            };

            settingsService.saveSystemSettings(function(data) {
                if (data.message) {
                    $scope.message = data.message;
                    $scope.messageStatus = 1;
                } else {
                    $scope.message = $filter('translate')('settings.saved.successfully');
                    $scope.messageStatus = 2;
                }
                $scope.isLoading = false;
                $scope.scrollTo('settings-pane');
            }, params, function(response) {
                $scope.isLoading = false;
            });
        }

        $scope.showSMTPPassword = function() {
            $scope.configurations.showSMTPPassword = true;
        }

        $scope.setFormDirty = function(formName) {
            formName.$setDirty();
        }

        loadConfigurationSetting();
    }
]);
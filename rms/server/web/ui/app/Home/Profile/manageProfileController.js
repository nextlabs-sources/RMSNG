mainApp.controller('manageProfileController', ['$scope', '$rootScope', '$cookies', '$timeout', '$state', 'dialogService', 'uploadDialogService',
    '$location', 'networkService', 'initSettingsService', '$filter', 'navService', '$uibModalInstance',
    function($scope, $rootScope, $cookies, $timeout, $state, dialogService, uploadDialogService,
        $location, networkService, initSettingsService, $filter, navService, $uibModalInstance) {

        $scope.page = "summary";
        $scope.profile = {};
        $scope.profile.displayName = "";
        $scope.profile.previousDisplayName = "";
        $scope.profile.email = "";
        $scope.isModalOpen = true;

        $scope.isLoading = false;
        $scope.changePasswordData = {};
        $scope.changePasswordData.oldPassword = "";
        $scope.changePasswordData.newPassword = "";
        $scope.changePasswordData.newPasswordConfirm = "";

        $scope.emailRegex = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        $scope.nameRegex = /^((?![\~\!\#\$\%\^\&\*\(\)\+\=\[\]\{\}\;\:\"\\\/\<\>\?]).)+$/;

        $scope.dismissStatus = function() {
            $scope.messageStatus = 0;
        }

        $scope.handleSuccess = function(message) {
            $scope.messageStatus = 2;
            $scope.message = message;
            $scope.isLoading = false;
        }

        $scope.handleError = function(message) {
            $scope.messageStatus = 1;
            $scope.message = message;
            $scope.isLoading = false;
        }

        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var getHeaders = function() {
            return {
                'Content-Type': 'application/json; charset=utf-8',
                'userId': $cookies.get('userId'),
                'ticket': $cookies.get('ticket'),
                'clientId': $cookies.get('clientId'),
                'platformId': $cookies.get('platformId')
            };
        }

        var toTitleCase = function(str) {
            return str.replace(/\w\S*/g, function(txt) {
                return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
            });
        }

        $scope.getProfile = function() {
            $scope.isLoading = true;
            networkService.get(RMS_CONTEXT_NAME + "/rs/usr/v2/profile/", getHeaders(), function(data) {
                $scope.isLoading = false;
                if (data != null && data.statusCode == 200) {
                    if ($scope.profile.previousDisplayName && $scope.profile.previousDisplayName !== data.extra.name) {
                        $scope.$emit('profile.displayName.updated', data.extra.name);
                    }
                    $scope.profile.email = data.extra.email;
                    $scope.profile.displayName = data.extra.name;
                    $scope.profile.previousDisplayName = $scope.profile.displayName;
                    $scope.loginAccountType = toTitleCase(initSettingsService.getSettings().loginAccountType);
                    $scope.editProfileAllowed = initSettingsService.getSettings().isManageProfileAllowed;
                } else {
                    $scope.handleError($filter('translate')('manage.profile.error'));
                }
            });
        }

        $scope.changeDisplayName = function() {
            $scope.profile.displayName = $scope.profile.displayName.trim();
            if (!$scope.nameRegex.test($scope.profile.displayName)) {
                $scope.handleError($filter('translate')('manage.profile.displayName.error.invalid'));
                $scope.getProfile();
                return;
            }

            if ($scope.profile.displayName.length > 150) {
                $scope.handleError($filter('translate')('manage.profile.displayName.long'));
                $scope.profile.displayName = $scope.profile.previousDisplayName;
                return;
            }

            $scope.isLoading = true;
            var params = {
                parameters: {
                    displayName: $scope.profile.displayName
                }
            };
            networkService.post(RMS_CONTEXT_NAME + "/rs/usr/profile", params, getHeaders(), function(data) {
                $scope.isLoading = false;
                if (data != null && data.statusCode == 200) {
                    $scope.handleSuccess($filter('translate')('manage.profile.displayName.success'));
                    $scope.getProfile();
                } else {
                    $scope.handleError($filter('translate')('manage.profile.displayName.error'));
                    $scope.profile.displayName = $scope.profile.previousDisplayName;
                }
            });
        }

        $scope.goToChangePassword = function() {
            $scope.page = "changePassword";
            $timeout(function() {
                document.getElementById("old-pwd-textbox").focus();
            });
        }

        $scope.goToManageProfile = function() {
            $scope.page = "manageProfile";
            $scope.profile.previousDisplayName = $scope.profile.displayName;
            $timeout(function() {
                document.getElementById("displayNameTextBox").focus();
            });
        }

        $scope.changePassword = function(form) {
            if ($scope.changePasswordData.oldPassword == $scope.changePasswordData.newPassword) {
                $scope.handleError($filter('translate')('user_newpassword_oldpassword_same'));
                return;
            }

            $scope.isLoading = true;
            var params = {
                parameters: {
                    oldPassword: CryptoJS.SHA256($scope.changePasswordData.oldPassword).toString(),
                    newPassword: CryptoJS.SHA256($scope.changePasswordData.newPassword).toString()
                }
            };

            networkService.post(RMS_CONTEXT_NAME + "/rs/usr/changePassword", params, getHeaders(), function(data) {
                $scope.isLoading = false;
                if (data != null && data.statusCode == 200) {
                    $scope.handleSuccess($filter('translate')('manage.profile.change.password.success'));
                    $scope.resetChangePasswordForm(form);
                } else if (data && data.statusCode == 4001) {
                    $scope.handleError($filter('translate')('manage.profile.change.password.error.invalid_current_password'));
                    document.getElementById("old-pwd-textbox").focus();
                } else if (data && data.statusCode == 4002) {
                    $scope.handleError($filter('translate')('manage.profile.change.password.error.locked'));
                    $scope.resetChangePasswordForm(form);
                    $timeout(initSettingsService.logout, 5000);
                } else {
                    $scope.handleError($filter('translate')('manage.profile.change.password.error'));
                    document.getElementById("old-pwd-textbox").focus();
                    $scope.resetChangePasswordForm(form);
                }
            });
        }

        $scope.cancel = function() {
            switch ($scope.page) {
                case 'manageProfile':
                    $scope.cancelManageProfile();
                    break;
                case 'changePassword':
                    $scope.cancelChangePassword();
                    break;
                default:
                    $scope.back();
            }
        }

        $scope.cancelChangePassword = function(form) {
            $scope.resetChangePasswordForm(form);
            $scope.back();
        }

        $scope.cancelManageProfile = function() {
            $scope.profile.displayName = $scope.profile.previousDisplayName;
            $scope.back();
        }

        $scope.resetChangePasswordForm = function(form) {
            $scope.changePasswordData = {};
            $scope.changePasswordData.oldPassword = "";
            $scope.changePasswordData.newPassword = "";
            $scope.changePasswordData.newPasswordConfirm = "";
            if(form) form.$setUntouched();
        }

        $scope.closeModalDialog = function() {
            $uibModalInstance.dismiss('cancel');
            $scope.isModalOpen = false;
        }

        $scope.back = function() {
            $scope.dismissStatus();
            $scope.page = "summary";
        }

        $scope.getProfile();
    }
]);
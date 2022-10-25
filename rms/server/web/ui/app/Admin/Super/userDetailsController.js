mainApp.controller('userDetailsController', ['$scope', 'userService', '$filter', 'Upload',
    function($scope, userService, $filter, Upload) {

        $scope.deleteCert = function(user) {
            userService.removeAPIUserCert(user.userId, function (data) {
                if (data && data.statusCode == 200) {
                    user.appCertImported = false;
                    showSnackbar({
                        isSuccess: true,
                        messages: $filter('translate')('api.user.cert.delete.success')
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: data.message
                    });
                }
                $scope.isLoading = false;
            });
        }

        $scope.uploadCert = function(files, user) {
            if (!files || !user) {
                return;
            }
            var url = CONTEXT_PATH + "/rs/usr/apiUser/cert";
            $scope.isLoading = true;
            var uploader = Upload.upload({
                url: url,
                headers: {
                    'userId': window.readCookie('userId'),
                    'ticket': window.readCookie('ticket'),
                    'clientId': window.readCookie('clientId'),
                    'platformId': window.readCookie('platformId')
                },
                data: {
                    file: files,
                    "API-input": JSON.stringify({
                        'parameters': {
                            'apiUserId' : user.userId
                        }
                    })
                }
            });
            
            uploader.then(function(response) {
                $scope.isLoading = false;
                var error = response.data.statusCode !== 200;
                if (error) {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('api.user.cert.upload.fail'),
                    });
                } else {
                    user.appCertImported = true;
                    showSnackbar({
                        isSuccess: true,
                        messages: $filter('translate')('api.user.cert.upload.success'),
                    });
                }
            }, errorHandler);
        }
    }
]);
mainApp.filter('reverse', function() {
    return function(items) {
        return items.slice().reverse();
    };
});

mainApp.controller('uploadProgressController', ['$interval', '$scope', '$rootScope', '$http', '$location', '$state', 'initSettingsService', 'dialogService', 'uploadFileService', '$filter', 'projectService', '$uibModal', '$uibModalStack',
    function($interval, $scope, $rootScope, $http, $location, $state, initSettingsService, dialogService, uploadFileService, $filter, projectService, $uibModal, $uibModalStack) {
        $scope.checked = true;
        $scope.uploadFileService = uploadFileService;
        $scope.minimize = uploadFileService.getMinimizeStatus();
        $scope.disabled = uploadFileService.getDisabledStatus();
        $scope.fileUploadingList = uploadFileService.getUploadingFileList();
        $scope.fileUploaded = uploadFileService.getUploadedStatus();
        $scope.uploadFileList = uploadFileService.getUploadFileList();
        $scope.close = uploadFileService.getCloseStatus();
        $scope.uploadFileSummary = $filter('translate')('file.upload.summary.default');

        $scope.closeNotification = function() {
            if ($scope.close === false) {
                if (checkUploadingStatus()) {
                    return;
                }
                uploadFileService.setUploadFileList([]);
                uploadFileService.setCloseStatus(true);
            }
        }

        var checkUploadingStatus = function() {
            for (var i = 0; i < $scope.uploadFileList.length; i++) {
                if ($scope.uploadFileList[i].fileUploading === true) {
                    return true;
                }
            }
            return false;
        }

        var getUploadingFileNum = function() {
            var uploadingFileNum = 0;
            for (var i = 0; i < $scope.uploadFileList.length; i++) {
                if ($scope.uploadFileList[i].fileUploaded === false && $scope.uploadFileList[i].fileUploading === true && $scope.uploadFileList[i].uploadFailed !== true) {
                    uploadingFileNum++;
                }
            }
            return uploadingFileNum;
        }

        var getUploadedFileNum = function() {
            var uploadedFileNum = 0;
            for (var i = 0; i < $scope.uploadFileList.length; i++) {
                if ($scope.uploadFileList[i].fileUploaded === true && $scope.uploadFileList[i].fileUploading === false && $scope.uploadFileList[i].uploadFailed !== true) {
                    uploadedFileNum++;
                }
            }
            return uploadedFileNum;
        }

        var constructUploadFileSummary = function() {
            var uploadingFileNum = getUploadingFileNum();
            if (uploadingFileNum === 0) {
                var uploadedFileNum = getUploadedFileNum();
                if (uploadedFileNum === 1) {
                    return $filter('translate')('file.upload.summary.uploaded.single');
                } else if (uploadedFileNum > 1) {
                    return $filter('translate')('file.upload.summary.uploaded.multiple', {
                        uploaded: uploadedFileNum
                    });
                }
            } else {
                if (uploadingFileNum === 1) {
                    return $filter('translate')('file.upload.summary.uploading.single');
                } else if (uploadingFileNum > 1) {
                    return $filter('translate')('file.upload.summary.uploading.multiple', {
                        uploading: uploadingFileNum
                    });
                }
            }
            return $filter('translate')('file.upload.summary.default');
        }

        $scope.toggleNotification = function() {
            if ($scope.minimize === false) {
                uploadFileService.setMinimizeStatus(true);
            } else if ($scope.minimize === true) {
                uploadFileService.setMinimizeStatus(false);
            }
        }

        $(window).on('hashchange', function() {
            $uibModalStack.dismissAll();
        });

        $scope.$watch('uploadFileService.getUploadFileList().length', function(newValue, oldValue) {
            $scope.uploadFileList = uploadFileService.getUploadFileList();
        });

        $scope.$watch('uploadFileService.getCloseStatus()', function(newValue, oldValue) {
            $scope.close = uploadFileService.getCloseStatus();
        });

        $scope.$watch('uploadFileService.getMinimizeStatus()', function(newValue, oldValue) {
            $scope.minimize = uploadFileService.getMinimizeStatus();
        });

        $scope.$watch('uploadFileService.getUploadingStatus()', function(newValue, oldValue) {
            $scope.fileUploadingList.push(uploadFileService.getUploadingStatus());
        });

        $scope.$watch(function() {
            return getUploadedFileNum();
        }, function(newValue, oldValue) {
            $scope.uploadFileSummary = constructUploadFileSummary();
        });

        $scope.$watch(function() {
            return getUploadingFileNum();
        }, function(newValue, oldValue) {
            $scope.uploadFileSummary = constructUploadFileSummary();
        });
    }
]);
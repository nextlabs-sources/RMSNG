mainApp.service('uploadFileService', ['$filter', function($filter) {
    var close = true;
    var minimize = true;
    var disabled = false;
    var fileUploadingList = [];
    var fileName = "";
    var fileUploaded = false;
    var fileUploading = false;
    var uploadFileList = [];

    var getUploadFileName = function() {
        return fileName;
    };

    var getUploadingFileList = function() {
        return fileUploadingList;
    };

    var getCloseStatus = function() {
        return close;
    };

    var getMinimizeStatus = function() {
        return minimize;
    };

    var getDisabledStatus = function() {
        return disabled;
    };

    var getUploadingStatus = function() {
        return fileUploading;
    };

    var getUploadedStatus = function() {
        return fileUploaded;
    };

    var getUploadFileList = function() {
        return uploadFileList;
    };

    var setUploadingStatus = function(value) {
        fileUploading = value;
    }

    var setUploadedStatus = function(value) {
        fileUploaded = value;
    }


    var setUploadFileList = function(value) {
        uploadFileList = value;
    }

    var setCloseStatus = function(value) {
        close = value;
    };

    var setMinimizeStatus = function(value) {
        minimize = value;
    };

    var fileStillUploading = function(fileName, filePath, repoId, fileList) {
        for (var i = 0; i < fileList.length; i++) {
            if (fileName.toLowerCase() === fileList[i].hoverFileName.toLowerCase() &&
                filePath === fileList[i].filePath &&
                repoId === fileList[i].repoId &&
                (fileList[i].fileUploading === true)) {
                return true;
            }
        }
        return false;
    }

    var getIndexOfUploadedFileName = function(fileName, filePath, repoId, fileList) {
        for (var i = fileList.length - 1; i >= 0; i--) {
            if (fileName === fileList[i].fileName && filePath === fileList[i].filePath && repoId === fileList[i].repoId) {
                return i;
            }
        }
    }

    var removeRedundantMessage = function(fileName) {
        for (var i = uploadFileList.length - 1; i >= 0; i--) {
            if (uploadFileList[i].fileName === (fileName + " " + $filter('translate')('file.uploading'))) {
                uploadFileList.splice(i, 1);
            }
        }
    }

    return {
        getUploadFileName: getUploadFileName,
        getUploadingFileList: getUploadingFileList,
        getCloseStatus: getCloseStatus,
        getMinimizeStatus: getMinimizeStatus,
        getDisabledStatus: getDisabledStatus,
        getUploadingStatus: getUploadingStatus,
        getUploadedStatus: getUploadedStatus,
        getUploadFileList: getUploadFileList,
        setCloseStatus: setCloseStatus,
        setMinimizeStatus: setMinimizeStatus,
        setUploadFileList: setUploadFileList,
        setUploadingStatus: setUploadingStatus,
        setUploadedStatus: setUploadedStatus,
        fileStillUploading: fileStillUploading,
        getIndexOfUploadedFileName: getIndexOfUploadedFileName,
        removeRedundantMessage: removeRedundantMessage,
    }
}]);
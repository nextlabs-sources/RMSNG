mainApp.controller('shareFileController', ['$scope', '$uibModal', '$timeout', 'networkService', 'dialogService', '$location', 
    '$filter', '$rootScope', 'initSettingsService', 'navService', '$cookies', 'shareDialogService', 'digitalRightsExpiryService',
    function($scope, $uibModal, $timeout, networkService, dialogService, $location, 
        $filter, $rootScope, initSettingsService, navService, $cookies, shareDialogService, digitalRightsExpiryService) {

        initSettingsService.setRightPanelMinHeight();
        var isRHViewer = false;
        $scope.expiryInfo = false;

		$scope.onClickInfo = function() {
			$uibModal.open({
				animation: true,
				templateUrl: '/viewer/ui/app/Home/SharedFiles/partials/DisplayInfo.html',
				windowClass: 'app-modal-window app-modal-window-mobile modal fade',
				controller: ['$uibModalInstance', '$uibModalStack', '$scope', '$filter', '$timeout', '$sce',
					function($uibModalInstance, $uibModalStack, $scope, $filter, $timeout, $sce) {
						$uibModalStack.dismissAll();
						var fileSize = getReadableFileSize(metaData.fileSize, 2);
						var lastModifiedDate = "";
						if(metaData.lastModifiedDate != 0) {
							lastModifiedDate = getReadableDate(metaData.lastModifiedDate);
						} else {
							lastModifiedDate = "-";
						}
						$scope.tags = metaData.tagsMap;
						$scope.tagsExist = $scope.tags && Object.keys($scope.tags).length > 0 ? true : false;
						$scope.originalFileName = metaData.originalFileName;
						$scope.fileType = metaData.displayName.split('.').pop();
						$scope.fileSize = fileSize;
						$scope.isProjectFile = metaData.projectFile;
						$scope.isWorkspaceFile = metaData.workspaceFile;
						$scope.expiryInfo = true;
						$scope.lastModifiedDate = lastModifiedDate;
						$scope.isNxl = metaData.isNXL;
						if (metaData.isNXL === true) {
							if (metaData.protectionType === 1 ) {
								$scope.fileRightsSpan = i18n_data['company.defined.rights'];
							} else {
								$scope.fileRightsSpan = i18n_data['user.defined.rights'];
							}
						} else {
							$scope.fileRightsSpan = i18n_data['file.unprotected.description'];   
						}
						if (metaData.owner === true && metaData.isNXL === true) {
							$scope.owner = true;
						}
						$scope.filePathDisplay = i18n_data['not-applicable'];
						if(metaData.showPathInfo === true) {
							$scope.repoName = metaData.repoName;
							$scope.filePathDisplay = metaData.filePathDisplay;
						}
						if(metaData.isNXL === true) {
							$scope.protectionType = metaData.protectionType;
							$scope.fileRightsValue = getRightsFromMetaData(metaData);
							$scope.fileTagsValue = getTagsFromMetaData(metaData);
						}		
						var fullDateFormat = "dddd, mmmm d, yyyy";				
						if(metaData.validity.startDate == undefined && metaData.validity.endDate == undefined) {
							$scope.expiration = $filter('translate')('never.expire');
						} else if(metaData.validity.startDate == undefined && metaData.validity.endDate != undefined) {
							$scope.expiration = $filter('translate')('until') + dateFormat(new Date(metaData.validity.endDate), fullDateFormat);
						} else if(metaData.validity.startDate != undefined && metaData.validity.endDate == undefined) {
							$scope.expiration = $filter('translate')('from') + dateFormat(new Date(metaData.validity.startDate), fullDateFormat);
						} else {
							$scope.expiration = dateFormat(new Date(metaData.validity.startDate), fullDateFormat) + " - " + dateFormat(new Date(metaData.validity.endDate), fullDateFormat);
						}						
						$scope.ok = function() {
							$uibModalInstance.dismiss('cancel');
							if(isRHViewer) {
								RHCloseDialogCallback();
							}
						}
					}
				]
			});
		}

        $scope.onClickShare = function() {
            $scope.isLoading = true;
            $scope.expiryInfo = (metaData.projectFile)? false : true;
            $scope.protectionType = metaData.protectionType;
            initSettingsService.setRMSContextName(metaData.rmsURL);
            var headers = {
                'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
            };
			if (metaData.transactionId && metaData.transactionCode) {
				$scope.isLoading = false;
				$scope.selectedFile = {
					duid: metaData.duid,
					isOwner: metaData.owner,
					name: metaData.originalFileName,
					protectedFile: true,
					rights: metaData.rights,
					size: metaData.fileSize,
					transactionCode: metaData.transactionCode,
					transactionId: metaData.transactionId
				}
				shareDialogService.reshareFile({
					file: $scope.selectedFile,
					rights: metaData.rights,
					transactionCode: metaData.transactionCode,
					transactionId: metaData.transactionId,
					rhcallback: isRHViewer ? RHCloseDialogCallback : null,
                    fromViewer: true,
                    startDate: metaData.validity.startDate,
                    endDate: metaData.validity.endDate,
                    protectionType: metaData.protectionType
				});
			} else {
				$scope.selectedFile = {
					path: metaData.filePathDisplay,
					pathId: metaData.filePath,
					repoId: metaData.repoId,
					repoName: metaData.repoName,
					name: metaData.originalFileName,
					efsId: metaData.efsId,
					nxl: metaData.isNXL
				};
				var params = $.param({
					repoId: metaData.repoId,
					filePath: metaData.filePath,
					filePathDisplay: metaData.filePathDisplay,
					fileName: metaData.originalFileName
				});
				if (isLocalFile(metaData)) {
					shareFile(params, metaData.rights, metaData.owner, metaData.isNXL, "shareLocal", metaData.validity, metaData.protectionType);
				} else {
					networkService.post(metaData.rmsURL + "/RMSViewer/CheckSharedRight", params, headers, function(data) {
						var error;
						$scope.isLoading = false;
						if (data.statusCode == 200) {
							var rights = data.results.r;
							var owner = data.results.o;
							var nxl = data.results.nxl;
							if (owner || rights.indexOf('SHARE') >= 0) {
								if(isLocalFile(metaData)){
									shareFile(params, rights, owner, nxl, "shareLocal",metaData.validity);
								} else {
									shareFile(params, rights, owner, nxl, "share",metaData.validity, metaData.protectionType);
								}
							} else {
								error = $filter('translate')('share.file.unauthorized.reshare');
							}
						} else {
							error = data.message;
							if (data.statusCode === 404) {
								refreshFolder();
							}
						}
						if (error) {
							showSnackbar({
								isSuccess: false,
								messages: error
							});
						}
					});
				}
			}
			
        }
        $scope.onClickProtect = function() {
            $scope.expiryInfo = (metaData.projectFile)? false : true;
			initSettingsService.setRMSContextName(metaData.rmsURL);
            $scope.selectedFile = {
                path: metaData.filePathDisplay,
                pathId: metaData.filePath,
                repoId: metaData.repoId,
                repoName: metaData.repoName,
                name: metaData.originalFileName,
                efsId: metaData.efsId,
                nxl: metaData.isNXL
            };
            var params = $.param({
                repoId: metaData.repoId,
                filePath: metaData.filePath,
                filePathDisplay: metaData.filePathDisplay
            });
            shareDialogService.shareFile({
                file: $scope.selectedFile,
                operation: isLocalFile(metaData) ? "protectLocal" : "protect",
                rhcallback: isRHViewer ? RHCloseDialogCallback : null,
                fromViewer: true
            });
        }
		$scope.onClickRHShare = function() {
            isRHViewer = true;
			$scope.onClickShare();
			var fromDiv = document.getElementById("viewer-dialog");
			var toDiv = document.getElementById("overlay-iframe");
			cloneCSSProperties(fromDiv, toDiv);
			toDiv.style.height = "100%";
			toDiv.style.width = "600px";
		}
		$scope.onClickRHProtect = function() {
			isRHViewer = true;
			$scope.onClickProtect();
			var fromDiv = document.getElementById("viewer-dialog");
			var toDiv = document.getElementById("overlay-iframe");
			cloneCSSProperties(fromDiv, toDiv);
			toDiv.style.height = "100%";
			toDiv.style.width = "600px";
		}
		$scope.onClickRHInfo = function() {
			isRHViewer = true;
			$scope.onClickInfo();
			var fromDiv = document.getElementById("viewer-dialog");
			var toDiv = document.getElementById("overlay-iframe");
			cloneCSSProperties(fromDiv, toDiv);
			toDiv.style.height = "100%";
			toDiv.style.width = "600px";
		}

        var shareFile = function(data, rights, owner, nxl, operation, expiry, protectionType) {
            $scope.isLoading = false;
            shareDialogService.shareFile({
                file: $scope.selectedFile,
                rights: rights,
                owner: owner,
                nxl: nxl,
                operation: operation,
                rhcallback: isRHViewer ? RHCloseDialogCallback : null,
                fromViewer: true,
                startDate: expiry.startDate,
                endDate: expiry.endDate,
                protectionType: protectionType
            });
        }
        var RHCloseDialogCallback = function() {
            var div = $('#viewer-dialog');
            div.dialog("close");
            div.empty();
            $("#overlay-iframe").hide();
            resetViewerDialogStyle();
        }
    }
]);
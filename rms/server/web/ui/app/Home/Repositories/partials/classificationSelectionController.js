mainApp.controller('classificationSelectionController', ['$scope', '$state', '$stateParams', '$filter', 'dialogService', 'networkService', 'initSettingsService', 'projectStateService', 'projectService', 'workSpaceService', 
    function($scope, $state, $stateParams, $filter, dialogService, networkService, initSettingsService, projectStateService, projectService, workSpaceService) {
        
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var maxNumLabels = 0;
        var LABEL_LIMIT = 5;
        $scope.showAllLabels = false;

        var getClassification = function(tokenGroupName) {
            networkService.get(RMS_CONTEXT_NAME + "/rs/classification/" + tokenGroupName, getJsonHeaders(), function(data){
                if (data.statusCode == 200) {
                  $scope.categoryList = data.results.categories;
                  for (var i = 0; i < $scope.categoryList.length; i++) {
                    var category = $scope.categoryList[i];
                    maxNumLabels = Math.max(
                      maxNumLabels,
                      category.labels.length
                    );
                    for (var j = 0; j < category.labels.length; j++) {
                      category.labels[j].selected = category.labels[j].default;
                      if (
                        $scope.tags != undefined &&
                        $scope.tags[category.name] != undefined
                      ) {
                        category.labels[j].isClassified =
                          $scope.tags[category.name].indexOf(
                            category.labels[j].name
                          ) > -1;
                      } else {
                        category.labels[j].isClassified = false;
                      }
                    }
                  }
                  $scope.loadedCategoryList = angular.copy($scope.categoryList);
                  $scope.showAllLabels = maxNumLabels <= LABEL_LIMIT;
                } else {
                  if (
                    $state.current.name != STATE_ADMIN_LANDING &&
                    $state.current.name != STATE_TENANT_ADMIN_LANDING
                  ) {
                    showSnackbar({
                      isSuccess: false,
                      messages: $filter("translate")(
                        "project.classification.get.error"
                      )
                    });
                  }
                }
            });
        }

        $scope.modifyRights = function(file) {
            $scope.showMandatoryErrorMessage = updateModifyRightsTemplateBasedOnMandatory();
            if($scope.showMandatoryErrorMessage) {
                $scope.isModifiedFailure = false;
                $scope.isModifiedSuccess = false;
                return;
            }
            var fileTags = {};
            $scope.categoryList.map(function(category){
                fileTags[category.name] = category.labels.filter(function(label){
                    if (label.isClassified) return label;
                }).map(function(label){
                    return label.name;
                });
                if(fileTags[category.name].length == 0) {
                    delete fileTags[category.name];
                }
            });
            var params = {
                "parameters": {
                    "fileName": file.fileName,
                    "parentPathId": file.path.substring(0, file.path.indexOf(file.fileName)),
                    "fileTags": JSON.stringify(fileTags)
                }
            }
            $scope.isLoading = true;
            
            if($stateParams.projectId){
            	projectService.modifyRights(params, $stateParams.projectId, function(data){
                    if(data.statusCode == 200 && data.results.entry != null) {
                        $scope.isModifiedFailure = false;
                        $scope.isModifiedSuccess = true;
                        $scope.loadedCategoryList = angular.copy($scope.categoryList);
                    } 
                    else if (data.statusCode == 403) {
                        $scope.isModifiedFailure = true;
                        $scope.isModifiedSuccess = false;
                        $scope.errorMessage = $filter('translate')('modify.rights.failure.unauthorised');
                    }
                    else {
                        $scope.isModifiedFailure = true;
                        $scope.isModifiedSuccess = false;
                        $scope.errorMessage = $filter('translate')('modify.rights.failure');
                    }
                    $scope.isLoading = false;
                });
            } else {
            	workSpaceService.modifyRights(params, function(data){
                    if(data.statusCode == 200 && data.results.entry != null) {
                        $scope.isModifiedFailure = false;
                        $scope.isModifiedSuccess = true;
                        $scope.loadedCategoryList = angular.copy($scope.categoryList);
                    } else {
                        $scope.isModifiedFailure = true;
                        $scope.isModifiedSuccess = false;
                        $scope.errorMessage = data.message;
                    }
                    $scope.isLoading = false;
                });
            }
            
        }

        $scope.pristine = function() {
            return angular.equals($scope.categoryList, $scope.loadedCategoryList);
        }

        $scope.chooseClassifyLabels = function(category, label, mode) {
            if(mode == "clickBtn") {
                label.isClassified = !label.isClassified;
            }
            updateModifyRightsTemplateBasedOnMultiSelect(label, category);
        }

        var updateModifyRightsTemplateBasedOnMandatory = function() {
            return $scope.categoryList.filter(function(category){
                        if(category.mandatory) return category;
                    }).map(function(category){
                        return category.labels;
                    }).map(function(labels){
                        return labels.filter(function(label){
                            return label.isClassified;
                        }).map(function(label){
                            return label.isClassified;
                        }).length;
                    }).indexOf(0) > -1 ? true : false;
        }

        var updateModifyRightsTemplateBasedOnMultiSelect = function(individualLabel, category) {
            if(category.multiSelect) {
                return;
            }
            category.labels.filter(function(label){
                if(label.name != individualLabel.name) {
                    label.isClassified = false;
                }
            });
        }
        
        $scope.getClassificationProfile = function() {
            if (!$stateParams.tenantId) {
                if ($stateParams.projectId) {
                    projectService.getProject($stateParams.projectId, function(data) {
                        if (data.statusCode == 200 && data.results.detail) {
                            projectStateService.setTokenGroupName(data.results.detail.tokenGroupName);
                            getClassification(data.results.detail.tokenGroupName);
                        } else {
                            showSnackbar({
                                isSuccess: false,
                                messages: $filter('translate')('project.classification.get.error')
                            });
                        }
                    });
                } else {
                    getClassification(readCookie('lt'));
                }
            } else {
                getClassification($stateParams.tenantId);
            }
        };

        $scope.buildSelectedClassifications = function() {
            $scope.classifications = {};
            $scope.validClassificationChoice = true;
            for(var i=0; i<$scope.categoryList.length; i++) {
                var category = $scope.categoryList[i];
                var container = [];
                for(var j=0; j<category.labels.length; j++) {
                    if(category.labels[j].selected) {
                        container.push(category.labels[j].name);
                    }
                }
                if(container.length > 0) {
                    $scope.classifications[category.name] = container;
                } else if(category.mandatory) {
                    $scope.validClassificationChoice = false;
                }
            }
        }

        $scope.labelEnabled = function(category) {
            if(category.multiSelect) {
                return true;
            }
            for(var i=0; i<category.labels.length; i++) {
                if(category.labels[i].selected == true) {
                    return false;
                }
            }
            return true;
        }

        $scope.labelEnabledForModifyRightTemplate = function(category) {
            if(category.multiSelect) {
                return true;
            }
            for(var i=0; i<category.labels.length; i++) {
                if(category.labels[i].isClassified) {
                    return false;
                }
            }
            return true;
        }

        $scope.showDropDown = function(category){
            for(var j=0; j<category.labels.length; j++) {
                if(category.labels[j].selected == false) {
                    return true;
                }
            }
            return false;
        }
        
        $scope.getClassificationProfile();
    }
]);
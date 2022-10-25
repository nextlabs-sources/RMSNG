mainApp.controller('classificationController', ['$scope', '$state', '$stateParams', '$timeout', '$filter', 'dialogService', 'networkService', 'initSettingsService', 'projectStateService', 
    function($scope, $state, $stateParams, $timeout, $filter, dialogService, networkService, initSettingsService, projectStateService) {

        var REGEX = /^[^%'"]+$/;

        $scope.maxNumCategories = {
            max: 5
        };
        $scope.maxNumLabels = 10;
        $scope.selectedCategories = [];
        $scope.loadedCategories = [];
        $scope.inheritParent = false;

        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();

        var init = function(){
            $scope.isClassificationLoading = true;
            var tokenGroupName;
            if ($state.current.name == STATE_ADMIN_TENANT_CLASSIFICATION || $state.current.name == STATE_TENANT_ADMIN_TENANT_CLASSIFICATION) {
                tokenGroupName = readCookie('lt');
            } else {
                tokenGroupName = projectStateService.getTokenGroupName();
            }
            networkService.get(RMS_CONTEXT_NAME + "/rs/classification/" + tokenGroupName, getJsonHeaders(), function(data){
                $scope.isClassificationLoading = false;
                if(data.statusCode == 200) {
                    $scope.selectedCategories = data.results.categories;
                    $scope.loadedCategories = angular.copy($scope.selectedCategories);
                    $scope.maxNumCategories = {
                        max: data.results.maxCategoryNum
                    };
                    $scope.maxNumLabels = data.results.maxLabelNum;
                    if ($scope.selectedCategories.length == 0) {
                        if (readCookie("lt")) {
                            networkService.get(RMS_CONTEXT_NAME + "/rs/classification/" + readCookie("lt"), getJsonHeaders(), function(data){
                                if(data.statusCode == 200 && data.results.categories.length > 0) {
                                    $scope.inheritParent = true;
                                    $scope.selectedCategories = data.results.categories;
                                    $scope.loadedCategories = angular.copy($scope.selectedCategories);
                                    $scope.maxNumCategories = {
                                        max: data.results.maxCategoryNum
                                    };
                                    $scope.maxNumLabels = data.results.maxLabelNum;
                                }
                            });
                        }
                    }
                } else {
                    showSnackbar({
                        messages: $filter('translate')('project.classification.get.error'),
                        isSuccess: false
                    });
                }
            });
        }

        $scope.pristine = function(){
            return angular.equals($scope.selectedCategories, $scope.loadedCategories) && !$scope.inheritParent;
        }

        $scope.addCategory = function(){
            var input = $scope.categoryInput.trim();
            if (!REGEX.test(input)) {
                showSnackbar({
                    messages: $filter('translate')("project.classification.category.pattern.error"),
                    isSuccess: false
                });
                return;
            }

            for(var i=0; i<$scope.selectedCategories.length; i++) {
                if($scope.selectedCategories[i].name.toLowerCase() == input.toLowerCase()) {
                    showSnackbar({
                        messages: $filter('translate')("project.classification.duplicate.error"),
                        isSuccess: false
                    });
                    return;
                }
            }
            var category = {
                "name": input,
                "mandatory": false,
                "multiSelect": false,
                "labels": [
                    {
                        "name": input,
                        "default": false
                    }
                ]
            };
            $scope.selectedCategories.push(category);
            $scope.categoryInput = "";

            $timeout(function() {
                var catId = "#cat" + ($scope.selectedCategories.length - 1);
                $("#rms-inner-container").animate({scrollTop: $(catId).offset().top}, 800);
                $(catId + "lbl0 input.lbl-name-input").focus();
            });
        }

        $scope.removeCategory = function(category) {
            var index = $scope.selectedCategories.indexOf(category);
            if(index >= 0) {
                $scope.selectedCategories.splice(index, 1);
            }
        }

        $scope.hasMoreThanOneLabel = function(category){
            return category.labels.length > 1;
        }

        $scope.hasAtLeastOneLabel = function(category){
            return category.labels.length >= 1;
        }

        $scope.allowLabelDefault = function(category, label) {
            if(!$scope.hasAtLeastOneLabel(category)) {
                return false;
            }
            if(category.multiSelect == true || label.default==true) {
                return true;
            }
            for(var i=0; i<category.labels.length; i++) {
                if(category.labels[i].default == true) {
                    return false;
                }
            }
            return true;
        }

        $scope.updateMultiSelect = function(category) {
            if(!category.multiSelect) {
                var foundDefault = false;
                for(var i=0; i<category.labels.length; i++) {
                    if(category.labels[i].default == true) {
                        if(!foundDefault) {
                            foundDefault = true;
                        } else {
                            category.labels[i].default = false;
                        }
                    }
                }
            }
        }

        function uniq(a) {
            var seen = {};
            return a.filter(function(item) {
                return seen.hasOwnProperty(item.name.toLowerCase()) ? false : (seen[item.name.toLowerCase()] = true);
            });
        }

        $scope.checkDuplicateLabels = function(category, label) {

            var uniqueLabels = uniq(category.labels);
            if(uniqueLabels.length != category.labels.length) {
                showSnackbar({
                        messages: $filter('translate')("project.classification.duplicate.error"),
                        isSuccess: false
                });
                $scope.removeLabel(category, label);
            }
        }


        var checkInvalidCharacters = function (labelName) {
            if (labelName) {
                var regex = /^[\w -]*$/;
                var results = regex.test(labelName);
                return results;
            }
            return false;
        };


        $scope.addLabel = function(category, catIdx){
            category.labels.push({
                "name": ""
            })
            $timeout(function() {
                var labelId = "#cat" + catIdx + "lbl" + (category.labels.length-1);
                $(labelId + " input.lbl-name-input").focus();
            });
        }

        $scope.removeLabel = function(category, label) {
            var index = category.labels.indexOf(label);
            if(index >= 0) {
                if (jscd.browser == "Microsoft Internet Explorer") {
                    $scope.isClassificationLoading = true;
                    $timeout(function(){
                        remove(category, index);
                        $scope.isClassificationLoading = false;
                    }, 1000);
                } else {
                    remove(category, index); 
                }
            }
        }

        var remove = function(category, index) {
            category.labels.splice(index, 1);
        }

        $scope.back = function(){
            $scope.onClickTab('configuration', true);
        }

        $scope.skip = function(){
            $scope.onClickTab('preference', true);
        }

        $scope.proceed = function(){       
            for(var i=0; i<$scope.selectedCategories.length; i++) {
                var category = $scope.selectedCategories[i];
                for(var j=0; j<category.labels.length; j++) {
                    var labelName = category.labels[j].name;
                    if( category.labels[j].name == 0) {
                        showSnackbar({
                            messages: $filter('translate')('project.classification.label.empty.error', {categoryName : category.name}),
                            isSuccess: false
                        });
                        return false;
                    }
                    if (!checkInvalidCharacters(category.labels[j].name)) {
                        showSnackbar({
                            messages: $filter('translate')('project.classification.label.pattern.error', {"labelName" : category.labels[j].name}),
                            isSuccess: false
                        });
                        return false;
                    }
                }
            }

            if($scope.selectedCategories.length == 0) {
                dialogService.confirm({
                    msg: $filter('translate')('project.classification.category.delete.warn'),
                    ok: function() {
                        $scope.save();
                    },
                    cancel: function() {}
                });
            } else {
                $scope.save();
            }
        }

        $scope.save = function(){
            var parameter = {
                "parameters": {
                    "categories": $scope.selectedCategories
                }
            }
            $scope.isClassificationLoading = true;
            var tokenGroupName;
            if ($state.current.name == STATE_ADMIN_TENANT_CLASSIFICATION || $state.current.name == STATE_TENANT_ADMIN_TENANT_CLASSIFICATION) {
                tokenGroupName = readCookie('lt');
            } else {
                tokenGroupName = projectStateService.getTokenGroupName();
            }
            networkService.post(RMS_CONTEXT_NAME + "/rs/classification/" + tokenGroupName, parameter, getJsonHeaders(), function(data) {
                $scope.isClassificationLoading = false;
                if(data.statusCode == 201) {
                    $scope.loadedCategories = angular.copy($scope.selectedCategories);
                    $scope.inheritParent = false;
                    if($scope.fromCreateProject) {
                        $state.go(STATE_PROJECT_CONFIGURATION_PREFERENCE, {
                            fromCreateProject: true
                        });
                        $timeout(function() {
                            showSnackbar({
                                isSuccess: true,
                                messages: $filter('translate')('project.classification.post.success')
                            });
                        });
                    } else {
                        showSnackbar({
                            messages: $filter('translate')("project.classification.post.success"),
                            isSuccess: true
                        });
                    }
                } else {
                    var message;                    
                    if (data.statusCode == 403) {
                        message = 'project.classification.permission.error';
                    } else if (data.statusCode == 4003) {
                        message = 'project.classification.duplicate.error';
                    } else if (data.statusCode == 5003) {
                        message = 'project.classification.cc.error';                        
                    } else {
                        message = 'project.classification.post.error';
                    }
                    showSnackbar({
                        messages: $filter('translate')(message),
                        isSuccess: false
                    });
                }
            });
        }
        init();
    }
]);
mainApp.controller('manageProjectTagsController', ['$scope', '$filter', '$state','manageTagsService','$stateParams', function($scope, $filter, $state, manageTagsService, $stateParams){

	$scope.max = 10;
	$scope.formData = {};
	$scope.formData.projectTagsName;
	$scope.projectTagList = [];
	$scope.loadedProjectTags = [];

	var init = function() {
		var type = 0;
		$scope.isLoading = true;
		manageTagsService.getTenantTags(function(data){
			$scope.isLoading = false;
			if(data.statusCode == 200) {
				$scope.projectTagsList = data.results.tags;
				$scope.projectTagsList.forEach(function(projectTag) {
					projectTag.selected = false;
				});
				if ($state.current.name === STATE_PROJECT_CONFIGURATION_ACCESS_PERMISSIONS) {
					$scope.initProjectTags();
				}
			}
		},type);
	}

	$scope.initProjectTags = function() {
		manageTagsService.getProjectTags(function(data){
			if (data.statusCode == 200) {
				var projectTags = [];
				data.results.tags.forEach(function(tag) {
					projectTags.push(tag.id);
				});
				$scope.projectTagsList.forEach(function(tag) {
					if (projectTags.indexOf(tag.id) > -1) {
						tag.selected = true;
					}
				});
				$scope.loadedProjectTags = angular.copy($scope.projectTagsList);
			}
		}, $stateParams.projectId);
	}

	$scope.cancel = function() {
		$state.go(STATE_ADMIN_LANDING);
	}

	$scope.save = function() {
		var params = {
			"parameters":{
				"tagType":0,
				"tags": $scope.projectTagsList
			}
		};
		$scope.isLoading = true;
		manageTagsService.updateTenantTags(function(data){
			$scope.isLoading = false;
			if(data.statusCode == 200) {
				showSnackbar({
					isSuccess: true,
					messages: $filter('translate')('tag.save.success')
				});
			} else {
				showSnackbar({
					isSuccess: false,
					messages: $filter('translate')('tag.save.fail')
				});
			}
		}, params);
	}

	$scope.updateProjectTags = function() {
		var projectTags = [];
		$scope.projectTagsList.forEach(function(tag) {
			if (tag.selected) {
				projectTags.push(tag.id);
			}
		});
		var params = {
			"parameters":{
				"projectTags": projectTags
			}
		};
		manageTagsService.updateProjectTags(function(data){
			if(data.statusCode == 200) {
				showSnackbar({
					isSuccess: true,
					messages: $filter('translate')('tag.save.success')
				});
				$scope.loadedProjectTags = angular.copy($scope.projectTagsList);
			} else {
				showSnackbar({
					isSuccess: false,
					messages: $filter('translate')('tag.save.fail')
				});
			}
		}, $stateParams.projectId, params);
	}

	$scope.pristine = function(){
		return angular.equals($scope.projectTagsList, $scope.loadedProjectTags);
	}

	var isDuplicateProjectTagsName = function() {
		for (var i = 0; i < $scope.projectTagsList.length; i++) {
			if($scope.projectTagsList[i].name.toLowerCase() == $scope.formData.projectTagsName.toLowerCase()) {
				return true;
			}
		};
		return false;
	}

	$scope.addTag = function() {
		if(isDuplicateProjectTagsName()) {
			showSnackbar({
				messages: $filter('translate')('duplicate.user.project.tag.error'),
				isSuccess: false
			});
			$scope.formData.projectTagsName = "";
			return;
		}
		var tags = {
			"name": $scope.formData.projectTagsName
		}
		$scope.projectTagsList.push(tags);
		$scope.formData.projectTagsName = "";
	}

	$scope.removeTag = function(index) {
		$scope.projectTagsList.splice(index, 1);
	}

	$scope.toggleProjectTags = function(tags) {
        if(tags.selected) {
            $scope.projectTagList.splice($scope.projectTagList.indexOf(tags.id),1);
            tags.selected = false;
        } else {
            $scope.projectTagList.push(tags.id);
            tags.selected = true;
        }
        $scope.$emit("projectTagList", {data: $scope.projectTagList});
    }

    $scope.$on('projectTagsId', function(event, results){
    	$scope.projectTagsId = results; 
    	$scope.projectTagsList.forEach(function(tag){
    		if($scope.projectTagsId.indexOf(tag.id) > -1) {
    			tag.selected = true;
    			$scope.projectTagList.push(tag.id);
    		}
    	});
    	$scope.$emit("projectTagList", {data: $scope.projectTagList});
    });

	init();
}]);
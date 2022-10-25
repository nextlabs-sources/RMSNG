mainApp.controller('userPreferenceController',[ '$scope', '$state', '$filter', '$controller', 'userPreferenceService',
	function($scope, $state, $filter, $controller, userPreferenceService){
		
		var watermarkScope = $controller('watermarkController',{$scope: $scope});
		var digitalRightsExpiryScope = $controller('digitalRightsExpiryDateController', {$scope: $scope});
		var projectSaved = false;

		$scope.goBack = function() {
			if ($scope.fromState) {
				$state.go($scope.fromState, $scope.fromParams);
			} else {
				$state.go(STATE_LANDING);
			}
		}

		$scope.back = function() {
			$scope.onClickTab('classification', true);
		}

		$scope.skip = function() {
			projectSaved = true;
			$scope.$emit('projectSaved',{data:projectSaved});
			$scope.onClickTab('summary', true);
		}

		$scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
			if(to.name === STATE_USER_PREFERENCE) {
				$scope.fromState = from.name;
				$scope.fromParams = fromParams;
			}
		});

		$scope.$on('onExpiryTypeChange',function(event,result){
			$scope.expiryType = result.data;
			$scope.userPreferenceForm.$setDirty();
		});

		$scope.$on('onExpiryYearChange',function(event,result){
			$scope.expiry.year = result.data;
		});

		$scope.$on('onExpiryMonthChange',function(event,result){
			$scope.expiry.month = result.data;
		});

		$scope.$on('onExpiryWeekChange',function(event,result){
			$scope.expiry.week = result.data;
		});

		$scope.$on('onExpiryDayChange',function(event,result){
			$scope.expiry.day = result.data;
		});

		$scope.$on('onDatePickerMilliSecChange',function(event, result){
			$scope.datePickerMilliSec = result.data;
		});

		$scope.$on('onStartDatePickerMilliSecChange',function(event, result){
			$scope.startDatePickerMilliSec = result.data;
		});

		$scope.$on('onEndDatePickerMilliSecChange',function(event, result){
			$scope.endDatePickerMilliSec = result.data;
		});

		$scope.$on('onWatermarkChanged',function(event, result){
			$scope.userPreferenceForm.$setDirty();
			$scope.watermarkError = result.error;
		});

		$scope.addUserPreference = function(mode) {
			var parameter;
			var watermarkStr = $scope.addWatermarkStr();	
			var expiry = $scope.addExpiry();
			if ($scope.relativeExpiryZero()) {
				showSnackbar({
					isSuccess: false,
					messages: $filter('translate')('digital.rights.validity.relative.error')
				});
				return;
			}		
			if(watermarkStr.length > 50) {
				showSnackbar({
					isSuccess: false,
					messages: $filter('translate')('watermark.too.long')
				});
				return;
			} else if(watermarkStr.length == 0) {
				showSnackbar({
					isSuccess: false,
					messages: $filter('translate')('user.preference.watermark.default.empty')
				});
				return;
			}
			parameter = {
				"parameters": {
					"expiry": expiry,
					"watermark": watermarkStr
				}
			};
			$scope.userPreferenceLoading =  true;
			userPreferenceService.putPreference(parameter, function(data){
				$scope.userPreferenceLoading =  false;
				var messages;
				var isSuccess = false; 
				if(data.statusCode == 200) {
					if($scope.fromCreateProject) {
						$scope.skip();
					}
					isSuccess = true;
					if(mode == "user-preference") {
						messages = $filter('translate')('user.preference.update.success');
					} else {
						messages = $filter('translate')('project.preference.update.success');
					}
					$scope.userPreferenceForm.$setPristine();
				} else if (data.statusCode == 4003) {
					messages = $filter('translate')('user.preference.expiry.invalid');
					$scope.userPreferenceForm.$setPristine();
				} else {
					if(mode == "user-preference") {
						messages = $filter('translate')('user.preference.update.fail');
					} else {
						messages = $filter('translate')('project.preference.update.fail');
					}
				}
				showSnackbar({
					isSuccess: isSuccess,
					messages: messages
				});
			});
		}
	}
])

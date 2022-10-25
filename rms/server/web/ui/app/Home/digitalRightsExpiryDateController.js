mainApp.controller('digitalRightsExpiryDateController',[ '$scope', '$filter', '$animate', 'userPreferenceService', '$timeout', 'digitalRightsExpiryService', '$state',
	function($scope, $filter, $animate, userPreferenceService, $timeout, digitalRightsExpiryService, $state) {
		$scope.expiryTypeList = ["Never Expire", "Relative", "Absolute Date", "Date Range"];
		$scope.expiryType = $scope.expiryTypeList[0];
		$scope.minStartDate = new Date();
		$scope.minEndDate = new Date();
		$scope.defaultExpiry = {
			year: 0,
			month: 1,
			week: 0,
			day: 0
		}
		$scope.expiry = $scope.defaultExpiry;
		$scope.minDate = new Date();
		$scope.finalRelativeDate = new Date(); 

		var init = function() {
			if ($scope.expiryJson) {
				$timeout(function () {
					parseExpiry($scope.expiryJson);
				}, 100);
			} else {
				userPreferenceService.getPreference(function(data){
					if(data.statusCode == 200) {
						if(data.results != undefined){
							if(data.results.expiry){
								parseExpiry(data.results.expiry);
							}
						}
					} else {
						var messages = $filter('translate')('user.preference.load.fail');
						var isSuccess = false;
						showSnackbar({
							isSuccess: isSuccess,
							messages: messages
						});
					}
				});
			}
			angular.element(function () {
				var elems = angular.element(".ui-datepicker");
				for (var index = 0; index < elems.length; index++) {
					$animate.enabled(elems[index], false);
				}
			});
		}

		var parseExpiry = function (obj) {
			var currentTime = new Date();
			$scope.expiryType = $scope.expiryTypeList[obj.option];
			$scope.expiry.startDatePicker = currentTime;
			$scope.expiry.endDatePicker = calculateDefaultEndTime();
			$scope.expiry.datePicker = $scope.expiry.endDatePicker;
			if(obj.option == 1) {
				$scope.expiry.year = obj.relativeDay.year != undefined ? obj.relativeDay.year : 0;
				$scope.expiry.month = obj.relativeDay.month != undefined ? obj.relativeDay.month : 0;
				$scope.expiry.week = obj.relativeDay.week != undefined ? obj.relativeDay.week : 0;
				$scope.expiry.day = obj.relativeDay.day != undefined ? obj.relativeDay.day : 0;
				$scope.expiry.endDatePicker = calculateRelativeTime();	//recalculate end date with new expiry options
				$scope.expiry.datePicker = $scope.expiry.endDatePicker;
			} else if(obj.option == 2) {
				$scope.datePickerMilliSec = new Date(obj.endDate);
				$scope.expiry.datePicker = $scope.datePickerMilliSec;
			} else if(obj.option == 3) {
				$scope.startDatePickerMilliSec = new Date(obj.startDate);
				$scope.expiry.startDatePicker = $scope.startDatePickerMilliSec;
				$scope.endDatePickerMilliSec = new Date(obj.endDate);
				$scope.expiry.endDatePicker = $scope.endDatePickerMilliSec;
			}
		}

		$scope.clear = function(expiry){
			angular.element(function () {
				var elems = angular.element(".ui-datepicker");
				for (var index = 0; index < elems.length; index++) {
					$animate.enabled(elems[index], false);
				}
			});
			var currentTime = new Date();
			if(expiry == "datePicker") {
				$scope.expiry.datePicker = calculateDefaultEndTime();
			} else if(expiry == "startDatePicker") {
				$scope.expiry.startDatePicker = currentTime;
				$scope.minStartDate = currentTime;
				$scope.minEndDate = currentTime;
			} else if(expiry == "endDatePicker") {
				var startDate = $scope.expiry.startDatePicker > currentTime ? new Date($scope.expiry.startDatePicker): currentTime;
				$scope.minEndDate = startDate.setHours(0,0,0,0);
				$scope.expiry.endDatePicker = calculateDefaultEndTime(startDate);
			}
		}

		var calculateRelativeTime = function(fromTime) {
			var finalRelativeDateMilliSec = digitalRightsExpiryService.calculateRelativeEndDate($scope.expiry.year, $scope.expiry.month, $scope.expiry.week, $scope.expiry.day, fromTime);
			$scope.expiryDays = Math.ceil((finalRelativeDateMilliSec - new Date().setHours(0,0,0,0))/(1000 * 3600 * 24));
			$scope.finalRelativeDate = new Date(finalRelativeDateMilliSec);
			return $scope.finalRelativeDate;
		}
		var calculateDefaultEndTime = function(fromTime) {
			var finalRelativeDateMilliSec = digitalRightsExpiryService.calculateRelativeEndDate($scope.defaultExpiry.year, $scope.defaultExpiry.month, $scope.defaultExpiry.week, $scope.defaultExpiry.day, fromTime);
			$scope.finalRelativeDate = new Date(finalRelativeDateMilliSec);
			return $scope.finalRelativeDate;
		}
		$scope.relativeExpiryZero = function () {
			return $scope.expiryType === "Relative" && $scope.expiry.year == 0 && $scope.expiry.month == 0 && $scope.expiry.week == 0 && $scope.expiry.day == 0;
		}
		$scope.$watch('expiry.year',function(){
			if (!($scope.expiry.year >= 0 && $scope.expiry.year <=100)) {
				$scope.expiry.year = 0;
			}
			if ($scope.relativeExpiryZero()) {
				$scope.relativeExpiryError = true;
			} else {
				$scope.relativeExpiryError = false;
			}
			$scope.$emit('onExpiryYearChange', {data: $scope.expiry.year});
			calculateRelativeTime();
		});

		$scope.$watch('expiry.month',function(){
			if (!($scope.expiry.month >= 0 && $scope.expiry.month <=100)) {
				$scope.expiry.month = 0;
			}
			if ($scope.relativeExpiryZero()) {
				$scope.relativeExpiryError = true;
			} else {
				$scope.relativeExpiryError = false;
			}
			$scope.$emit('onExpiryMonthChange', {data: $scope.expiry.month});
			calculateRelativeTime();
		});

		$scope.$watch('expiry.week',function(){
			if (!($scope.expiry.week >= 0 && $scope.expiry.week <=100)) {
				$scope.expiry.week = 0;
			}
			if ($scope.relativeExpiryZero()) {
				$scope.relativeExpiryError = true;
			} else {
				$scope.relativeExpiryError = false;
			}
			$scope.$emit('onExpiryWeekChange', {data: $scope.expiry.week});
			calculateRelativeTime();
		});

		$scope.$watch('expiry.day',function(){
			if ($scope.relativeExpiryZero()) {
				$scope.relativeExpiryError = true;
			} else {
				$scope.relativeExpiryError = false;
			}
			if (!($scope.expiry.day >= 0 && $scope.expiry.day <=1000)) {
				$scope.expiry.day = 0;
			}
			$scope.$emit('onExpiryDayChange', {data: $scope.expiry.day});
			calculateRelativeTime();
		});

		$scope.$watch('expiry.datePicker',function(){
			$scope.datePickerMilliSec = new Date($scope.expiry.datePicker).setHours(0,0,0,0) + Date.now() - new Date().setHours(0,0,0,0);
			$scope.$emit('onDatePickerMilliSecChange',{data: $scope.datePickerMilliSec});
			$scope.expiryDays = Math.ceil(($scope.datePickerMilliSec - new Date().setHours(0,0,0,0))/(1000 * 3600 * 24));
		});

		$scope.$watch('expiry.startDatePicker',function(){
			var isSuccess = true;
			var messages;
			$scope.startDatePickerMilliSec = ($scope.expiry.startDatePicker != null) ? new Date($scope.expiry.startDatePicker).setHours(0,0,0,0) + Date.now() - new Date().setHours(0,0,0,0) : undefined;
			$scope.$emit('onStartDatePickerMilliSecChange',{data: $scope.startDatePickerMilliSec});
			if($scope.startDatePickerMilliSec == undefined) {
				messages = $filter('translate')('start.date.empty');
				isSuccess = false;
				return;
			}
			if($scope.startDatePickerMilliSec > $scope.endDatePickerMilliSec){
				$scope.expiry.endDatePicker = new Date($scope.expiry.startDatePicker).setHours(0,0,0,0);
			}
			var currentTime = new Date();
			$scope.minEndDate = $scope.expiry.startDatePicker > currentTime ? new Date(new Date($scope.expiry.startDatePicker).setHours(0,0,0,0)): currentTime;
			$scope.expiryDays = Math.ceil(($scope.endDatePickerMilliSec - new Date($scope.startDatePickerMilliSec).setHours(0,0,0,0))/(1000 * 3600 * 24));
		});

		$scope.$watch('expiry.endDatePicker',function(){
			var isSuccess = true;
			var messages;
			$scope.endDatePickerMilliSec = ($scope.expiry.endDatePicker != null) ? new Date($scope.expiry.endDatePicker).setHours(0,0,0,0) + Date.now() - new Date().setHours(0,0,0,0) : undefined;
			$scope.$emit('onEndDatePickerMilliSecChange',{data: $scope.endDatePickerMilliSec});
			if($scope.endDatePickerMilliSec == undefined) {
				messages = $filter('translate')('end.date.empty');
				isSuccess = false;
			}
			if ($scope.expiryType == $scope.expiryTypeList[2]) {
				$scope.expiryDays = Math.ceil(($scope.datePickerMilliSec - new Date().setHours(0,0,0,0))/(1000 * 3600 * 24));
			} else {
				$scope.expiryDays = Math.ceil(($scope.endDatePickerMilliSec - new Date($scope.startDatePickerMilliSec).setHours(0, 0, 0, 0)) / (1000 * 3600 * 24));
			}
		});

		$scope.addExpiry = function() {
			if($scope.expiryType == $scope.expiryTypeList[0]){
				var expiry =  {
					"option":0
				};
			} else if($scope.expiryType == $scope.expiryTypeList[1]){
				if($scope.expiry.year == null){
					$scope.expiry.year = 0;
				}
				if($scope.expiry.month == null){
					$scope.expiry.month = 0;
				}
				if($scope.expiry.week == null){
					$scope.expiry.week = 0;
				}
				if($scope.expiry.day == null){
					$scope.expiry.day = 0;
				}
				var expiry = {
					"option": 1,
					"relativeDay" : {
						"year": $scope.expiry.year,
						"month": $scope.expiry.month,
						"week": $scope.expiry.week,
						"day": $scope.expiry.day
					}
				};
			} else if($scope.expiryType == $scope.expiryTypeList[2]){
				var expiry = {
					"option":2,
					"endDate": $scope.datePickerMilliSec
				};
			} else if($scope.expiryType == $scope.expiryTypeList[3]){
				var expiry = {
					"option":3,
					"startDate":$scope.startDatePickerMilliSec,
					"endDate":$scope.endDatePickerMilliSec
				};				
			}
			if (expiry.startDate) {
				var start = new Date(expiry.startDate);
				start.setHours(0,0,0,0);
				expiry.startDate = start.getTime();
			}
			if (expiry.endDate) {
				var end = new Date(expiry.endDate);
				end.setHours(23,59,59,999);
				expiry.endDate = end.getTime();
			}
			return expiry;
		}

		$scope.selectExpiryType = function(type) {
			$scope.expiryType = type;
			$scope.$emit('onExpiryTypeChange', {data: $scope.expiryType});
			$scope.relativeExpiryError = false;
			switch (type) {
				case $scope.expiryTypeList[0]:
					break;
				case $scope.expiryTypeList[1]:
					calculateRelativeTime();
					break;
				case $scope.expiryTypeList[2]:
					$scope.expiryDays = Math.ceil(($scope.datePickerMilliSec - new Date().setHours(0,0,0,0))/(1000 * 3600 * 24));
					break;
				case $scope.expiryTypeList[3]:
					$scope.expiryDays = Math.ceil(($scope.endDatePickerMilliSec - new Date($scope.startDatePickerMilliSec).setHours(0,0,0,0))/(1000 * 3600 * 24));
					break;
			}
		}

		init();
	}
]);
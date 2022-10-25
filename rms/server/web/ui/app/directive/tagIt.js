mainApp.directive('tagIt', ['$translate', function($translate){
    return function($scope, elem) {
		$scope.doApply = true;
		$scope.mailPristine = true;
		function createTagOnStr(elem, shareWithStr) {
			var pastedValArr = shareWithStr.replace(/(^\s+|\s+$)/g, '').split(/[\s,;]+/);
			if (pastedValArr.length > 1) {
				for (var i = 0; i < pastedValArr.length; i++) {
					elem.tagit('createTag', pastedValArr[i]);
				}
			}
			return pastedValArr;
		}		
		if(!$scope.revoked){
			elem.bind('input', function() {
				createTagOnStr(elem, elem.data("ui-tagit").tagInput.val());
			});	
			elem.tagit({
				singleField: true,
				singleFieldNode: $("#"+elem[0].previousElementSibling.id),
				caseSensitive: 0,
				afterTagAdded: function(event, ui) {
					$scope.validateEmail(event.target.id);
				},
				afterTagRemoved: function(event, ui) {
					$scope.validateEmail(event.target.id);
				}, 
				beforeTagAdded: function(event, ui) {
					var str_array = createTagOnStr(elem, elem.tagit('tagLabel', ui.tag));
					return str_array.length <= 1;
				}
			});
			$translate('share.email.placeholder').then(function (translatedValue) {
				elem.data("ui-tagit").tagInput.attr('placeholder', translatedValue);
				elem.data("ui-tagit").tagInput.attr('autofocus',true);
			});
		}else {
			elem.tagit({
				placeholderText: null,
				singleField: true,
				singleFieldNode: $('#shareWith'),
				caseSensitive: 0,
				readOnly: 1
			});
		}
		if ($.isArray($scope.sharedWithArr)) {
			for (var i=0; i < $scope.sharedWithArr.length; i++) {
				$scope.doApply = false;
				elem.tagit('createTag', $scope.sharedWithArr[i]);
				$scope.doApply = true;
				$scope.mailPristine = true;
			}
		}
	}
}]);
mainApp.directive('reservedNameValidator', function() {
    return {
        restrict: 'A', // only activate on element attribute
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.reservedNameValidator = function(modelValue, viewValue) {
                return viewValue != 'Public';
            };
        }
    };
});
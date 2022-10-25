mainApp.directive('backImg', function() {
    return {
        link: function(scope, element, attrs) {
            var ext = attrs.imgExt;
            var data = attrs.base64Data;
            scope.$watch('profilePictureUrl', function(newValue, oldValue, scope) {
                if (scope.profilePictureUrl) {
                    element.css({
                        'background-image': 'url(\'' + scope.profilePictureUrl + '\')',
                        'background-size':'contain',
                        'background-repeat':'no-repeat',
                        'background-position':'left top',
                        'width':'200px',
                        'height':'200px'
                    });
                }
            });
        }
    };
});
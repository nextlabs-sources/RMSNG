mainApp.directive('scrollTopOnRefresh', function() {
    return {
        restrict: 'EA',
        link: function (scope, elem){
                scope.$watch('scrollToTop', function(newValue){
                   if(newValue) {
                       elem.scrollTop(0);
                       scope.scrollToTop = false;
                   }
                });
            }
    };
});
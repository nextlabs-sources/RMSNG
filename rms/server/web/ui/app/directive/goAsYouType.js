mainApp.directive("goAsYouType", ['$filter', "$timeout", 
function($filter, $timeout) {
  return {
    restrict: "AE",
    replace: true,
    transclude: true,
    templateUrl: 'ui/app/Home/PolicyStudio/partials/go-as-you-type.html',
    scope: {
      condition: '=',  
      placeholder: '@',
      inputMax: '@'
    },
    link: function(scope, element, attrs) { 
      scope.policyValue = "";
      scope.duplicateError = "";

      scope.noMatchedTag = function() {
        var noMatched = true;
        scope.duplicateError = "";
        angular.forEach(scope.condition.value, function(data) {
          if(data.toLowerCase() == scope.policyValue.toLowerCase()) {
            noMatched = false;
            scope.duplicateError = $filter('translate')('duplicate.value');
          } 
        })
        return noMatched;
      }

      scope.deleteItem = function($event, index) {
        scope.condition.value.splice(index, 1);
        $event.stopPropagation();
      }

      scope.addTag =  function() {
        if(scope.policyValue.trim().length == 0) return;
        scope.condition.value.push(scope.policyValue);
        scope.policyValue = "";
      }
    }
  };
}]);
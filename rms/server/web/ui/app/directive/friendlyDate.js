mainApp.directive('friendlyDate', ['$filter', function($filter) {
  return {
    scope: {
      date: '@',
      fullFormat: '@',
      shortFormat: '@',
      contentClass: '@',
      dateType: '@',
    },
    template: "<p class=\"{{contentClass}}\">{{content}}</p>",
    link: function(scope, element, attrs) {
      var date = null;
      date = new Date(parseInt(scope.date));
      var now = new Date();
      var content = '';
      if (now.getFullYear() == date.getFullYear() && now.getMonth() == date.getMonth() && now.getDate() == date.getDate()) {
        // today
        if (now.getHours() > date.getHours()) {
          var hrPassed = now.getHours() - date.getHours();
          content = 'today, ' + hrPassed + ' ' + (hrPassed == 1 ? 'hour' : 'hours') + ' ago'
        } else if (now.getMinutes() > date.getMinutes()) {
          var minPassed = now.getMinutes() - date.getMinutes();
          content = 'today, ' + minPassed + ' ' + (minPassed == 1 ? 'min' : 'mins') + ' ago'
        } else {
          content = 'a moment ago';
        }
      } else {
        var yesterday = new Date();
        yesterday.setDate(now.getDate() - 1);
        if (now.getFullYear() == date.getFullYear() && now.getMonth() == date.getMonth() && now.getDate() == date.getDate() + 1) {
          // yesterday
          content = 'yesterday, ' + $filter('date')(date, scope.shortFormat);
        } else {
          content = $filter('date')(date, scope.fullFormat)
        }
      }
      scope.content = content;
    }
  };
}]);
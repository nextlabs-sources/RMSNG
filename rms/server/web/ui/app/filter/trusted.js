/*
See https://docs.angularjs.org/error/$sce/insecurl
*/
mainApp.filter('trusted', ['$sce', function($sce) {
    return function(url) {
        return $sce.trustAsResourceUrl(url);
    };
}]);

mainApp.filter('initials', function() {
    return function(input) {
        if (input === undefined) {
            return null;
        }
        var names = input.split(' '),
            initials = names[0].substring(0, 1).toUpperCase();

        if (names.length > 1) {
            initials += names[names.length - 1].substring(0, 1).toUpperCase();
        }
        return initials;
    }
});
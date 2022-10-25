mainApp.controller('RightsProtectionMethodController', [ '$scope', 'RightsProtectionMethodService', '$filter', function($scope, RightsProtectionMethodService, $filter){

    $scope.toggle = function() {
        if($scope.isAdhocRightEnabled == true) {
            $scope.isAdhocRightEnabled = false;
        } else {
            $scope.isAdhocRightEnabled = true;
        }
        setRightsProtectionMethod();
    }

    var setRightsProtectionMethod = function() {
        var params = {
            "parameters" : {
                "adhocEnabled": $scope.isAdhocRightEnabled
            }
        }
        RightsProtectionMethodService.setAdhocRights(params, function(data){
            if(data.statusCode == 200) {
                showSnackbar({
                    isSuccess: true,
                    messages: $filter('translate')('adhoc.rights.enable.success')
                });
            } else {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('adhoc.rights.enable.failure')
                });
            }
        });
    }

    var init = function() {
        RightsProtectionMethodService.getAdhocRights(function(data){
            if(data.statusCode == 200) {
                $scope.isAdhocRightEnabled = data.extra["ADHOC_ENABLED"];
            } else {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter('translate')('adhoc.rights.enable.failure')
                });
                $scope.isAdhocRightEnabled = true;
            }
        });
    }

    init();

}]);
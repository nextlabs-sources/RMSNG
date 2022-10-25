mainApp.controller('repositoryController', ['$scope', '$stateParams', '$filter', 'repositoryService', function($scope, $stateParams, $filter, repositoryService) {
        repositoryService.getRepositories(function(data) {
            var allRepositories = data.results.repoItems;
            var currentRepoId = $stateParams.repoId;
            $scope.repoClass = "";

            for(var i = 0; i < allRepositories.length; i++) {
                var currRepo = allRepositories[i];

                if(currRepo.repoId === currentRepoId) {
                    $scope.repoClass = currRepo.providerClass;
                    break;
                }
            }

            if($scope.repoClass === "") {
                // Bug: using $filter('translate')('error key here') does not work here because the en.json is not loaded before the call below.
                showSnackbar({
                    isSuccess: false,
                    messages: "Repository cannot be found"
                });
            }
        });
    }
]);
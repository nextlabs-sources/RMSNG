mainApp.controller('userController', ['$scope', 'userService', '$filter',
    function($scope, userService, $filter) {
        $scope.menuClickedMode = false;
        $scope.userList;
        $scope.formData = {};
        $scope.getUserList = function() {
            $scope.isLoading = true;
            var queryParams = {};
            if ($scope.formData.searchString) {
                queryParams.searchString = $scope.formData.searchString;
            }
            userService.getUserList(queryParams, function(data) {
                if (data && data.statusCode == 200) {
                    $scope.numOfUsers = data.results.totalUsers;
                    $scope.userList = data.results.users;
                } else {
                    showSnackbar({
                        isSuccess: true,
                        messages: data.message
                    });
                }
                $scope.isLoading = false;
            });
        }

        $scope.createAPIUser = function(user, idx) {
            $scope.isLoading = true;
            userService.assignAPIUser(user.userId, function (data) {
                if (data && data.statusCode == 200) {
                    $scope.userList[idx].ticket = data.results.ticket;
                    showSnackbar({
                        isSuccess: true,
                        messages: $filter('translate')('api.user.success.message')
                    });
                } else {
                    showSnackbar({
                        isSuccess: true,
                        messages: data.message
                    });
                }
                $scope.isLoading = false;
            });
        }

        $scope.init = function() {
            $scope.getUserList();
        }

        $scope.init();

        $scope.clearSearch = function () {
            $scope.formData.searchString = '';
            $scope.getUserList();
        }

        $scope.onClickMenu = function(user) {
            $scope.selectedUserId = user.userId;
            $scope.toggleMenuMode();
        }

        $scope.showMenu = function(tenant) {
            $scope.selectedUserId = user.userId;
            $scope.menuClickedMode = true;
        }

        $scope.hideMenu = function(tenant) {
            $scope.selectedUserId = user.userId;
            $scope.menuClickedMode = false;
        }

        $scope.toggleMenuMode = function() {
            $scope.menuClickedMode = !$scope.menuClickedMode;
        }
    }
]);
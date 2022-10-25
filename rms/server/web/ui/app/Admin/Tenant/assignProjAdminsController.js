mainApp.controller('assignProjAdminsController', ['$scope', '$state', '$cookies', '$filter', '$controller', 'configurationService', 'dialogService', 'initSettingsService', function($scope, $state, $cookies, $filter, $controller, configurationService, dialogService, initSettingsService) {
        
        $scope.isDefaultTenant = readCookie('ltId') === readCookie('tenantId');

        $scope.formData = {};

        $scope.validateEmail = function(projectAdminElement) {
            if(projectAdminElement.email != undefined) {
                var emailRegExp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
                return emailRegExp.test(projectAdminElement.email);                
            }
            return false;
        }

        var isDuplicateProjectAdmin = function(projectAdminElement) {
            for(var i = 0; i < $scope.projectAdminsList.length; i++) {
                if($scope.projectAdminsList[i].email.toLowerCase() == projectAdminElement.email.toLowerCase()) {
                    return true;
                }
            }
            return false;
        }

        $scope.addProjectAdmin = function(projectAdminElement) {

            if( !$scope.validateEmail(projectAdminElement) ) {                
                $scope.projectAdminNamingError = true;                
                return;
            }

            if(isDuplicateProjectAdmin(projectAdminElement)) {
                showSnackbar({
                    messages: $filter('translate')('config.project_admin.duplicate_project_admin_error', {email:$scope.formData.projectAdmin.email}),
                    isSuccess: false
                });
                $scope.formData.projectAdmin.email = "";
                return;
            }
            
            $scope.projectAdminsList.push(projectAdminElement);
            $scope.projectAdminNamingError = false;
            $scope.formData.projectAdmin.email = "";
            if(!projectAdminElement.tenantAdmin) $scope.changedProjectAdmin = true;
        }

        $scope.removeProjectAdmin = function(index) {

            $scope.projectAdminsList.splice(index, 1);
            $scope.changedProjectAdmin = true;
        }

        $scope.saveProjectAdmins = function() {
            
            $scope.isLoading = true;
            var projectAdministratorList = [];
            for(var i = 0; i < $scope.projectAdminsList.length; i++) {
                projectAdministratorList.push($scope.projectAdminsList[i].email);
            }           
            
            var projectAdmins = {
                "parameters": {
                    "projectAdmin": projectAdministratorList
                }
            };            
                    
            configurationService.setProjectAdmin(projectAdmins, function(data) {
                $scope.isLoading = false;
                if (data.statusCode == 200) {
                    showSnackbar({
                        messages: $filter('translate')('config.project_admin.save_success'),
                        isSuccess: true
                    });
                    $scope.changedProjectAdmin = false;
                } else {
                    showSnackbar({
                        messages: $filter('translate')('config.project_admin.save_error'),
                        isSuccess: false
                    });                    
                }                                    
            });
            
        }

        $scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
            if(!($scope.isSystemAdmin || $scope.isTenantAdmin)) {
                $state.go(STATE_PAGE_UNAUTHORIZED);
                return;
            }
        });

        $scope.saml_ldap_idps = [];
        $scope.non_saml_ldap_idps = [];

        var getProjectAdminSetting = function() {
            $scope.hideAdminLabel = true;
            $scope.changedProjectAdmin = false;
            $scope.projectAdminsList = [];
            $scope.formData.projectAdmin = {
                "email": "",
                "tenantAdmin": false
            };
            configurationService.getProjectAdmin(function(data){
                if(data.statusCode == 200 ) {
                    for(var i = 0; i < data.results.projectAdmin.length; i++) {
                        var projectAdminElement = {
                            "email": data.results.projectAdmin[i].email,
                            "tenantAdmin": data.results.projectAdmin[i].tenantAdmin ? true : false
                        };
                        
                        $scope.projectAdminsList.push(projectAdminElement);
                        $scope.hideAdminLabel = false;
                    }
                    $scope.projectAdminNamingError = false;
                } else {
                    $scope.hideAdminLabel = true;
                }
            });
        }

        $scope.$watch('formData.projectAdmin.email',function(){
            if($scope.formData.projectAdmin != undefined) {
               if($scope.projectAdminNamingError === true) {
                    $scope.projectAdminNamingError = false;
                } 
            }
        });       

        var init = function(){
            if(!($scope.isSystemAdmin || $scope.isTenantAdmin)) {
                $state.go(STATE_PAGE_UNAUTHORIZED);
                return;
            }
            getProjectAdminSetting();
        }

        init();
    }
]);

mainApp.controller('tenantController', ['$scope', 'tenantService', '$state', '$filter', 'navService', '$rootScope', 'dialogService',
    function($scope, tenantService, $state, $filter, navService, $rootScope, dialogService) {
        $scope.menuClickedMode = false;
        $scope.sortOptions = tenantService.getSortOptions();
        $scope.selectedSort = $scope.sortOptions[0].lookupCode;
        $scope.tenantList = [];
        $scope.showMenuFlag = true;
        $scope.superAdminList = tenantService.getSuperAdminList();
        $scope.formData = {};
        $scope.isLoading = false;
        $scope.getTenantList = function() {
            $scope.isLoading = true;
            var queryParams = {};
            tenantService.getTenantList(queryParams, function(data) {
                $scope.isLoading = false;
                if (data) {
                    $scope.numOfTenants = data.details.totalTenants;
                    $scope.tenantList = data.details.tenantList;
                    if ($scope.superAdminList && $scope.superAdminList.length > 0) {
                        return;
                    }
                    for (var i = 0; i < $scope.tenantList.length; i++) {
                        if ($scope.tenantList[i].isDefault) {
                            $scope.superAdminList = $scope.tenantList[i].adminList;
                            tenantService.setSuperAdminList($scope.superAdminList);
                            break;
                        }
                    }
                }
            });
        }

        $scope.init = function() {
            switch ($state.current.name) {
                case STATE_ADMIN_TENANT_LIST:
                    navService.setCurrentTab('tenantList');
                    if ($state.params.message) {
                        showSnackbar($state.params.message);
                    }
                    $scope.getTenantList();
                    break;
                case STATE_ADMIN_TENANT_CREATE:
                    if (!$scope.superAdminList) {
                        $state.go(STATE_ADMIN_TENANT_LIST, {reload: true});
                        break;
                    }
                    $scope.isAddTenant = true;
                    $scope.tenant = {};
                    $scope.tenant.adminList = [];
                    for (var i = 0; i < $scope.superAdminList.length; i++) {
                        $scope.tenant.adminList.push($scope.superAdminList[i]);
                    }
                    break;
                case STATE_ADMIN_TENANT_EDIT:
                    $scope.isAddTenant = false;
                    $scope.tenant = $state.params.tenant;
                    if (!$scope.tenant) {
                        $state.go(STATE_ADMIN_TENANT_LIST, {reload: true});
                        break;
                    }
                    break;
                case STATE_ADMIN_LANDING:
                    $scope.showMenuFlag = false;
                    $scope.getTenantList();
                    break;
                default:
                    break;
            }
        }

        $scope.init();

        $scope.onClickTab = function(tab) {
            navService.setCurrentTab(tab);
            $rootScope.$emit("toggleSideBar");
            var tenantId = readCookie("ltId");
            if (tab == 'tenantList') {
                navService.setIsInAllFilesPage(false);
                $state.go(STATE_ADMIN_TENANT_LIST, {tenantId: tenantId}, {reload: true});
                return;
            }
        }

        $scope.onClickMenu = function(tenant) {
            $scope.selectedTenantId = tenant.id;
            $scope.toggleMenuMode();
        }

        $scope.showMenu = function(tenant) {
            $scope.selectedTenantId = tenant.id;
            $scope.menuClickedMode = true;
        }

        $scope.hideMenu = function(tenant) {
            $scope.selectedTenantId = tenant.id;
            $scope.menuClickedMode = false;
        }

        $scope.toggleMenuMode = function() {
            $scope.menuClickedMode = !$scope.menuClickedMode;
        }

        $scope.goToAddTenant = function() {
            $state.go(STATE_ADMIN_TENANT_CREATE);
            return;
        }

        $scope.validateEmail = function(id) {
            validateEmail(id, $scope);
        }

        $scope.goToList = function(message) {
            $state.go(STATE_ADMIN_TENANT_LIST, {message: message});
        }

        $scope.goToEditTenant = function(tenant) {
            $state.go(STATE_ADMIN_TENANT_EDIT, {tenant: tenant});
        }

        $scope.addTenant = function(tenant) {
            $scope.isLoading = true;
            tenantService.createTenant(tenant, function(data) {
                if (data.statusCode == 200) {
                    $scope.goToList({
                        isSuccess: true,
                        messages: $filter('translate')('tenant.create.success', {
                            tenantName: tenant.name
                        })
                    });
                } else if (data.statusCode == 4001 || data.statusCode == 4002 || data.statusCode == 503 || data.statusCode == 4004) {
                    showSnackbar({
                        isSuccess: false,
                        messages: data.message
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('tenant.create.error', {
                            tenantName: tenant.name
                        })
                    });
                }
                $scope.isLoading = false;
            });
        }

        $scope.updateTenant = function(tenant) {
            $scope.isLoading = true;
            tenantService.updateTenant(tenant, function(data) {
                if (data.statusCode == 200) {
                    $scope.goToList({
                        isSuccess: true,
                        messages: $filter('translate')('tenant.update.success', {
                            tenantName: tenant.name
                        })
                    });
                } else {
                    showSnackbar({
                        isSuccess: false,
                        messages: $filter('translate')('tenant.update.error', {
                            tenantName: tenant.name
                        })
                    });
                }
                $scope.isLoading = false;
            });
        }

        $scope.validationCheck = function(tenant) {
            return tenant.adminList.length > 0 && tenant.name && tenant.name.length > 0 && tenant.dnsName;
        }

        $scope.addOrUpdateTenant = function(tenant) {
            if ($scope.validationCheck(tenant)) {
                tenant.adminListStr = tenant.adminList.join(',');
                if ($scope.isAddTenant) {
                    $scope.addTenant(tenant);
                } else {
                    $scope.updateTenant(tenant);
                }
            }
        }

        $scope.deleteTenant = function(tenantName) {
            dialogService.confirm({
                msg: $filter('translate')('tenant.delete.confirm',{tenantName: tenantName}),
                ok: function(data) {
                    $scope.isLoading = true;
                    tenantService.deleteTenant(tenantName, function(data) {
                        if (data.statusCode == 204) {
                            $scope.tenantList = $scope.tenantList.filter(function(tenant) {
                                return tenant.name !== tenantName;
                            });
                            showSnackbar({
                                isSuccess: true,
                                messages: $filter('translate')('tenant.delete.success', {
                                    tenantName: tenantName
                                })
                            });
                        } else {
                            showSnackbar({
                                isSuccess: false,
                                messages: $filter('translate')('tenant.delete.error', {
                                    tenantName: tenantName
                                })
                            });
                        }
                        $scope.isLoading = false;
                    });
                }
            });
        }

        $scope.removeAdmin = function(deletedAdmin) {
            $scope.tenant.adminList = $scope.tenant.adminList.filter(function(admin) {
                return admin !== deletedAdmin;
            });
        }

        $scope.checkAdminEmail = function() {
            return $scope.tenant.adminList && $scope.tenant.adminList.indexOf($scope.formData.newAdmin.toLowerCase()) >= 0;
        }

        $scope.addAdmin = function() {
            if (!$scope.tenant.adminList) {
                $scope.tenant.adminList = [];
            }
            $scope.tenant.adminList.push($scope.formData.newAdmin.toLowerCase());
            $scope.formData.newAdmin = "";
        }

        $scope.goToTenantConsole = function (tenant) {
            if (tenant.isDefault) {
                $rootScope.showTenantConsole = true;
                $state.go(STATE_ADMIN_LANDING);
            }
        }
    }
])
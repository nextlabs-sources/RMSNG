mainApp.controller('settingsController', ['$scope', '$rootScope', '$filter', '$state', 'networkService', '$location', 'settingsService',
    function($scope, $rootScope, $filter, $state, networkService, $location, settingsService) {
        var configurations = null;
        //Load the data from server
        $scope.isLoading = false;
        $scope.cachedConfigurations = {};
        $scope.isDefaultTenant = readCookie('ltId') === readCookie('tenantId');

        $scope.clientManagementList = [{ "field" : $filter('translate')('config.win_client_version_number.label'), "selected" : false, "alias" : "RMC_CURRENT_VERSION"},
                                       { "field" : $filter('translate')('config.client_package_download_url_32.label'), "selected" : false, "alias" : "RMC_UPDATE_URL_32BITS"},
                                       { "field" : $filter('translate')('config.rmc_crc_checksum_32.label'), "selected" : false, "alias" : "RMC_CRC_CHECKSUM_32BITS"},
                                       { "field" : $filter('translate')('config.rmc_sha1_checksum_32.label'), "selected" : false, "alias" : "RMC_SHA1_CHECKSUM_32BITS"},
                                       { "field" : $filter('translate')('config.client_package_download_url_64.label'), "selected" : false, "alias" : "RMC_UPDATE_URL_64BITS"},
                                       { "field" : $filter('translate')('config.rmc_crc_checksum_64.label'), "selected" : false, "alias" : "RMC_CRC_CHECKSUM_64BITS"},
                                       { "field" : $filter('translate')('config.rmc_sha1_checksum_64.label'), "selected" : false, "alias" : "RMC_SHA1_CHECKSUM_64BITS"},
                                       { "field" : $filter('translate')('config.rmc_force_downgrade.label'), "selected" : false, "alias" : "RMC_FORCE_DOWNGRADE"},
                                       { "field" : $filter('translate')('config.mac_client_version_number.label'), "selected" : false, "alias" : "RMC_MAC_CURRENT_VERSION"},
                                       { "field" : $filter('translate')('config.rmc_crc_checksum_mac.label'), "selected" : false, "alias" : "RMC_CRC_CHECKSUM_MAC"},
                                       { "field" : $filter('translate')('config.rmc_sha1_checksum_mac.label'), "selected" : false, "alias" : "RMC_SHA1_CHECKSUM_MAC"}];

        $scope.downloadLinkList = [{ "field" : $filter('translate')('config.download.rm.rmd.win32'), "selected" : false, "alias" : "RMD_WIN_32_DOWNLOAD_URL"}, 
                                   { "field" : $filter('translate')('config.download.rm.rmd.win64'), "selected" : false, "alias" : "RMD_WIN_64_DOWNLOAD_URL"}, 
                                   { "field" : $filter('translate')('config.download.rm.rmd.mac'), "selected" : false, "alias" : "RMD_MAC_DOWNLOAD_URL"}, 
                                   { "field" : $filter('translate')('config.download.rm.rmc.iOS'), "selected" : false, "alias" : "RMC_IOS_DOWNLOAD_URL"}, 
                                   { "field" : $filter('translate')('config.download.rm.rmc.Android'), "selected" : false, "alias" : "RMC_ANDROID_DOWNLOAD_URL"}];

        $scope.tenantManagementList = [{ "field" : $filter('translate')('config.client_heartbeat_frequency'), "selected" : false, "alias" : "CLIENT_HEARTBEAT_FREQUENCY"}];

        $scope.dismissStatus = function() {
            $scope.messageStatus = 0;
        }

        $scope.resetDataTypes = function() {
            $scope.messageStatus = 0;
        }

        var updateLandingPage = function(list) {
            for(var i = 0; i < list.length; i++) {
                for(var j in $scope.configurations) {
                    if(list[i].alias == j) {
                        if($scope.configurations[j] == "") {
                            list[i].selected = false;
                        } else {
                            list[i].selected = true;
                        }
                    }
                }
            }
        }

        var loadConfigurationSetting = function() {
            settingsService.getSettings(function(data) {
                $scope.configurations = data;
                updateLandingPage($scope.clientManagementList);
                updateLandingPage($scope.downloadLinkList);
                updateLandingPage($scope.tenantManagementList);
                $scope.cachedConfigurations = angular.copy(data);
                $scope.resetDataTypes();
            });
        }

        var isValidInput = function(data) {

            if (data) {
                return true;
            }
            return false;
        }


        $scope.scrollTo = function(id) {
            var pos = $("#" + id).position();
            $(".container-fluid.rms-container-fluid").scrollTop(pos.top);
            $(".container-fluid.rms-container-fluid").scrollLeft(pos.left);
        };

        $scope.doReset = function() {
            $scope.configurations = angular.copy($scope.cachedConfigurations);
            $scope.resetDataTypes();
        };

        function is_natural(s) {
            var n = parseInt(s, 10);
            return n >= 0 && n.toString() === s;
        }

        function validateParams(params) {
                
            if (isNaN(params.CLIENT_HEARTBEAT_FREQUENCY) || !is_natural(params.CLIENT_HEARTBEAT_FREQUENCY)) {                                               
                return false;
            }                
            return true;
        }
            
        $scope.doSave = function() {
            $scope.isLoading = true;
            var params = {
                "RMC_CURRENT_VERSION": ($scope.configurations.RMC_CURRENT_VERSION) ? $scope.configurations.RMC_CURRENT_VERSION : "",
                "RMC_UPDATE_URL_32BITS": ($scope.configurations.RMC_UPDATE_URL_32BITS) ? $scope.configurations.RMC_UPDATE_URL_32BITS : "",
                "RMC_CRC_CHECKSUM_32BITS": ($scope.configurations.RMC_CRC_CHECKSUM_32BITS) ? $scope.configurations.RMC_CRC_CHECKSUM_32BITS : "",
                "RMC_SHA1_CHECKSUM_32BITS": ($scope.configurations.RMC_SHA1_CHECKSUM_32BITS) ? $scope.configurations.RMC_SHA1_CHECKSUM_32BITS : "",
                "RMC_UPDATE_URL_64BITS": ($scope.configurations.RMC_UPDATE_URL_64BITS) ? $scope.configurations.RMC_UPDATE_URL_64BITS : "",
                "RMC_CRC_CHECKSUM_64BITS": ($scope.configurations.RMC_CRC_CHECKSUM_64BITS) ? $scope.configurations.RMC_CRC_CHECKSUM_64BITS : "",
                "RMC_SHA1_CHECKSUM_64BITS": ($scope.configurations.RMC_SHA1_CHECKSUM_64BITS) ? $scope.configurations.RMC_SHA1_CHECKSUM_64BITS : "",
                "RMC_MAC_CURRENT_VERSION": ($scope.configurations.RMC_MAC_CURRENT_VERSION) ? $scope.configurations.RMC_MAC_CURRENT_VERSION : "",
                "RMC_CRC_CHECKSUM_MAC": ($scope.configurations.RMC_CRC_CHECKSUM_MAC) ? $scope.configurations.RMC_CRC_CHECKSUM_MAC : "",
                "RMC_SHA1_CHECKSUM_MAC": ($scope.configurations.RMC_SHA1_CHECKSUM_MAC) ? $scope.configurations.RMC_SHA1_CHECKSUM_MAC : "",
                "RMC_FORCE_DOWNGRADE": ($scope.configurations.RMC_FORCE_DOWNGRADE) ? $scope.configurations.RMC_FORCE_DOWNGRADE : false,
                "RMD_WIN_32_DOWNLOAD_URL": ($scope.configurations.RMD_WIN_32_DOWNLOAD_URL) ? $scope.configurations.RMD_WIN_32_DOWNLOAD_URL : "",
                "RMD_WIN_64_DOWNLOAD_URL": ($scope.configurations.RMD_WIN_64_DOWNLOAD_URL) ? $scope.configurations.RMD_WIN_64_DOWNLOAD_URL : "",
                "RMD_MAC_DOWNLOAD_URL": ($scope.configurations.RMD_MAC_DOWNLOAD_URL) ? $scope.configurations.RMD_MAC_DOWNLOAD_URL : "",
                "RMC_IOS_DOWNLOAD_URL": ($scope.configurations.RMC_IOS_DOWNLOAD_URL) ? $scope.configurations.RMC_IOS_DOWNLOAD_URL : "",
                "RMC_ANDROID_DOWNLOAD_URL": ($scope.configurations.RMC_ANDROID_DOWNLOAD_URL) ? $scope.configurations.RMC_ANDROID_DOWNLOAD_URL : "",
                "CLIENT_HEARTBEAT_FREQUENCY": ($scope.configurations.CLIENT_HEARTBEAT_FREQUENCY) ? $scope.configurations.CLIENT_HEARTBEAT_FREQUENCY : ""
            };

            if (!$scope.isDefaultTenant && !validateParams(params)) {                
                
                $scope.cachedConfigurations = angular.copy($scope.configurations);
                $scope.resetDataTypes();
                $scope.message = $filter('translate')('config.client_heartbeat_frequency.invalid');
                $scope.messageStatus = 1;
                $scope.isLoading = false;
                return;
            }

            settingsService.saveSettings(function(data) {
                if (!data.result) {
                    $scope.message = data.message;
                    $scope.messageStatus = 1;
                } else {
                    $scope.clientMgmtForm.$setPristine();
                    $scope.cachedConfigurations = angular.copy($scope.configurations);
                    $scope.resetDataTypes();
                    $scope.message = data.message;
                    $scope.messageStatus = 2;
                }
                $scope.isLoading = false;
                $scope.scrollTo('settings-pane');
            }, params, function(response) {
                $scope.isLoading = false;
            });
        }

        $scope.setFormDirty = function(formName) {
            formName.$setDirty();
        }

        loadConfigurationSetting();

    }
]);
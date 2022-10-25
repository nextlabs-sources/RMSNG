mainApp.controller('userAttributeMappingController', ['$scope', '$state', '$cookies', '$filter', '$controller', 'configurationService', 'dialogService', 'initSettingsService', function($scope, $state, $cookies, $filter, $controller, configurationService, dialogService, initSettingsService) {
        
        $scope.isDefaultTenant = readCookie('ltId') === readCookie('tenantId');
        $scope.selectUserAttribute = true;

        var getIdpUserAttributesMap = function () {
            $scope.isLoading = true;
            configurationService.getIdpUserAttributesMap(function(data) {
                $scope.isLoading = false;
                if (data.statusCode != 200) {
                    showSnackbar({
                        messages: $filter('translate')('config.attr_mapping.save_error'),
                        isSuccess: false
                    });
                } else {
                    $scope.idps = data.results.idps;
                    filter_saml_ldap_idps();
                }
            });
        }

        var setIdpUserAttributesMap = function () {
            var message;
            $scope.isLoading = true;
            dialogService.confirm({
                msg: $filter('translate')('invalidate.session_message'),
                showCheckbox: $filter('translate')('invalidate.session_checkbox'),
                setAttribute: $filter('translate')('invalidate.session_info'),
                ok: function(data) {
                    $scope.checkboxModel = data.checkboxModel;
                    if(!$scope.checkboxModel) {
                        message = $filter('translate')('config.attr_mapping.save_success') + $filter('translate')('config.redirect');
                    } else {
                        message = $filter('translate')('config.attr_mapping.save_success');
                    }
                    var attributes = {
                        "parameters": {
                            "attributes": $scope.attributeList
                        }
                    };
                    
                    configurationService.setUserAttribute(attributes,function(data){
                        if(data.statusCode == 200) {
                            configurationService.setIdpUserAttributesMap($scope.saml_ldap_idps, !$scope.checkboxModel, function(data, invalidateSessions) {
                                $scope.isLoading = false;
                                if (data.statusCode == 200) {
                                    showSnackbar({
                                        messages: message,
                                        isSuccess: true
                                    });
                                    if(invalidateSessions) {
                                        setTimeout(function(){initSettingsService.logout();},5000);
                                    }
                                } else {
                                    showSnackbar({
                                        messages: $filter('translate')('config.attr_mapping.save_error'),
                                        isSuccess: false
                                    });
                                }
                            });
                        } else {
                            showSnackbar({
                                messages: $filter('translate')('config.attr_mapping.save_error'),
                                isSuccess: false
                            });
                        }
                    });   
                }
            });
        }

        $scope.formData = {};

        $scope.goBackToSelectingAttributePage = function() {
            $scope.selectUserAttribute = true;
        }

        $scope.save = function() {
            for(var key1 in $scope.saml_ldap_idps) {
                for(var key2 in $scope.saml_ldap_idps[key1].userAttrMap) {
                    if (!/^[\u00C0-\u1FFF\u2C00-\uD7FF\w \x5F\x2D]+$/g.test($scope.saml_ldap_idps[key1].userAttrMap[key2])) {
                        showSnackbar({
                            isSuccess: false,
                            messages: $filter('translate')('idp.name.validation.pattern')
                        });
                        return;
                    }
                }
            }
            setIdpUserAttributesMap();
        }

        $scope.cancel = function() {
            $scope.goBackToSelectingAttributePage();
            $scope.selectedAttributes = [];
            $scope.attributeList.length;
            $scope.formData.customAttributes = "";
            var firstIndex = -1;
            var numOfCustom = 0;
            for(var i = 0; i < $scope.attributeList.length; i++) {
                if($scope.attributeList[i].custom) {
                    if(firstIndex == -1) {
                        firstIndex = i;
                    }
                    numOfCustom++;
                } else if($scope.attributeList[i].selected) {
                    $scope.attributeList[i].selected = false;
                }
            }
            if(firstIndex != -1) {
                $scope.attributeList.splice(firstIndex,numOfCustom);
            }
        }

        $scope.proceed = function() {
            $scope.selectUserAttribute = false;
            var present = false;
            var key;
            for(var i = 0; i < $scope.saml_ldap_idps.length; i++) {
                for(key in $scope.saml_ldap_idps[i].userAttrMap) {
                    for(var j = 0; j < $scope.attributeList.length; j++) {
                        if(key == $scope.attributeList[j].name && $scope.attributeList[j].selected) {
                            present = true;
                            break;
                        }
                    }
                    if(present == false) {
                        delete $scope.saml_ldap_idps[i].userAttrMap[key];
                    }
                    present = false;
                }
            }
        }

        var getIndexOfAttribute = function(name, list) {
            var position;
            for(var i = 0; i < list.length; i++) {
                if(name == list[i].name) {
                    position = i;
                    break;
                }
            }
            return position;
        }

        $scope.toggleAttribute = function(attribute) {
            if(attribute.selected == false) {
                attribute.selected = true;
                $scope.selectedAttributes.push(attribute);
            } else if(attribute.selected == true) {
                var position;
                if(attribute.custom) {
                    position = getIndexOfAttribute(attribute.name, $scope.attributeList);
                    if(position != undefined) {
                        $scope.attributeList.splice(position, 1);
                    }
                } else {
                    attribute.selected = false;
                }
                position = getIndexOfAttribute(attribute.name, $scope.selectedAttributes);
                if(position != undefined) {
                    $scope.selectedAttributes.splice(position, 1);
                }
            }
        }

        var isDuplicateCustomAttributes = function() {
            for(var i = 0; i < $scope.attributeList.length; i++) {
                if($scope.attributeList[i].name.toLowerCase() == $scope.formData.customAttributes.toLowerCase()) {
                    return true;
                }
            }
            return false;
        }

        $scope.addCustomAttributes = function() {
            if(isDuplicateCustomAttributes()) {
                showSnackbar({
                    messages: $filter('translate')('duplicate.user.attribute.error'),
                    isSuccess: false
                });
                $scope.formData.customAttributes = "";
                return;
            }
            var attribute = {
                "name": $scope.formData.customAttributes,
                "selected": true,
                "custom": true
            };
            $scope.attributeList.push(attribute); 
            $scope.formData.customAttributes = "";
            $scope.selectedAttributes.push(attribute);
        }

        $scope.$on('$stateChangeSuccess', function(ev, to, toParams, from, fromParams) {
            if(!($scope.isSystemAdmin || $scope.isTenantAdmin)) {
                $state.go(STATE_ADMIN_UNAUTHORIZED);
                return;
            }
        });

        $scope.saml_ldap_idps = [];
        $scope.non_saml_ldap_idps = [];

        $scope.removeUserAttribute = function(attribute) {
            var position = getIndexOfAttribute(attribute.name, $scope.selectedAttributes);
            if(!$scope.selectedAttributes[position].custom) {
                $scope.selectedAttributes.splice(position,1);
                position = getIndexOfAttribute(attribute.name, $scope.attributeList);
                $scope.attributeList[position].selected = false;
                $scope.proceed();
                return;
            } 
            $scope.selectedAttributes.splice(position,1);
            position = getIndexOfAttribute(attribute.name, $scope.attributeList);
            $scope.attributeList.splice(position,1);  
            $scope.proceed();
        }

        var filter_saml_ldap_idps = function () {
            var samlIdx = 1;
            var ldapIdx = 1;
            var azureIdx = 1;
            for (var index = 0; index < $scope.idps.length; index ++) {
                var idp = $scope.idps[index];
                if (idp.type == 1) {
                    if (idp.name == null) {
                        idp.name = 'SAML(' +  samlIdx + ')';
                        samlIdx++;
                    }
                    $scope.saml_ldap_idps.push(idp);
                } else if (idp.type == 4) {
                    if (idp.name == null) {
                        idp.name = 'LDAP(' +  ldapIdx + ')';
                        ldapIdx++;
                    }
                    $scope.saml_ldap_idps.push(idp);
                } else if (idp.type == 6) {
                    if (idp.name == null) {
                        idp.name = 'AZUREAD(' +  azureIdx + ')';
                        azureIdx++;
                    }
                    $scope.saml_ldap_idps.push(idp);
                } else if (idp.type == 0) {
                    $scope.non_saml_ldap_idps.push("RMS");
                } else if (idp.type == 2) {
                    $scope.non_saml_ldap_idps.push("Google");
                } else if (idp.type == 3) {
                    $scope.non_saml_ldap_idps.push("Facebook");
                }
            }
            if ( $scope.non_saml_ldap_idps.length == 1) {
                $scope.non_saml_ldap_idps_text = $scope.non_saml_ldap_idps[0] + ".";
            } else if ($scope.non_saml_ldap_idps.length > 1) {
                $scope.non_saml_ldap_idps_text = $scope.non_saml_ldap_idps[0];
                for (var i = 1; i < $scope.non_saml_ldap_idps.length; i ++) {
                    $scope.non_saml_ldap_idps_text += ", " + $scope.non_saml_ldap_idps[i];
                }
                $scope.non_saml_ldap_idps_text += ".";
            }
        }

        var getUserAttributeSetting = function() {
            $scope.selectedAttributes = [];
            configurationService.getUserAttribute(function(data){
                if(data.statusCode == 200) {
                    $scope.attributeList = data.results.attrList;
                    $scope.max = data.results.maxSelectNum;
                    $scope.selectMaxValue = $filter('translate')('config.attr_mapping.select_max_values',{max:$scope.max});
                    for(var i = 0; i < $scope.attributeList.length; i++) {
                        if($scope.attributeList[i].selected) {
                            $scope.selectedAttributes.push($scope.attributeList[i]);
                        }
                    }
                } else {
                    $scope.attributeList = [];
                    $scope.selectMaxValue = $filter('translate')('config.attr_mapping.select_max_values',{max:0});
                    $scope.max = 0;
                    $scope.selectedAttributes = [];
                }
            });
        }

        $scope.$watch('selectedAttributes.length',function(){
            if($scope.selectedAttributes != undefined) {
                if($scope.selectedAttributes.length >= $scope.max) {
                    $scope.maxSelected = $filter('translate')('config.attr_max_values_already_selected',{max:$scope.max});
                }
            }
        });

        $scope.$watch('formData.customAttributes',function(){
            if($scope.formData.customAttributes != undefined) {
               if($scope.formData.customAttributes.match(/^[\u00C0-\u1FFF\u2C00-\uD7FF\w \x5F\x2D]+$/g) || $scope.formData.customAttributes == "") {
                    $scope.attributeNamingError = false;
                } else {
                    $scope.attributeNamingError = true;
                } 
            }
        });   

        var init = function(){
            if(!($scope.isSystemAdmin || $scope.isTenantAdmin)) {
                $state.go(STATE_ADMIN_UNAUTHORIZED);
                return;
            }
            getIdpUserAttributesMap();
        }

        init();
        getUserAttributeSetting();
    }
]);

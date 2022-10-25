mainApp.controller("policyStudioController", [
    "$scope",
    "$rootScope",
    "$filter",
    "$state",
    "$stateParams",
    "$timeout",
    "$location",
    "$anchorScroll",
    "projectStateService",
    "dialogService",
    "initSettingsService",
    "networkService",
    "navService",
    "policyService",
    function (
        $scope,
        $rootScope,
        $filter,
        $state,
        $stateParams,
        $timeout,
        $location,
        $anchorScroll,
        projectStateService,
        dialogService,
        initSettingsService,
        networkService,
        navService,
        policyService
    ) {
        var RMS_CONTEXT_NAME = initSettingsService.getRMSContextName();
        var POLICY_PAGE = 0;
        var POLICY_SIZE = 0;

        /*Variables used for Date Range*/
        var scheduleConfig = {
            startDateTime: null,
            endDateTime: null,
            recurrenceDateOfMonth: -1,
            recurrenceDayInMonth: -1,
            sunday: false,
            monday: false,
            tuesday: false,
            wednesday: false,
            thursday: false,
            friday: false,
            saturday: false,
            recurrenceStartTime: "12:00:00 AM",
            recurrenceEndTime: "11:59:59 PM"
        };

        /* Variables used for Relative Date*/
        $scope.effect_type = true;

        /*Used to set time for Policy Expiry */
        $scope.amPmOptions = ["AM", "PM"];

        $scope.policyRecur = {
            val: null,
            always: true,
            specificDays: false,
            from: null,
            to: null
        };

        $scope.policyRecur.from = new Date();
        $scope.policyRecur.from.setHours(0);
        $scope.policyRecur.from.setMinutes(0);
        $scope.policyRecur.from.setSeconds(0);
        $scope.policyRecur.to = new Date();
        $scope.policyRecur.to.setHours(23);
        $scope.policyRecur.to.setMinutes(59);
        $scope.policyRecur.to.setSeconds(59);

        /*Used to set Days for Policy Expiry*/
        $scope.recurByDaysOptions = [
            {
                label: "SUN",
                value: "sunday",
                checked: true
            },
            {
                label: "MON",
                value: "monday",
                checked: true
            },
            {
                label: "TUE",
                value: "tuesday",
                checked: true
            },
            {
                label: "WED",
                value: "wednesday",
                checked: true
            },
            {
                label: "THU",
                value: "thursday",
                checked: true
            },
            {
                label: "FRI",
                value: "friday",
                checked: true
            },
            {
                label: "SAT",
                value: "saturday",
                checked: true
            }
        ];

        $scope.policyValidity = {
            from: {
                val: "today",
                today: true,
                specificDate: false,
                popupOpen: false,
                date: null
            },
            to: {
                val: "neverExpire",
                neverExpire: true,
                specificDate: false,
                popupOpen: false,
                date: null
            }
        };

        var bool =
            $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES ||
                $state.current.name ==
                STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE ||
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT ||
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST
                ? true
                : false;
        $scope.isDefaultTenant = readCookie("ltId") === readCookie("tenantId");
        $scope.policyModel = {};

        $scope.toggleAdvance = function () {
            $scope.showAdvance = !$scope.showAdvance;
        };
        $scope.advancedValidated = false;
        $scope.advancedValid = false;
        $scope.$watch(
            "policyModel",
            function (newVal, oldVal) {
                if (newVal != oldVal) $scope.$broadcast("policyModelLoaded", newVal);
            },
            true
        );

        $scope.showAdvance = false;
        $scope.policyFilter = {};
        $scope.policyFilter.searchPolicyString = "";

        $rootScope.sortPolicyOptions = [
            {
                lookupCode: "-lastUpdatedDate",
                description: "last.modified"
            },
            {
                lookupCode: "lastUpdatedDate",
                description: "first.modified"
            },
            {
                lookupCode: "name",
                description: "filename.ascending"
            },
            {
                lookupCode: "-name",
                description: "filename.descending"
            }
        ];

        $scope.onClickMenu = function (id) {
            $scope.selectedFileId = id;
            $scope.toggleMenuMode();
        };

        $scope.showMenu = function (id) {
            $scope.selectedFileId = id;
            $scope.MenuClickedMode = true;
        };

        $scope.hideMenu = function (id) {
            $scope.selectedFileId = id;
            $scope.MenuClickedMode = false;
        };

        $scope.MenuClickedMode = false;

        $scope.toggleMenuMode = function () {
            $scope.MenuClickedMode = !$scope.MenuClickedMode;
        };

        var checkInvalidCharacters = function (policyName) {
            if (policyName) {
                var regex = /^[\w -]*$/;
                var results = regex.test(policyName);
                return results;
            }
            return false;
        };

        $scope.componentOperators = [
            {
                text: "in",
                value: "IN"
            },
            {
                text: "not in",
                value: "NOT"
            }
        ];

        $scope.goToCreate = function () {
            if (
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST
            ) {
                $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE);
            } else if ($state.current.name == STATE_ADMIN_TENANT_POLICIES_LIST) {
                $state.go(STATE_ADMIN_TENANT_POLICIES_CREATE);
            } else if (
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_LIST
            ) {
                $state.go(STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE);
            } else {
                $state.go(STATE_PROJECT_POLICIES_CREATE);
            }
            $scope.getDefaultModels();
            $scope.resetForm();
        };

        $scope.cancel = function () { };

        $scope.goToList = function () {
            if (
                $state.current.name == STATE_ADMIN_TENANT_POLICIES_CREATE ||
                $state.current.name == STATE_ADMIN_TENANT_POLICIES_EDIT
            ) {
                $state.go(STATE_ADMIN_TENANT_POLICIES_LIST);
            } else if (
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE ||
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_EDIT
            ) {
                $state.go(STATE_TENANT_ADMIN_TENANT_POLICIES_LIST);
            } else if (
                $state.current.name ==
                STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE ||
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT
            ) {
                $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST);
            } else {
                $state.go(STATE_PROJECT_POLICIES_LIST);
            }
            $scope.getPolicies();
        };

        $scope.toggleSelection = function (id) {
            var idx = $scope.rights.indexOf(id);
            if (idx > -1) {
                $scope.rights.splice(idx, 1);
            } else {
                $scope.rights.push(id);
            }
        };

        $scope.$on("projectTagList", function (event, result) {
            $scope.projectTagList = result.data;
        });

        $scope.createOrUpdatePolicy = function (toDeploy) {
            if (!$scope.policy.name) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter("translate")("policy.name.cannot.empty")
                });
                $timeout(function () {
                    document.getElementById("policyName").focus();
                });
                return;
            } else if ($scope.policy.name.length > 255) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter("translate")("policy.name.exceed.maxlength")
                });
                $timeout(function () {
                    document.getElementById("policyName").focus();
                });
                return;
            } else if (!checkInvalidCharacters($scope.policy.name)) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter("translate")("policy.name.invalid.chars")
                });
                $timeout(function () {
                    document.getElementById("policyName").focus();
                });
                return;
            }

            if ($scope.policy.description && !checkInvalidCharacters($scope.policy.description)) {
                showSnackbar({
                    isSuccess: false,
                    messages: $filter("translate")("policy.description.invalid.chars")
                });
                $timeout(function () {
                    document.getElementById("policyDescription").focus();
                });
                return;
            }

            if (!$scope.effect_type) {
                var message = calculateDateRange();
                if (message) {
                    showSnackbar({
                        isSuccess: false,
                        messages: message
                    });
                    return;
                }
            }

            $scope.isLoading = true;
            var userComponents = [];
            var resourceComponents = [];
            var applicationComponents = [];
            var actions = [];
            var jsonABACMembershipObligation = [
                {
                    tagIds: [],
                    allowAll: false
                }
            ];

            if ($scope.policy.resources && $scope.policy.resources.length > 0) {
                resourceComponents = getSimplifiedComponents(
                    $scope.policy.resources,
                    $scope.policyModel.resource
                );
            }
            if (
                $scope.policy.userComponents &&
                $scope.policy.userComponents.length > 0
            ) {
                userComponents = getSimplifiedComponents(
                    $scope.policy.userComponents,
                    null
                );
            }
            if (
                $scope.policy.applicationComponents &&
                $scope.policy.applicationComponents.length > 0
            ) {
                applicationComponents = getSimplifiedComponents(
                    $scope.policy.applicationComponents,
                    null
                );
            }
            if ($scope.projectTagList) {
                jsonABACMembershipObligation = [
                    {
                        tagIds: $scope.projectTagList,
                        allowAll: false
                    }
                ];
            }
            var policy = {
                parameters: {
                    policy: {
                        name: $scope.policy.name,
                        description: $scope.policy.description,
                        resources: resourceComponents,
                        userComponents: userComponents,
                        actions: $scope.rights,
                        toDeploy: toDeploy,
                        jsonABACMembershipObligation: jsonABACMembershipObligation,
                        scheduleConfig: null
                    }
                }
            };
            if (!$scope.effect_type) {
                policy.parameters.policy.scheduleConfig = scheduleConfig;
            } else {
                policy.parameters.policy.scheduleConfig = null;
            }

            if ($scope.showAdvance) {
                policy.parameters.policy.applicationComponents = applicationComponents;
                policy.parameters.policy.advancedConditions =
                    $scope.policy.advancedConditions;
                if (
                    policy.parameters.policy.advancedConditions != undefined &&
                    policy.parameters.policy.advancedConditions.split(" ").join("")
                ) {
                    var tokenGroupName = getTokenGroupName();
                    var url =
                        RMS_CONTEXT_NAME +
                        "/rs/policy/" +
                        tokenGroupName +
                        "/policies/expressionValidate";
                    networkService.post(
                        url,
                        $scope.policy.advancedConditions,
                        getTextPlainHeaders(),
                        function (data) {
                            if (data.statusCode === 200) {
                                $scope.advancedValidated = true;
                                $scope.advancedValid = data.results.valid;
                                if ($scope.advancedValid == false) {
                                    $scope.isLoading = false;
                                    $location.hash("advanced");
                                    $anchorScroll();
                                } else {
                                    submitPolicy(policy);
                                }
                            } else {
                                $scope.isLoading = false;
                                message = $filter("translate")(
                                    "policy.advanced.validate.error"
                                );
                            }
                        }
                    );
                } else {
                    submitPolicy(policy);
                }
            } else {
                submitPolicy(policy);
            }
        };

        var getTokenGroupName = function () {
            if (APPNAME === "admin") {
                return readCookie("lt");
            } else {
                return $stateParams.tenantId
                    ? $stateParams.tenantId
                    : projectStateService.getTokenGroupName();
            }
        };

        var submitPolicy = function (policy) {
            if ($scope.policy.id) {
                policy.parameters.policy.id = $scope.policy.id;
                policy.parameters.policy.version = $scope.policy.version;
                var tokenGroupName = getTokenGroupName();
                var url =
                    RMS_CONTEXT_NAME +
                    "/rs/policy/" +
                    tokenGroupName +
                    "/policies/update";
                networkService.post(url, policy, getJsonHeadersPolicy(bool), function (
                    data
                ) {
                    $scope.isLoading = false;
                    if (data.statusCode === 200) {
                        isSuccess = true;
                        message = $filter("translate")("policy.update.success");
                        $scope.skipReminder = true;
                        $scope.policyModel = {};
                        $scope.goToList();
                    } else {
                        isSuccess = false;
                        if (data.statusCode === 409) {
                            message = $filter("translate")("policy.already.exists");
                        } else {
                            message = $filter("translate")("policy.update.error");
                        }
                    }
                    showSnackbar({
                        isSuccess: isSuccess,
                        messages: message
                    });
                });
            } else {
                var tokenGroupName = getTokenGroupName();
                var url =
                    RMS_CONTEXT_NAME + "/rs/policy/" + tokenGroupName + "/policies";
                networkService.put(url, policy, getJsonHeadersPolicy(bool), function (
                    data
                ) {
                    $scope.isLoading = false;
                    if (data.statusCode === 200) {
                        isSuccess = true;
                        message = $filter("translate")("policy.create.success");
                        $scope.resetForm();
                        $scope.skipReminder = true;
                        $scope.policyModel = {};
                        $scope.goToList();
                    } else {
                        isSuccess = false;
                        if (data.statusCode === 409) {
                            message = $filter("translate")("policy.already.exists");
                        } else {
                            message = $filter("translate")("policy.creation.error");
                        }
                    }
                    showSnackbar({
                        isSuccess: isSuccess,
                        messages: message
                    });
                });
            }
        };

        $scope.validate = function () {
            $scope.isLoading = true;
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME +
                "/rs/policy/" +
                tokenGroupName +
                "/policies/expressionValidate";
            networkService.post(
                url,
                $scope.policy.advancedConditions,
                getTextPlainHeaders(),
                function (data) {
                    $scope.isLoading = false;
                    if (data.statusCode === 200) {
                        $scope.advancedValidated = true;
                        $scope.advancedValid = data.results.valid;
                    } else {
                        message = $filter("translate")("policy.advanced.validate.error");
                    }
                }
            );
        };

        var convertConditionsToComponent = function (conditions, policyModel) {
            var component = {
                conditions: [],
                type: policyModel.type,
                policyModel: {
                    id: policyModel.id,
                    name: policyModel.name
                }
            };
            conditions.forEach(function (condition) {
                component.conditions.push({
                    attribute: condition.attribute.shortName,
                    operator: condition.operator.key,
                    value: condition.value
                });
            });
            return component;
        };

        var convertComponentToConditions = function (component) {
            var conditions = [];
            for (var i = 0; i < component.conditions.length; i++) {
                var condition = {};
                condition.attribute = {
                    dataType: "STRING",
                    name: component.conditions[i].attribute,
                    shortName: component.conditions[i].attribute,
                    operatorConfigs: defaultOperatorConfigs
                };
                condition.operator =
                    component.conditions[i].operator == "="
                        ? defaultOperatorConfigs[0]
                        : defaultOperatorConfigs[1];
                condition.$$hashKey = 100 + i;
                condition.id = null;
                condition.isEdit = false;
                conditions.push(condition);
            }
            return conditions;
        };

        var getSimplifiedComponents = function (componentSets, policyModel) {
            var simplifiedComponentSets = [];
            for (var i = 0; i < componentSets.length; i++) {
                var componentSet = {
                    operator: componentSets[i].operator.value,
                    components: []
                };
                for (var j = 0; j < componentSets[i].components.length; j++) {
                    var componentVerbose = componentSets[i].components[j];
                    var component = {
                        conditions: []
                    };
                    if (policyModel) {
                        component.policyModel = {
                            id: policyModel.id,
                            name: policyModel.name
                        };
                        component.id = componentVerbose.id;
                        component.type = policyModel.type;
                        component.name = componentVerbose.name;
                        component.description = componentVerbose.description;
                        component.version = componentVerbose.version;
                    }
                    for (var k = 0; k < componentVerbose.conditions.length; k++) {
                        var condition = {
                            attribute: componentVerbose.conditions[k].attribute.name,
                            operator: componentVerbose.conditions[k].operator.key,
                            value: componentVerbose.conditions[k].value,
                            combiner: componentVerbose.conditions[k].combiner
                        };
                        if (!policyModel) {
                            if (!Array.isArray(condition.value)) {
                                condition.value = condition.value.split(",");
                            }
                        }
                        component.conditions.push(condition);
                    }
                    componentSet.components.push(component);
                }
                if (componentSet.components.length) {
                    simplifiedComponentSets.push(componentSet);
                }
            }
            return simplifiedComponentSets;
        };

        $scope.resetForm = function () {
            $scope.subjectComponentIds = [];
            $scope.resourceComponentIds = [];
            $scope.policy = {};
            $scope.policy.subjectConditions = [];
            $scope.policy.resourceConditions = [];
            $scope.rights = ["VIEW"];
            $scope.toProjectSpace = true;
            $scope.advancedValidated = false;
            $scope.showAdvance = false;
            $scope.scheduleConfig = {};
            $scope.effect_type = true;
        };

        $scope.getDefaultModels = function () {
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME + "/rs/policy/" + tokenGroupName + "/policies/models";
            $scope.isLoading = true;
            networkService.get(url, getJsonHeadersPolicy(bool), function (data) {
                $scope.isLoading = false;
                if (data.statusCode === 200) {
                    $scope.policyModel = data.results;
                } else {
                    var message = $filter("translate")(
                        "policy.initializing.components.error"
                    );
                    if (data.statusCode == 5001) {
                        message = $filter("translate")(
                            "policy.classification.complete.pending"
                        );
                    }
                    showSnackbar({
                        isSuccess: false,
                        messages: message
                    });
                }
            });
        };

        $scope.deployPolicy = function (id) {
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME + "/rs/policy/" + tokenGroupName + "/policies/deploy";
            var ids = [];
            ids.push(id);
            var params = {
                parameters: {
                    ids: ids
                }
            };
            $scope.isLoading = true;
            networkService.post(url, params, getJsonHeadersPolicy(bool), function (
                data
            ) {
                $scope.isLoading = false;
                if (data.statusCode === 200) {
                    isSuccess = true;
                    message = $filter("translate")("policy.deploy.success");
                    $scope.getPolicies();
                } else {
                    isSuccess = false;
                    message = $filter("translate")("policy.deploy.error");
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: message
                });
            });
        };

        $scope.undeployPolicy = function (id) {
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME +
                "/rs/policy/" +
                tokenGroupName +
                "/policies/undeploy";
            var ids = [];
            ids.push(id);
            var params = {
                parameters: {
                    ids: ids
                }
            };
            $scope.isLoading = true;
            networkService.post(url, params, getJsonHeadersPolicy(bool), function (
                data
            ) {
                $scope.isLoading = false;
                if (data.statusCode === 200) {
                    isSuccess = true;
                    message = $filter("translate")("policy.undeploy.success");
                    $scope.getPolicies();
                } else {
                    isSuccess = false;
                    message = $filter("translate")("policy.undeploy.error");
                }
                showSnackbar({
                    isSuccess: isSuccess,
                    messages: message
                });
            });
        };

        $scope.deletePolicy = function (id) {
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME + "/rs/policy/" + tokenGroupName + "/policies/" + id;
            $scope.isLoading = true;
            networkService.deleteRequest(
                url,
                null,
                getJsonHeadersPolicy(bool),
                function (data) {
                    $scope.isLoading = false;
                    if (data.statusCode === 200) {
                        isSuccess = true;
                        message = $filter("translate")("policy.delete.success");
                        $scope.getPolicies();
                    } else {
                        isSuccess = false;
                        message = $filter("translate")("policy.delete.error");
                    }
                    showSnackbar({
                        isSuccess: isSuccess,
                        messages: message
                    });
                }
            );
        };

        $scope.editPolicy = function (id) {
            if ($state.current.name == STATE_ADMIN_TENANT_POLICIES_LIST) {
                $state.go(STATE_ADMIN_TENANT_POLICIES_EDIT, {
                    policyId: id
                });
            } else if (
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_LIST
            ) {
                $state.go(STATE_TENANT_ADMIN_TENANT_POLICIES_EDIT, {
                    policy: id
                });
            } else if (
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST
            ) {
                $state.go(STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT, {
                    policy: id
                });
            } else {
                $state.go(STATE_PROJECT_POLICIES_EDIT, {
                    policyId: id
                });
            }

            $scope.resetForm();
            var tokenGroupName = getTokenGroupName();
            var url =
                RMS_CONTEXT_NAME + "/rs/policy/" + tokenGroupName + "/policies/models";
            $scope.isLoading = true;
            networkService.get(url, getJsonHeadersPolicy(bool), function (data) {
                if (data.statusCode === 200) {
                    $scope.policyModel = data.results;
                    var tokenGroupName = getTokenGroupName();
                    url =
                        RMS_CONTEXT_NAME +
                        "/rs/policy/" +
                        tokenGroupName +
                        "/policies/" +
                        id;
                    networkService.get(url, getJsonHeadersPolicy(bool), function (data) {
                        $scope.isLoading = false;
                        if (data.statusCode === 200) {
                            $rootScope.onResourceComponentsEvent = undefined;
                            $rootScope.onSubjectComponentsEvent = undefined;
                            $rootScope.onApplicationComponentsEvent = undefined;
                            $scope.policy = data.results.policy;
                            $scope.policyNameCopy = $scope.policy.name;
                            $scope.resourceComponentSets = $scope.policy.resources;
                            $scope.subjectComponentSets = $scope.policy.userComponents;
                            $scope.applicationComponentSets =
                                $scope.policy.applicationComponents;
                            $scope.advancedConditions = $scope.policy.advancedConditions;

                            $scope.policyValidity.from.val = "today";
                            $scope.policyValidity.to.val = "neverExpire";
                            if (!$scope.policy.scheduleConfig) {
                                $scope.effect_type = true;
                                scheduleConfig = null;
                            } else {
                                scheduleConfig = $scope.policy.scheduleConfig;
                                $scope.effect_type = false;

                                angular.forEach($scope.recurByDaysOptions, function (day) {
                                    day.checked = scheduleConfig[day.value];
                                });

                                if (scheduleConfig.startDateTime) {
                                    $scope.policyValidity.from.val = "specificDate";
                                    $scope.policyValidity.from.specificDate = true;
                                    $scope.policyValidity.from.today = false;
                                    $scope.policyValidity.from.date = moment(
                                        scheduleConfig.startDateTime,
                                        "MMM D, YYYY hh:mm:ss a"
                                    ).toDate();
                                }

                                if (scheduleConfig.endDateTime) {
                                    $scope.policyValidity.to.val = "specificDate";
                                    $scope.policyValidity.to.specificDate = true;
                                    $scope.policyValidity.to.neverExpire = false;
                                    $scope.policyValidity.to.date = moment(
                                        scheduleConfig.endDateTime,
                                        "MMM D, YYYY hh:mm:ss a"
                                    ).toDate();
                                }

                                if (scheduleConfig.recurrenceStartTime) {
                                    $scope.policyRecur.from = moment(
                                        scheduleConfig.recurrenceStartTime,
                                        "hh:mm:ss a"
                                    ).toDate();
                                }
                                if (scheduleConfig.recurrenceEndTime) {
                                    $scope.policyRecur.to = moment(
                                        scheduleConfig.recurrenceEndTime,
                                        "hh:mm:ss a"
                                    ).toDate();
                                }
                            }

                            if ($scope.policy.jsonABACMembershipObligation) {
                                $scope.jsonABACMembershipObligationSets =
                                    $scope.policy.jsonABACMembershipObligation[0];
                                $scope.projectTagsId =
                                    $scope.jsonABACMembershipObligationSets.tagIds;
                                $scope.$broadcast("projectTagsId", $scope.projectTagsId);
                            }

                            $scope.rights = $scope.policy.actions;
                            if (
                                ($scope.applicationComponentSets &&
                                    $scope.applicationComponentSets.length > 0) ||
                                $scope.advancedConditions
                            ) {
                                $scope.showAdvance = true;
                            }
                            $scope.$broadcast("policyModelLoaded", $scope.policyModel);
                        } else {
                            var message = $filter("translate")("policy.get.error");
                            showSnackbar({
                                isSuccess: false,
                                messages: message
                            });
                        }
                        $scope.getPolicies();
                    });
                } else {
                    $scope.isLoading = false;
                    var message = $filter("translate")(
                        "policy.initializing.components.error"
                    );
                    if (data.statusCode == 5001) {
                        message = $filter("translate")(
                            "policy.classification.complete.pending"
                        );
                    }
                    showSnackbar({
                        isSuccess: false,
                        messages: message
                    });
                }
            });
        };

        $scope.copyPolicy = function () { };

        $scope.closeModal = function () {
            $state.go(STATE_PROJECT_HOME);
        };

        $scope.clearPolicySearch = function () {
            $scope.policyFilter.searchPolicyString = "";
            $scope.getPolicies();
        };

        $scope.getPolicies = function () {
            var query = [];
            $scope.page = POLICY_PAGE;
            $scope.size = POLICY_SIZE;
            query.push("page=" + encodeURIComponent($scope.page));
            query.push("size=" + encodeURIComponent($scope.size));
            if ($scope.policyFilter.orderPolicyBy) {
                query.push(
                    "orderBy=" + encodeURIComponent($scope.policyFilter.orderPolicyBy)
                );
            }
            if ($scope.policyFilter.searchPolicyString) {
                query.push(
                    "searchString=" +
                    encodeURIComponent($scope.policyFilter.searchPolicyString)
                );
            }
            $rootScope.policies = [];
            $rootScope.isLoading = true;
            var tokenGroupName = getTokenGroupName();
            policyService.getPolicies(
                {
                    query: query.join("&"),
                    tokenGroupName: tokenGroupName
                },
                function (data) {
                    $rootScope.isLoading = false;
                    var message;
                    if (data.statusCode === 200) {
                        $rootScope.policies = data.results.policies;
                        $rootScope.totalPolicies = $scope.policies.length;
                        $scope.disableAddPolicy = !data.results.hasClassification;
                    } else if (data.statusCode === 5001) {
                        $scope.disableAddPolicy = true;
                    } else if (
                        $state.current.name != STATE_ADMIN_LANDING &&
                        $state.current.name != STATE_TENANT_ADMIN_LANDING
                    ) {
                        message = $filter("translate")("policy.list.error");
                    }
                    if (
                      $scope.disableAddPolicy &&
                      $state.current.name !=
                        STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST &&
                      $state.current.name !=
                        STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE &&
                      $state.current.name !=
                        STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT
                    ) {
                      if (
                        $state.current.name != STATE_ADMIN_LANDING &&
                        $state.current.name != STATE_TENANT_ADMIN_LANDING
                      ) {
                        message = $filter("translate")(
                          "policy.classification.complete.pending"
                        );
                      }
                    }
                    if (message) {
                      showSnackbar({
                        isSuccess: false,
                        messages: message
                      });
                    }
                }
            );
        };

        var calculateDateRange = function () {
            var message;
            scheduleConfig = {
                startDateTime: null,
                endDateTime: null,
                recurrenceDateOfMonth: -1,
                recurrenceDayInMonth: -1,
                sunday: false,
                monday: false,
                tuesday: false,
                wednesday: false,
                thursday: false,
                friday: false,
                saturday: false,
                recurrenceStartTime: "12:00:00 AM",
                recurrenceEndTime: "11:59:59 PM"
            };
            scheduleConfig.startDateTime = $filter("date")(
                $scope.policyValidity.from.date,
                "MMM d, yyyy hh:mm:ss a"
            );
            scheduleConfig.endDateTime = $filter("date")(
                $scope.policyValidity.to.date,
                "MMM d, yyyy hh:mm:ss a"
            );
            scheduleConfig.recurrenceStartTime = $filter("date")(
                $scope.policyRecur.from,
                "hh:mm:ss a"
            );
            scheduleConfig.recurrenceEndTime = $filter("date")(
                $scope.policyRecur.to,
                "hh:mm:ss a"
            );
            //validate date and time
            var startDate = new Date(scheduleConfig.startDateTime);
            startDate.setHours(0, 0, 0, 0);
            var endDate = new Date(scheduleConfig.endDateTime);
            endDate.setHours(0, 0, 0, 0);

            var beginningTime = moment(
                scheduleConfig.recurrenceStartTime,
                "hh:mm:ss a"
            );
            var endTime = moment(
                scheduleConfig.recurrenceEndTime,
                "hh:mm:ss a"
            );
            if (!beginningTime.isValid() || !endTime.isValid() || endTime.isBefore(beginningTime)) {
                message = $filter("translate")(
                    "policy.effective.duration.time.error"
                );
            }

            var recurOnDays = $filter("filter")($scope.recurByDaysOptions, {
                checked: true
            });
            angular.forEach(recurOnDays, function (day) {
                scheduleConfig[day.value] = true;
            });

            return message;
        };

        $scope.policyEffect = function (effectType) {
            if (!effectType) {
                $scope.effect_type = false;
                var today = new Date();
                today.setHours(0);
                today.setMinutes(0);
                today.setSeconds(0);
                $scope.policyValidity.from.date = today;
                var lastDateOfThisCentury = new Date();
                lastDateOfThisCentury.setFullYear(2099);
                lastDateOfThisCentury.setMonth(11);
                lastDateOfThisCentury.setDate(31);
                lastDateOfThisCentury.setHours(23);
                lastDateOfThisCentury.setMinutes(59);
                lastDateOfThisCentury.setSeconds(59);
                $scope.policyValidity.to.date = lastDateOfThisCentury;
                calculateDateRange();
            } else {
                $scope.effect_type = true;
            }
        };

        $scope.init = function () {
            $scope.policyFilter.orderPolicyBy =
                $rootScope.sortPolicyOptions[0].lookupCode;
            if ($stateParams.projectId) {
                projectStateService.setProjectId($stateParams.projectId);
            }

            if ($stateParams.tenantId) {
                projectStateService.setTenantId($stateParams.tenantId);
            }

            if ($stateParams.policyId) {
                $scope.editPolicy($stateParams.policyId);
                return;
            }

            if (
                $state.current.name == STATE_PROJECT_POLICIES_LIST ||
                $state.current.name == STATE_ADMIN_TENANT_POLICIES_LIST ||
                $state.current.name == STATE_ADMIN_LANDING ||
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_LIST ||
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_LIST
            ) {
                if ($state.current.name == STATE_ADMIN_LANDING) {
                    projectStateService.setTenantId(readCookie("ltId"));
                }
                $scope.getPolicies();
            } else if (
                $state.current.name == STATE_PROJECT_POLICIES_CREATE ||
                $state.current.name == STATE_ADMIN_TENANT_POLICIES_CREATE ||
                $state.current.name == STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE ||
                $state.current.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE
            ) {
                $scope.resetForm();
                $scope.getDefaultModels();
            }
        };

        $scope.init();
        $scope.contentRightsAvailable = dialogService.getContentRights();
        $scope.collaborationRightsAvailable = dialogService.getCollaborationRights();
        $scope.effectRightsAvailable = dialogService.getEffectRights();
        $scope.hiddenSectionRights = dialogService.getHiddenSectionRights();
        $scope.showOptions = true;
        $scope.hideValidity = true;

        $scope.$on("$stateChangeStart", function (
            event,
            toState,
            toParams,
            fromState,
            fromParams
        ) {
            if ($scope.skipReminder) {
                $scope.skipReminder = false;
                return;
            }
            if (
                fromState.name == STATE_PROJECT_POLICIES_CREATE ||
                fromState.name == STATE_PROJECT_POLICIES_EDIT ||
                fromState.name == STATE_ADMIN_TENANT_POLICIES_CREATE ||
                fromState.name == STATE_ADMIN_TENANT_POLICIES_EDIT ||
                fromState.name == STATE_TENANT_ADMIN_TENANT_POLICIES_CREATE ||
                fromState.name == STATE_TENANT_ADMIN_TENANT_POLICIES_EDIT ||
                fromState.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_CREATE ||
                fromState.name == STATE_ADMIN_TENANT_MANAGE_PROJECT_POLICIES_EDIT
            ) {
                var answer = confirm(
                    $filter("translate")("policy.leave.page.reminder")
                );
                if (!answer) {
                    event.preventDefault();
                    navService.setCurrentTab("policies");
                } else {
                    $scope.policyModel = {};
                }
            }
        });
    }
]);

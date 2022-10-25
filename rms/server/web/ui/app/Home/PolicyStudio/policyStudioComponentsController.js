mainApp.controller('policyStudioComponentsController', ['$scope', '$filter', '$timeout', '$controller', '$rootScope',
  function ($scope, $filter, $timeout, $controller, $rootScope) {

  $scope.policyModelLoaded = false;
  $scope.$on('policyModelLoaded', function (event, policyModel) {
    if ($scope.type === 'SUBJECT') {
      $scope.attributes = policyModel.subject.attributes;
    } else if ($scope.type === 'RESOURCE') {
      $scope.attributes = policyModel.resource.attributes;
    }
    $scope.policyModelLoaded = true;
    initializeComponents();
  });

  $scope.changeOptions = function (condition) {
    condition.value = ($scope.type == 'RESOURCE') ? '' : [];
  }

  $scope.getAttributes = function () {
    if ($scope.type === 'APPLICATION') {
      return [{
        '$$hashKey': 'object:001',
        'id': 0,
        'name': "Path",
        'shortName': 'path',
        'dataType': 'STRING',
        'operatorConfigs': defaultOperatorConfigs
      }];
    } else {
      if ($scope.type === 'SUBJECT') {
        return $scope.$parent.$parent.policyModel.subject.attributes;
      } else if ($scope.type === 'RESOURCE') {
        return $scope.$parent.$parent.policyModel.resource.attributes;
      }
    }
  }
  var defaultOperatorConfigs = [
    {
      'dataType': 'STRING',
      'id': 1,
      'key': '=',
      'label': 'is'
    }, {
      'dataType': 'STRING',
      'id': 2,
      'key': '!=',
      'label': 'is not'
    }, {
      'dataType': 'STRING',
      'id': 3,
      'key': '<',
      'label': '<'
    }, {
      'dataType': 'STRING',
      'id': 4,
      'key': '<=',
      'label': '<='
    }, {
      'dataType': 'STRING',
      'id': 5,
      'key': '>',
      'label': '>'
    }, {
      'dataType': 'STRING',
      'id': 6,
      'key': '>=',
      'label': '>='
    }];

  $scope.getComponentType = function () {
    if ($scope.type === 'RESOURCE') {
      return $filter('translate')('policy.resources');
    } else if ($scope.type === 'SUBJECT') {
      return $filter('translate')('policy.user');
    } else if ($scope.type === 'APPLICATION') {
      return $filter('translate')('policy.application');
    }
  }

  $scope.getComponentDescription = function () {
    if ($scope.type === 'RESOURCE') {
      return $filter('translate')('policy.resources.description');
    } else if ($scope.type === 'SUBJECT') {
      return $filter('translate')('policy.user.description');
    } else if ($scope.type === 'APPLICATION') {
      return $filter('translate')('policy.application.description');
    }
  }

  var getEmptyCondition = function () {
    return {
      id: null,
      key: '',
      attr: '',
      value: ($scope.type == 'RESOURCE') ? '' : [],
    }
  }

  var convertComponentToConditions = function (component) {
    var conditions = [];
    for (var i = 0; i < component.conditions.length; i++) {
      var attributesList = $scope.getAttributes();
      for (var j = 0; j < attributesList.length; j++) {
        if (component.conditions[i].attribute === attributesList[j].name || component.conditions[i].attribute === attributesList[j].shortName || component.conditions[i].attribute.toLowerCase() == attributesList[j].name.toLowerCase()) {
          var condition = {};
          condition.attribute = angular.copy(attributesList[j]);
          condition.operator =  getOperatorConfig(component.conditions[i].operator);
          condition.$$hashKey = 100 + i;
          condition.id = null;
          condition.value = component.conditions[i].value;
          condition.combiner = component.conditions[i].combiner;
          condition.uniqueInputId = component.conditions[i].uniqueInputId;
          conditions.push(condition);
          break;
        }
      }
    }
    return conditions;
  }

  var getOperatorConfig = function (operator) {
    for (var i = 0; i < defaultOperatorConfigs.length; i ++) {
      if (defaultOperatorConfigs[i].key === operator) {
        return defaultOperatorConfigs[i];
      }
    }
  }

  $scope.initComponents = function (componentType) {
    $scope.type = componentType;
    if ($scope.policyModelLoaded) {
      initializeComponents();
    }
  }

  var initializeComponents = function () {
    if ($scope.type === 'RESOURCE') {
      $scope.policy.resources = $scope.policy.resources ? $scope.policy.resources : [];
      $scope.componentSets = $scope.policy.resources;
    } else if ($scope.type === 'SUBJECT') {
      $scope.policy.userComponents = $scope.policy.userComponents ? $scope.policy.userComponents : [];
      $scope.componentSets = $scope.policy.userComponents;
    } else if ($scope.type === 'APPLICATION') {
      $scope.policy.applicationComponents = $scope.policy.applicationComponents ? $scope.policy.applicationComponents : [];
      $scope.componentSets = $scope.policy.applicationComponents;
    }
    for (var i = 0; i < $scope.componentSets.length; i++) {
        if ("in" === $scope.componentSets[i].operator.toLowerCase()) {
           $scope.componentSets[i].operator = {text: "in", value: "IN"};
        } else if ("not" === $scope.componentSets[i].operator.toLowerCase()) {
           $scope.componentSets[i].operator = {text: "not in", value: "NOT"};
        }
        for (var j = 0; j < $scope.componentSets[i].components.length; j++) {
          var conditions = convertComponentToConditions($scope.componentSets[i].components[j]);
          $scope.componentSets[i].components[j].conditions = conditions;
        }
    }
    if($scope.policy.id === undefined && $scope.type !== 'APPLICATION') {
        $scope.addComponentSet();
    }
  }

  $scope.initConditions = function (condition) {
    if (!condition.attribute) {
      condition.attribute = $scope.getAttributes()[0];
    }
  }

  $scope.initOperator = function (condition) {
    if (!condition.operator) {
      condition.operator = condition.attribute.operatorConfigs[0];
    }
  }

  var notifyOnConditionTimer = null;
  var setNotification = function (table) {
    table.notify = true;
    notifyOnConditionTimer && $timeout.cancel(notifyOnConditionTimer);
    notifyOnConditionTimer = $timeout(function () {
      table.notify = false;
    }, 3000);
  }

  var resetWithVal = function (obj, key, val) {
    switch (typeof (val)) {
      case 'object':
        !obj[key] && (obj[key] = {});
        angular.forEach(val, function (subVal, subKey) {
          resetWithVal(obj[key], subKey, subVal);
        })
        break;
      default:
        obj[key] = val;
        break;
    }
  }

  var getEmptyComponentSet = function () {
    return {
      "operator": $scope.componentOperators[0],
      "components": []
    }
  }

  var getEmptyComponent = function () {
    return {
      id: null,
      name: '',
      description: '',
      tags: [],
      type: '',
      conditions: [],
      policyModel: {}
    }
  }

  $scope.addComponentSet = function () {
    if ($scope.attributes && $scope.attributes.length == 0) {
      return;
    }
    var componentSet = getEmptyComponentSet();
    var component = getEmptyComponent();
    $scope.addCondition(component);
    $scope.addComponent(componentSet);
    $scope.componentSets.push(componentSet);
  }

  $scope.addComponent = function (componentSet) {
    var component = getEmptyComponent();
    $scope.addCondition(component);
    componentSet.components.push(component);
  }

  $scope.addCondition = function (component) {
    var condition = getEmptyCondition();
    component.conditions.push(condition);
  }

  $scope.removeCondition = function (con, component, componentSet, conditionIndex, componentIndex, componentSets) {
    component.conditions.splice(conditionIndex, 1);
    if (component.conditions.length == 0) {
      $scope.removeComponent(component, componentSet, componentIndex, componentSets);
    }
  }

  $scope.removeComponent = function (component, componentSet, componentIndex, componentSets) {
    componentSet.components.splice(componentIndex, 1);
  }

  $scope.removeComponentSet = function (componentSet, componentSets, componentSetIndex) {
    componentSets.splice(componentSetIndex, 1);
  }

  var validateCondition = function (condition) {
    if (!condition.value)
      return $filter('translate')('component.condition.value.validation.required');
  }

}]);
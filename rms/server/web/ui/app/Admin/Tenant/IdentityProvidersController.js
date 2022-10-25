mainApp.controller('IdentityProvidersController', ['$scope', 'IdentityProvidersService', 'dialogService', '$filter',
	function($scope, IdentityProvidersService, dialogService, $filter){
	var getIDPs = function () {
		IdentityProvidersService.getIDPDetails(function(data) {
			if(data.statusCode == 200) {
				$scope.identityProviderSettingsMap = [];
				$scope.idps = data.results.idps;
				$scope.totalIdp = $scope.idps.length;
				$scope.idps.forEach(function(item){
					var details = JSON.parse(item.attributes);
					if(item.type == 6) {
						var providerType = "AZUREAD";
						var name = "Azure";
						var providerTypeDisplayName = "Azure AD";
					} else if(item.type == 4) {
						var providerType = "LDAP";
						var name = "LDAP";
						var providerTypeDisplayName = "LDAP - " + details.name;
					} else if(item.type == 3) {
						var providerType = "FACEBOOK";
						var name = "Facebook";
						var providerTypeDisplayName = "Facebook";
					} else if(item.type == 2) {
						var providerType = "GOOGLE";
						var name = "Google";
						var providerTypeDisplayName = "Google";
					} else if(item.type == 1) {
						var providerType = "SAML";
						var name = "SAML";
						var providerTypeDisplayName = "SAML - " + details.name;
					}
					$scope.identityProviderSettingsMap.push({
						"providerType": providerType,
						"providerTypeDisplayName": providerTypeDisplayName,
						"id": item.id,
						"attributes": details,
						"type": item.type,
						"name": name
					})
				});
			}
		});
	};

	var getAllowedIDPs = function () {
		IdentityProvidersService.getAllowedIDPs(function (data) {
			if (data.statusCode == 200) {
				$scope.idps = data.results.IDPs;
				$scope.allowedIDPMap = [];
				$scope.idps.forEach(function(item){
					switch (item.type) {
						case 1:
							$scope.allowedIDPMap.push({type: 1, name: "SAML", count: item.number});
							break;
						case 2:
							$scope.allowedIDPMap.push({type: 2, name: "Google"});
							break;
						case 3:
							$scope.allowedIDPMap.push({type: 3, name: "Facebook"});
							break;
						case 4:
							$scope.allowedIDPMap.push({type: 4, name: "LDAP", count: item.number});
							break;
						case 6:
							$scope.allowedIDPMap.push({type: 6, name: "Azure"});
							break;
						default:
							break;
					}
				});
			}
		});
	};

	var init = function() {
		getIDPs();
		getAllowedIDPs();
	}

	init();

	$scope.showAddIDPDialog = function () {
		var parameter = {};
		parameter.allowedIDPs = $scope.allowedIDPMap;
		dialogService.addIDPDialog(parameter, init);
	}

	$scope.showEditIDPDialog = function (setting) {
		var parameter = {};
		parameter.selectedIDP = setting.attributes;
		parameter.id = setting.id;
		parameter.type = setting.type;
		parameter.name = setting.name;
		dialogService.addIDPDialog(parameter, getIDPs);
	}

	$scope.deleteIDP = function (setting) {
		dialogService.confirm({
			msg: $filter('translate')('idp.delete.confirm'),
			ok: function() {
				IdentityProvidersService.deleteAnIDP(setting.id, function(data) {
					if (data.statusCode === 200) {
						showSnackbar({
							isSuccess: true,
							messages: $filter('translate')('idp.delete.success')
						});
						init();
					} else if (data.statusCode == 4001) {
						showSnackbar({
							isSuccess: false,
							messages: data.message
						});
					} else {
						showSnackbar({
							isSuccess: false,
							messages: $filter('translate')('idp.delete.error')
						});
					}
				});
			},
			cancel: function() {}
		});
	}
}]);
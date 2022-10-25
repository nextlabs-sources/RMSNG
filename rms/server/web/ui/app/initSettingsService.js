mainApp.factory('initSettingsService', ['$http', 'networkService', '$filter', '$rootScope', function($http, networkService, $filter, $rootScope) {
    var initSettings = null;
    var RMSContextName = null;
    var onlineHelpPage = null;
    var clientSettings = {};

    var isDefaultRepo = function(data) {
        return initSettings.inbuiltServiceProvider === data.repoType && data.repoName === $filter('translate')('MyDrive');
    }

    var reloadScopeSettings = function(controllerScope) {        
        $rootScope.isSystemAdmin = controllerScope.isSystemAdmin = checkRoles(initSettings.roles,'SYSTEM_ADMIN');
        $rootScope.isTenantAdmin = controllerScope.isTenantAdmin = checkRoles(initSettings.roles,'TENANT_ADMIN');
        $rootScope.isProjectAdmin = controllerScope.isProjectAdmin = checkRoles(initSettings.roles,'PROJECT_ADMIN');
        $rootScope.saasMode = controllerScope.saasMode = initSettings.isSaasMode;
        $rootScope.hideWorkspace = controllerScope.hideWorkspace = initSettings.isHideWorkspace;
    }

    var reloadSettings = function(successCallback, errorCallback) {
        networkService.get(RMSContextName + "/RMSViewer/GetInitSettings", null, function(data) {
            if (data) {
                initSettings = data;
                successCallback();
            } else {
                errorCallback();
            }
        });
    }

    var loadSettings = function(data) {
        initSettings = data;
        clientSettings["isWin64"] = navigator.userAgent.indexOf("WOW64") != -1 || navigator.userAgent.indexOf("Win64") != -1;
        clientSettings["isRMDWin32Downloadable"] = data.rmDownloadUrls.rmdWin32Url && data.rmDownloadUrls.rmdWin32Url != "";
        clientSettings["isRMDWin64Downloadable"] = data.rmDownloadUrls.rmdWin64Url && data.rmDownloadUrls.rmdWin64Url != "";
        clientSettings["isRMDMacDownloadable"] = data.rmDownloadUrls.rmdMacUrl && data.rmDownloadUrls.rmdMacUrl != "";
        clientSettings["isRMCiOSDownloadable"] = data.rmDownloadUrls.rmciOSURL && data.rmDownloadUrls.rmciOSURL != "";
        clientSettings["isRMCAndroidDownloadable"] = data.rmDownloadUrls.rmcAndroidURL && data.rmDownloadUrls.rmcAndroidURL != "";

        clientSettings["isRMDDownloadable"] = !jscd.mobile && (jscd.os == "Windows" && (clientSettings["isRMDWin32Downloadable"] || clientSettings["isRMDWin64Downloadable"]) || (jscd.os == "Mac OS X" && clientSettings["isRMDMacDownloadable"]));
        clientSettings["isRMCDownloadable"] = jscd.mobile && (jscd.os == "iOS" && clientSettings["isRMCiOSDownloadable"] || jscd.os == "Android" && clientSettings["isRMCAndroidDownloadable"]);
        clientSettings["os"] = jscd.os;

    };

    var getSettings = function() {
        return initSettings;
    };

    var getClientSettings = function() {
        return clientSettings;
    };

    var setRMSContextName = function(data) {
        RMSContextName = data;
    }

    var getRMSContextName = function() {
        return RMSContextName;
    }

    var getTimeOutUrl = function() {
        return RMSContextName + "/timeout";
    }

    var setRightPanelMinHeight = function() {
        var containerHeight = (jscd.mobile ? Math.max(document.body.offsetHeight, document.body.offsetWidth) : document.body.offsetHeight) - $('#nextlabs-header').height();
        var bannerHeight = $("#banner").height();
        var footerHeight = getFooterHeight();
        $("#rms-inner-container").css({
            height: containerHeight - bannerHeight,
            overflow: "auto"
        });
        $("#rms-right-panel").css("min-height", containerHeight - footerHeight - bannerHeight);
    }

    var getFooterHeight = function() {
        if ($("#rms-home-footer-desktop").height() != null && $("#rms-home-footer-desktop").height() != 0) {
            return $("#rms-home-footer-desktop").height();
        }
        if ($("#rms-home-footer-mobile").height() != null && $("#rms-home-footer-mobile").height() != 0) {
            return $("#rms-home-footer-mobile").height();
        }
        return 0;
    }

    var logout = function() {
        var lt = readCookie('lt');
        var loginTenant = lt != undefined ? lt : PUBLIC_TENANT;
        var headers = {
            'Content-Type': 'application/json',
            'userId': window.readCookie("userId"),
            'ticket': window.readCookie("ticket"),
            'clientId': window.readCookie("clientId"),
            'platformId': window.readCookie("platformId")
        };
        if (!String.prototype.includes) {
            Object.defineProperty(String.prototype, 'includes', {
                value: function(search, start) {
                    if (typeof start !== 'number') {
                        start = 0
                    }

                    if (start + search.length > this.length) {
                        return false
                    } else {
                        return this.indexOf(search, start) !== -1
                    }
                }
            })
        }
        networkService.get(RMSContextName + "/rs/usr/logout", headers, function(data) {
            var redirectUrl = (APPNAME.includes("admin")) ? RMSContextName + "/loginAdmin" : RMSContextName + "/login";
            document.location.href = loginTenant === PUBLIC_TENANT ? redirectUrl : redirectUrl + "?tenant=" + loginTenant;
        });
    }

    var downloadRMC = function() {
        switch (clientSettings.os) {
            case "iOS":
                url = initSettings.rmDownloadUrls.rmciOSURL;
                break;
            case "Android":
                url = initSettings.rmDownloadUrls.rmcAndroidURL;
                break;
        }
        window.open(url, 'Downloading SkyDRM App', '');
    }

    var downloadRMD = function(type) {
        switch (type) {
            case "win32":
                url = initSettings.rmDownloadUrls.rmdWin32Url;
                break;
            case "win64":
                url = initSettings.rmDownloadUrls.rmdWin64Url;
                break;
            case "mac":
                url = initSettings.rmDownloadUrls.rmdMacUrl;
                break;
        }
        window.open(url, 'Downloading SkyDRM', '');
    }

    return {
        loadSettings: loadSettings,
        getSettings: getSettings,
        reloadSettings: reloadSettings,
        reloadScopeSettings: reloadScopeSettings,
        getClientSettings: getClientSettings,
        setRMSContextName: setRMSContextName,
        getRMSContextName: getRMSContextName,
        setRightPanelMinHeight: setRightPanelMinHeight,
        isDefaultRepo: isDefaultRepo,
        logout: logout,
        downloadRMC: downloadRMC,
        downloadRMD: downloadRMD,
        getFooterHeight: getFooterHeight
    };
}]);
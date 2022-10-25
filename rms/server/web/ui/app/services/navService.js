mainApp.factory('navService', function() {
    var currentTab = 0;
    var getCurrentTab = function() {
        return currentTab;
    };

    var setCurrentTab = function(value) {
        currentTab = value;
    };

    var inAll = true;

    var getIsInAllFilesPage = function() {
        return inAll;
    };

    var setIsInAllFilesPage = function(value) {
        inAll = value;
    };

    var collapseStatus = false;

    var getCollapseStatus = function() {
        return collapseStatus;
    };

    var setCollapseStatus = function(value) {
        collapseStatus = value;
    };

    return {
        getCurrentTab: getCurrentTab,
        setCurrentTab: setCurrentTab,
        getIsInAllFilesPage: getIsInAllFilesPage,
        setIsInAllFilesPage: setIsInAllFilesPage,
        getCollapseStatus: getCollapseStatus,
        setCollapseStatus: setCollapseStatus,
    }
});
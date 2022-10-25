mainApp.factory('projectStateService', [function() {

    var projectId;
    var fromCreateProject;
    var tenantId;
    var tokenGroupName;

    var getProjectId = function() {
        return projectId;
    }

    var setProjectId = function(id) {
        projectId = id;
    }

    var setFromCreateProject = function(fromCreate){
        fromCreateProject = fromCreate;
    }

    var isFromCreateProject = function(){
        return fromCreateProject;
    }

    var getTenantId = function() {
        return tenantId;
    }

    var setTenantId = function(id) {
        tenantId = id;
    }

    var getTokenGroupName = function () {
        return tokenGroupName;
    }

    var setTokenGroupName = function (name) {
        tokenGroupName = name;
    }

    return {
        getProjectId: getProjectId,
        setProjectId: setProjectId,
        setFromCreateProject: setFromCreateProject,
        isFromCreateProject: isFromCreateProject,
        getTenantId: getTenantId,
        setTenantId: setTenantId,
        getTokenGroupName: getTokenGroupName,
        setTokenGroupName: setTokenGroupName
    }
}]);
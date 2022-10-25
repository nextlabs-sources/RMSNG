mainApp.factory('landingPageService', ['$http', '$rootScope', '$timeout',
    function($http, $rootScope, $timeout) {
        var afterBindingsQueue = [];
        var workSpaceStatus;
        var workSpaceTotalFiles;
        var afterBindings = function(callback, initialWait) {
            var CHECK_TICK = 100,
                watchersCount = -1;

            afterBindingsQueue.push(callback);

            function waitOnInit() {
                var initEnds = true;

                //check for new watchers
                initEnds = initEnds && ($rootScope.$$watchersCount == watchersCount);
                watchersCount = $rootScope.$$watchersCount;

                //check for pending requests
                initEnds = initEnds && $http.pendingRequests.length == 0;

                if (initEnds) {
                    //execute first callback from queue
                    $timeout(function() {
                        afterBindingsQueue.shift()();
                    });
                } else {
                    setTimeout(function() {
                        waitOnInit();
                    }, CHECK_TICK);
                }
            }

            if (typeof initialWait != 'undefined' && initialWait > 0) {
                setTimeout(function() {
                    waitOnInit();
                }, initialWait);
            } else {
                waitOnInit();
            }
        };

        var getWorkSpaceStatus = function() {
            return workSpaceStatus;
        }

        var setWorkSpaceStatus = function(status) {
            workSpaceStatus = status;
        }

        return {
            afterBindings: afterBindings,
            getWorkSpaceStatus: getWorkSpaceStatus,
            setWorkSpaceStatus: setWorkSpaceStatus
        }
    }
]);
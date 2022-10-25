mainApp.factory('digitalRightsExpiryService',[ '$rootScope', '$filter', function($rootScope, $filter){

    var addExpiryInfo = function(startDate, endDate) {
        var fullDateFormat = "dddd, mmmm d, yyyy";
        if(startDate == undefined && endDate == undefined) {
            $rootScope.expiration = $filter('translate')('never.expire');
        } else if(startDate == undefined && endDate != undefined) {
            $rootScope.expiration = $filter('translate')('until') + dateFormat(new Date(endDate), fullDateFormat);
        } else if(startDate != undefined && endDate == undefined) {
            $rootScope.expiration = $filter('translate')('from') + dateFormat(new Date(startDate), fullDateFormat);
        } else {
            $rootScope.expiration = dateFormat(new Date(startDate), fullDateFormat) + " - " + dateFormat(new Date(endDate), fullDateFormat);
        }
    }

    var getExpiryStr = function (expiryJson) {
        if (expiryJson != null) {
            if (expiryJson.option === 0) {
                return $filter('translate')('access.rights.will') + $filter('translate')('never.expire');
            } else if (expiryJson.option === 1) {
                if(!expiryJson.relativeDay.year) {
                   expiryJson.relativeDay.year = 0; 
                }
                if(!expiryJson.relativeDay.month) {
                   expiryJson.relativeDay.month = 0; 
                }
                if(!expiryJson.relativeDay.week) {
                   expiryJson.relativeDay.week = 0; 
                }
                if(!expiryJson.relativeDay.day) {
                   expiryJson.relativeDay.day = 0; 
                }

                var minDateMilliSec =  new Date().setHours(0,0,0,0);
                var finalRelativeDateMilliSec = calculateRelativeEndDate(expiryJson.relativeDay.year, expiryJson.relativeDay.month, expiryJson.relativeDay.week, expiryJson.relativeDay.day);
                finalRelativeDateMilliSec = new Date(finalRelativeDateMilliSec);
                return $filter('date')(minDateMilliSec, 'longDate') + ' ' + $filter('translate')('to') + ' ' + $filter('date')(new Date(finalRelativeDateMilliSec), 'longDate');
            } else if (expiryJson.option === 2) {
                return  $filter('translate')('until') + ' ' + $filter('date')(new Date(expiryJson.endDate).setHours(0,0,0,0), 'longDate');
            } else if (expiryJson.option === 3) {
                return $filter('date')(new Date(expiryJson.startDate), 'longDate') + ' ' + $filter('translate')('to') + ' ' + $filter('date')(new Date(expiryJson.endDate), 'longDate');
            }
        }
        return '';
    }

    var calculateRelativeEndDate = function(year, month, week, day, base) {
        base = base ? JSJoda.LocalDate.ofInstant(JSJoda.Instant.ofEpochMilli(base)) : JSJoda.LocalDate.now();
        var jodaTime = base.plusDays(day).plusMonths(month).plusWeeks(week).plusYears(year);
        var endDate = new Date(jodaTime._year, jodaTime._month-1, jodaTime._day).getTime();
        return endDate - 1;
    }

    return {
        addExpiryInfo: addExpiryInfo,
        getExpiryStr: getExpiryStr,
        calculateRelativeEndDate: calculateRelativeEndDate
    }
}]);
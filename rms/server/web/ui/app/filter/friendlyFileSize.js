mainApp.filter('filesize', function() {
    var units = [
        'bytes',
        'KB',
        'MB',
        'GB',
        'TB',
        'PB',
        'EB',
        'ZB',
        'YB'
    ];

    return function(bytes, precision) {
        if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
            return '-';
        }
        if (typeof precision === 'undefined') {
            precision = bytes === 0 ? 0 : 1;
        }
        var unit = 0;

        while (bytes >= 1024) {
            bytes /= 1024;
            unit++;
        }
        if (units[unit] == 'KB') {
            return bytes.toFixed() + ' ' + units[unit];
        }
        return bytes.toFixed(+precision) + ' ' + units[unit];
    };
});
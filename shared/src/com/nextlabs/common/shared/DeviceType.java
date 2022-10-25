package com.nextlabs.common.shared;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public enum DeviceType {

    WINDOWS_DESKTOP(0, 99, "Windows Desktop"),
    WINDOWS_SERVER(100, 199, "Windows Server"),
    LINUX_UNIX(200, 299, "Linux/Unix"),
    MAC_OS(300, 399, "Mac OS"),
    WINDOWS_PHONE(400, 499, "Windows Phone"),
    WINDOWS_TABLET(500, 599, "Windows Tablet"),
    IPHONE(600, 699, "iPhone"),
    IPAD(700, 799, "iPad"),
    ANDROID_PHONE(800, 899, "Android Phone"),
    ANDROID_TABLET(900, 999, "Android Tablet"),
    WEB(1000, 1999, "WebApp"),
    RMX(2000, 2999, "RMX");

    private static NavigableMap<Integer, DeviceType> map = new TreeMap<Integer, DeviceType>();
    private int low;
    private int high;
    private String displayName;

    private DeviceType(int low, int high, String displayName) {
        this.low = low;
        this.high = high;
        this.displayName = displayName;
    }

    static {
        map.put(0, WINDOWS_DESKTOP);
        map.put(100, WINDOWS_SERVER);
        map.put(200, LINUX_UNIX);
        map.put(300, MAC_OS);
        map.put(400, WINDOWS_PHONE);
        map.put(500, WINDOWS_TABLET);
        map.put(600, IPHONE);
        map.put(700, IPAD);
        map.put(800, ANDROID_PHONE);
        map.put(900, ANDROID_TABLET);
        map.put(1000, WEB);
        map.put(2000, RMX);
    }

    /**
     *
     * @param value
     * @return
     */
    public static DeviceType getDeviceType(int value) {
        Map.Entry<Integer, DeviceType> entry = map.floorEntry(value);
        if (entry == null) {
            return null;
        } else if (value <= entry.getValue().high) {
            return entry.getValue();
        } else {
            return null;
        }
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isIOS() {
        return this == DeviceType.IPHONE || this == DeviceType.IPAD;
    }

    public boolean isAndroid() {
        return this == DeviceType.ANDROID_PHONE || this == DeviceType.ANDROID_TABLET;
    }
}

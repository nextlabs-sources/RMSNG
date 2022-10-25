package com.nextlabs.rms.cc.pojos;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Control Center version.
 *
 * @author Mohammed Sainal Shah
 */
public class ControlCenterVersion implements Comparable<ControlCenterVersion> {

    public static final String VERSION_DELIMITER = "\\.";
    public static final ControlCenterVersion V_2021_03 = new ControlCenterVersion("2021.03");

    private String ccVersion;

    public ControlCenterVersion() {
    }

    public ControlCenterVersion(String ccVersion) {
        // The version can include the build number which is separated from the version using "-".
        this.ccVersion = ccVersion.split("-")[0].trim();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ccVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ControlCenterVersion version = (ControlCenterVersion)o;
        return compareTo(version) == 0;
    }

    @Override
    public String toString() {
        return ccVersion;
    }

    @Override
    public int compareTo(ControlCenterVersion o) {
        String[] thisVersion = this.ccVersion.split(VERSION_DELIMITER);
        String[] otherVersion = o.ccVersion.split(VERSION_DELIMITER);
        for (int i = 0; i < Math.max(thisVersion.length, otherVersion.length); i++) {
            String thisVersionComponent = i < thisVersion.length ? thisVersion[i] : "0";
            String otherVersionComponent = i < otherVersion.length ? otherVersion[i] : "0";
            int result = new BigInteger(thisVersionComponent).compareTo(new BigInteger(otherVersionComponent));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    public boolean before(ControlCenterVersion version) {
        return compareTo(version) < 0;
    }

    public String getCcVersion() {
        return ccVersion;
    }

    public void setCcVersion(String ccVersion) {
        // The version can include the build number which is separated from the version using "-".
        this.ccVersion = ccVersion.split("-")[0].trim();
    }
}

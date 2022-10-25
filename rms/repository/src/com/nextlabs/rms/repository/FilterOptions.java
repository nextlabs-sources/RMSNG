package com.nextlabs.rms.repository;

import com.nextlabs.common.util.StringUtils;

import java.util.List;

public class FilterOptions {

    private boolean hideNxl;
    private boolean hideNonNxl;

    public FilterOptions() {
        this.hideNxl = false;
        this.hideNonNxl = false;
    }

    public FilterOptions(String filterList) {
        this.hideNxl = false;
        this.hideNonNxl = false;
        if (StringUtils.hasText(filterList)) {
            List<String> list = StringUtils.tokenize(filterList, ",");
            for (String field : list) {
                if (StringUtils.equalsIgnoreCase(field, "nxl")) {
                    hideNxl = true;
                } else if (StringUtils.equalsIgnoreCase(field, "nonNxl")) {
                    hideNonNxl = true;
                }
            }
        }
    }

    public FilterOptions(boolean hideNxl, boolean hideNonNxl) {
        this.hideNxl = hideNxl;
        this.hideNonNxl = hideNonNxl;
    }

    public boolean showOnlyFolders() {
        return hideNxl && hideNonNxl;
    }

    public boolean hideNxl() {
        return hideNxl;
    }

    public boolean hideNonNxl() {
        return hideNonNxl;
    }

}

package com.nextlabs.rms.eval;

import com.nextlabs.common.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {

    private Map<String, List<String>> attributes = new HashMap<String, List<String>>();
    private String ipAddress;
    private String hostname;

    public Host(String ipAddress, String hostname) {
        if (ipAddress == null && hostname == null) {
            throw new IllegalArgumentException("both ipAddress and hostname are null");
        }
        this.ipAddress = ipAddress;
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getIpAddress() throws PolicyEvalException {
        if (!StringUtils.hasText(ipAddress)) {
            return 0;
        }
        return CentralPoliciesEvaluationHandler.ipAddressToInteger(ipAddress);
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}

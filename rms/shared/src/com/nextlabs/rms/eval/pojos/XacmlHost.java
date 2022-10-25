package com.nextlabs.rms.eval.pojos;

import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.Host;
import com.nextlabs.rms.eval.PolicyEvalException;

public final class XacmlHost extends XacmlCategory {

    public static final String XACML_INET_ADDR_KEY = "urn:nextlabs:names:evalsvc:1.0:host:inet_address";
    public static final String XACML_HOSTNAME_KEY = "urn:nextlabs:names:evalsvc:1.0:host:name";

    private XacmlHost() {
    }

    public static XacmlHost getXacmlHost(Host host) throws PolicyEvalException {
        XacmlHost xacmlHost = new XacmlHost();
        if (host == null) {
            xacmlHost.addAttribute(new XacmlAttribute(XACML_INET_ADDR_KEY, String.valueOf(CentralPoliciesEvaluationHandler.ipAddressToInteger(CentralPoliciesEvaluationHandler.LOCALHOST_IP))));
            return xacmlHost;
        }
        if (host.getIpAddress() == null) {
            throw new IllegalArgumentException("IP address is mandatory");
        }
        if (host.getIpAddress() != null) {
            xacmlHost.addAttribute(new XacmlAttribute(XACML_INET_ADDR_KEY, String.valueOf(host.getIpAddress())));
        }
        if (host.getHostname() != null) {
            xacmlHost.addAttribute(new XacmlAttribute(XACML_HOSTNAME_KEY, String.valueOf(host.getHostname())));
        }
        xacmlHost.addAttributes(host.getAttributes());
        return xacmlHost;
    }
}

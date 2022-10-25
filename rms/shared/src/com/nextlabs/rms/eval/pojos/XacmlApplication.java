package com.nextlabs.rms.eval.pojos;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.eval.Application;

public final class XacmlApplication extends XacmlCategory {

    public static final String XACML_APPLICATION_ID = "urn:nextlabs:names:evalsvc:1.0:application:application-id";

    private XacmlApplication() {
    }

    private XacmlApplication(String id) {
        super(XACML_APPLICATION_ID, id);
    }

    public static XacmlApplication getXacmlApplication(Application application) {
        if (application == null) {
            return new XacmlApplication("RMS");
        }
        XacmlApplication xacmlApplication = new XacmlApplication(application.getName());
        if (application.getPid() != null) {
            xacmlApplication.addAttribute(new XacmlAttribute("pid", String.valueOf(application.getPid())));
        }
        if (StringUtils.hasText(application.getPath())) {
            xacmlApplication.addAttribute(new XacmlAttribute("path", application.getPath()));
        }
        if (application.getAttributes() != null) {
            xacmlApplication.addAttributes(application.getAttributes());
        }
        return xacmlApplication;
    }
}

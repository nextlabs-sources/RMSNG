package com.nextlabs.rms.eval.pojos;

import com.nextlabs.rms.eval.User;

public final class XacmlSubject extends XacmlCategory {

    public static final String XACML_SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    public static final String XACML_SUBJECT_NAME = "urn:oasis:names:tc:xacml:1.0:subject:name";

    private XacmlSubject() {
    }

    private XacmlSubject(String id) {
        super(XACML_SUBJECT_ID, id);
    }

    public static XacmlSubject getXacmlSubject(User user) {
        XacmlSubject xacmlSubject = new XacmlSubject(user.getId());
        xacmlSubject.addAttribute(new XacmlAttribute(XACML_SUBJECT_NAME, user.getEmail()));
        xacmlSubject.addAttributes(user.getAttributes());
        return xacmlSubject;
    }
}

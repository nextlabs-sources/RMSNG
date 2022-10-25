package com.nextlabs.rms.eval.pojos;

import com.nextlabs.rms.eval.NamedAttributes;

import java.util.List;
import java.util.Map;

public final class XacmlEnvironment extends XacmlCategory {

    private XacmlEnvironment() {
    }

    public static XacmlEnvironment getXacmlEnvironment(List<NamedAttributes> attributes) {
        XacmlEnvironment environment = new XacmlEnvironment();
        for (NamedAttributes namedAttribute : attributes) {
            for (Map.Entry<String, List<String>> entry : namedAttribute.getAttributes().entrySet()) {
                entry.getValue().forEach(value -> environment.addAttribute(new XacmlAttribute(entry.getKey(), value)));
            }
        }
        return environment;
    }

    public static XacmlEnvironment getDefaultXacmlEnvironment() {
        XacmlEnvironment environment = new XacmlEnvironment();
        environment.addAttribute(new XacmlAttribute("dont-care-acceptable", "yes"));
        return environment;
    }
}

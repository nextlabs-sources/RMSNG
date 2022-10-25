package com.nextlabs.rms.eval.pojos;

import com.nextlabs.rms.eval.Resource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class XacmlResource extends XacmlCategory {

    public static final String XACML_RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    public static final String XACML_RESOURCE_TYPE = "urn:nextlabs:names:evalsvc:1.0:resource:resource-type";

    private XacmlResource() {
    }

    public XacmlResource(String type, String name) {
        super(XACML_RESOURCE_ID, name);
        XacmlAttribute resourceTypeAttr = new XacmlAttribute();
        resourceTypeAttr.setAttributeId(XACML_RESOURCE_TYPE);
        resourceTypeAttr.setValue(type);
        this.addAttribute(resourceTypeAttr);
    }

    public static XacmlResource getXacmlResource(Resource resource) {
        XacmlResource xacmlResource = new XacmlResource(resource.getResourceType(), resource.getResourceName());
        Map<String, String[]> attributesList = resource.getAttributes();
        Map<String, String[]> classification = resource.getClassification();
        if (classification != null) {
            if (attributesList == null) {
                attributesList = classification;
            } else {
                attributesList.putAll(classification);
            }
        }
        if (attributesList != null && !attributesList.isEmpty()) {
            Map<String, List<String>> attrMap = new HashMap<>();
            attributesList.forEach((key, value) -> {
                attrMap.put(key, Arrays.asList(value));
            });
            xacmlResource.addAttributes(attrMap);
        }
        return xacmlResource;
    }
}

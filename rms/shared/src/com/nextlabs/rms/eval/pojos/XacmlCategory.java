package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "CategoryId")
@JsonSubTypes({
    @JsonSubTypes.Type(name = XacmlCategory.XACML_SUBJECT_CATEGORY_ID, value = XacmlSubject.class),
    @JsonSubTypes.Type(name = XacmlCategory.XACML_RESOURCE_CATEGORY_ID, value = XacmlResource.class),
    @JsonSubTypes.Type(name = XacmlCategory.XACML_ENVIRONMENT_CATEGORY_ID, value = XacmlEnvironment.class),
    @JsonSubTypes.Type(name = XacmlCategory.XACML_APPLICATION_CATEGORY_ID, value = XacmlApplication.class),
    @JsonSubTypes.Type(name = XacmlCategory.XACML_HOST_CATEGORY_ID, value = XacmlHost.class)
})
public class XacmlCategory {

    public static final String XACML_SUBJECT_CATEGORY_ID = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    public static final String XACML_RESOURCE_CATEGORY_ID = "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
    public static final String XACML_ENVIRONMENT_CATEGORY_ID = "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";
    public static final String XACML_APPLICATION_CATEGORY_ID = "urn:nextlabs:names:evalsvc:1.0:attribute-category:application";
    public static final String XACML_HOST_CATEGORY_ID = "urn:nextlabs:names:evalsvc:1.0:attribute-category:host";

    @JsonProperty("Attribute")
    private final List<XacmlAttribute> attributes = new ArrayList<>();

    @JsonProperty("Id")
    private String categoryId;

    protected XacmlCategory() {
        this.categoryId = UUID.randomUUID().toString();
    }

    protected XacmlCategory(String categoryIdentifier, String categoryIdentifierValue) {
        this.categoryId = UUID.randomUUID().toString();
        XacmlAttribute attribute = new XacmlAttribute(categoryIdentifier, categoryIdentifierValue);
        this.addAttribute(attribute);
    }

    protected final void addAttributes(Map<String, List<String>> attributes) {
        Set<Map.Entry<String, List<String>>> entrySet = attributes.entrySet();
        for (Map.Entry<String, List<String>> entry : entrySet) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                if (values.size() == 1) {
                    this.addAttribute(new XacmlAttribute(entry.getKey(), values.get(0)));
                } else {
                    this.addAttribute(new XacmlAttribute(entry.getKey(), values));
                }
            }
        }
    }

    public final void addAttribute(XacmlAttribute attribute) {
        this.attributes.add(attribute);
    }

    public List<XacmlAttribute> getAttributes() {
        return attributes;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}

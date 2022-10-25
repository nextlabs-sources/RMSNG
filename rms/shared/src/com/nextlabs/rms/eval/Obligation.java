package com.nextlabs.rms.eval;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Obligation implements Serializable {

    private static final long serialVersionUID = -4703764521185695046L;
    public static final String WATERMARK = "OB_OVERLAY";
    public static final String WATERMARK_TEXT_KEY = "Text";

    private Integer id;
    private String name;
    private String right;
    private String policyName;
    private final List<Attribute> attributes = new ArrayList<>();

    public void addAttribute(Attribute attribute) {
        if (attribute != null) {
            attributes.add(attribute);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Obligation) {
            Obligation oth = (Obligation)obj;
            return id != null ? id.equals(oth.getId()) : oth.getId() == null && StringUtils.equals(getName(), oth.getName()) && StringUtils.equals(getPolicyName(), oth.getPolicyName());
        }
        return false;
    }

    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPolicyName() {
        return policyName;
    }

    @Override
    public int hashCode() {
        int hash = id != null ? id.hashCode() : 0;
        hash = 31 * hash + (name != null ? name.hashCode() : 0);
        hash = 31 * hash + (policyName != null ? policyName.hashCode() : 0);
        return hash;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPolicyName(String policy) {
        this.policyName = policy;
    }

    @Override
    public String toString() {
        return "Obligation{id: " + getId() + ", name: " + getName() + ", policy: " + getPolicyName() + "}";
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }
}

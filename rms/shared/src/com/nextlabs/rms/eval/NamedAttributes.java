package com.nextlabs.rms.eval;

import com.nextlabs.common.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedAttributes implements Serializable {

    private static final long serialVersionUID = 9130030066954963104L;
    private final String name;
    private Map<String, List<String>> attributes = new HashMap<String, List<String>>();

    public NamedAttributes(String name) {
        this.name = name;
    }

    public void addAttribute(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value is null");
        }
        List<String> list = this.attributes.get(key);
        if (list == null) {
            list = new ArrayList<>();
            this.attributes.put(key, list);
        }
        list.add(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof NamedAttributes) {
            NamedAttributes oth = (NamedAttributes)obj;
            return StringUtils.equals(getName(), oth.getName()) && getAttributes().equals(oth.getAttributes());
        }
        return false;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public int hashCode() {
        int hash = getName() != null ? getName().hashCode() : 0;
        hash = 31 * hash + getAttributes().hashCode();
        return hash;
    }
}

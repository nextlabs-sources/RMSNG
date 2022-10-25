package com.nextlabs.rms.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    private final String name;
    private Long pid;
    private String path;
    private Map<String, List<String>> attributes = new HashMap<String, List<String>>();

    public Application(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is required");
        }
        this.name = name;
    }

    public Application(String name, Long pid, String path) {
        this(name, path);
        this.pid = pid;
    }

    public Application(String name, String path) {
        this(name);
        this.path = path;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public Long getPid() {
        return pid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

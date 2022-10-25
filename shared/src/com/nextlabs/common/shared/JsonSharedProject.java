package com.nextlabs.common.shared;

public class JsonSharedProject {

    private int id;
    private String name;
    private String sharedByUserEmail;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSharedByUserEmail() {
        return sharedByUserEmail;
    }

    public void setSharedByUserEmail(String sharedByUserEmail) {
        this.sharedByUserEmail = sharedByUserEmail;
    }

}

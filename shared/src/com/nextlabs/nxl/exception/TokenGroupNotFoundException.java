package com.nextlabs.nxl.exception;

public class TokenGroupNotFoundException extends Exception {

    private static final long serialVersionUID = 786854858071644255L;
    private final String name;

    public TokenGroupNotFoundException(String tokenGroupName) {
        this.name = tokenGroupName;
    }

    public String getName() {
        return name;
    }
}

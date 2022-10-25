package com.nextlabs.rms.mail;

public final class SmtpConfig {

    private static final SmtpConfig INSTANCE = new SmtpConfig();

    private static final String SERVER = "server";
    private static final String PORT = "port";
    private static final String SSL = "ssl";
    private static final String USER_NAME = "user_name";
    private static final String PASS = "password";

    private String server;
    private int port;
    private boolean ssl;
    private String userName;
    private String password;

    private SmtpConfig() {
        server = "localhost";
        port = 25;
    }

    public static SmtpConfig getIntance() {
        return INSTANCE;
    }

    public void setProperty(String key, String value) {
        if (SERVER.equals(key)) {
            server = value;
        } else if (PORT.equals(key)) {
            port = Integer.parseInt(value);
        } else if (SSL.equals(key)) {
            ssl = Boolean.parseBoolean(value);
        } else if (USER_NAME.equals(key)) {
            userName = value;
        } else if (PASS.equals(key)) {
            password = value;
        }
    }

    public String getServer() {
        return server;
    }

    public int getPort() {
        return port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}

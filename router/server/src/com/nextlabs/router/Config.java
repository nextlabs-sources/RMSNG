package com.nextlabs.router;

import com.nextlabs.common.cli.CLI;
import com.nextlabs.common.cli.Parameter;

import java.io.File;

@CLI
public class Config {

    @Parameter(defaultValue = "Init", description = "Command to execute: Init/Tenant", hasArgs = true, mandatory = false)
    private String cmd;

    @Parameter(defaultValue = "false", description = "Run in web container", hasArgs = false, mandatory = false)
    private boolean inWebContainer;

    private File confDir;

    public Config() {
    }

    public String getCmd() {
        return cmd;
    }

    public boolean isInWebContainer() {
        return inWebContainer;
    }

    public void setConfDir(File confDir) {
        this.confDir = confDir;
    }

    public File getConfDir() {
        return confDir;
    }
}

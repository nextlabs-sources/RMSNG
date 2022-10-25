package com.nextlabs.common;

import com.nextlabs.common.util.StringUtils;

import java.io.File;

public final class Environment {

    private static final File DEFAULT_WIN_INSTALL_DIR = new File("C:/Program Files/NextLabs/RMS/");
    private static final File DEFAULT_UNIX_INSTALL_DIR = new File("/opt/nextlabs/rms/");
    private static final File DEFAULT_WIN_DATA_DIR = new File("C:/ProgramData/NextLabs/RMS/datafiles/");
    private static final File DEFAULT_UNIX_DATA_DIR = new File("/var/opt/nextlabs/rms/datafiles/");
    private static final File DEFAULT_WIN_SHARED_DIR = new File("C:/ProgramData/NextLabs/RMS/shared/");
    private static final File DEFAULT_UNIX_SHARED_DIR = new File("/var/opt/nextlabs/rms/shared/");

    private static final Environment INSTANCE = new Environment();
    private boolean isUnix;
    private File installDir;
    private File dataDir;
    private File sharedDir;
    private File sharedConfDir;
    private File sharedTempDir;

    private Environment() {
        String osName = System.getProperty("os.name");
        isUnix = osName != null && !osName.toLowerCase().startsWith("win");
    }

    public static Environment getInstance() {
        return INSTANCE;
    }

    public boolean isUnix() {
        return isUnix;
    }

    public File getInstallDir() {
        if (installDir == null) {
            String dir = System.getenv("RMS_INSTALL_DIR");
            installDir = StringUtils.hasText(dir) ? new File(dir) : isUnix() ? DEFAULT_UNIX_INSTALL_DIR : DEFAULT_WIN_INSTALL_DIR;
        }
        return installDir;
    }

    public File getDataDir() {
        if (dataDir == null) {
            String dir = System.getenv("RMS_DATA_DIR");
            dataDir = StringUtils.hasText(dir) ? new File(dir) : isUnix() ? DEFAULT_UNIX_DATA_DIR : DEFAULT_WIN_DATA_DIR;
        }
        return dataDir;
    }

    public File getSharedDir() {
        if (sharedDir == null) {
            String dir = System.getenv("RMS_SHARED_DIR");
            sharedDir = StringUtils.hasText(dir) ? new File(dir) : isUnix() ? DEFAULT_UNIX_SHARED_DIR : DEFAULT_WIN_SHARED_DIR;
        }
        return sharedDir;
    }

    public File getSharedConfDir() {
        if (sharedConfDir == null) {
            sharedConfDir = new File(getSharedDir(), "conf/");
        }
        return sharedConfDir;
    }

    public File getSharedTempDir() {
        if (sharedTempDir == null) {
            sharedTempDir = new File(getSharedDir(), "temp/");
        }
        return sharedTempDir;
    }

}

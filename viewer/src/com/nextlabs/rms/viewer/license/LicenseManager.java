package com.nextlabs.rms.viewer.license;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LicenseManager {

    private static final LicenseManager INSTANCE = new LicenseManager();

    private static final String LICENSE_JARCHECKER_GETPROPERTIES_METHOD_NAME = "getProperties";

    private static final String LICENSE_JARCHECKER_CHECK_METHOD_NAME = "check";

    private static final String LICENSE_JARCHECKER_SETCLASSLOADER_METHOD_NAME = "setClassLoader";

    private static final String LICENSE_JARCHECKER_SETJARFILENAME_METHOD_NAME = "setJarFileName";

    private static final String LICENSE_JARCHECKER_CLASS_NAME = "com.wald.license.checker.JarChecker";

    private static final String LICENSE_CLASSLOADER_CLASS_NAME = "com.wald.license.checker.LicenseClassLoader";

    private static final String LICENSE_JAR_NAME = "license.jar";

    private static final String LICENSE_FILE_NAME = "license.dat";

    private static final String LICENSE_STRING_CAD_VIEWER = "rms_cad_viewer";

    private static final String LICENSE_STRING_SAP_3D_VIEWER = "rms_sap_3d_viewer";

    private static final String LICENSE_STRING_SECURE_VIEWER = "rms_secure_viewer";

    private Properties licenseProperties = new Properties();

    private final Logger logger = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private LicenseManager() {
        try {
            init();
        } catch (Exception e) {
            logger.debug("Error occurred while initializing License Manager.", e);
        }
    }

    private void init() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        String licenseJarFileLocation = ViewerConfigManager.getInstance().getLicDir() + File.separator + LICENSE_JAR_NAME;
        File licJarFile = new File(licenseJarFileLocation);
        if (!licJarFile.exists()) {
            logger.error("License jar file not found. Not Initializing License Manager.");
            return;
        }
        File licFile = new File(ViewerConfigManager.getInstance().getLicDir(), LICENSE_FILE_NAME);
        if (!licFile.exists()) {
            logger.info("License file not found. Not Initializing License Manager.");
            return;
        }
        URL jarLocation = licJarFile.toURI().toURL();
        URL dataFileParentFolderLocation = ViewerConfigManager.getInstance().getLicDir().toURI().toURL();
        URL[] classLoaderURLs = { jarLocation, dataFileParentFolderLocation };
        try (URLClassLoader licenseLocationClassLoader = new URLClassLoader(classLoaderURLs, this.getClass().getClassLoader())) {
            Class<?> licenseClassLoaderClass = licenseLocationClassLoader.loadClass(LICENSE_CLASSLOADER_CLASS_NAME);
            Constructor<?> parentClassLoaderConstructor = licenseClassLoaderClass.getConstructor(ClassLoader.class);
            Object licenseClassLoader = parentClassLoaderConstructor.newInstance(licenseLocationClassLoader);

            Class<?> jarCheckerClass = licenseLocationClassLoader.loadClass(LICENSE_JARCHECKER_CLASS_NAME);
            Object jarCheckerInstance = jarCheckerClass.newInstance();
            Method setJarFileMethod = jarCheckerClass.getMethod(LICENSE_JARCHECKER_SETJARFILENAME_METHOD_NAME, java.lang.String.class);
            setJarFileMethod.invoke(jarCheckerInstance, licenseJarFileLocation);

            Class<?> setClassLoaderMethodParams = licenseClassLoader.getClass();
            Method setClassLoaderMethod = jarCheckerClass.getMethod(LICENSE_JARCHECKER_SETCLASSLOADER_METHOD_NAME, setClassLoaderMethodParams);
            setClassLoaderMethod.invoke(jarCheckerInstance, licenseClassLoader);

            Method checkMethod = jarCheckerClass.getMethod(LICENSE_JARCHECKER_CHECK_METHOD_NAME);
            checkMethod.invoke(jarCheckerInstance);

            Method getPropertiesMethod = jarCheckerClass.getMethod(LICENSE_JARCHECKER_GETPROPERTIES_METHOD_NAME);
            this.licenseProperties = (Properties)getPropertiesMethod.invoke(jarCheckerInstance);
        } catch (IOException e) {
            logger.error("Error occurred loading license jar", e);
        }
        StringBuilder sb = new StringBuilder(66);
        sb.append("Licensed Viewers: ");
        if (isFeatureLicensed(LicensedFeature.FEATURE_VIEW_CAD_FILE)) {
            sb.append("CAD Viewer, ");
        }
        if (isFeatureLicensed(LicensedFeature.FEATURE_VIEW_SAP_3D_FILE)) {
            sb.append("SAP 3D Viewer, ");
        }
        if (isFeatureLicensed(LicensedFeature.FEATURE_VIEW_GENERIC_FILE)) {
            sb.append("Secure Viewer");
        }
        if (StringUtils.equals("Licensed Viewers: ", sb.toString())) {
            logger.info("No valid License found for viewer");
        } else {
            logger.info(sb.toString());
        }
    }

    public static LicenseManager getInstance() {
        return INSTANCE;
    }

    public boolean isFeatureLicensed(LicensedFeature featureName) {
        switch (featureName) {
            case FEATURE_VIEW_CAD_FILE:
                if (licenseProperties.get(LICENSE_STRING_CAD_VIEWER) != null && "1".equals(licenseProperties.getProperty(LICENSE_STRING_CAD_VIEWER))) {
                    return true;
                }
                break;
            case FEATURE_VIEW_SAP_3D_FILE:
                if (licenseProperties.get(LICENSE_STRING_SAP_3D_VIEWER) != null && "1".equals(licenseProperties.getProperty(LICENSE_STRING_SAP_3D_VIEWER))) {
                    return true;
                }
                break;
            case FEATURE_VIEW_GENERIC_FILE:
                if (licenseProperties.get(LICENSE_STRING_SECURE_VIEWER) != null && "1".equals(licenseProperties.getProperty(LICENSE_STRING_SECURE_VIEWER))) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

}

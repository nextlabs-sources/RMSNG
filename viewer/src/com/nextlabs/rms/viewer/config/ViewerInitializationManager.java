package com.nextlabs.rms.viewer.config;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.ZipUtil;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewerInitializationManager {

    private static final String RMS_PERCEPTIVE_VERSION_TXT = "rms-perceptive-version.txt";
    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public void initViewer(String webDir, File confDir) throws IOException {
        ViewerConfigManager.getInstance(confDir);
        ViewerConfigManager.getInstance().setWebDir(webDir);
        ViewerConfigManager.getInstance().setWebViewerDir();
        ViewerCacheManager.getInstance();
        initLicenseManager();
        try {
            deployCADConverter();
        } catch (Exception e) {
            LOGGER.error("Error occured while deploying CAD Converter", e);
        }
        try {
            deploySAPConverter();
        } catch (Exception e) {
            LOGGER.error("Error occured while deploying VDS Converter", e);
        }
        try {
            deployDocConverter();
        } catch (Exception e) {
            LOGGER.error("Error occured while deploying Doc Converter", e);
        }
    }

    private void initLicenseManager() {
        //LicenseManager.getInstance();
    }

    private void deployCADConverter() throws IOException, ZipException {
        File targetFolder = ViewerConfigManager.getInstance().getCadConverterDir();
        File viewerInfoFile = new File(ViewerConfigManager.getInstance().getCadBinDir(), "rms-cad-version.txt");
        String currentViewerVer = viewerInfoFile.exists() ? FileUtils.readFileToString(viewerInfoFile).trim() : "";

        boolean converterDeployed = extractZipFile(targetFolder, ViewerConfigManager.CAD_CONVERTER_ZIP_REGEX, currentViewerVer);
        if (converterDeployed) {
            LOGGER.info("Deployed CAD Converter.");
            if (ViewerConfigManager.getInstance().isUnix()) {
                String unixCadConverterDir = ViewerConfigManager.getInstance().getCadBinDir() + "/linux64/";
                setPOSIXFilePermissions(unixCadConverterDir + "converter");
            }
        }

        File cadViewer = new File(targetFolder, ViewerConfigManager.CADVIEWER_WEBDIR_NAME);
        File webCadViewer = new File(ViewerConfigManager.getInstance().getWebViewerDir(), ViewerConfigManager.CADVIEWER_WEBDIR_NAME);
        if (converterDeployed || !webCadViewer.exists()) {
            FileUtils.deleteQuietly(webCadViewer);
            FileUtils.copyDirectory(cadViewer, webCadViewer);
            LOGGER.info("Deployed CAD Viewer.");
        }
    }

    private void deploySAPConverter() throws IOException, ZipException {
        File targetFolder = ViewerConfigManager.getInstance().getSapBinDir();
        File sapViewer = new File(targetFolder, ViewerConfigManager.SAPVIEWER_WEBDIR_NAME);
        File viewerInfoFile = new File(sapViewer, "rms-sap-version.txt");
        String currentViewerVer = viewerInfoFile.exists() ? FileUtils.readFileToString(viewerInfoFile).trim() : "";
        File webSAPViewer = ViewerConfigManager.getInstance().getSAPViewerDir();
        boolean converterDeployed = extractZipFile(targetFolder, ViewerConfigManager.SAP_CONVERTER_ZIP_REGEX, currentViewerVer);
        if (converterDeployed || !webSAPViewer.exists()) {
            FileUtils.deleteQuietly(webSAPViewer);
            FileUtils.copyDirectory(sapViewer, webSAPViewer);
            LOGGER.info("Deployed SAP Viewer.");
        }
    }

    private void deployDocConverter() {
        String targetFolder = ViewerConfigManager.getInstance().getDocConverterDir().getAbsolutePath();
        File viewerInfoFile = new File(targetFolder, RMS_PERCEPTIVE_VERSION_TXT);
        String currentViewerVer;

        boolean viewerDeployed = false;
        try {
            String tempPath = ViewerConfigManager.getInstance().getViewerPlugInDir() + File.separator + ViewerConfigManager.DOC_VIEWER_DEPLOYED_PATH;
            currentViewerVer = viewerInfoFile.exists() ? FileUtils.readFileToString(viewerInfoFile).trim() : "";
            viewerDeployed = extractZipFile(new File(tempPath), ViewerConfigManager.PERCEPTIVE_ZIP_REGEX, currentViewerVer);
            if (viewerDeployed) {
                FileUtils.copyDirectory(new File(tempPath + File.separator + (ViewerConfigManager.getInstance().isUnix() ? "linux" + File.separator + "intel-64" : "windows" + File.separator + "intel-64")), new File(targetFolder));
                FileUtils.copyFile(new File(tempPath + File.separator + RMS_PERCEPTIVE_VERSION_TXT), new File(targetFolder, RMS_PERCEPTIVE_VERSION_TXT));
                FileUtils.copyFile(new File(tempPath + File.separator + ViewerConfigManager.ISYS11DF_JAR), new File(targetFolder, ViewerConfigManager.ISYS11DF_JAR));
                FileUtils.copyFile(new File(tempPath + File.separator + ViewerConfigManager.MEMORY_STREAM_JAR), new File(targetFolder, ViewerConfigManager.MEMORY_STREAM_JAR));
                FileUtils.deleteDirectory(new File(tempPath));
                if (ViewerConfigManager.getInstance().isUnix()) {
                    setPOSIXFilePermissions(targetFolder);
                }
                LOGGER.info("Deployed Doc Viewer.");
            }
        } catch (IOException | ZipException e) {
            LOGGER.error("Error occured while deploying doc viewer ", e);
        }
    }

    private boolean extractZipFile(File targetFolder, String converterZipPartialName, String currentZipVer)
            throws ZipException {
        File pluginDir = ViewerConfigManager.getInstance().getViewerPlugInDir();
        String converterZipRegex = "^" + converterZipPartialName + ".*\\.zip$";
        File[] zipFiles = pluginDir.listFiles(new RegexFileFilter(converterZipRegex));
        if (zipFiles == null || zipFiles.length == 0) {
            LOGGER.debug("No " + converterZipPartialName + " zip file is found.");
            return false;
        }
        File latestZipFile = zipFiles[0];
        String converterPath = latestZipFile.getAbsolutePath();

        if (StringUtils.hasText(converterPath) && !StringUtils.equals(latestZipFile.getName(), currentZipVer)) {
            LOGGER.debug("Unzipping " + converterPath + " to " + targetFolder);
            FileUtils.deleteQuietly(targetFolder);
            ZipUtil.unZip(converterPath, targetFolder.getAbsolutePath());
            return true;
        }
        return false;
    }

    private void setPOSIXFilePermissions(String filePath) {
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);

        try {
            java.nio.file.Files.setPosixFilePermissions(Paths.get(filePath), perms);
        } catch (IOException e) {
            LOGGER.error("Failed to set file permission.", e);
        }
    }

    static class RegexFileFilter implements java.io.FileFilter {

        final java.util.regex.Pattern pattern;

        public RegexFileFilter(String regex) {
            pattern = java.util.regex.Pattern.compile(regex);
        }

        public boolean accept(File f) {
            return pattern.matcher(f.getName()).find();
        }
    }
}

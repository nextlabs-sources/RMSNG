/**
 *
 */
package com.nextlabs.rms.viewer.config;

import com.nextlabs.common.Environment;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.FIPSError;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author nnallagatla
 *
 */
public final class ViewerConfigManager {

    public static final String VIEWER_CONTEXT_NAME = "/viewer";

    private Properties properties = new Properties();

    private File dataDir;

    private File installDir;

    private File cadConverterDir;

    private File cadBinDir;

    private File unixCadConverter;

    private File winCadConverter;

    private File viewerPlugInDir;

    private File webViewerDir;

    private File docConverterDir;

    private File sapBinDir;

    private File sapViewerDir;

    private File tempDir;

    private String webDir;

    private File commonSharedTempDir;

    private File viewerSharedTempDir;

    private File licDir;

    private String clientId;

    private File configDir;

    public static final String CONFIG_FILENAME = "viewer.properties";

    public static final String ROUTER_URL = "web.router_url";

    public static final String ROUTER_INTERNAL_URL = "web.router_internal_url";

    public static final String PREFER_EXTERNAL_PDP = "web.prefer_external_pdp";

    public static final String CC_OAUTH_CLIENT_ID = "web.cc.oauth.client_id";

    public static final String CC_OAUTH_CLIENT_SECRET = "web.cc.oauth.client_secret";

    public static final String CC_CONSOLE_URL = "web.cc.console_url";

    public static final String PDP_POLICY_CONTROLLER_URL = "web.pdp.policy_controller_url";

    public static final String RMS_INTERNAL_URL = "web.rms_internal_url";

    public static final String STATELESS_MODE = "STATELESS_MODE";

    public static final String ICENET_URL = "web.icenet.url";

    public static final String CACHING_MODE_SERVER = "web.caching.mode.server";

    public static final String CACHING_SERVER_HOSTNAME = "web.caching.server.hostname";

    public static final String CACHING_SERVER_CLIENT_PORT = "web.caching.server.client.port";

    public static final String TEMPDIR_NAME = "temp";

    public static final String USE_2D_PDF_VIEWER = "USE_2D_PDF_VIEWER";

    public static final String SUPPORTED_FILE_FORMATS = "SUPPORTED_FILE_FORMATS";

    public static final String SUPPORTED_EXCEL_FORMATS = "SUPPORTED_EXCEL_FORMATS";

    public static final String SUPPORTED_HOOPSASSEMBLY_FORMATS = "SUPPORTED_HOOPSASSEMBLY_FORMATS";

    public static final String SUPPORTED_HOOPSNONASSEMBLY_FORMATS = "SUPPORTED_HOOPSNONASSEMBLY_FORMATS";

    public static final String ALLOWED_FILE_EXTN = ".doc,.docx,.ppt,.pptx,.pdf,.txt,.jpg,.jpeg,.png,.vsd,.vsdx,.tif,.tiff,.dxf,.dwg,.rtf,.c,.h,.js,.xml,.json,.log,.bmp,.vb,.m,.swift,.py,.java,.cpp,.err,.md,.sql,.csv,.dotx,.docm,.potm,.potx,.properties";

    public static final String ALLOWED_EXCEL_EXTN = ".xlsx,.xls,.xltm,.xlsb,.xlsm,.xlt,.xltx";

    public static final String ALLOWED_HOOPSNONASSEMBLY_EXTN = ".hsf,.obj,.pdf,.prc,.pts,.ptx,.stl,.u3d,.wrl,.vrml,.xyz,.3mf,.sat,.sab,.dwg,.dxf,.ipt,.model,\n" + ".session,.dlv,.exp,.catdrawing,.catpart,.catshape,.cgr,.3dxml,.dae,.neu,.prt,.xpr,.fbx,.gltf,.glb,.mf1,\n" + ".unv,.ifc,.ifczip,.igs,.iges,.jt,.x_b,.x_t,.xmt_txt,.rvt,.rfa,.3dm,.par,.pwd,.psm,.sldasm,.sldprt,.stp,.step,.vda";

    public static final String ALLOWED_HOOPSASSEMBLY_EXTN = "";

    public static final String ZIP_EXTN = ".zip";

    public static final String FILE_UPLD_THRESHOLD_SIZE = "FILE_UPLD_THRESHOLD_SIZE";

    public static final String FILE_UPLD_MAX_REQUEST_SIZE = "FILE_UPLD_MAX_REQUEST_SIZE";

    public static final String LOG4J_CONFIG_FILE = "viewer-log.properties";

    public static final String FILECONTENT_CACHE_TIMEOUT_MINS = "FILECONTENT_CACHE_TIMEOUT_MINS";

    public static final String FILECONTENT_CACHE_MAXMEM_MB = "FILECONTENT_CACHE_MAXMEM_MB";

    public static final String WATERMARK_CACHE_TIMEOUT_MINS = "WATERMARK_CACHE_TIMEOUT_MINS";

    public static final String WATERMARK_CACHE_MAXMEM_MB = "WATERMARK_CACHE_MAXMEM_MB";

    public static final String WRITE_DECRYPTED_FILE_TO_DISK = "WRITE_DECRYPTED_FILE_TO_DISK";

    public static final String USE_IMG_FOR_EXCEL = "USE_IMG_FOR_EXCEL";

    private boolean isUnix;

    private static Logger logger = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public static final String RH_FILE_EXTN = ".rh";

    public static final String VDS_FILE_EXTN = ".vds";

    public static final String PDF_FILE_EXTN = ".pdf";

    public static final String DWG_FILE_EXTN = ".dwg";

    public static final String TIFF_FILE_EXTN = ".tiff";

    public static final String USE_FILENAME_AS_DOCUMENTID = "USE_FILENAME_AS_DOCUMENTID";

    public static final String CONVERTER_IMAGE_DPI = "CONVERTER_IMAGE_DPI";

    public static final String CONVERTER_PAGE_LIMIT = "CONVERTER_PAGE_LIMIT";

    public static final String TRUST_SELF_SIGNED_CERTS = "TRUST_SELF_SIGNED_CERTS";

    public static final String CAD_CONVERTER_ZIP_REGEX = "RightsManagementServer-CADViewer";

    public static final String CAD_VIEWER_DEPLOYED_PATH = "RMSCADCONVERTER";

    public static final String CADVIEWER_WEBDIR_NAME = "cadviewer";

    public static final String INSTALL_EXTERNAL = "external";

    public static final String INSTALL_PLUGINS = "viewers";

    public static final String SAP_CONVERTER_ZIP_REGEX = "RightsManagementServer-SAPViewer";

    public static final String PERCEPTIVE_ZIP_REGEX = "RightsManagementServer-DocViewer";

    public static final String SAPVIEWER_WEBDIR_NAME = "SAPViewer";

    public static final String DISABLE_WEBSVC_AUTH = "DISABLE_WEBSVC_AUTH";

    public static final String ENABLE_WEBSVC_DEBUG_LOGS = "ENABLE_WEBSVC_DEBUG_LOGS";

    private static final String LICENSE_FOLDER_NAME = "license";

    private static volatile ViewerConfigManager instance;

    private List<String> supportedFileFormatList;

    private List<String> supportedExcelFormatList;

    private List<String> supportedHOOPSAssemblyFormatList;

    private List<String> supportedHOOPSFileFormatList;

    private List<String> supportedCADFileFormatList;

    private List<String> supportedHOOPSNonAssemblyFormatList;

    private List<String> supportedFeedbackAttachmentList;

    public static final String ISYS11DF_JAR = "ISYS11df.jar";

    public static final String MEMORY_STREAM_JAR = "RMS-Perceptive-Lib.jar";

    public static final String DOC_VIEWER_DEPLOYED_PATH = "perceptive";

    public static final String SAP_VIEWER_DEPLOYED_PATH = "rms-sap-viewer";

    public static synchronized ViewerConfigManager getInstance(File configDir) throws IOException {
        if (instance == null) {
            synchronized (ViewerConfigManager.class) {
                if (instance == null) {
                    instance = new ViewerConfigManager(configDir);
                }
            }
        }
        return instance;
    }

    public static ViewerConfigManager getInstance() { //NOPMD
        if (instance == null) {
            throw new NullPointerException("ViewerConfigManager has not been initialized yet");
        }
        return instance;
    }

    private ViewerConfigManager(File configDir) throws IOException {
        this.configDir = configDir;
        if (logger.isInfoEnabled()) {
            logger.info("ViewerConfigManager Created");
        }
        loadConfigParams();
        init();
    }

    private void init() throws IOException {
        checkOSType();
        setInstallDir();
        setDataDir();
        setCartridgeDirs();
        //initLogging();
        setTempDir();
        setSharedTempDirs();
        createMissingDirs();
    }

    private void createMissingDirs() {
        File file = new File(installDir, INSTALL_EXTERNAL);
        if (!file.exists()) {
            logger.warn("INSTALL_EXTERNAL missing. Trying to create at " + file.getAbsolutePath());
            try {
                FileUtils.mkdir(file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void setTempDir() {
        tempDir = new File(dataDir, "temp");
        if (!tempDir.exists()) {
            try {
                FileUtils.mkdir(tempDir);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Temp dir set to: " + tempDir.getAbsolutePath());
        }
    }

    private void setSharedTempDirs() {
        commonSharedTempDir = new File(Environment.getInstance().getSharedTempDir(), "common");
        viewerSharedTempDir = new File(Environment.getInstance().getSharedTempDir(), "viewer");
    }

    private void checkOSType() {
        String osName = System.getProperty("os.name");
        if (osName != null && !osName.toLowerCase().startsWith("win")) {
            isUnix = true;
        }
    }

    private void setInstallDir() throws IOException {
        installDir = new File(Environment.getInstance().getInstallDir(), "viewer");
    }

    private void setDataDir() throws IOException {
        dataDir = new File(Environment.getInstance().getDataDir(), "viewer");

        if (!dataDir.exists()) {
            try {
                FileUtils.mkdir(dataDir);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        File clientFile = new File(dataDir, ".client_id");
        if (!clientFile.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(clientFile), "UTF-8"))) {
                final byte[] b = new byte[14];
                final SecureRandom random = SecureRandom.getInstance("DEFAULT", "BCFIPS");
                random.setSeed(System.currentTimeMillis());
                random.nextBytes(b);
                String s = "WEBV" + Hex.toHexString(b).toUpperCase();
                bw.write(s);
                clientId = s;
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                logger.error("DRBG algorithm or provider not available");
                throw new FIPSError("DRBG algorithm or provider not available", e);
            } catch (IOException e) {
                logger.error("Unable to create client id");
                throw e;
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(clientFile), "UTF-8"))) {
                String client = null;
                if ((client = br.readLine()) != null) {
                    clientId = client;
                }
            }
        }
        licDir = new File(dataDir, LICENSE_FOLDER_NAME);
    }

    public String getClientId() {
        return clientId;
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setWebDir(String webDir) {
        this.webDir = webDir;
    }

    public String getWebDir() {
        return webDir;
    }

    public File getLicDir() {
        return licDir;
    }

    public File getCadConverterDir() {
        return cadConverterDir;
    }

    public File getWebViewerDir() {
        return webViewerDir;
    }

    public File getViewerPlugInDir() {
        return viewerPlugInDir;
    }

    public File getWinCadConverter() {
        return winCadConverter;
    }

    public File getUnixCadConverter() {
        return unixCadConverter;
    }

    public File getCadBinDir() {
        return cadBinDir;
    }

    public File getSAPViewerDir() {
        return sapViewerDir;
    }

    public File getCommonSharedTempDir() {
        return commonSharedTempDir;
    }

    public void setCommonSharedTempDir(File commonSharedTempDir) {
        this.commonSharedTempDir = commonSharedTempDir;
    }

    public File getViewerSharedTempDir() {
        return viewerSharedTempDir;
    }

    public void setViewerSharedTempDir(File viewerSharedTempDir) {
        this.viewerSharedTempDir = viewerSharedTempDir;
    }

    public void setWebViewerDir() {
        webViewerDir = new File(webDir, "ui/app/viewers");
        sapViewerDir = new File(webViewerDir, SAPVIEWER_WEBDIR_NAME);
    }

    public void setCartridgeDirs() {
        File installExternalDir = new File(installDir, INSTALL_EXTERNAL);
        cadConverterDir = new File(installExternalDir, CAD_VIEWER_DEPLOYED_PATH);
        cadBinDir = new File(cadConverterDir, "bin");
        unixCadConverter = new File(cadBinDir, "linux64/converter.sh");
        winCadConverter = new File(cadBinDir, "win64/converter.exe");
        viewerPlugInDir = new File(installDir, INSTALL_PLUGINS);
        docConverterDir = new File(installExternalDir, DOC_VIEWER_DEPLOYED_PATH);
        sapBinDir = new File(installExternalDir, SAP_VIEWER_DEPLOYED_PATH);
    }

    private void loadConfigParams() throws IOException {
        BufferedInputStream inStream = null;
        try {
            inStream = new BufferedInputStream(new FileInputStream(new File(configDir, CONFIG_FILENAME)));
            properties.load(inStream);
            properties = PropertiesFileUtils.decryptPropertyValues(properties);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException e) {
                logger.error("Error occurred while closing stream");
            }
        }
        setSupportedFileFormats();
        setSupportedNonAssemblyHOOPSFileFormats();
        setSupportedHOOPSAssemblyFormats();
        setSupportedHOOPSFileFormats();
        setSupportedCADFileFormats();
        setSupportedExcelFormats();
    }

    private void setSupportedFileFormats() {
        String fileFormats = getStringProperty(SUPPORTED_FILE_FORMATS).trim();
        if (!StringUtils.hasText(fileFormats)) {
            supportedFileFormatList = Arrays.asList(ALLOWED_FILE_EXTN.split("\\s*,\\s*"));
            logger.info("Supported file formats are " + supportedFileFormatList.toString());
            return;
        }
        supportedFileFormatList = Arrays.asList(fileFormats.split("\\s*,\\s*"));
        logger.info("Supported file formats are " + supportedFileFormatList.toString());
    }

    private void setSupportedExcelFormats() {
        String excelFormats = getStringProperty(SUPPORTED_EXCEL_FORMATS).trim();
        if (!StringUtils.hasText(excelFormats)) {
            supportedExcelFormatList = Arrays.asList(ALLOWED_EXCEL_EXTN.split("\\s*,\\s*"));
            logger.info("Supported excel formats are " + supportedExcelFormatList.toString());
            return;
        }
        supportedExcelFormatList = Arrays.asList(excelFormats.split("\\s*,\\s*"));
        logger.info("Supported excel formats are " + supportedExcelFormatList.toString());
    }

    private void setSupportedNonAssemblyHOOPSFileFormats() {
        String hoopsNonAssemblyFormats = getStringProperty(SUPPORTED_HOOPSNONASSEMBLY_FORMATS).trim();
        supportedHOOPSNonAssemblyFormatList = new ArrayList<String>();
        if (!StringUtils.hasText(hoopsNonAssemblyFormats)) {
            List<String> supportedNonAssemblyFormats = Arrays.asList(ALLOWED_HOOPSNONASSEMBLY_EXTN.split("\\s*,\\s*"));
            supportedHOOPSNonAssemblyFormatList.addAll(supportedNonAssemblyFormats);
            logger.info("Supported HOOPS non assembly formats are " + supportedHOOPSNonAssemblyFormatList.toString());
            return;
        }
        supportedHOOPSNonAssemblyFormatList = Arrays.asList(hoopsNonAssemblyFormats.split("\\s*,\\s*"));
        logger.info("Supported HOOPS non assembly formats are " + supportedHOOPSNonAssemblyFormatList.toString());
    }

    private void setSupportedHOOPSAssemblyFormats() {
        String hoopsAssemblyFormats = getStringProperty(SUPPORTED_HOOPSASSEMBLY_FORMATS).trim();
        supportedHOOPSAssemblyFormatList = new ArrayList<String>();
        if (!StringUtils.hasText(hoopsAssemblyFormats)) {
            List<String> supportedAssemblyFormats = Arrays.asList(ALLOWED_HOOPSASSEMBLY_EXTN.split("\\s*,\\s*"));
            supportedHOOPSAssemblyFormatList.addAll(supportedAssemblyFormats);
            logger.info("Supported HOOPS assembly formats are " + supportedHOOPSAssemblyFormatList.toString());
            return;
        }
        supportedHOOPSAssemblyFormatList = Arrays.asList(hoopsAssemblyFormats.split("\\s*,\\s*"));
        logger.info("Supported HOOPS assembly formats are " + supportedHOOPSAssemblyFormatList.toString());
    }

    private void setSupportedHOOPSFileFormats() {
        supportedHOOPSFileFormatList = new ArrayList<String>();
        supportedHOOPSFileFormatList.addAll(supportedHOOPSNonAssemblyFormatList);
        supportedHOOPSFileFormatList.addAll(supportedHOOPSAssemblyFormatList);
        logger.info("Supported HOOPS file formats are " + supportedHOOPSFileFormatList.toString());
    }

    private void setSupportedCADFileFormats() {
        supportedCADFileFormatList = new ArrayList<String>();
        supportedCADFileFormatList.addAll(supportedHOOPSFileFormatList);
        supportedCADFileFormatList.add(RH_FILE_EXTN);
        supportedCADFileFormatList.add(VDS_FILE_EXTN);
        logger.info("Supported CAD file formats are " + supportedCADFileFormatList.toString());

    }

    public List<String> getSupportedFileFormat() {
        return supportedFileFormatList;
    }

    public List<String> getSupportedHOOPSAssemblyFormatList() {
        return supportedHOOPSAssemblyFormatList;
    }

    public List<String> getSupportedHOOPSFileFormatList() {
        return supportedHOOPSFileFormatList;
    }

    public List<String> getSupportedCADFileFormatList() {
        return supportedCADFileFormatList;
    }

    public List<String> getSupportedHOOPSNonAssemblyFormatList() {
        return supportedHOOPSNonAssemblyFormatList;
    }

    public List<String> getSupportedExcelFormatList() {
        return supportedExcelFormatList;
    }

    public boolean getBooleanProperty(String key) {
        String val = properties.getProperty(key);
        if (val == null) {
            return false;
        }
        return "true".equalsIgnoreCase(val.trim()) || "yes".equalsIgnoreCase(val.trim());
    }

    public String getStringProperty(String key) {
        return properties.getProperty(key, "").trim();
    }

    public int getIntProperty(String key) {
        int val = -1;
        try {
            String strVal = properties.getProperty(key);
            if (strVal != null && strVal.length() > 0) {
                val = Integer.parseInt(strVal.trim());
            }
            return val;
        } catch (Exception e) {
            logger.error("Error occurred while getting value for key:" + key);
            return val;
        }
    }

    public long getLongProperty(String key) {
        long val = -1;
        try {
            String strVal = properties.getProperty(key);
            if (strVal != null && strVal.length() > 0) {
                val = Long.parseLong(strVal.trim());
            }
            return val;
        } catch (NumberFormatException e) {
            logger.error("Error occurred while getting value for key:" + key);
            return val;
        }
    }

    public Locale getCurrentUserLocale() {
        // This can be used in the future for MUI(Multilingual UI). The client locale should be set when the session is initialized.
        // For now, we'll just use the server locale.
        return Locale.getDefault();
    }

    public File getDocConverterDir() {
        return docConverterDir;
    }

    public File getSapBinDir() {
        return sapBinDir;
    }

    public void setDocConverterDir(File docViewerDir) {
        this.docConverterDir = docViewerDir;
    }

    public List<String> getSupportedFeedbackAttachmentList() {
        return supportedFeedbackAttachmentList;
    }

    public boolean isUnix() {
        return isUnix;
    }
}

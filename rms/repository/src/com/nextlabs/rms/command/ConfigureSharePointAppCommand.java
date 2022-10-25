package com.nextlabs.rms.command;

import com.nextlabs.common.Environment;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.config.SettingManager;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConfigureSharePointAppCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    public static final String SHAREPOINT_ONLINE_APP_NAME = "SecureCollaboration_SPOnlineApp.zip";
    public static final String SHAREPOINT_ON_PREMISE_APP_NAME = "SecureCollaboration_SPOnPremiseApp.zip";
    public static final String SHAREPOINT_ONLINE_REPOSITORY_APP_NAME = "SecureCollaboration_SPOnlineRepositoryApp.zip";
    public static final String SP_DATA_DIR = "sharepoint";
    private static final String SP_APP_VERSION_FILE = "spVersionFile";
    private static final String SP_ONLINE_REPO_APP_VERSION_FILE = "spOnlineRepoVersionFile";
    private static final String SP_APP_VERSION_FILE_ON_PREMISE = "spVersionFileOnPremise";

    @Override
    public void doAction(HttpServletRequest request,
        HttpServletResponse response) {
        DbSession session = DbSession.newSession();
        String contextPath = request.getContextPath();
        try {
            RMSUserPrincipal userPrincipal = authenticate(session, request);
            if (userPrincipal == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String type = request.getParameter("type");
            String appPath = "";
            if (ServiceProviderType.SHAREPOINT_ONLINE_CROSSLAUNCH.name().equals(type)) {
                appPath = new File(Environment.getInstance().getDataDir(), SP_DATA_DIR + File.separator + SHAREPOINT_ONLINE_APP_NAME).getAbsolutePath();
            } else if (ServiceProviderType.SHAREPOINT_ONLINE.name().equals(type)) {
                appPath = new File(Environment.getInstance().getDataDir(), SP_DATA_DIR + File.separator + SHAREPOINT_ONLINE_REPOSITORY_APP_NAME).getAbsolutePath();
            }
            if (ServiceProviderType.SHAREPOINT_CROSSLAUNCH.name().equals(type)) {
                appPath = new File(Environment.getInstance().getDataDir(), SP_DATA_DIR + File.separator + SHAREPOINT_ON_PREMISE_APP_NAME).getAbsolutePath();
            }

            ServiceProviderSetting storageProviderSettings = null;
            try {
                storageProviderSettings = SettingManager.getStorageProviderSettings(session, userPrincipal.getTenantId(), ServiceProviderType.valueOf(type));
            } finally {
                session.close();
            }

            //Unzip files into a folder
            String unzippedAppName = FilenameUtils.getBaseName(appPath);
            String unzippedAppPath = new File(WebConfig.getInstance().getTmpDir(), unzippedAppName).getAbsolutePath();
            ZipUtil.unZip(appPath, unzippedAppPath);
            //Read the appManifest.xml file and change the values
            File appFile = new File(unzippedAppPath + File.separator + "AppManifest.xml");
            Map<String, String> attributesMap = storageProviderSettings.getAttributes();
            String newClientId = attributesMap.get(ServiceProviderSetting.APP_ID);
            String appRedirectUrl = attributesMap.get(ServiceProviderSetting.REDIRECT_URL);

            if (!StringUtils.hasText(appRedirectUrl)) {
                appRedirectUrl = HTTPUtil.getURI(request);
            }

            String newStartPageUrl = appRedirectUrl + "/SharepointApp.jsp";//request.getParameter("newStartPageUrl");
            String remoteWebUrl = attributesMap.get(ServiceProviderSetting.REMOTE_WEB_URL);
            String appMenuDisplayString = attributesMap.get(ServiceProviderSetting.APP_DISPLAY_MENU);

            if (!StringUtils.hasText(appMenuDisplayString)) {
                appMenuDisplayString = RMSMessageHandler.getClientString("sp_default_app_display_menu");
            }

            modifyManifestFile(appFile, newClientId, newStartPageUrl, type);
            File unzippedDir = new File(unzippedAppPath);
            File[] listOfFiles = unzippedDir.listFiles();
            if (listOfFiles == null) {
                throw new IllegalArgumentException("listOfFiles is Null");
            }
            StringBuilder redirectUrl = new StringBuilder();
            String tenantId = userPrincipal.getTenantId();
            if (type.equals(ServiceProviderType.SHAREPOINT_CROSSLAUNCH.name())) {
                redirectUrl.append(remoteWebUrl);
                redirectUrl.append("?isFromMenuItem=yes&SPHostUrl={HostUrl}&siteName={HostUrl}&path={ItemUrl}&clientID={ClientID}&RMSURL=");
                redirectUrl.append(appRedirectUrl);
                redirectUrl.append(contextPath);
                redirectUrl.append("/SharePointAuth/OnPremiseAuth");
            } else if (type.equals(ServiceProviderType.SHAREPOINT_ONLINE_CROSSLAUNCH.name())) {
                redirectUrl.append(appRedirectUrl);
                redirectUrl.append(contextPath);
                redirectUrl.append("/ExternalViewer.jsp?siteName={HostUrl}&path={ItemUrl}&clientID=");
                redirectUrl.append(newClientId);
                redirectUrl.append("&tenantID=");
                redirectUrl.append(tenantId);
            }
            //Read the element file and change the value
            for (File file : listOfFiles) {
                if (file.getName().contains("elements")) {
                    modifyElementFile(redirectUrl.toString(), appMenuDisplayString, file);
                    break;
                }
            }
            //zip back the file
            ZipUtil.zipFolder(unzippedAppPath, unzippedAppPath + ".zip");
            writeToStream(response, unzippedAppPath + ".zip");
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException | ZipException
                | InterruptedException | IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.sendRedirect(contextPath + "/error?code=err.sp.app.create");
                }
            } catch (IOException e1) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(e1.getMessage(), e1);
                }
            }
        }
    }

    private void writeToStream(HttpServletResponse response, String zippedAppPath) throws IOException {
        File file = new File(zippedAppPath);
        String pathName = file.getName();
        FileInputStream fileIn = null;
        ServletOutputStream out = null;
        try {
            fileIn = new FileInputStream(file);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Disposition", "attachment;filename=" + pathName);
            response.setContentLength((int)file.length());
            out = response.getOutputStream();
            IOUtils.copy(fileIn, out);
        } finally {
            if (fileIn != null) {
                fileIn.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private void modifyElementFile(String redirectUrl, String appMenuDisplayString, File elementFile)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(elementFile);
        doc.getDocumentElement().normalize();
        doc.setXmlStandalone(true);
        if (redirectUrl != null) {
            NodeList urlActionList = doc.getElementsByTagName("UrlAction");
            Node urlActionNode = urlActionList.item(0);
            Element urlActionElement = (Element)urlActionNode;
            String url = urlActionElement.getAttribute("Url");
            urlActionElement.setAttribute("Url", redirectUrl);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Existing URL is {}, changed to {}", url, urlActionElement.getAttribute("Url"));
            }
            NodeList customActionList = doc.getElementsByTagName("CustomAction");
            Node customActionNode = customActionList.item(0);
            Element customActionElement = (Element)customActionNode;
            String title = customActionElement.getAttribute("Title");
            customActionElement.setAttribute("Title", appMenuDisplayString);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Existing title is {}, changed to {}", title, customActionElement.getAttribute("Title"));
            }
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(elementFile);
        transformer.transform(source, result);
        LOGGER.debug("Sharepoint App File updated.");
    }

    private void modifyManifestFile(File appFile, String newClientId, String newStartPageUrl, String type)
            throws ParserConfigurationException, SAXException, IOException, TransformerException, InterruptedException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(appFile);
        doc.getDocumentElement().normalize();
        doc.setXmlStandalone(true);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Root element: {}", doc.getDocumentElement().getNodeName());
        }
        if (newClientId != null) {
            NodeList remoteWebAppList = doc.getElementsByTagName("RemoteWebApplication");
            Node remoteWebAppNode = remoteWebAppList.item(0);
            Element remoteWebAppElement = (Element)remoteWebAppNode;
            String clientId = remoteWebAppElement.getAttribute("ClientId");
            remoteWebAppElement.setAttribute("ClientId", newClientId);
            LOGGER.debug("Existing client ID is {}, changed to {}", clientId, remoteWebAppElement.getAttribute("ClientId"));
        }
        if (newStartPageUrl != null) {
            NodeList startPageList = doc.getElementsByTagName("StartPage");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("StartPage list length is {}", startPageList.getLength());
            }
            Node startPageNode = startPageList.item(0);
            Element startPageElement = (Element)startPageNode;
            String existingURL = startPageElement.getTextContent();
            startPageElement.setTextContent(newStartPageUrl);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Existing StartPage URL is '{}', changed to '{}'", existingURL, startPageElement.getTextContent());
            }
        }
        int version = 0;
        LOGGER.debug("About to change version number for SharePoint Application");
        if (type.equalsIgnoreCase(ServiceProviderType.SHAREPOINT_ONLINE_CROSSLAUNCH.name())) {
            version = findVersion(newClientId, SP_APP_VERSION_FILE);
        } else if (type.equalsIgnoreCase(ServiceProviderType.SHAREPOINT_CROSSLAUNCH.name())) {
            version = findVersion(newClientId, SP_APP_VERSION_FILE_ON_PREMISE);
        } else if (type.equalsIgnoreCase(ServiceProviderType.SHAREPOINT_ONLINE.name())) {
            version = findVersion(newClientId, SP_ONLINE_REPO_APP_VERSION_FILE);
        }
        NodeList appList = doc.getElementsByTagName("App");
        Node appNode = appList.item(0);
        Element appElement = (Element)appNode;
        String appCurrentVersion = appElement.getAttribute("Version");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The current version of the app is {}", appCurrentVersion);
        }
        String[] versionArray = appCurrentVersion.split(Pattern.quote("."));
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < versionArray.length - 1; a++) {
            sb.append(versionArray[a]);
            sb.append('.');
        }
        String versionString = sb.append(String.valueOf(version)).toString();
        appElement.setAttribute("Version", versionString);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The new app version set in the XML file is {}", appElement.getAttribute("Version"));
        }
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(appFile);
        transformer.transform(source, result);
        LOGGER.debug("Sharepoint App Manifest File updated.");
    }

    private int findVersion(String clientId, String fileName) throws InterruptedException, IOException {
        Environment environment = Environment.getInstance();
        if (environment.isUnix()) {
            fileName = "." + fileName;
        }
        File tmpFolder = new File(Environment.getInstance().getDataDir(), Constants.TEMPDIR_NAME);
        if (!tmpFolder.exists()) {
            FileUtils.mkdir(tmpFolder);
        }
        File file = new File(tmpFolder, fileName);
        LOGGER.debug("SharePoint App version file is " + file.getPath());
        //If the file is not found
        if (!file.exists()) {
            Properties prop = new Properties();
            prop.setProperty(clientId, "0");
            try (OutputStream output = new FileOutputStream(file)) {
                prop.store(output, null);
                if (!environment.isUnix()) {
                    Process hide = Runtime.getRuntime().exec("attrib +H " + file.getPath());
                    hide.waitFor();
                }
                LOGGER.debug("SharePoint App Version file created");
            }
            return 0;
        } else {//If file exists
                //Read the file
            Properties prop = new Properties();
            int newVersion = 0;
            if (!environment.isUnix()) {
                Process unhide = Runtime.getRuntime().exec("attrib -H " + file.getPath());
                unhide.waitFor();
            }
            try (FileInputStream input = new FileInputStream(file)) {
                // load a properties file
                prop.load(input);
            }
            //Check the version
            String version = prop.getProperty(clientId);
            try (OutputStream output = new FileOutputStream(file)) {
                if (version != null) {
                    newVersion = Integer.parseInt(version);
                    prop.setProperty(clientId, Integer.toString(++newVersion));
                } else {
                    prop.setProperty(clientId, Integer.toString(0));
                }
                // Update the file
                prop.store(output, null);
            }
            if (!environment.isUnix()) {
                Process hide = Runtime.getRuntime().exec("attrib +H " + file.getPath());
                hide.waitFor();
            }
            //If update is successful, return the correct version else return 0
            return newVersion;
        }
    }
}

/**
 *
 */
package com.nextlabs.rms.viewer.servlets;

import com.bluejungle.destiny.services.management.types.CommProfileDTO;
import com.nextlabs.common.Environment;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.shared.LocalizationUtil;
import com.nextlabs.rms.viewer.cache.ViewerCacheManager;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.config.ViewerInitializationManager;
import com.nextlabs.rms.viewer.config.ViewerNightlyMaintenanceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.axis2.databinding.types.URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsStatus;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.xml.sax.InputSource;

/**
 * @author nnallagatla
 *
 */
public class ViewerContextListener implements ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    public ViewerContextListener() {
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        ViewerCacheManager.getInstance().shutdown();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
        if (!setFIPSContext()) {
            LOGGER.fatal("FIPS compliant provider not initialized/available");
            System.exit(-1);
        }

        try {
            RestClient.disableSSLCertificateChecking();
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error occurred while disabling SSL certificate checking.", e);
        }
        String webDir = sce.getServletContext().getRealPath("/");
        ViewerInitializationManager initMgr = new ViewerInitializationManager();
        try {
            File baseDir = Environment.getInstance().getSharedConfDir();
            initMgr.initViewer(webDir, baseDir);
            ViewerConfigManager viewerConfigManager = ViewerConfigManager.getInstance();
            if (StringUtils.hasText(viewerConfigManager.getStringProperty(ViewerConfigManager.ICENET_URL)) && !viewerConfigManager.getBooleanProperty(ViewerConfigManager.PREFER_EXTERNAL_PDP)) {
                configureEmbeddedPdp(viewerConfigManager.getStringProperty(ViewerConfigManager.ICENET_URL));
            } else {
                LOGGER.warn("ICENet URL is not configured.");
            }
            //TODO: Deploy RMS and VIEWER in separate tomcats to avoid class loading issues. (https://stackoverflow.com/questions/38142719/infinispan-unique-cache-manager-for-deployed-web-applications)
            boolean isCacheServerMode = viewerConfigManager.getBooleanProperty(ViewerConfigManager.CACHING_MODE_SERVER);
            String cacheHost = viewerConfigManager.getStringProperty(ViewerConfigManager.CACHING_SERVER_HOSTNAME);
            cacheHost = StringUtils.hasText(cacheHost) ? cacheHost : "cacheserver";
            int connectorPort = viewerConfigManager.getIntProperty(ViewerConfigManager.CACHING_SERVER_CLIENT_PORT);
            connectorPort = connectorPort == -1 ? 8080 : connectorPort;
            RMSCacheManager.init(baseDir, new File(Environment.getInstance().getSharedDir(), "cachestore/embedded/viewer"), isCacheServerMode, cacheHost, connectorPort);
            TokenGroupCacheManager.init(Collections.emptyList());
            sce.getServletContext().setAttribute("version", com.nextlabs.common.BuildConfig.VERSION);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
        EvaluationAdapterFactory.configureExternalPdp(ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.PREFER_EXTERNAL_PDP), ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.CC_CONSOLE_URL), ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.CC_OAUTH_CLIENT_ID), ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.CC_OAUTH_CLIENT_SECRET), ViewerConfigManager.getInstance().getStringProperty(ViewerConfigManager.PDP_POLICY_CONTROLLER_URL));
        LocalizationUtil.setBasenameResourceBundles(sce.getServletContext(), "com.nextlabs.rms.viewer.locale.ViewerMessages");
        ViewerNightlyMaintenanceManager nmMgr = new ViewerNightlyMaintenanceManager();
        nmMgr.scheduleNightlyMaintenance();
    }

    private void configureEmbeddedPdp(String icenetUrl)
            throws IOException, MappingException, MarshalException, ValidationException {
        Mapping unmarshalMapping = new Mapping();
        File mappingFile = Paths.get(Environment.getInstance().getDataDir().getAbsolutePath(), "javapc", "config", "mapping.xml").toFile();
        unmarshalMapping.loadMapping(new InputSource(Files.newBufferedReader(mappingFile.toPath(), StandardCharsets.UTF_8)));
        Unmarshaller unmar = new Unmarshaller(unmarshalMapping);
        File commProfileFile = Paths.get(Environment.getInstance().getDataDir().getAbsolutePath(), "javapc", "config", "commprofile.xml").toFile();
        CommProfileDTO commProfileDTO = (CommProfileDTO)unmar.unmarshal(Files.newBufferedReader(commProfileFile.toPath(), StandardCharsets.UTF_8));
        String icenetServiceUrl = icenetUrl.endsWith("/") ? icenetUrl + "dabs" : icenetUrl + "/dabs";
        commProfileDTO.setDABSLocation(new URI(icenetServiceUrl));

        OutputStream outputStream = new FileOutputStream(commProfileFile);
        Marshaller marshaller = new Marshaller(new OutputStreamWriter(outputStream, "UTF-8"));
        Mapping marshalMapping = new Mapping();
        marshalMapping.loadMapping(new InputSource(Files.newBufferedReader(mappingFile.toPath(), StandardCharsets.UTF_8)));
        marshaller.setMapping(marshalMapping);
        marshaller.marshal(commProfileDTO);
        EvaluationAdapterFactory.configureEmbeddedPdp(LOGGER, icenetUrl);
    }

    private boolean setFIPSContext() {
        boolean approvedOnly = Boolean.parseBoolean(System.getProperty("org.bouncycastle.fips.approved_only"));
        LOGGER.debug("JVM property org.bouncycastle.fips.approved_only: " + approvedOnly);
        if (!approvedOnly) {
            System.setProperty("org.bouncycastle.fips.approved_only", "true");
            approvedOnly = Boolean.parseBoolean(System.getProperty("org.bouncycastle.fips.approved_only"));
            // Note: to be effective this property needs to be set for the JVM or before the CryptoServicesRegistrar class has loaded.
            LOGGER.info("Set JVM property org.bouncycastle.fips.approved_only: " + approvedOnly);
        }
        boolean approvedMode = CryptoServicesRegistrar.isInApprovedOnlyMode(); // used to check if mode of operation is approved.
        LOGGER.debug("CryptoServicesRegistrar isInApprovedOnlyMode: " + approvedMode);
        if (!approvedMode) {
            CryptoServicesRegistrar.setApprovedOnlyMode(true);
            approvedMode = CryptoServicesRegistrar.isInApprovedOnlyMode();
            LOGGER.info("Set CryptoServicesRegistrar isInApprovedOnlyMode: " + approvedMode);
        }
        boolean fipsReady = FipsStatus.isReady();
        LOGGER.info("FipsStatus isReady: " + fipsReady); // self tests (statics called by JRE) passed and module is ready only
        LOGGER.info("List of installed Security Providers in order of preference: " + Arrays.toString(Security.getProviders()));
        return approvedOnly && approvedMode && fipsReady;
    }
}

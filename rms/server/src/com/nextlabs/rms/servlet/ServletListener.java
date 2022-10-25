package com.nextlabs.rms.servlet;

import com.bluejungle.destiny.services.management.types.ActivityJournalingSettingsDTO;
import com.bluejungle.destiny.services.management.types.CommProfileDTO;
import com.bluejungle.domain.types.ActionTypeDTOList;
import com.nextlabs.common.Environment;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cache.RMSCacheManager;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cc.exception.ControlCentreNotSupportedException;
import com.nextlabs.rms.cc.exception.DelegationPolicyException;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.config.MyDriveSizeCalculatorManager;
import com.nextlabs.rms.config.RMSNightlyMaintenanceManager;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.exception.SystemBucketException;
import com.nextlabs.rms.exception.TenantException;
import com.nextlabs.rms.hibernate.HibernateUtils;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.idp.IdpManager;
import com.nextlabs.rms.mail.SmtpConfig;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryManager;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.Heartbeat;
import com.nextlabs.rms.rs.TenantMgmt;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.services.manager.LockManager;
import com.nextlabs.rms.services.manager.TaskManager;
import com.nextlabs.rms.shared.LocalizationUtil;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.RabbitMQUtil;
import com.nextlabs.rms.task.ProjectCleanupManager;
import com.nextlabs.rms.task.UserSessionCleanup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.axis2.databinding.types.URI;
import org.apache.commons.io.FileUtils;
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

public class ServletListener implements ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    private static final String RESOURCE = "RESOURCE_DEFAULT_TENANT_CREATION";

    private static final String PS_RESOURCE_BOOTSTRAP = "RESOURCE_POLICY_STUDIO_BOOTSTRAP";

    private static final String PORTAL_JOURNALING_SETTINGS_NAME = "PORTAL:Portal Enforcer Default Profile";

    public ServletListener() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
        if (!setFIPSContext()) {
            LOGGER.fatal("FIPS compliant provider not initialized/available");
            System.exit(-1);
        }
        FileInputStream fis = null;
        try {
            RestClient.disableSSLCertificateChecking();
            ServletContext context = sce.getServletContext();
            File realPath = new File(context.getRealPath(""));
            File baseDir = Environment.getInstance().getSharedConfDir();

            Properties prop = new Properties();
            File file = new File(baseDir, "admin.properties");
            fis = new FileInputStream(file);
            prop.load(fis);
            prop = PropertiesFileUtils.decryptPropertyValues(prop);
            Enumeration<String> en = context.getInitParameterNames();
            while (en.hasMoreElements()) {
                String key = en.nextElement();
                if (!prop.contains(key)) {
                    // provide default values
                    prop.setProperty(key, context.getInitParameter(key));
                }
            }

            Properties hibernateProp = new Properties();
            SmtpConfig smtpConfig = SmtpConfig.getIntance();
            WebConfig webConfig = WebConfig.getInstance();
            Environment environment = Environment.getInstance();
            webConfig.setTmpDir(new File(environment.getDataDir(), "rms/temp"));
            webConfig.setCommonSharedTempDir(new File(Environment.getInstance().getSharedTempDir(), "common"));
            webConfig.setRmsSharedTempDir(new File(Environment.getInstance().getSharedTempDir(), "rms"));
            webConfig.setWebBaseDir(realPath);
            webConfig.setConfigDir(baseDir);
            for (String key : prop.stringPropertyNames()) {
                if (key.startsWith("hibernate.")) {
                    hibernateProp.put(key.substring(10), prop.getProperty(key));
                } else if (key.startsWith("web.")) {
                    webConfig.setProperty(key.substring(4), prop.getProperty(key));
                } else if (key.startsWith("smtp.")) {
                    smtpConfig.setProperty(key.substring(5), prop.getProperty(key));
                }
            }
            if (WebConfig.getInstance().getIntProperty(WebConfig.RESTCLIENT_CONNECTION_TIMEOUT) != -1) {
                RestClient.setConnectionTimeout(WebConfig.getInstance().getIntProperty(WebConfig.RESTCLIENT_CONNECTION_TIMEOUT));
                LOGGER.debug("Rest Client connection timeout: {}", RestClient.getConnectionTimeout());
            }
            if (WebConfig.getInstance().getIntProperty(WebConfig.RESTCLIENT_READ_TIMEOUT) != -1) {
                RestClient.setReadTimeout(WebConfig.getInstance().getIntProperty(WebConfig.RESTCLIENT_READ_TIMEOUT));
                LOGGER.debug("Rest Client read timeout: {}", RestClient.getReadTimeout());
            }
            try {
                if (!RabbitMQUtil.checkUserExists(webConfig.getProperty(WebConfig.RABBITMQ_USER)) && !RabbitMQUtil.addAUser(webConfig.getProperty(WebConfig.RABBITMQ_USER), webConfig.getProperty(WebConfig.RABBITMQ_PASSWORD))) {
                    LOGGER.error("RabbitMQ user creation failed.");
                }
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Error occurring when creating RabbitMQ user.", e);
            }
            HibernateUtils.bootstrap(hibernateProp, true);
            String tenantAdmin = webConfig.getProperty(WebConfig.PUBLIC_TENANT_ADMIN);
            String publicTenant = webConfig.getProperty(WebConfig.PUBLIC_TENANT);
            boolean lock = LockManager.getInstance().acquireLock(RESOURCE, TimeUnit.MINUTES.toMillis(5));
            if (lock) {
                try {
                    TenantMgmt tenantMgmt = new TenantMgmt();
                    Tenant tenant = AbstractLogin.getDefaultTenant();
                    if (tenant == null) {
                        try {
                            if (!StringUtils.hasText(publicTenant)) {
                                publicTenant = UUID.randomUUID().toString();
                                try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file.getAbsoluteFile(), true), "UTF-8")) {
                                    fw.write(System.lineSeparator());
                                    try (BufferedWriter bw = new BufferedWriter(fw)) {
                                        bw.append("web." + WebConfig.PUBLIC_TENANT + "=" + publicTenant);
                                    }
                                }
                            }
                            if (!StringUtils.hasText(tenantAdmin)) {
                                LOGGER.warn("Tenant admin not configured.");
                                tenantAdmin = "";
                            }
                            tenantMgmt.createDefaultTenant(publicTenant, tenantAdmin);
                            webConfig.setProperty(WebConfig.PUBLIC_TENANT, publicTenant);
                            WebConfig config = WebConfig.getInstance();
                            File defaultWatermark = new File(Environment.getInstance().getDataDir() + File.separator + Heartbeat.WATERMARK_CONFIG);
                            File tenantFolder = new File(config.getConfigDir() + File.separator + "tenants" + File.separator + publicTenant);
                            if (!tenantFolder.exists()) {
                                boolean folderCreation = tenantFolder.mkdirs();
                                if (!folderCreation) {
                                    LOGGER.error("Failed to create folders for watermark config.");
                                }
                            }
                            FileUtils.copyFile(defaultWatermark, new File(tenantFolder.getPath() + File.separator + Heartbeat.WATERMARK_CONFIG));
                        } catch (TenantException | IOException e) {
                            LOGGER.error(new StringBuilder().append("Error occurred during default tenant creation").append(e.getMessage()).toString(), e);
                            System.exit(-1);
                        }
                    } else {
                        try {
                            if (StringUtils.hasText(tenantAdmin)) {
                                String existingAdmins = tenant.getAdmin();
                                HashSet<String> existingAdminSet;
                                if (existingAdmins == null || "".equalsIgnoreCase(existingAdmins.trim())) {
                                    existingAdminSet = new HashSet<>();
                                } else {
                                    existingAdminSet = new HashSet<>(Arrays.asList(existingAdmins.toLowerCase().split(",")));
                                }
                                StringTokenizer st = new StringTokenizer(tenantAdmin.toLowerCase(), ",");
                                if (existingAdminSet.isEmpty()) {
                                    LOGGER.info("Creating new Tenant Admins");
                                    tenantMgmt.updateTenantAdmin(publicTenant, tenantAdmin);
                                } else {
                                    LOGGER.info("Tenant Admins already exists. Merging existing admin in DB and admin.properties:");
                                    while (st.hasMoreTokens()) {
                                        existingAdminSet.add(st.nextToken());
                                    }
                                    tenantMgmt.updateTenantAdmin(publicTenant, String.join(",", existingAdminSet));
                                }
                            }
                        } catch (TenantException e) {
                            LOGGER.error("Error occurred while updating tenant admin" + e.getMessage(), e);
                        }
                    }
                } finally {
                    LockManager.getInstance().releaseLock(RESOURCE);
                }
            }

            if (AbstractLogin.getDefaultTenant() == null) {
                throw new TenantException("Exiting as default tenant not found or not created; please ensure consistency across router database and config properties file(s)");
            }

            boolean isCacheServerMode = WebConfig.getInstance().getBooleanProperty(WebConfig.CACHING_MODE_SERVER);
            String cacheHost = WebConfig.getInstance().getProperty(WebConfig.CACHING_SERVER_HOSTNAME, "cacheserver");
            int connectorPort = WebConfig.getInstance().getIntProperty(WebConfig.CACHING_SERVER_CLIENT_PORT);
            connectorPort = connectorPort == -1 ? 8080 : connectorPort;
            RMSCacheManager.init(webConfig.getConfigDir(), new File(environment.getSharedDir(), "cachestore/embedded/rms"), isCacheServerMode, cacheHost, connectorPort);
            IdpManager.bootstrap();
            DefaultRepositoryManager.getInstance().updateStorageProvider(AbstractLogin.getDefaultTenant().getId());

            if (StringUtils.hasText(webConfig.getProperty(WebConfig.ICENET_URL)) && !WebConfig.getInstance().getBooleanProperty(WebConfig.PREFER_EXTERNAL_PDP)) {
                configureEmbeddedPdp(webConfig.getProperty(WebConfig.ICENET_URL));
            } else {
                LOGGER.warn("ICENet URL is not configured.");
            }
            EvaluationAdapterFactory.configureExternalPdp(WebConfig.getInstance().getBooleanProperty(WebConfig.PREFER_EXTERNAL_PDP), WebConfig.getInstance().getProperty(WebConfig.CC_CONSOLE_URL), WebConfig.getInstance().getProperty(WebConfig.CC_OAUTH_CLIENT_ID), WebConfig.getInstance().getProperty(WebConfig.CC_OAUTH_CLIENT_SECRET), WebConfig.getInstance().getProperty(WebConfig.PDP_POLICY_CONTROLLER_URL));

            if (StringUtils.hasText(webConfig.getProperty(WebConfig.CC_CONSOLE_URL))) {
                initControlCenter();
                TokenGroupCacheManager.init(TokenGroupManager.getTokenGroupResourceTypeMappings());
                ControlCenterManager.createDefaultTenantPolicyModel(Constants.TokenGroupType.TOKENGROUP_TENANT);
                TenantMgmt tenantMgmt = new TenantMgmt();
                List<Tenant> tenantList = tenantMgmt.getTenantList();
                try {
                    if (!tenantList.isEmpty()) {
                        for (Tenant tenant : tenantList) {
                            tenantMgmt.createSystemBucket(tenant, true);
                        }
                    }
                } catch (SystemBucketException e) {
                    LOGGER.error("Error occurred while creating system project", e);
                }
            }

            LocalizationUtil.setBasenameResourceBundles(sce.getServletContext(), "com.nextlabs.rms.locale.RMSMessages");
            sce.getServletContext().setAttribute("version", com.nextlabs.common.BuildConfig.VERSION);

            ScheduledExecutorService sessionExecutor = Executors.newSingleThreadScheduledExecutor();
            sessionExecutor.scheduleAtFixedRate(new UserSessionCleanup(), 0, 6, TimeUnit.HOURS);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            IOUtils.closeQuietly(fis);
            System.exit(-1);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            IOUtils.closeQuietly(fis);
            System.exit(-1);
        } finally {
            IOUtils.closeQuietly(fis);
        }
        RMSNightlyMaintenanceManager nmMgr = new RMSNightlyMaintenanceManager();
        nmMgr.scheduleNightlyMaintenance();
        MyDriveSizeCalculatorManager dsMgr = new MyDriveSizeCalculatorManager();
        dsMgr.scheduleReadDefaultStorage();
        ProjectCleanupManager prMgr = new ProjectCleanupManager();
        prMgr.scheduleInvitationCleanup();
    }

    private void configureEmbeddedPdp(String icenetUrl)
            throws IOException, MappingException, MarshalException, ValidationException {
        Mapping unmarshalMapping = new Mapping();
        File mappingFile = Paths.get(Environment.getInstance().getDataDir().getAbsolutePath(), "javapc", "config", "mapping.xml").toFile();
        Reader reader = new InputStreamReader(new FileInputStream(mappingFile), StandardCharsets.UTF_8);
        unmarshalMapping.loadMapping(new InputSource(reader));
        Unmarshaller unmar = new Unmarshaller(unmarshalMapping);
        File commProfileFile = Paths.get(Environment.getInstance().getDataDir().getAbsolutePath(), "javapc", "config", "commprofile.xml").toFile();
        Reader commProfileReader = new InputStreamReader(new FileInputStream(commProfileFile), StandardCharsets.UTF_8);
        CommProfileDTO commProfileDTO = (CommProfileDTO)unmar.unmarshal(new InputSource(commProfileReader));
        String icenetServiceUrl = icenetUrl.endsWith("/") ? icenetUrl + "dabs" : icenetUrl + "/dabs";
        commProfileDTO.setDABSLocation(new URI(icenetServiceUrl));
        ActivityJournalingSettingsDTO journalingSettingsDTO = new ActivityJournalingSettingsDTO();
        journalingSettingsDTO.setName(PORTAL_JOURNALING_SETTINGS_NAME);
        journalingSettingsDTO.setLoggedActivities(new ActionTypeDTOList());
        commProfileDTO.setCustomActivityJournalingSettings(journalingSettingsDTO);

        OutputStream outputStream = new FileOutputStream(commProfileFile);
        Marshaller marshaller = new Marshaller(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        Mapping marshalMapping = new Mapping();
        reader = new InputStreamReader(new FileInputStream(mappingFile), StandardCharsets.UTF_8);
        marshalMapping.loadMapping(new InputSource(reader));
        marshaller.setMapping(marshalMapping);
        marshaller.marshal(commProfileDTO);
        EvaluationAdapterFactory.configureEmbeddedPdp(LOGGER, icenetUrl);
    }

    private void initControlCenter() {
        Date date = TaskManager.getInstance().getLastSuccessfulUpdateTime(PS_RESOURCE_BOOTSTRAP);
        if (date == null) {
            boolean lock = LockManager.getInstance().acquireLock(PS_RESOURCE_BOOTSTRAP, TimeUnit.MINUTES.toMillis(5));
            if (lock) {
                try {
                    ControlCenterManager.bootstrap();
                    TaskManager.getInstance().updateSuccess(PS_RESOURCE_BOOTSTRAP);
                } catch (ControlCenterServiceException | ControlCenterRestClientException
                        | ControlCentreNotSupportedException e) {
                    TaskManager.getInstance().updateFail(PS_RESOURCE_BOOTSTRAP);
                    LOGGER.error("Error occurred while bootstrapping Policy Studio for the first time", e);
                } catch (DelegationPolicyException e) {
                    LOGGER.info(ControlCenterManager.getControlCenterVersion().toString(), e.getMessage());
                } finally {
                    LockManager.getInstance().releaseLock(PS_RESOURCE_BOOTSTRAP);
                }
            }
        }
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

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        HibernateUtils.shutdown();
        RMSCacheManager.getInstance().shutdown();
    }

}

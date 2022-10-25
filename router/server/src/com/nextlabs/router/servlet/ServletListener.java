package com.nextlabs.router.servlet;

import com.nextlabs.common.Environment;
import com.nextlabs.common.cli.CommandParser;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.router.Config;
import com.nextlabs.router.cli.InitAction;
import com.nextlabs.router.hibernate.HibernateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsStatus;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

public class ServletListener implements ServletContextListener {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

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
            ServletContext context = sce.getServletContext();
            File realPath = new File(context.getRealPath(""));
            File baseDir = Environment.getInstance().getSharedConfDir();

            Properties prop = new Properties();
            File file = new File(baseDir, "router.properties");
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
            WebConfig webConfig = WebConfig.getInstance();
            webConfig.setWebBaseDir(realPath);
            webConfig.setConfigDir(baseDir);
            for (String key : prop.stringPropertyNames()) {
                if (key.startsWith("hibernate.")) {
                    hibernateProp.put(key.substring(10), prop.getProperty(key));
                } else if (key.startsWith("web.")) {
                    webConfig.setProperty(key.substring(4), prop.getProperty(key));
                }
            }

            HibernateUtils.bootstrap(hibernateProp);

            String cmdLine = prop.getProperty("router.cli", "--inWebContainer");

            CommandParser<Config> parser = new CommandParser<Config>(Config.class);
            String[] args = cmdLine.split(" ");
            Config config = parser.parse(args);
            config.setConfDir(baseDir);

            InitAction init = new InitAction();
            init.execute(config, args);
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
    }
}

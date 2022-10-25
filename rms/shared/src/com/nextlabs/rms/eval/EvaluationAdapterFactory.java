package com.nextlabs.rms.eval;

import com.bluejungle.destiny.agent.pdpapi.PDPSDK;
import com.nextlabs.common.Environment;
import com.nextlabs.common.util.StringUtils;

import java.io.File;

import org.apache.logging.log4j.Logger;

public final class EvaluationAdapterFactory {

    private static final EvaluationAdapterFactory INSTANCE = new EvaluationAdapterFactory();
    private static final Object LOCK = new Object();
    private static boolean preferExternalPDP;
    private static String consoleUrl;
    private static String pdpUrl;
    private static String clientId;
    private static String clientSecret;
    private static boolean initialized;

    private IEvalAdapter adapter;

    private EvaluationAdapterFactory() {
    }

    public static EvaluationAdapterFactory getInstance() {
        return INSTANCE;
    }

    public static void configureExternalPdp(boolean preferExternalPDP, String consoleUrl, String clientId,
        String clientSecret,
        String pdpUrl) {
        if (preferExternalPDP && StringUtils.hasText(consoleUrl) && StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)) {
            EvaluationAdapterFactory.preferExternalPDP = true;
            EvaluationAdapterFactory.consoleUrl = consoleUrl;
            EvaluationAdapterFactory.clientId = clientId;
            EvaluationAdapterFactory.clientSecret = clientSecret;
            EvaluationAdapterFactory.pdpUrl = pdpUrl;
            initialized = true;
        }
    }

    public static void configureEmbeddedPdp(Logger logger, String icenetUrl) {
        if (!StringUtils.hasText(icenetUrl)) {
            logger.warn("ICENet URL is not configured. JavaPC won't be initialized.");
            return;
        }
        synchronized (LOCK) {
            File f = new File(Environment.getInstance().getDataDir(), "javapc");
            if (f.exists() && f.isDirectory()) {
                new PDPInitThread(f.getAbsolutePath(), logger).start();
            } else {
                logger.error("Embedded JavaPC folder is not found: {}. Terminating application.", f.getAbsolutePath());
                System.exit(-1);
            }
        }
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public IEvalAdapter getAdapter() {
        if (adapter == null) {
            if (preferExternalPDP) {
                adapter = new RestPDPEvalAdapter(consoleUrl, clientId, clientSecret, pdpUrl);
            } else {
                adapter = new EmbeddedPDPEvalAdapter();
            }
        }
        return adapter;
    }

    private static final class PDPInitThread extends Thread {

        private String dpcPath;
        private Logger logger;

        public PDPInitThread(String dpcPath, Logger logger) {
            this.dpcPath = dpcPath;
            this.logger = logger;
        }

        public void run() {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                logger.info("Initializing PDPSDK ...");
                PDPSDK.initializePDP(dpcPath);
            } catch (Throwable e) {
                logger.error("Error occured while initializing PDP: " + e.getMessage(), e);
            } finally {
                if (!Thread.currentThread().getContextClassLoader().equals(originalClassLoader)) {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }
        }
    }
}

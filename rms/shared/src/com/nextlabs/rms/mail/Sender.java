package com.nextlabs.rms.mail;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.shared.LogConstants;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Sender implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static Sender sender = new Sender();

    private ConcurrentLinkedQueue<Mail> queue;
    private AtomicBoolean running;

    private Sender() {
        queue = new ConcurrentLinkedQueue<Mail>();
        running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        try {
            SimpleSmtp smtp = new SimpleSmtp();
            while (!queue.isEmpty()) {
                Mail mail = null;
                for (int i = 0; i < 60; ++i) {
                    mail = queue.poll();
                    if (mail != null) {
                        break;
                    }
                    Thread.sleep(2000);
                }
                if (mail != null) {
                    if (!smtp.connect()) {
                        return;
                    }
                    smtp.send(mail);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Sent the message.");
                    }
                    smtp.disconnect();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Dropped the connection.");
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Send mail thread is interrupted.");
            Thread.currentThread().interrupt(); //"InterruptedException" should not be ignored (Sonar)
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            running.set(false);
        }
    }

    public static void send(Properties prop, String templateName, Locale locale) {
        prop.setProperty(Mail.EMAIL_DOMAIN_NAME, WebConfig.getInstance().getProperty(WebConfig.EMAIL_DOMAIN_NAME, "skydrm.com"));
        prop.setProperty(Mail.EMAIL_SENDER_NAME, WebConfig.getInstance().getProperty(WebConfig.EMAIL_SENDER_NAME, "no-reply"));
        sender.add(new Mail(prop, templateName, locale));
    }

    private void add(Mail mail) {
        queue.add(mail);
        if (!running.getAndSet(true)) {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }
}

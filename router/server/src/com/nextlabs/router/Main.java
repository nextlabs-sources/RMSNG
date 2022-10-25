package com.nextlabs.router;

import com.nextlabs.common.Environment;
import com.nextlabs.common.cli.CommandParser;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.router.cli.Action;
import com.nextlabs.router.hibernate.HibernateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Main {

    private static final String DEFAULT_LOG_NAME = "com.nextlabs.router";
    private static final Logger LOGGER = LogManager.getLogger(DEFAULT_LOG_NAME);

    private Main() {
    }

    public static void main(String[] args) {
        File home = new File(System.getProperty("user.home"), ".router");
        if (!home.exists() && !home.mkdir()) {
            System.out.println("Can't create log folder."); // NOPMD
            return;
        }

        CommandParser<Config> p = new CommandParser<Config>(Config.class);
        FileInputStream fis = null;
        try {
            File baseDir = Environment.getInstance().getSharedConfDir();
            LOGGER.debug("Base dir: {}", baseDir.getAbsolutePath());

            Properties hibernateProp = new Properties();
            Properties prop = new Properties();
            File file = new File(baseDir, "router.properties");
            if (file.exists()) {
                fis = new FileInputStream(new File(baseDir, "router.properties"));
                prop.load(fis);
                prop = PropertiesFileUtils.decryptPropertyValues(prop);
            } else {
                LOGGER.error("File not found: {}", file.getAbsolutePath());
                return;
            }

            for (String key : prop.stringPropertyNames()) {
                if (key.startsWith("hibernate.")) {
                    hibernateProp.put(key.substring(10), prop.getProperty(key));
                }
            }

            String[] arguments;
            if (args.length > 0) {
                arguments = args;
                LOGGER.debug("Command line args: {}", Arrays.toString(args));
            } else {
                String cmdLine = prop.getProperty("router.cli", "");
                arguments = cmdLine.split(" ");
                LOGGER.debug("router.cli: {}", cmdLine);
            }

            Config config = p.parse(arguments);
            config.setConfDir(baseDir);

            HibernateUtils.bootstrap(hibernateProp);

            String cmd = config.getCmd();
            Class<?> c = Class.forName("com.nextlabs.router.cli." + cmd + "Action");
            Action action = (Action)c.newInstance();
            action.execute(config, arguments);
        } catch (ClassNotFoundException e) {
            p.printHelp("java com.nextlabs.router.Main [options]");
        } catch (ParseException e) {
            p.printHelp("java com.nextlabs.router.Main [options]");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }
}

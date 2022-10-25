package com.nextlabs.rms.cli;

import com.nextlabs.common.cli.CLI;
import com.nextlabs.common.cli.CommandParser;
import com.nextlabs.common.cli.Parameter;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.PropertiesFileUtils;
import com.nextlabs.common.util.RestClient;
import com.nextlabs.rms.Config;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.shared.LogConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteProjectAction implements Action {

    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void execute(Config config, String[] args) {

        try (Scanner input = new Scanner(System.in, "utf-8")) {
            System.out.print("This will delete all project related data and files. Do you want to continue? (Yes/No): "); //NOPMD
            if (!"yes".equalsIgnoreCase(input.next())) {
                System.out.print("Operation cancelled."); //NOPMD
                System.exit(0);
            }
        }
        CommandParser<DeleteConfig> p = new CommandParser<DeleteConfig>(DeleteConfig.class);
        try {
            File file = new File(config.getConfDir(), "admin.properties");
            try (InputStream fis = new FileInputStream(file)) {
                Properties prop = new Properties();
                prop.load(fis);
                prop = PropertiesFileUtils.decryptPropertyValues(prop);
                WebConfig webConfig = WebConfig.getInstance();
                for (String key : prop.stringPropertyNames()) {
                    if (key.startsWith("web.")) {
                        webConfig.setProperty(key.substring(4), prop.getProperty(key));
                    }
                }
            } catch (IOException e) {
                logger.error("Error occurred while parsing admin.properties", e);
                System.exit(-1);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(-1);
            }

            DeleteConfig delConfig = p.parse(args);
            int projectId = delConfig.getProjectId();
            RestClient.disableSSLCertificateChecking();
            ProjectService.deleteProject(projectId);
            return;
        } catch (ParseException e) {
            p.printHelp("java com.nextlabs.rms.Main --cmd=DeleteProject --projectId={projectId}");
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            System.out.println("Error occurred while deleting the project."); // NOPMD
            System.out.println(e.getMessage()); // NOPMD
        }
        System.exit(-1);
    }

    @CLI
    public static final class DeleteConfig {

        @Parameter(description = "Project ID", hasArgs = true, mandatory = true)
        private int projectId;

        public int getProjectId() {
            return projectId;
        }
    }
}

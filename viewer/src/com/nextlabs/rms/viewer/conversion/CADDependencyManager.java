package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CADDependencyManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private static final String HOOPS_DEPENDENCIES_MISSING = "#MISSING";

    public List<String> getDependencies(String assemblyFile) throws RMSException, IOException {
        if (generateDependenciesFile(assemblyFile, assemblyFile)) {
            return readDependenciesFile(assemblyFile + ".txt");
        } else {
            LOGGER.error("Error occurred while processing the file:" + FilenameUtils.getBaseName(assemblyFile));
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"));
        }
    }

    private List<String> readDependenciesFile(String dependenciesFile) throws RMSException, IOException {
        List<String> missing = new ArrayList<>();
        int assemblyTagCount = 0;
        int partsTagCount = 0;
        int missingTagCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(dependenciesFile), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.equals(HOOPS_DEPENDENCIES_MISSING, line)) {
                    missingTagCount++;
                    while ((line = br.readLine()) != null && StringUtils.hasText(line)) {
                        int winIdx = line.lastIndexOf('\\');
                        int idx = line.lastIndexOf('/');
                        int index = Math.max(winIdx, idx);
                        String missingPart = line.substring(index + 1);
                        missing.add(missingPart);
                    }
                }
            }
            if (assemblyTagCount == 0 || partsTagCount == 0 || missingTagCount == 0) {
                StringBuilder msg = new StringBuilder(50);
                msg.append("Dependencies file is not in the correct format. ");
                if (assemblyTagCount == 0) {
                    msg.append("No Assemblies Tag is found. ");
                }
                if (partsTagCount == 0) {
                    msg.append("No Parts Tag is found. ");
                }
                if (missingTagCount == 0) {
                    msg.append("No Missing Tag is found. ");
                }
                LOGGER.error(msg.toString());
                throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"));
            }
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return missing;
    }

    private boolean generateDependenciesFile(String inputPath, String destinationPath) throws RMSException {
        return new CADConverter().executeHOOPSConverter(inputPath, destinationPath, "--output_dependencies");
    }
}

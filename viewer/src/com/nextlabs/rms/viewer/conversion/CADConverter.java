package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CADConverter implements IFileConverter {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private static final String LICENSE_KEY = "4DN0ACQ05RU20fMR0CruCuN63gYz3gMOvRNz8AN9vQY_AEMKj7DFjBby5uYT8Vj32DJlCuyj1U7mvbDF0QV$CCyX8UztChMT8wz5BUj2Bg6S9EUU1CE78iV6xjb9wSEWvDZ58fmJDhmUATJ8CfaSCRf1DEjrCDQ0wwJ61Si34yeXDyB89zb2zTUW8vno9jeHBUIXAiV75fiVwznr0UYJ4gB2AU2Z9EaX1BRs0iFt3xnE4vj0xjm8DGtPj7F34yU5GrPFjEZzxUpGj7DF2y262LtKj7EHAgnrarXFjANn1E677TNoDiI_Cgvm1eMI5SvaBg3yCwbtwUM4xVjl1gEH6hYz0CAJwhV58Szb5yaQ1xrd5yBu9ijyBDZavTqIBEE05sQYzyYgj7DF2NHFjBaN3fnt9wQHwSVp0ReT1SYPvUN48TZaCyF1BhZ63SZ6DeMG0hQSxCu49SQRwViZ6eQR6jaOCCe_3DV92wFvAVfv2i2X4AQPAEmS1UJbCUj24i7bAEFa3ybw4yz$9BNwAUQYBUz62xRu4RY8BSE0xzaPAEVdwxQ77QJ2xCV41eRbBDrtAgR18iE3DBMTAEE12RYI3Fe0DyBnxzn00TeMAyEJ2guS9C38BCN83uYz5fa22C3v0fmL1Ra55Bnd0Sjs1zj98BQYwg30DC3zzRRv1g6O0Cfd5vi50yQH1iaO9wrw0if7wgUIweN5wvMI2jbd8RY2AwYOACJ2Bhrdwfnu3AF20yU0AviGwhUNvSI30Uj6drPFjFfv9gehj7DF8BazwQI5xuI8xeRmwhU5xeNkxAVlwfjoxeNm9bE68eJkvAU19hQ1weU4whI0wvm0weRnwUa8vDU48fflxBi7vRi3wQRkxEbmvRjk8fm3xRm28hNlwAZoxAMzxfm3wvjmwfi6xBi7vQQ48TQ4xDRn8uFovTQ18hM4xAZn8QYzxfflvDU09fa8wAM8whZmweY59eM3xRnp8fjmwibk8xY67Rjn9eI1vQRmwhNkweI78TY5xAEzxxI49ffnwDY1wxQ79ibmvDVlxuE38AI6xTZnwhU37Re79hY77QM18vjlxya5xTY1xxQ18ibmwQNnxuM78Ua0wxQ3wuYzwAI6wuY8xeI8xARk8hY1vQVkwuNk8Ubp8QFk9eZovTUz9hY2xRbk9hUz9hQ3wfa8wTU58AZn8xI5wRa79ibmvDY7xeFk8uI8wiblvBe8xDU4vAI58QE6vQM38xIzxRi8xhJk8hM2xTVovAM0wTU2xrEV0RM0";

    @Override
    public boolean convertFile(String inputPath, String destinationPath) throws RMSException {
        /* obtaining the file extension from destination path */
        int index = destinationPath.toLowerCase().lastIndexOf('.');
        String fileExtension = destinationPath.substring(index + 1, destinationPath.length());
        return executeHOOPSConverter(inputPath, destinationPath, "--output_" + fileExtension);
    }

    /**
     * Taken from Java 8 java.lang.Process and modified to add process parameter.
     * @param timeout
     * @param unit
     * @param process
     * @return exitValue of the process or null if it's timeout
     * @throws InterruptedException
     */
    private Integer waitFor(long timeout, TimeUnit unit, Process process) throws InterruptedException {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);
        do {
            try {
                return process.exitValue();
            } catch (IllegalThreadStateException ex) {
                if (rem > 0) {
                    Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                }
            }
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (rem > 0);
        return null;
    }

    public boolean executeHOOPSConverter(String inputPath, String destinationPath, String command) throws RMSException {
        File binLocation = null;
        boolean conversionresult = false;
        ProcessBuilder pb = null;
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Input Path: {} ; Output Path: {}", inputPath, destinationPath);
            }
            boolean runShellCommand = false;
            File converterLocation = null;

            if (!ViewerConfigManager.getInstance().isUnix()) {
                converterLocation = ViewerConfigManager.getInstance().getWinCadConverter();
            } else {
                binLocation = ViewerConfigManager.getInstance().getCadBinDir();
                converterLocation = ViewerConfigManager.getInstance().getUnixCadConverter();
                runShellCommand = true;
            }
            if (!converterLocation.exists()) {
                LOGGER.error("Converter does not exist in the specified path: {}", converterLocation);
                throw new RMSException(ViewerMessageHandler.getClientString("err.missing.viewer.package", "CAD Viewer", FileUtils.getRealFileExtension(inputPath).substring(1).toUpperCase()));
            }
            pb = new ProcessBuilder(runShellCommand ? "sh" : "", converterLocation.getAbsolutePath(), "--input", inputPath, command, destinationPath, "--license", LICENSE_KEY);
            if (binLocation != null) {
                File directory = binLocation;
                pb.directory(directory);
            }
            Process process = pb.start();
            try {
                try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String s = null;
                    while ((s = stdOut.readLine()) != null) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Converter Output : {}", s);
                        }
                    }
                }
                try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String s = null;
                    while ((s = stdOut.readLine()) != null) {
                        LOGGER.error("Converter Output : {}", s);
                    }
                }
                Integer exitValue = waitFor(7, TimeUnit.MINUTES, process);
                if (exitValue == null) {
                    LOGGER.error("Terminating the converter since it failed to convert file within 7 minutes. Input path: {}", inputPath);
                    process.destroy();
                    return false;
                }
                if (exitValue == 0) {
                    conversionresult = true;
                }
            } finally {
                IOUtils.closeQuietly(process.getOutputStream());
                IOUtils.closeQuietly(process.getInputStream());
                IOUtils.closeQuietly(process.getErrorStream());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while executing: {}", (pb != null ? pb.command() : "<unknown>"));
        }
        return conversionresult;
    }
}

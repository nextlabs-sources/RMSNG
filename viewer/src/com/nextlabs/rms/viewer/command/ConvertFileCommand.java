package com.nextlabs.rms.viewer.command;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.HTTPUtil;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.conversion.ConverterFactory;
import com.nextlabs.rms.viewer.conversion.IFileConverter;
import com.nextlabs.rms.viewer.eval.EvaluationHandler;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.servlets.LogConstants;
import com.nextlabs.rms.viewer.util.Audit;
import com.nextlabs.rms.viewer.visualization.PDFVisManager;
import com.nextlabs.rms.viewer.visualization.PDFVisManager.PDFType;
import com.nextlabs.rms.viewer.visualization.VisManagerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConvertFileCommand extends AbstractCommand {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    @Override
    public void doAction(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        File tempInput = null;
        File tempOutput = null;
        try {
            String fileName = request.getParameter("fileName");
            String toFormat = request.getParameter("toFormat");
            if (!validate(fileName)) {
                response.sendError(400, "Invalid input");
                return;
            }

            toFormat = StringUtils.hasText(toFormat) ? new StringBuilder(".").append(toFormat).toString() : ".";
            if (!VisManagerFactory.HSF_FILE_EXTN.equals(toFormat) && !VisManagerFactory.SCS_FILE_EXTN.equals(toFormat)) {
                toFormat = VisManagerFactory.HSF_FILE_EXTN;
            }

            Audit.audit(request, "Command", "ConvertFile", "ConvertFile", 0, fileName, toFormat);

            String fileNameWithoutNXL = EvaluationHandler.getFileNameWithoutNXL(fileName);
            File f = new File(fileNameWithoutNXL);
            String name = FileUtils.getBaseName(f.getName());
            String destFileName = name + toFormat;
            String ext = FileUtils.getRealFileExtension(fileNameWithoutNXL);
            tempInput = File.createTempFile("input", ext);
            tempOutput = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString() + toFormat);
            Files.copy(request.getInputStream(), tempInput.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (ViewerConfigManager.PDF_FILE_EXTN.equalsIgnoreCase(ext)) {
                try (FileInputStream tmp = new FileInputStream(tempInput)) {
                    PDFType pdfType = PDFVisManager.determinePDFType(tmp, fileNameWithoutNXL);
                    if (pdfType != PDFType.PDF_3D) {
                        response.sendError(400, "Unsupported PDF format");
                        return;
                    }
                }
            }

            IFileConverter fileConverter = ConverterFactory.getInstance().getConverter(ConverterFactory.CONVERTER_TYPE_CAD);
            boolean conversionResult = fileConverter.convertFile(tempInput.getAbsolutePath(), tempOutput.getAbsolutePath());
            if (!conversionResult) {
                LOGGER.error("Error occurred while converting the file for: " + destFileName);
                response.sendError(400, "Unable to convert file");
                return;
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", HTTPUtil.getContentDisposition(destFileName));
            response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(tempOutput.length()));
            OutputStream os = response.getOutputStream();
            Files.copy(tempOutput.toPath(), os);
            os.close();
        } catch (RuntimeException e) {
            LOGGER.error("Error in ConvertFileCommand", e);
            response.sendError(500, "Internal Server Error");
        } catch (RMSException e) {
            LOGGER.error("Error in ConvertFileCommand", e);
            response.sendError(500, "Internal Server Error");
        } finally {
            if (tempInput != null) {
                FileUtils.deleteQuietly(tempInput);
            }
            if (tempOutput != null) {
                FileUtils.deleteQuietly(tempOutput);
            }
        }
    }

    private boolean validate(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Filename is required");
            }
            return false;
        } else if (fileName.lastIndexOf('.') == -1) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invalid fileName: {}", fileName);
            }
            return false;
        }
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();
        if ("nxl".equalsIgnoreCase(ext)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("NXL files are not supported for file conversion: {}", fileName);
            }
            return false;
        } else {
            List<String> supportedExtensions = ViewerConfigManager.getInstance().getSupportedHOOPSFileFormatList();
            if (!supportedExtensions.contains('.' + ext)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unsupported CAD format: {}", fileName);
                }
                return false;
            }
        }
        return true;
    }
}

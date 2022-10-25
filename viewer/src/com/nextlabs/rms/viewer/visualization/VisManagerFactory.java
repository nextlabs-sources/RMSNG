package com.nextlabs.rms.viewer.visualization;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.UnsupportedFormatException;

import java.util.List;

public final class VisManagerFactory {

    private static final VisManagerFactory INSTANCE = new VisManagerFactory();

    public static final String SCS_FILE_EXTN = ".scs";

    public static final String HSF_FILE_EXTN = ".hsf";

    private List<String> hoopsFileFormats = ViewerConfigManager.getInstance().getSupportedHOOPSFileFormatList();

    private List<String> excelFileFormats = ViewerConfigManager.getInstance().getSupportedExcelFormatList();

    private VisManagerFactory() {
    }

    public static VisManagerFactory getInstance() {
        return INSTANCE;
    }

    public IVisManager getVisManager(String fileNameWithoutNXL) throws UnsupportedFormatException {
        boolean isUseImgForExcel = ViewerConfigManager.getInstance().getBooleanProperty(ViewerConfigManager.USE_IMG_FOR_EXCEL);
        String fileExtension = FileUtils.getRealFileExtension(fileNameWithoutNXL);
        if (excelFileFormats.contains(fileExtension) && !isUseImgForExcel) {
            return new ExcelVisManager();
        } else if (fileExtension.equalsIgnoreCase(ViewerConfigManager.PDF_FILE_EXTN)) {
            return new PDFVisManager();
        } else if (hoopsFileFormats.contains(fileExtension)) {
            return new CADVisManager();
        } else if (fileExtension.equalsIgnoreCase(ViewerConfigManager.VDS_FILE_EXTN)) {
            return new SAPVdsVisManager();
        } else if (fileExtension.equalsIgnoreCase(ViewerConfigManager.RH_FILE_EXTN)) {
            return new SAPRHVisManager();
        } else {
            List<String> supportedFileFormats = ViewerConfigManager.getInstance().getSupportedFileFormat();
            if (supportedFileFormats.contains(fileExtension)) {
                return new GenericVisManager();
            } else {
                throw new UnsupportedFormatException(fileExtension);
            }
        }
    }
}

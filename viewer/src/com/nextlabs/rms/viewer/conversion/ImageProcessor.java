package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.RMSException;
import com.nextlabs.rms.viewer.json.PrintFileUrl;
import com.nextlabs.rms.viewer.locale.ViewerMessageHandler;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ImageProcessor {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private static final String LICENSE_KEY = "RPMUH GC75V 59RF6 25V7T GGW29 XRV4J";

    private static final int RESOLUTIONFONTSIZETHRESHOLD = 2500;

    private Object docFilters;

    private static final ImageProcessor INSTANCE = new ImageProcessor();

    private String extractorOpenOptions;//="LIMITS_PAGE_COUNT=2500;GRAPHIC_DPI=120;ISYS_MAX_DOCHANDLES=1000";

    private String canvasOptions;//="WATERMARK=$USERNAME;GRAPHIC_DPI=120;ISYS_MAX_DOCHANDLES=1000";

    private Class<?> docFiltersClass;
    private Class<?> extractorClass;
    private Class<?> igrStreamClass;
    private Class<?> subFileClass;
    private Class<?> memoryStreamClass;
    private Class<?> pageClass;
    private Class<?> canvasClass;
    private Class<?> igrExceptionClass;

    private Integer igrDeviceHtml;
    private Integer igrDeviceXml;
    private Integer igrBodyAndMeta;
    private Integer igrFormatHtml;
    private Integer igrFormatImage;
    private Integer igrDeviceImagePng;
    private Integer igrDeviceImagePdf;
    private int defaultPageCount;

    private ImageProcessor() {
        initialize();
    }

    public static ImageProcessor getInstance() {
        return INSTANCE;
    }

    private void initialize() {

        try {
            ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
            String perceptiveJarPath = ViewerConfigManager.getInstance().getDocConverterDir() + File.separator + ViewerConfigManager.ISYS11DF_JAR;
            String memoryStreamJarPath = ViewerConfigManager.getInstance().getDocConverterDir() + File.separator + ViewerConfigManager.MEMORY_STREAM_JAR;
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new File(perceptiveJarPath).toURI().toURL(),
                new File(memoryStreamJarPath).toURI().toURL() }, currentThreadClassLoader);
            docFiltersClass = Class.forName("com.perceptive.documentfilters.DocumentFilters", true, urlClassLoader);
            extractorClass = Class.forName("com.perceptive.documentfilters.Extractor", true, urlClassLoader);
            igrStreamClass = Class.forName("com.perceptive.documentfilters.IGRStream", true, urlClassLoader);
            subFileClass = Class.forName("com.perceptive.documentfilters.SubFile", true, urlClassLoader);
            memoryStreamClass = Class.forName("com.nextlabs.rms.viewer.conversion.MemoryStream", true, urlClassLoader);
            pageClass = Class.forName("com.perceptive.documentfilters.Page", true, urlClassLoader);
            canvasClass = Class.forName("com.perceptive.documentfilters.Canvas", true, urlClassLoader);
            igrExceptionClass = Class.forName("com.perceptive.documentfilters.IGRException", true, urlClassLoader);

            Class<?> filtersConstantsClass = Class.forName("com.perceptive.documentfilters.isys_docfiltersConstants", true, urlClassLoader);
            igrDeviceHtml = (Integer)filtersConstantsClass.getDeclaredField("IGR_DEVICE_HTML").get(Integer.class);
            igrDeviceXml = (Integer)filtersConstantsClass.getDeclaredField("IGR_DEVICE_XML").get(Integer.class);
            igrBodyAndMeta = (Integer)filtersConstantsClass.getDeclaredField("IGR_BODY_AND_META").get(Integer.class);
            igrFormatHtml = (Integer)filtersConstantsClass.getDeclaredField("IGR_FORMAT_HTML").get(Integer.class);
            igrFormatImage = (Integer)filtersConstantsClass.getDeclaredField("IGR_FORMAT_IMAGE").get(Integer.class);
            igrDeviceImagePng = (Integer)filtersConstantsClass.getDeclaredField("IGR_DEVICE_IMAGE_PNG").get(Integer.class);
            igrDeviceImagePdf = (Integer)filtersConstantsClass.getDeclaredField("IGR_DEVICE_IMAGE_PDF").get(Integer.class);

            int imageDPI = ViewerConfigManager.getInstance().getIntProperty(ViewerConfigManager.CONVERTER_IMAGE_DPI);
            imageDPI = imageDPI < 0 ? 120 : imageDPI;
            int pageCount = ViewerConfigManager.getInstance().getIntProperty(ViewerConfigManager.CONVERTER_PAGE_LIMIT);
            defaultPageCount = pageCount < 0 ? 2500 : pageCount;

            extractorOpenOptions = createExtractorOpenOptions(imageDPI, defaultPageCount);
            canvasOptions = createCanvasOptions(imageDPI);

            docFilters = docFiltersClass.newInstance();
            Method method = docFiltersClass.getDeclaredMethod("Initialize", new Class<?>[] { String.class,
                String.class });
            method.invoke(docFilters, LICENSE_KEY, ".");
            LOGGER.info("Doc viewer jar loaded successfully");
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | InstantiationException | NoSuchFieldException
                | MalformedURLException e) {
            LOGGER.error("Error occurred while loading DocViewer jars and initializing DocumentFilters", e);
        } catch (Throwable e) {
            LOGGER.error("Error occurred while loading DocViewer jars and initializing DocumentFilters: {}", e.getMessage(), e);
        }
    }

    private String createExtractorOpenOptions(int imageDPI, int pageCount) {
        String extractorBuff = "LIMITS_PAGE_COUNT=%d;GRAPHIC_DPI=%d;ISYS_MAX_DOCHANDLES=1000;IMAGE_PROCESSOR=GDI";
        return String.format(extractorBuff, pageCount, imageDPI);
    }

    private String createCanvasOptions(int imageDPI) {
        StringBuilder builder = new StringBuilder(55);
        builder.append("GRAPHIC_DPI=");
        builder.append(imageDPI);
        builder.append(";ISYS_MAX_DOCHANDLES=1000;");
        return builder.toString();
    }

    public int getNumPages(byte[] inputFileByteArr, String fileName) throws RMSException {
        Object docExtractor = null;
        int numPages = -1;
        long start = System.currentTimeMillis();
        try {
            Method getExtractor = docFiltersClass.getDeclaredMethod("GetExtractor", byte[].class);
            docExtractor = getExtractor.invoke(docFilters, inputFileByteArr);
            Method openDocExtractor = extractorClass.getDeclaredMethod("Open", int.class, String.class);
            openDocExtractor.invoke(docExtractor, igrBodyAndMeta | igrFormatImage, extractorOpenOptions);
            Method getPageCount = extractorClass.getDeclaredMethod("GetPageCount");
            numPages = (int)getPageCount.invoke(docExtractor);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Number of pages in file (file: {}): {}", fileName, numPages);
            }
            if (numPages <= 0) {
                handlePerceptiveGeneralException(fileName);
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException
                | IllegalArgumentException e) {
            LOGGER.error("Error occurred while getting page number: {}", e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable t = getRootCause(e);
            LOGGER.error("InvocationTargetException occurred while getting page number.", t);
            handleInvocationTargetException(t, fileName);
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Time taken to get total pages (file: {}): {} ms", fileName, (System.currentTimeMillis() - start));
            }
            try {
                if (docExtractor != null) {
                    Method close = extractorClass.getDeclaredMethod("Close");
                    close.invoke(docExtractor);
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                LOGGER.error("Error occured while closing docExtractor", e);
            } catch (Exception e) {
                LOGGER.error("Error occured while closing docExtractor", e);
            }
        }
        return numPages;
    }

    private Throwable getRootCause(Throwable e) {
        Throwable root = e;
        Throwable ex = null;
        while ((ex = e.getCause()) != null) {
            root = ex;
            e = ex;
        }
        return root;
    }

    private void handleInvocationTargetException(Throwable t, String fileName) throws RMSException {
        Exception err = (Exception)t;
        try {
            if (igrExceptionClass.isInstance(err)) {
                LOGGER.error("Error occurred while getting number of pages in file.", err);
                Method getErrorCode = igrExceptionClass.getDeclaredMethod("getErrorCode");
                int errorCode = (int)getErrorCode.invoke(err);
                if (errorCode == 4 || errorCode == 17) { //IGR_E_NOT_READABLE, IGR_E_FILE_CORRUPT
                    throw new RMSException(ViewerMessageHandler.getClientString("err.corrupted.file"), err);
                } else if (errorCode == 5) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.pass.protected.file"), err);
                } else if (errorCode == 12) {
                    throw new RMSException(ViewerMessageHandler.getClientString("err.file.empty"), err);
                } else {
                    handlePerceptiveGeneralException(fileName);
                }
            } else {
                LOGGER.error("Error occurred while getting number of pages in file.", err);
                handlePerceptiveGeneralException(fileName);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e1) {
            LOGGER.error("Error occurred while processing an exception", e1);
            handlePerceptiveGeneralException(fileName);
        }
    }

    private void handlePerceptiveGeneralException(String fileName) throws RMSException {
        List<String> fileFormats = ViewerConfigManager.getInstance().getSupportedFileFormat();
        String currentFileExt = FileUtils.getRealFileExtension(fileName);
        if (fileFormats.contains(currentFileExt)) {
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"));
        } else {
            throw new RMSException(ViewerMessageHandler.getClientString("err.unsupported"));
        }
    }

    public boolean convertFileToHTML(byte[] inputFileByteArr, String folderPath, String fileName) throws RMSException {
        boolean result = false;
        Object docExtractor = null;
        try {
            Method getExtractor = docFiltersClass.getDeclaredMethod("GetExtractor", byte[].class);
            docExtractor = getExtractor.invoke(docFilters, inputFileByteArr);
            Method openDocExtractor = extractorClass.getDeclaredMethod("Open", int.class, String.class);
            openDocExtractor.invoke(docExtractor, igrDeviceHtml | igrDeviceXml | igrBodyAndMeta | igrFormatHtml, "IMAGES=Yes;" + extractorOpenOptions);
            Method getFirstImage = extractorClass.getDeclaredMethod("GetFirstImage");
            Object file = getFirstImage.invoke(docExtractor);
            Method copyTo = subFileClass.getDeclaredMethod("CopyTo", String.class);
            Method getId = subFileClass.getDeclaredMethod("getID");
            Method close = extractorClass.getDeclaredMethod("Close");
            while (file != null) {
                try {
                    copyTo.invoke(file, folderPath + File.separator + getId.invoke(file));
                } finally {
                    close.invoke(file);
                }
                Method getNextImage = extractorClass.getMethod("GetNextImage");
                file = getNextImage.invoke(docExtractor);
            }
            Method saveTo = extractorClass.getDeclaredMethod("SaveTo", String.class);
            saveTo.invoke(docExtractor, folderPath + File.separator + fileName);
            result = true;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
            LOGGER.error("Error processing file: {}", folderPath + File.separator + fileName, e);
            return false;
        } catch (InvocationTargetException e) {
            Throwable t = getRootCause(e);
            LOGGER.error("InvocationTargetException occurred while getting page number.", t);
            handleInvocationTargetException(t, fileName);
        } finally {
            try {
                if (docExtractor != null) {
                    Method close = extractorClass.getDeclaredMethod("Close");
                    close.invoke(docExtractor);
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                LOGGER.error("Error occurred when closing doc extractor: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Couldn't close doc extractor", e);
            }
        }
        return result;
    }

    public PrintFileUrl convertFileToPDF(byte[] inputFileByteArr, String folderPath, String fileName,
        WaterMark waterMarkObj) throws RMSException {
        PrintFileUrl printFileUrl = null;
        String result = "";
        Object docExtractor = null;
        String filepdf = "";
        FileOutputStream fos = null;
        Object stream = null;
        try {
            stream = memoryStreamClass.newInstance();
            Method getExtractor = docFiltersClass.getDeclaredMethod("GetExtractor", byte[].class);
            docExtractor = getExtractor.invoke(docFilters, inputFileByteArr);
            Method openDocExtractor = extractorClass.getDeclaredMethod("Open", int.class, String.class);
            openDocExtractor.invoke(docExtractor, igrBodyAndMeta | igrFormatImage, extractorOpenOptions);
            String canvasOpenOpt = canvasOptions;
            Method makeOutputCanvas = docFiltersClass.getDeclaredMethod("MakeOutputCanvas", igrStreamClass, int.class, String.class);
            Object canvas = makeOutputCanvas.invoke(docFilters, stream, igrDeviceImagePdf, canvasOpenOpt);
            try {
                Method getPageCount = extractorClass.getDeclaredMethod("GetPageCount");
                Method getPage = extractorClass.getDeclaredMethod("GetPage", int.class);
                Method renderPage = canvasClass.getDeclaredMethod("RenderPage", pageClass);
                Method pageClose = pageClass.getDeclaredMethod("Close");
                int numPages = (int)getPageCount.invoke(docExtractor);
                for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                    Object page = getPage.invoke(docExtractor, pageIndex);
                    try {
                        renderPage.invoke(canvas, page);
                        addWaterMark(waterMarkObj, canvas, page);
                    } finally {
                        if (page != null) {
                            pageClose.invoke(page);
                        }
                    }
                }
            } finally {
                Method canvasClose = canvasClass.getDeclaredMethod("Close");
                if (canvas != null) {
                    canvasClose.invoke(canvas);
                }
            }
            String tempname = folderPath.replace(ViewerConfigManager.getInstance().getWebDir(), "");
            String outFile = "";
            if (fileName.indexOf('.') > 0) {
                filepdf = fileName.substring(0, fileName.lastIndexOf('.')) + ".pdf";
                outFile = URLEncoder.encode(filepdf, "UTF-8");
                outFile = outFile.replaceAll("[+]", "%20");
            }
            result = tempname + File.separator + outFile;
            File tempWebDir = new File(folderPath);
            if (!tempWebDir.exists()) {
                FileUtils.mkdir(tempWebDir);
            }
            fos = new FileOutputStream(new File(folderPath, filepdf));
            Method writeTo = memoryStreamClass.getDeclaredMethod("writeTo", OutputStream.class);
            writeTo.invoke(stream, fos);

        } catch (IllegalAccessException | IllegalArgumentException | FileNotFoundException
                | NoSuchMethodException | SecurityException | InstantiationException | ClassNotFoundException e) {
            LOGGER.error("Error processing file " + folderPath + File.separator + fileName + ": " + e.getMessage(), e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } catch (InvocationTargetException e) {
            Throwable t = getRootCause(e);
            LOGGER.error("InvocationTargetException occurred while getting page number.", t);
            handleInvocationTargetException(t, fileName);
        } catch (IOException e) {
            LOGGER.error("Error processing file " + folderPath + File.separator + fileName + ": " + e.getMessage(), e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        } finally {
            if (stream != null) {
                try {
                    Method delete = igrStreamClass.getDeclaredMethod("delete");
                    delete.invoke(stream);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    LOGGER.error("Error occured while deleting IGRStream ", e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOGGER.error("Error occurred while closing stream: " + e.getMessage());
                }
            }
            try {
                if (docExtractor != null) {
                    Method closeExtractor = extractorClass.getDeclaredMethod("Close");
                    closeExtractor.invoke(docExtractor);
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                LOGGER.error("Error occurred while closing Extractor: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Error occurred while closing Extractor: " + e.getMessage());
            }
        }
        try {
            PDFCustomizer.addAutoPrintOption(new File(folderPath + File.separator + filepdf));
            printFileUrl = new PrintFileUrl(result.replace("\\", "/"), null);
        } catch (Exception e) {
            LOGGER.error("Error occurred while processing file " + fileName + ": " + e.getMessage(), e);
            throw new RMSException(ViewerMessageHandler.getClientString("err.file.processing"), e);
        }
        return printFileUrl;
    }

    public void generateDocContent(byte[] data, int page, WaterMark watermark, int zoom, OutputStream os,
        String fileName)
            throws ReflectiveOperationException, IOException {
        long startTime = System.currentTimeMillis();
        Object docExtractor = null;
        try {
            if (page <= 0) {
                throw new IllegalArgumentException("Invalid page");
            }
            String option = null;
            if (zoom > 0) {
                int dpi = 120 + zoom * 80;
                option = createExtractorOpenOptions(dpi, defaultPageCount);
            } else {
                option = extractorOpenOptions;
            }
            Method getExtractor = docFiltersClass.getDeclaredMethod("GetExtractor", byte[].class);
            docExtractor = getExtractor.invoke(docFilters, data);
            Method openDocExtractor = extractorClass.getDeclaredMethod("Open", int.class, String.class);
            openDocExtractor.invoke(docExtractor, igrBodyAndMeta | igrFormatImage, option);
            convertPage(docExtractor, page, watermark, os, zoom);
        } finally {
            try {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Time taken to convert page (file: {}, page: {}): {} ms", fileName, page, (System.currentTimeMillis() - startTime));
                }
                if (docExtractor != null) {
                    Method closeExtractor = extractorClass.getDeclaredMethod("Close");
                    closeExtractor.invoke(docExtractor);
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                LOGGER.error("Error occurred while closing Extractor: {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Error occurred while closing Extractor", e);
            }
        }
    }

    private void convertPage(Object docExtractor, int pageNum, WaterMark watermark, OutputStream os, int zoom)
            throws ReflectiveOperationException {
        Object stream = null;
        try {
            stream = memoryStreamClass.newInstance();
            String option = null;
            if (zoom > 0) {
                int dpi = 120 + zoom * 80;
                option = createCanvasOptions(dpi);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("dpi = " + dpi + ", zoom = " + zoom);
                }
            } else {
                option = canvasOptions;
            }
            Method outputCanvas = docFiltersClass.getDeclaredMethod("MakeOutputCanvas", igrStreamClass, int.class, String.class);

            Object canvas = outputCanvas.invoke(docFilters, stream, igrDeviceImagePng, option);
            Method getPage = extractorClass.getDeclaredMethod("GetPage", int.class);
            Object page = getPage.invoke(docExtractor, pageNum - 1);
            try {
                Method renderPage = canvasClass.getDeclaredMethod("RenderPage", pageClass);
                renderPage.invoke(canvas, page);
                addWaterMark(watermark, canvas, page);
            } finally {
                if (page != null) {
                    Method pageClose = pageClass.getDeclaredMethod("Close");
                    pageClose.invoke(page);
                }
                if (canvas != null) {
                    Method canvasClose = canvasClass.getDeclaredMethod("Close");
                    canvasClose.invoke(canvas);
                }
            }
            Method writeTo = memoryStreamClass.getDeclaredMethod("writeTo", OutputStream.class);
            writeTo.invoke(stream, os);
        } finally {
            if (stream != null) {
                Method delete = igrStreamClass.getDeclaredMethod("delete");
                delete.invoke(stream);
            }
        }
    }

    /*
     * Add watermark to the page
     */
    private void addWaterMark(WaterMark waterMarkObj, Object canvas,
        Object page) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (waterMarkObj == null) {
            return;
        }
        int waterMarkFontSize = waterMarkObj.getWaterMarkFontSize();
        Method getPageWidth = pageClass.getDeclaredMethod("getWidth");
        int width = (int)getPageWidth.invoke(page);
        Method getPageHeight = pageClass.getDeclaredMethod("getHeight");
        int height = (int)getPageHeight.invoke(page);
        int opacity = waterMarkObj.getWaterMarkTransparency();
        if (opacity > 100) {
            opacity = 100;
        }
        String waterMarkFontName = waterMarkObj.getWaterMarkFontName();
        if (waterMarkFontName == null || !WaterMark.FONTS.contains(waterMarkFontName)) {
            waterMarkFontName = WaterMark.WATERMARK_FONT_NAME_FALLBACK;
        }

        Method setCanvasOpacity = canvasClass.getDeclaredMethod("SetOpacity", int.class);
        Method setCanvasFont = canvasClass.getDeclaredMethod("SetFont", String.class, int.class, int.class);
        Method setCanvasBrush = canvasClass.getDeclaredMethod("SetBrush", int.class, int.class);
        Method textWidth = canvasClass.getDeclaredMethod("TextWidth", String.class);
        Method canvasRotation = canvasClass.getDeclaredMethod("Rotation", int.class);
        Method textOut = canvasClass.getDeclaredMethod("TextOut", int.class, int.class, String.class);
        Method textHeight = canvasClass.getDeclaredMethod("TextHeight", String.class);
        setCanvasOpacity.invoke(canvas, (int)(opacity * (2.5)));
        setCanvasFont.invoke(canvas, waterMarkFontName, waterMarkFontSize, 0);
        setCanvasBrush.invoke(canvas, Integer.valueOf(waterMarkObj.getWaterMarkFontColor().replaceFirst("#", ""), 16), 0);

        String watermarkString = waterMarkObj.getWaterMarkStr();
        int totalSize = (int)textWidth.invoke(canvas, watermarkString);

        if (width > RESOLUTIONFONTSIZETHRESHOLD && totalSize < width) {

            final NavigableMap<Integer, Integer> resolutionFontsizeMap = new TreeMap<>();

            resolutionFontsizeMap.put(0, waterMarkFontSize);
            resolutionFontsizeMap.put(RESOLUTIONFONTSIZETHRESHOLD, waterMarkFontSize + 25);
            resolutionFontsizeMap.put(RESOLUTIONFONTSIZETHRESHOLD + 1000, waterMarkFontSize + 50);
            resolutionFontsizeMap.put(RESOLUTIONFONTSIZETHRESHOLD + 2000, waterMarkFontSize + 75);
            resolutionFontsizeMap.put(RESOLUTIONFONTSIZETHRESHOLD + 3000, waterMarkFontSize + 100);
            //keep adding to support greater font sized based on resolution

            Integer updatedFontSize = resolutionFontsizeMap.get(resolutionFontsizeMap.floorKey(width));
            waterMarkFontSize = updatedFontSize == null ? waterMarkFontSize : updatedFontSize.intValue();
            setCanvasFont.invoke(canvas, waterMarkFontName, waterMarkFontSize, 0);
            totalSize = (int)textWidth.invoke(canvas, watermarkString);
        }

        while (totalSize > width) {
            waterMarkFontSize = waterMarkFontSize - 1;
            setCanvasFont.invoke(canvas, waterMarkFontName, waterMarkFontSize, 0);
            totalSize = (int)textWidth.invoke(canvas, watermarkString);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WaterMark text totalSize = " + totalSize + ", page width = " + width + ", page height = " + height + ", current waterMarkFontSize = " + waterMarkFontSize);
        }

        setCanvasFont.invoke(canvas, waterMarkFontName, waterMarkFontSize, 0);
        String[] lines = watermarkString.split(waterMarkObj.getWaterMarkSplit());

        int heightCount = 0;
        int maxWidth = 0;
        int heightOffset = 0;
        int widthOffset = 0;
        String orientation = waterMarkObj.getWaterMarkRotation();
        String density = waterMarkObj.getWaterMarkDensity();
        if (density.equals(WaterMark.WATERMARK_DENSITY_NORMAL_VALUE)) {
            heightOffset = 200;
            widthOffset = 100;
        } else if (density.equals(WaterMark.WATERMARK_DENSITY_DENSE_VALUE)) {
            heightOffset = 100;
            widthOffset = 100;
        } else {
            heightOffset = 200;
            widthOffset = 100;
        }
        heightOffset = heightOffset * waterMarkFontSize / 36;
        widthOffset = widthOffset * waterMarkFontSize / 36;

        for (String str : lines) {
            maxWidth = Math.max(maxWidth, (int)textWidth.invoke(canvas, str));
        }

        if (orientation.equals(WaterMark.WATERMARK_ROTATION_CLOCKWISE_VALUE)) {
            int offset = (int)(Math.sin(Math.PI / 4) * maxWidth);
            int lineOffset = (int)(heightOffset * 1.414);
            canvasRotation.invoke(canvas, 45);
            int lineCount = 0;
            if (density.equals(WaterMark.WATERMARK_DENSITY_DENSE_VALUE)) {
                offset += heightOffset - 100;
            }
            for (int i = -offset - 1000; i < height + 2000; i = i + heightCount + heightOffset) {
                for (int j = (offset) - 1000; j < width + 2000; j = j + maxWidth + widthOffset) {
                    heightCount = 0;
                    for (String str : lines) {
                        int whiteSpace = (maxWidth - (int)textWidth.invoke(canvas, str)) / 2;
                        textOut.invoke(canvas, j - lineCount * lineOffset + whiteSpace, i + heightCount, str);
                        heightCount = heightCount + (int)textHeight.invoke(canvas, str);
                    }
                }
                lineCount++;
            }
        } else if (orientation.equals(WaterMark.WATERMARK_ROTATION_ANTICLOCKWISE_VALUE)) {
            int offset = (int)(Math.sin(Math.PI / 4) * maxWidth);
            int lineOffset = (int)(heightOffset * 1.414);
            canvasRotation.invoke(canvas, -45);
            int lineCount = 0;
            if (density.equals(WaterMark.WATERMARK_DENSITY_DENSE_VALUE)) {
                offset -= heightOffset;
            }
            for (int i = offset - 1000; i < height + 1000; i = i + heightCount + heightOffset) {
                for (int j = -offset - 1000; j < width + 1000; j = j + maxWidth + widthOffset) {
                    heightCount = 0;
                    for (String str : lines) {
                        int whiteSpace = (maxWidth - (int)textWidth.invoke(canvas, str)) / 2;
                        textOut.invoke(canvas, j - lineCount * lineOffset + whiteSpace, i + heightCount, str);
                        heightCount = heightCount + (int)textHeight.invoke(canvas, str);
                    }
                }
                lineCount++;
            }
        } else {
            int offset = (int)(Math.sin(Math.PI / 4) * maxWidth);
            int lineOffset = (int)(heightOffset * 1.414);
            canvasRotation.invoke(canvas, -45);
            int lineCount = 0;
            if (density.equals(WaterMark.WATERMARK_DENSITY_DENSE_VALUE)) {
                offset -= heightOffset;
            }
            for (int i = offset - 1000; i < height + 1000; i = i + heightCount + heightOffset) {
                for (int j = -offset - 1000; j < width + 1000; j = j + maxWidth + widthOffset) {
                    heightCount = 0;
                    for (String str : lines) {
                        int whiteSpace = (maxWidth - (int)textWidth.invoke(canvas, str)) / 2;
                        textOut.invoke(canvas, j - lineCount * lineOffset + whiteSpace, i + heightCount, str);
                        heightCount = heightCount + (int)textHeight.invoke(canvas, str);
                    }
                }
                lineCount++;
            }
        }
    }
}

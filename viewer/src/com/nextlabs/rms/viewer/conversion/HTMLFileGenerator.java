package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.config.ViewerConfigManager;
import com.nextlabs.rms.viewer.exception.RMSException;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public final class HTMLFileGenerator {

    private static final String HTML_HEADER;
    private static final String HTML_TOOLBAR;
    private static final String HTML_WATERMARK;
    private static final int AVERAGE_FILE_SIZE = 65536;
    static {

        try (InputStream is = HTMLFileGenerator.class.getResourceAsStream("HTMLHeader.txt");
                InputStream toolbarIs = HTMLFileGenerator.class.getResourceAsStream("HTMLToolbar.txt");
                InputStream watermarkIs = HTMLFileGenerator.class.getResourceAsStream("HTMLWatermark.txt")) {
            HTML_HEADER = IOUtils.toString(is, StandardCharsets.UTF_8);
            HTML_TOOLBAR = IOUtils.toString(toolbarIs, StandardCharsets.UTF_8);
            HTML_WATERMARK = IOUtils.toString(watermarkIs, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private HTMLFileGenerator() {
    }

    public static WatermarkWrapper handleExcelFile(String sessionId, String docId, String htmlFileName,
        byte[] fileContent, String user, int offset, String domain, WaterMark waterMarkObj)
            throws IOException, RMSException {

        String webDir = ViewerConfigManager.getInstance().getWebDir();
        File tempWebDir = new File(webDir, ViewerConfigManager.TEMPDIR_NAME + File.separator + sessionId + File.separator + docId);
        if (!tempWebDir.exists()) {
            FileUtils.mkdir(tempWebDir);
        }
        File file = new File(tempWebDir.getAbsolutePath(), htmlFileName);
        //Save the the HTML file
        ImageProcessor.getInstance().convertFileToHTML(fileContent, tempWebDir.getAbsolutePath(), htmlFileName);
        Set<String> imageFilenames = new HashSet<>(Arrays.asList(tempWebDir.list()));
        imageFilenames.remove(htmlFileName);
        StringBuilder content = getHTMLFileContent(file);
        content = addExcelScript(content);
        WatermarkWrapper watermarkWrapper = generateWaterMark(waterMarkObj, sessionId, docId);
        content = addToolbar(content, FilenameUtils.getBaseName(htmlFileName), watermarkWrapper.getWatermarkHTML());
        String htmlContent = content.toString();
        for (String imageFilename : imageFilenames) {
            if (htmlContent.contains("src=\"" + imageFilename + "\"")) {
                try {
                    File imageFile = new File(tempWebDir.getAbsolutePath(), imageFilename);
                    if (imageFile.exists() && imageFile.isFile() && imageFile.canRead()) {
                        StringBuilder sb = new StringBuilder(AVERAGE_FILE_SIZE);
                        sb.append("data:image/png;base64,");
                        sb.append(imgToBase64String(ImageIO.read(imageFile), "png"));
                        String base64ImageFile = sb.toString();
                        htmlContent = htmlContent.replace("src=\"" + imageFilename + "\"", "src=\"" + base64ImageFile + "\"");
                    }
                } catch (IOException e) {
                    throw new IOException("Error occurred while rendering embedded image", e);
                }
            }
        }
        org.apache.commons.io.FileUtils.writeStringToFile(file, htmlContent, "UTF-8");
        watermarkWrapper.setFileHTML(file);
        return watermarkWrapper;
    }

    public static StringBuilder imgToBase64String(final RenderedImage img, final String formatName) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final StringBuilder sb = new StringBuilder(AVERAGE_FILE_SIZE);
        try {
            ImageIO.write(img, formatName, os);
            return sb.append(Base64.getEncoder().encodeToString(os.toByteArray()));
        } catch (IOException e) {
            throw new IOException("Error occurred while inlining embedded image to base64", e);
        }
    }

    private static WatermarkWrapper generateWaterMark(WaterMark waterMarkObj, String sessionId,
        String docId)
            throws IOException {
        if (waterMarkObj == null || !StringUtils.hasText(waterMarkObj.getWaterMarkStr())) {
            return new WatermarkWrapper(null, "");
        }

        int offset = StringUtils.equals(waterMarkObj.getWaterMarkDensity(), WaterMark.WATERMARK_DENSITY_NORMAL_VALUE) ? 100 : 20;
        boolean rotatedClockwise = StringUtils.equals(waterMarkObj.getWaterMarkRotation(), WaterMark.WATERMARK_ROTATION_CLOCKWISE_VALUE);
        String waterMarkFontName = waterMarkObj.getWaterMarkFontName();
        if (waterMarkFontName == null || !WaterMark.FONTS.contains(waterMarkFontName)) {
            waterMarkFontName = WaterMark.WATERMARK_FONT_NAME_FALLBACK;
        }

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        Font font = new Font(waterMarkFontName, Font.PLAIN, waterMarkObj.getWaterMarkFontSize());
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        String[] lines = waterMarkObj.getWaterMarkStr().split(waterMarkObj.getWaterMarkSplit());
        int width = 0;
        for (int i = 0; i < lines.length; i++) {
            width = width < fm.stringWidth(lines[i]) ? fm.stringWidth(lines[i]) : width;
        }

        int boundingWidth = width + offset;
        g2d.dispose();

        img = new BufferedImage(boundingWidth, boundingWidth, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.decode(waterMarkObj.getWaterMarkFontColor()));
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, waterMarkObj.getWaterMarkTransparency() / 100f));
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.rotate(Math.toRadians(rotatedClockwise ? 45 : -45), boundingWidth / 2.0, boundingWidth / 2.0);

        int y = width / 2;
        for (String line : lines) {
            int x = (width - fm.stringWidth(line)) / 2;
            g2d.drawString(line, x, y += fm.getHeight());
        }

        g2d.dispose();
        String pngURL = new StringBuilder().append("/viewer/RMSViewer/GetWatermarkContent?d=").append(docId).append("&s=").append(sessionId).toString();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return new WatermarkWrapper(imageInByte, HTML_WATERMARK.replace("{{width}}", String.valueOf(boundingWidth)).replace("{{watermark.png}}", pngURL));
    }

    private static StringBuilder addExcelScript(StringBuilder content) {
        int titleIndex = content.indexOf("<title></title>");
        if (titleIndex >= 0) {
            content.replace(titleIndex, titleIndex + 15, "");
        }
        int headIndex = content.indexOf("<head>");
        if (headIndex >= 0) {
            String header = HTML_HEADER.replace("{{version}}", com.nextlabs.common.BuildConfig.VERSION);
            content.replace(headIndex, headIndex + 6, header);
        }
        return content;
    }

    private static StringBuilder addToolbar(StringBuilder content, String docName, String watermark) {
        String toolbar = HTML_TOOLBAR.replace("{{docName}}", docName).replace("{{watermark}}", watermark);
        int bodyIndex = content.indexOf("<body>");
        if (bodyIndex >= 0) {
            content.replace(bodyIndex, bodyIndex + 6, toolbar);
            bodyIndex = content.indexOf("</body>");
            if (bodyIndex >= 0) {
                content.replace(bodyIndex, bodyIndex + 7, "</div></div></body>");
            }
        }
        return content;
    }

    private static StringBuilder getHTMLFileContent(File file)
            throws IOException {
        Path path = file.toPath();
        byte[] data = Files.readAllBytes(path);
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0xE || data[i] == 0xF || data[i] == 0x10) {
                data[i] = 0x0;
            }
        }
        StringBuilder builder = new StringBuilder(data.length + 5000);
        builder.append(new String(data, StandardCharsets.UTF_8));
        return builder;
    }

    public static class WatermarkWrapper {

        private File fileHTML;
        private final byte[] watermarkPNG;
        private final String watermarkHTML;

        public WatermarkWrapper(byte[] watermarkPNG, String watermarkHTML) {
            super();
            this.watermarkPNG = watermarkPNG;
            this.watermarkHTML = watermarkHTML;
        }

        public File getFileHTML() {
            return fileHTML;
        }

        public byte[] getWatermarkPNG() {
            return watermarkPNG;
        }

        public String getWatermarkHTML() {
            return watermarkHTML;
        }

        public void setFileHTML(File fileHTML) {
            this.fileHTML = fileHTML;
        }
    }
}

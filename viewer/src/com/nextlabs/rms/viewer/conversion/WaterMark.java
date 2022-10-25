package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.viewer.servlets.LogConstants;

import java.awt.GraphicsEnvironment;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WaterMark implements Serializable {

    public static final Set<String> FONTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())));

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.VIEWER_LOG_NAME);

    private static final long serialVersionUID = 5488939009797300607L;

    private static final Pattern COLOUR_HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    public static final String WATERMARK_TEXT_KEY = "Text";

    public static final String WATERMARK_FONT_SIZE_KEY = "FontSize";

    public static final String WATERMARK_FONT_STYLE_KEY = "FontName";

    public static final String WATERMARK_FONT_COLOR_KEY = "TextColor";

    public static final String WATERMARK_FONT_TRANSPARENCY_KEY = "Transparency";

    public static final String WATERMARK_ROTATION_KEY = "Rotation";

    /* Rotation Values */

    public static final String WATERMARK_ROTATION_ANTICLOCKWISE_VALUE = "Anticlockwise";

    public static final String WATERMARK_ROTATION_CLOCKWISE_VALUE = "Clockwise";

    /* Density Values */

    public static final String WATERMARK_DENSITY_NORMAL_VALUE = "Normal";

    public static final String WATERMARK_DENSITY_DENSE_VALUE = "Dense";

    /* Not supported in RMC */

    public static final String WATERMARK_DENSITY_KEY = "Density";

    public static final String WATERMARK_DENSITY_DEFAULT = WATERMARK_DENSITY_NORMAL_VALUE;

    public static final String WATERMARK_DELIMITER = "\n";

    /* Default Values */

    public static final String WATERMARK_TEXT_DEFAULT = "$(User) $(Time)";

    public static final String WATERMARK_FONT_NAME_DEFAULT = "Sitka Text";

    public static final String WATERMARK_FONT_NAME_FALLBACK = "Arial";

    public static final String WATERMARK_FONT_COLOR_DEFAULT = "#000000";

    public static final int WATERMARK_FONT_SIZE_DEFAULT = 36;

    public static final int WATERMARK_FONT_TRANSPARENCY_DEFAULT = 30;

    public static final String WATERMARK_ROTATION_DEFAULT = "Anticlockwise";

    public static final String WATERMARK_DATE_FORMAT_DEFAULT = "yyyy-MM-dd";

    public static final String WATERMARK_TIME_FORMAT_DEFAULT = "HH:mm:ss";

    private Map<String, String> waterMarkValues;

    private String waterMarkStr;

    private int waterMarkFontSize;

    private String waterMarkFontName;

    private String waterMarkFontColor;

    private int waterMarkTransparency;

    private String waterMarkSplit;

    private String waterMarkRotation;

    private String waterMarkDensity;

    private String waterMarkDateFormat;

    private String waterMarkTimeFormat;

    public String getWaterMarkDateFormat() {
        return waterMarkDateFormat;
    }

    public void setWaterMarkDateFormat(String waterMarkDateFormat) {
        this.waterMarkDateFormat = waterMarkDateFormat;
    }

    public String getWaterMarkTimeFormat() {
        return waterMarkTimeFormat;
    }

    public void setWaterMarkTimeFormat(String waterMarkTimeFormat) {
        this.waterMarkTimeFormat = waterMarkTimeFormat;
    }

    public String getWaterMarkStr() {
        return waterMarkStr;
    }

    public void setWaterMarkStr(String waterMarkStr) {
        this.waterMarkStr = waterMarkStr;
    }

    public int getWaterMarkFontSize() {
        return waterMarkFontSize;
    }

    public String getWaterMarkFontName() {
        return waterMarkFontName;
    }

    public String getWaterMarkFontColor() {
        return waterMarkFontColor;
    }

    public int getWaterMarkTransparency() {
        return waterMarkTransparency;
    }

    public String getWaterMarkSplit() {
        return waterMarkSplit;
    }

    public String getWaterMarkRotation() {
        return waterMarkRotation;
    }

    public String getWaterMarkDensity() {
        return waterMarkDensity;
    }

    public Map<String, String> getWaterMarkValues() {
        return waterMarkValues;
    }

    public void setWaterMarkValues(HashMap<String, String> valueMap, String watermarkText,
        JsonWatermark waterMarkConfig) {
        if (!valueMap.isEmpty()) {
            waterMarkStr = valueMap.get(WaterMark.WATERMARK_TEXT_KEY);
            if (waterMarkStr == null || waterMarkStr.length() <= 0) {
                waterMarkStr = WaterMark.WATERMARK_TEXT_DEFAULT;
            }
            //waterMarkStr = waterMarkStr.replaceAll("([^\\\\])\\\\n", "$1\n").replaceAll("(\\\\){2}", "\\\\");
            waterMarkStr = StringUtils.normalize(waterMarkStr);
            String waterMarkFontSizeStr = valueMap.get(WaterMark.WATERMARK_FONT_SIZE_KEY);
            try {
                waterMarkFontSize = Integer.parseInt(waterMarkFontSizeStr);
            } catch (Exception e) {
                LOGGER.error("Invalid Watermark Font Size Specified. Using Default.");
                waterMarkFontSize = WaterMark.WATERMARK_FONT_SIZE_DEFAULT;
            }
            waterMarkFontName = valueMap.get(WaterMark.WATERMARK_FONT_STYLE_KEY);
            waterMarkFontColor = getHexColorCode(valueMap.get(WaterMark.WATERMARK_FONT_COLOR_KEY));
            String waterMarkTransparencyStr = valueMap.get(WaterMark.WATERMARK_FONT_TRANSPARENCY_KEY);
            try {
                waterMarkTransparency = Integer.parseInt(waterMarkTransparencyStr);
                waterMarkTransparency = Math.min(waterMarkTransparency, 100);
            } catch (Exception e) {
                LOGGER.error("Invalid Watermark Font Transparency Specified. Using Default.");
                waterMarkTransparency = WaterMark.WATERMARK_FONT_TRANSPARENCY_DEFAULT;
            }

            waterMarkRotation = valueMap.get(WaterMark.WATERMARK_ROTATION_KEY);
            waterMarkDensity = valueMap.get(WaterMark.WATERMARK_DENSITY_KEY);
            waterMarkDateFormat = WaterMark.WATERMARK_DATE_FORMAT_DEFAULT;
            waterMarkTimeFormat = WaterMark.WATERMARK_TIME_FORMAT_DEFAULT;
        } else {
            waterMarkStr = StringUtils.hasText(watermarkText) ? watermarkText : waterMarkConfig.getText();
            waterMarkFontSize = waterMarkConfig.getFontSize();
            waterMarkFontName = waterMarkConfig.getFontName();
            waterMarkFontColor = getHexColorCode(waterMarkConfig.getFontColor());
            waterMarkTransparency = waterMarkConfig.getTransparentRatio();
            waterMarkRotation = waterMarkConfig.getRotation();
            waterMarkDensity = waterMarkConfig.getDensity();
            waterMarkDateFormat = waterMarkConfig.getDateFormat();
            if (!StringUtils.hasText(waterMarkDateFormat)) {
                waterMarkDateFormat = WaterMark.WATERMARK_DATE_FORMAT_DEFAULT;
                waterMarkTimeFormat = WaterMark.WATERMARK_TIME_FORMAT_DEFAULT;
            } else {
                waterMarkTimeFormat = null;
            }
        }
        waterMarkSplit = WATERMARK_DELIMITER;

        if (waterMarkFontSize <= 0) {
            waterMarkFontSize = WaterMark.WATERMARK_FONT_SIZE_DEFAULT;
        }

        if (waterMarkFontName == null || waterMarkFontName.length() <= 0) {
            waterMarkFontName = WaterMark.WATERMARK_FONT_NAME_DEFAULT;
        }

        if (waterMarkTransparency < 0 || waterMarkTransparency > 100) {
            waterMarkTransparency = WaterMark.WATERMARK_FONT_TRANSPARENCY_DEFAULT;
        }

        if (waterMarkRotation == null || waterMarkRotation.length() <= 0) {
            waterMarkRotation = WaterMark.WATERMARK_ROTATION_DEFAULT;
        }

        if (waterMarkDensity == null || waterMarkDensity.length() <= 0) {
            waterMarkDensity = WaterMark.WATERMARK_DENSITY_DEFAULT;
        }

        waterMarkTransparency = 100 - waterMarkTransparency;
        this.waterMarkValues = valueMap;
    }

    private String getHexColorCode(String color) {
        if (COLOUR_HEX_PATTERN.matcher(color).matches()) {
            return color;
        }
        switch (color) {
            case "Red":
                return "#ff0000";
            case "Lime":
                return "#00ff00";
            case "Blue":
                return "#0000ff";
            case "Yellow":
                return "#ffff00";
            case "Cyan / Aqua":
                return "#00ffff";
            case "Magenta / Fuchsia":
                return "#ff00ff";
            case "Gray":
                return "#808080";
            case "Dim Gray":
                return "#bfbfbf";
            case "Maroon":
                return "#800000";
            case "Olive":
                return "#808000";
            case "Green":
                return "#008000";
            case "Purple":
                return "#800080";
            case "Teal":
                return "#008080";
            case "Navy":
                return "#000080";
            default:
                return "#000000";
        }
    }
}

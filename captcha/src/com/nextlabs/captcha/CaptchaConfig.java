package com.nextlabs.captcha;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

public final class CaptchaConfig {

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String KEY = "key";
    private static final String BG_COLOR_START = "bg_color_start";
    private static final String BG_COLOR_END = "bg_color_end";
    private static final String NOISE_COLOR = "noise_color";
    private static final String TEXT_COLOR = "text_color";
    private static final String TEXT_SPACE = "text_space";
    private static final String FONT_SIZE = "font_size";

    private static final CaptchaConfig INSTANCE = new CaptchaConfig();

    private Properties prop;

    private CaptchaConfig() {
        prop = new Properties();
    }

    public static CaptchaConfig getInstance() {
        return INSTANCE;
    }

    public String getKey() {
        return prop.getProperty(KEY, "BC6DECD4373111E6AC629E71128CAE77");
    }

    public Font[] getTextFonts(int fontSize) {
        return new Font[] { new Font("Arial", Font.BOLD, fontSize), new Font("Courier", Font.BOLD, fontSize) };
    }

    public int getFontSize() {
        return getIntValue(FONT_SIZE, 40);
    }

    public Color getTextColor() {
        return getColor(TEXT_COLOR, Color.BLACK);
    }

    public int getTextSpace() {
        return getIntValue(TEXT_SPACE, 2);
    }

    public Color getNoiseColor() {
        return getColor(NOISE_COLOR, Color.BLACK);
    }

    public Color getBgColorStart() {
        return getColor(BG_COLOR_START, Color.LIGHT_GRAY);
    }

    public Color getBgColorEnd() {
        return getColor(BG_COLOR_END, Color.WHITE);
    }

    public int getWidth() {
        return getIntValue(WIDTH, 250);
    }

    public int getHeight() {
        return getIntValue(HEIGHT, 50);
    }

    private Color getColor(String paramName, Color defaultColor) {
        int value = getIntValue(paramName, defaultColor.getRGB());
        return new Color(value);
    }

    private int getIntValue(String key, int defaultInt) {
        try {
            String value = prop.getProperty(key);
            if (value == null) {
                return defaultInt;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultInt;
        }
    }
}

package com.nextlabs.common.shared;

import java.io.Serializable;

public class JsonWatermark implements Serializable {

    private static final long serialVersionUID = 1L;
    private String text;
    private int fontSize;
    private String repeat;
    private String fontName;
    private String fontColor;
    private int transparentRatio;
    private String rotation;
    private String density;
    private String dateFormat;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public int getTransparentRatio() {
        return transparentRatio;
    }

    public void setTransparentRatio(int transparentRatio) {
        this.transparentRatio = transparentRatio;
    }

    public String getRotation() {
        return rotation;
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    public String getDensity() {
        return density;
    }

    public void setDensity(String density) {
        this.density = density;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }
}

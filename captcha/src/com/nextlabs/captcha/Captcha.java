package com.nextlabs.captcha;

import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.TransformFilter;
import com.jhlabs.image.WaterFilter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

public class Captcha {

    private static final String LOWER_CASE_ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_CASE_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SPECIAL_CHARS = "!@#$%&*()_-+=[]{}\\|:/?.,><";
    private static final String NUMERIC = "0123456789";

    private CaptchaConfig config;
    private int width;
    private int height;
    private Random rand;

    static {
        ImageIO.setUseCache(false);
    }

    public Captcha() {
        config = CaptchaConfig.getInstance();
        rand = new Random();
        width = config.getWidth();
        height = config.getHeight();
    }

    public BufferedImage createCaptcha(String text) {
        BufferedImage img = paintText(text);
        img = addWaterRipple(img);
        img = addBackground(img);
        paintBorder(img);
        return img;
    }

    public String randomText(int len) {
        int minUpperCase = 2;
        return new String(generate(len, len, 0, minUpperCase, len - minUpperCase, 0));
    }

    private BufferedImage paintText(String word) {
        int fontSize = config.getFontSize();
        Font[] fonts = config.getTextFonts(fontSize);
        Color color = config.getTextColor();
        int charSpace = config.getTextSpace();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = image.createGraphics();
        g2D.setColor(color);

        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2D.setRenderingHints(hints);

        FontRenderContext ctx = g2D.getFontRenderContext();

        int startPosY = (height - fontSize) / 5 + fontSize;

        char[] wordChars = word.toCharArray();
        Font[] chosenFonts = new Font[wordChars.length];
        int[] charWidths = new int[wordChars.length];
        int widthNeeded = 0;
        for (int i = 0; i < wordChars.length; i++) {
            chosenFonts[i] = fonts[rand.nextInt(fonts.length)];

            char[] charToDraw = new char[] { wordChars[i] };
            GlyphVector gv = chosenFonts[i].createGlyphVector(ctx, charToDraw);
            charWidths[i] = (int)gv.getVisualBounds().getWidth();
            if (i > 0) {
                widthNeeded = widthNeeded + 2;
            }
            widthNeeded = widthNeeded + charWidths[i];
        }

        int startPosX = (width - widthNeeded) / 2;
        for (int i = 0; i < wordChars.length; i++) {
            g2D.setFont(chosenFonts[i]);
            char[] charToDraw = new char[] { wordChars[i] };
            g2D.drawChars(charToDraw, 0, charToDraw.length, startPosX, startPosY);
            startPosX = startPosX + charWidths[i] + charSpace;
        }

        return image;
    }

    private BufferedImage addWaterRipple(BufferedImage img) {
        BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = (Graphics2D)newImg.getGraphics();

        RippleFilter rippleFilter = new RippleFilter();
        rippleFilter.setWaveType(RippleFilter.SINE);
        rippleFilter.setXAmplitude(2.6f);
        rippleFilter.setYAmplitude(1.7f);
        rippleFilter.setXWavelength(15);
        rippleFilter.setYWavelength(5);
        rippleFilter.setEdgeAction(TransformFilter.NEAREST_NEIGHBOUR);

        WaterFilter waterFilter = new WaterFilter();
        waterFilter.setAmplitude(1.5f);
        waterFilter.setPhase(10);
        waterFilter.setWavelength(2);

        BufferedImage effectImage = waterFilter.filter(img, null);
        effectImage = rippleFilter.filter(effectImage, null);

        graphics.drawImage(effectImage, 0, 0, null, null);

        graphics.dispose();

        make4(newImg, .1f, .1f, .25f, .25f);
        make4(newImg, .1f, .25f, .5f, .9f);
        return newImg;
    }

    private void make4(BufferedImage img, float factor1, float factor2, float factor3, float factorFour) {
        Color color = config.getNoiseColor();

        // the curve from where the points are taken
        CubicCurve2D cc = new CubicCurve2D.Float(width * factor1, height * rand.nextFloat(), width * factor2, height * rand.nextFloat(), width * factor3, height * rand.nextFloat(), width * factorFour, height * rand.nextFloat());

        // creates an iterator to define the boundary of the flattened curve
        PathIterator pi = cc.getPathIterator(null, 2);
        Point2D[] tmp = new Point2D[200];
        int i = 0;

        // while pi is iterating the curve, adds points to tmp array
        while (!pi.isDone()) {
            float[] coords = new float[6];
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    tmp[i] = new Point2D.Float(coords[0], coords[1]);
                    break;
                default:
                    break;
            }
            i++;
            pi.next();
        }

        Point2D[] pts = new Point2D[i];
        System.arraycopy(tmp, 0, pts, 0, i);

        Graphics2D graph = (Graphics2D)img.getGraphics();
        graph.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        graph.setColor(color);

        for (i = 0; i < pts.length - 1; i++) {
            if (i < 3) {
                graph.setStroke(new BasicStroke(0.9f * (4 - i)));
            }
            graph.drawLine((int)pts[i].getX(), (int)pts[i].getY(), (int)pts[i + 1].getX(), (int)pts[i + 1].getY());
        }

        graph.dispose();
    }

    private BufferedImage addBackground(BufferedImage img) {
        BufferedImage imgWithBg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graph = (Graphics2D)imgWithBg.getGraphics();
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY));
        hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));

        graph.setRenderingHints(hints);

        Color colorStart = config.getBgColorStart();
        Color colorEnd = config.getBgColorEnd();
        GradientPaint paint = new GradientPaint(0, 0, colorStart, width, height, colorEnd);
        graph.setPaint(paint);
        graph.fill(new Rectangle2D.Double(0, 0, width, height));
        graph.drawImage(img, 0, 0, null);

        return imgWithBg;
    }

    private void paintBorder(BufferedImage img) {
        Graphics2D graphics = img.createGraphics();
        graphics.setColor(Color.BLACK);
        Line2D line1 = new Line2D.Double(0, 0, 0, width);
        graphics.draw(line1);
        Line2D line2 = new Line2D.Double(0, 0, width, 0);
        graphics.draw(line2);
        line2 = new Line2D.Double(0, height - 1, width, height - 1);
        graphics.draw(line2);
        line2 = new Line2D.Double(width - 1, height - 1, width - 1, 0);
        graphics.draw(line2);
    }

    private char[] generate(int minLength, int maxLength, int minLowerCase, int minUpperCase, int minNumeric,
        int minSpecialChars) {
        if (minLength > maxLength || minLowerCase < 0 || minUpperCase < 0 || minNumeric < 0 || minSpecialChars < 0) {
            throw new IllegalArgumentException("Invalid parameter");
        }
        int totalInput = minLowerCase + minUpperCase + minNumeric + minSpecialChars;
        if (totalInput > maxLength) {
            throw new IllegalArgumentException("Total input (" + totalInput + ") is longer than max length (" + maxLength + ")");
        } else if (totalInput > minLength) {
            throw new IllegalArgumentException("Total input (" + totalInput + ") is longer than min length (" + minLength + ")");
        }
        Random random = new Random(System.currentTimeMillis());
        final int length = maxLength == minLength ? minLength : minLength + random.nextInt(maxLength - minLength);
        List<Integer> orderList = new LinkedList<>();
        for (int i = 0; i < length; i++) {
            orderList.add(Integer.valueOf(i));
        }
        Collections.shuffle(orderList);
        char[] temp = new char[length];
        int start = 0;
        for (int i = start; i < start + minLowerCase; i++) {
            temp[orderList.get(i)] = LOWER_CASE_ALPHA.charAt(random.nextInt(LOWER_CASE_ALPHA.length()));
        }
        start += minLowerCase;
        for (int i = start; i < start + minUpperCase; i++) {
            temp[orderList.get(i)] = UPPER_CASE_ALPHA.charAt(random.nextInt(UPPER_CASE_ALPHA.length()));
        }
        start += minUpperCase;
        for (int i = start; i < start + minNumeric; i++) {
            temp[orderList.get(i)] = NUMERIC.charAt(random.nextInt(NUMERIC.length()));
        }
        start += minNumeric;
        for (int i = start; i < start + minSpecialChars; i++) {
            temp[orderList.get(i)] = SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length()));
        }
        start += minSpecialChars;
        if (length > start) {
            for (int i = start; i < length; i++) {
                temp[orderList.get(i)] = LOWER_CASE_ALPHA.charAt(random.nextInt(LOWER_CASE_ALPHA.length()));
            }
        }
        return temp;
    }
}

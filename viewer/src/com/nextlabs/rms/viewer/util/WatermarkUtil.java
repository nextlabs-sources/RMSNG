package com.nextlabs.rms.viewer.util;

import com.nextlabs.common.shared.JsonWatermark;
import com.nextlabs.rms.eval.Attribute;
import com.nextlabs.rms.eval.Obligation;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.shared.WatermarkConfigManager;
import com.nextlabs.rms.viewer.conversion.WaterMark;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public final class WatermarkUtil {

    private WatermarkUtil() {
    }

    public static void updateWaterMark(WaterMark waterMarkObj, String userName, int offset) {
        Date now = new Date();
        String[] timezones = TimeZone.getAvailableIDs(-1 * offset * 60 * 1000);
        String dateFormat = Nvl.nvl(waterMarkObj.getWaterMarkDateFormat(), WaterMark.WATERMARK_DATE_FORMAT_DEFAULT);
        String timeFormat = Nvl.nvl(waterMarkObj.getWaterMarkTimeFormat(), WaterMark.WATERMARK_TIME_FORMAT_DEFAULT);
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);
        SimpleDateFormat tf = new SimpleDateFormat(timeFormat);
        df.setTimeZone(TimeZone.getTimeZone(timezones[0]));
        tf.setTimeZone(TimeZone.getTimeZone(timezones[0]));

        String localDate = df.format(now);
        String localTime = tf.format(now);
        String lineBreak = "\n";

        String inputStr = waterMarkObj.getWaterMarkStr();
        inputStr = inputStr.replace("\n", "\\n");
        inputStr = inputStr.replace(WatermarkConfigManager.WATERMARK_USERNAME, userName);
        inputStr = inputStr.replace(WatermarkConfigManager.WATERMARK_LOCALTIME, localTime);
        inputStr = inputStr.replace(WatermarkConfigManager.WATERMARK_LOCALDATE, localDate);
        inputStr = inputStr.replace(WatermarkConfigManager.WATERMARK_LINEBREAK, lineBreak);
        inputStr = inputStr.replace(WatermarkConfigManager.WATERMARK_HOST, "");
        waterMarkObj.setWaterMarkStr(inputStr);
    }

    public static WaterMark build(Obligation obligation, String watermarkText, JsonWatermark watermarkConfig) {
        WaterMark waterMark = new WaterMark();
        Map<String, String> watermarkValues = new HashMap<>();
        if (obligation != null) {
            List<Attribute> attributes = obligation.getAttributes();
            for (Attribute attribute : attributes) {
                watermarkValues.put(attribute.getName(), attribute.getValue());
            }
        }
        waterMark.setWaterMarkValues((HashMap<String, String>)watermarkValues, watermarkText, watermarkConfig);
        return waterMark;
    }
}

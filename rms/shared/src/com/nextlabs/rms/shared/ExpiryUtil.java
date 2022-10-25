package com.nextlabs.rms.shared;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.util.DateUtils;
import com.nextlabs.common.util.GsonUtils;

import java.util.HashMap;
import java.util.Map;

public final class ExpiryUtil {

    private ExpiryUtil() {

    }

    public static boolean validateExpiry(String expiryJson) throws JsonSyntaxException {
        return validateExpiry(GsonUtils.GSON.fromJson(expiryJson, JsonExpiry.class));
    }

    public static boolean validateExpiry(JsonExpiry expiry) {
        boolean flag = true;
        int option = expiry.getOption();
        if (option >= 0 && option < 4) {
            switch (option) {
                case 1:
                    JsonExpiry.JsonRelativeDay relativeDay = expiry.getRelativeDay();
                    if (relativeDay == null) {
                        if (expiry.getEndDate() == null || expiry.getEndDate() < DateUtils.now()) {
                            flag = false;
                        }
                        break;
                    } else {
                        int year = relativeDay.getYear();
                        int month = relativeDay.getMonth();
                        int week = relativeDay.getWeek();
                        int day = relativeDay.getDay();
                        if (year < 0 || month < 0 || week < 0 || day < 0 || year + month + week + day == 0) {
                            flag = false;
                            break;
                        }
                    }
                    break;
                case 2:
                    if (expiry.getEndDate() == null || expiry.getEndDate() < DateUtils.now()) {
                        flag = false;
                    }
                    break;
                case 3:
                    if (expiry.getEndDate() == null || expiry.getStartDate() == null || expiry.getEndDate() < DateUtils.now() || expiry.getEndDate() < expiry.getStartDate()) {
                        flag = false;
                    }
                    break;
                default:
                    break;
            }
        }
        return flag;
    }

    public static Map<String, Object> validateExpiry(JsonObject expiry)
            throws IllegalStateException, NumberFormatException {
        Map<String, Object> prefsSavedMap = null;
        JsonPrimitive option = expiry.getAsJsonPrimitive("option");
        if (option != null) {
            int optionType = option.getAsInt();
            if (optionType >= 0 && optionType <= 3) {
                JsonPrimitive endDateJson;
                switch (optionType) {
                    case 1:
                        JsonObject relativeDayJson = expiry.getAsJsonObject("relativeDay");
                        if (relativeDayJson != null) {
                            Map<String, Integer> relativeDayMap = new HashMap<>();
                            JsonPrimitive year = relativeDayJson.getAsJsonPrimitive("year");
                            if (year != null && year.getAsInt() > 0) {
                                relativeDayMap.put("year", year.getAsInt());
                            }
                            JsonPrimitive month = relativeDayJson.getAsJsonPrimitive("month");
                            if (month != null && month.getAsInt() > 0) {
                                relativeDayMap.put("month", month.getAsInt());
                            }
                            JsonPrimitive week = relativeDayJson.getAsJsonPrimitive("week");
                            if (week != null && week.getAsInt() > 0) {
                                relativeDayMap.put("week", week.getAsInt());
                            }
                            JsonPrimitive day = relativeDayJson.getAsJsonPrimitive("day");
                            if (day != null && day.getAsInt() > 0) {
                                relativeDayMap.put("day", day.getAsInt());
                            }
                            if (!relativeDayMap.isEmpty()) {
                                prefsSavedMap = new HashMap<>();
                                prefsSavedMap.put("option", optionType);
                                prefsSavedMap.put("relativeDay", relativeDayMap);
                            }
                        }
                        break;
                    case 2:
                        endDateJson = expiry.getAsJsonPrimitive("endDate");
                        if (endDateJson != null && endDateJson.getAsLong() >= DateUtils.now()) {
                            prefsSavedMap = new HashMap<>();
                            prefsSavedMap.put("option", optionType);
                            prefsSavedMap.put("endDate", endDateJson);
                        }
                        break;
                    case 3:
                        JsonPrimitive startDateJson = expiry.getAsJsonPrimitive("startDate");
                        endDateJson = expiry.getAsJsonPrimitive("endDate");
                        if (startDateJson != null && endDateJson != null && endDateJson.getAsLong() > startDateJson.getAsLong() && endDateJson.getAsLong() >= DateUtils.now()) {
                            prefsSavedMap = new HashMap<>();
                            prefsSavedMap.put("option", optionType);
                            prefsSavedMap.put("startDate", startDateJson);
                            prefsSavedMap.put("endDate", endDateJson);
                        }
                        break;
                    default:
                        prefsSavedMap = new HashMap<>();
                        prefsSavedMap.put("option", optionType);
                        break;
                }
            }
        }
        return prefsSavedMap;
    }
}

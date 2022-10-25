package com.nextlabs.common.shared;

import java.util.regex.Pattern;

public interface RegularExpressions {

    public static final String PROFILE_DISPLAY_NAME = "^((?![\\~\\!\\#\\$\\%\\^\\&\\*\\(\\)\\+\\=\\[\\]\\{\\}\\;\\:\\\"\\\\\\/\\<\\>\\?]).)+$";
    public static final Pattern PROFILE_DISPLAY_NAME_PATTERN = Pattern.compile(PROFILE_DISPLAY_NAME);

    public static final String EMAIL = "^(([^<>()\\[\\]\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL);

    public static final String REPO_NAME = "^[\\w- ]+$";
    public static final Pattern REPO_NAME_PATTERN = Pattern.compile(REPO_NAME);

    public static final String OD4B_INVALIDCHARACTERS_FILENAME = "^.*[#%*:<>?/|\"].*$";
    public static final Pattern OD4B_INVALIDCHARACTERS_FILENAME_PATTERN = Pattern.compile(OD4B_INVALIDCHARACTERS_FILENAME);

    public static final String CLASSIFICATION_NAME = "[^%'\"]+";
    public static final Pattern CLASSIFICATION_NAME_PATTERN = Pattern.compile(CLASSIFICATION_NAME);

    public static final String CC_TENANT_SUPPORT_CHAR = "^[\\w .-]*$";
    public static final Pattern CC_TENANT_SUPPORT_CHAR_PATTERN = Pattern.compile(CC_TENANT_SUPPORT_CHAR);

    public static final String CC_POLICY_SUPPORT_CHAR = "^[\\w .-]*$";
    public static final Pattern CC_POLICY_SUPPORT_CHAR_PATTERN = Pattern.compile(CC_POLICY_SUPPORT_CHAR);

    // project does not support '_" as it throws error while viewing file
    public static final String CC_PROJECT_SUPPORT_CHAR = "^[A-Za-z0-9 .-]*$";
    public static final Pattern CC_PROJECT_SUPPORT_CHAR_PATTERN = Pattern.compile(CC_PROJECT_SUPPORT_CHAR);

    public static final String ATTRIBUTE_NAME = "^[\\u00C0-\\u1FFF\\u2C00-\\uD7FF\\w \\x5F\\x2D]+$";
    public static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile(ATTRIBUTE_NAME);

    public static final String EMPTY_ADVANCED_CONDITION = "\\s*[(\\s*)]+\\s*";
    public static final Pattern EMPTY_ADVANCED_CONDITION_PATTERN = Pattern.compile(EMPTY_ADVANCED_CONDITION);

    public static final String POLICY_NAME_INVALIDCHARACTERS = "^.*[~/*$&\\?].*$";
    public static final Pattern POLICY_NAME_INVALIDCHARACTERS_PATTERN = Pattern.compile(POLICY_NAME_INVALIDCHARACTERS);

    public static final String DYNAMIC_MEMBERSHIP = "^user[1-9]\\d*@.*$";
    public static final Pattern DYNAMIC_MEMBERSHIP_PATTERN = Pattern.compile(DYNAMIC_MEMBERSHIP);

    public static final String SHAREPOINT_SITE_URL = "\\b(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    public static final Pattern SHAREPOINT_SITE_URL_PATTERN = Pattern.compile(SHAREPOINT_SITE_URL);
}

package com.nextlabs.common.util;

import com.google.gson.Gson;
import com.nextlabs.common.security.PropertyEncryptDecryptUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class PropertiesFileUtils {

    public static Properties decryptPropertyValues(Properties prop) {
        String encryptedKeys = prop.getProperty("encrypt.secrets");
        if (StringUtils.hasText(encryptedKeys)) {
            encryptedKeys = encryptedKeys.trim();
            List<String> keys = Arrays.asList(encryptedKeys.split(" "));
            for (String key : keys) {
                if (key.contains("attributes")) {
                    Gson gson = new Gson();
                    Map<String, String> map = GsonUtils.GSON.fromJson(prop.getProperty(key), GsonUtils.GENERIC_MAP_TYPE);
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        if (entry.getKey().toUpperCase().contains("SECRET")) {
                            String decryptedValue = PropertyEncryptDecryptUtil.decrypt(entry.getValue());
                            if (map.containsKey(entry.getKey())) {
                                map.put(entry.getKey(), decryptedValue);
                            }
                        }
                    }
                    prop.setProperty(key, gson.toJson(map));

                } else {
                    prop.setProperty(key, PropertyEncryptDecryptUtil.decrypt(prop.getProperty(key)));
                }

            }
        }
        return prop;
    }

    private PropertiesFileUtils() {
    }
}

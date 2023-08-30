package org.example;

import org.ini4j.Ini;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class IniConfigManager {
    public Map<String, Map<String, String>> convertIniToObjects(String filePath) {
        Map<String, Map<String, String>> result = new HashMap<>();

        try {
            Ini ini = new Ini(new File(filePath));

            for (Ini.Section section : ini.values()) {
                Map<String, String> dbSection = new HashMap<>();
                dbSection.put("host", section.get("host"));
                dbSection.put("port", section.get("port"));
                dbSection.put("user", section.get("user"));
                dbSection.put("password", section.get("password"));

                if (dbSection.get("host") != null && dbSection.get("port") != null
                        && dbSection.get("user") != null && dbSection.get("password") != null) {
                    result.put(section.getName(), dbSection);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}

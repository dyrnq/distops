package com.dyrnq.utils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import cn.hutool.core.io.resource.ResourceUtil;

import java.util.Properties;

public class VersionUtils {
    public static String getGitRevision() throws Exception {
        try {
            Properties properties = new Properties();
            properties.load(ResourceUtil.getStreamSafe("build.info"));
            return properties.getProperty("git.revision");
        } catch (Exception e) {
            return "dev";
        }
    }

    public static String getVersion() throws Exception {
        try {
            Properties properties = new Properties();
            properties.load(ResourceUtil.getStreamSafe("build.info"));
            String v = properties.getProperty("project.version");
            if (v != null) return "v" + v;
        } catch (Exception ignored) {}
        return "v0.0.0-dev";
    }
    public static String getBuildDateTime() {
        try {
            Properties properties = new Properties();
            properties.load(ResourceUtil.getStreamSafe("build.info"));
            String bt = properties.getProperty("build.time");
            if (bt != null) return bt;
        } catch (Exception ignored) {}
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

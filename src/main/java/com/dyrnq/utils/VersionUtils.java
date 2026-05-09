package com.dyrnq.utils;


import cn.hutool.core.io.resource.ResourceUtil;
import com.dyrnq.distops.Constants;

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
            return properties.getProperty("project.version");
        } catch (Exception e) {
            return Constants.VERSION;
        }
    }
}

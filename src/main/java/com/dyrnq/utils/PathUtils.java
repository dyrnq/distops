package com.dyrnq.utils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;

public class PathUtils {

    public static String homeAbsolutePath(String home, String projectName) {
        String homeAbsolutePath = "";
        String systemUserDir = SystemUtils.getUserHome().getAbsolutePath();
        if (StringUtils.isBlank(home)) {
            homeAbsolutePath = StringUtils.joinWith(File.separator, systemUserDir, projectName);
        } else {
            if (StringUtils.startsWith(home, "~")) {
                homeAbsolutePath = RegExUtils.replaceFirst(home, "~", systemUserDir);
            } else {
                homeAbsolutePath = home;
            }
        }
        return homeAbsolutePath;
    }
}

package com.dyrnq.distops;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class H2FormatVersionChecker {
    public static boolean isVer2(String filePath) {
        return checkVersion(filePath, 2);
    }

    public static boolean isVer3(String filePath) {
        return checkVersion(filePath, 3);
    }

    private static boolean checkVersion(String filePath, int ver) {
        InputStream fis = null;
        byte[] header = new byte[512];
        try {
            fis = new FileInputStream(filePath);
            int total = 0;
            while (total < header.length) {
                int count = fis.read(header, total, header.length - total);
                if (count == -1) break;
                total += count;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fis);
        }

        String fileHeader = new String(header, java.nio.charset.StandardCharsets.UTF_8);
        return StringUtils.contains(fileHeader, "format:" + ver);
    }
}

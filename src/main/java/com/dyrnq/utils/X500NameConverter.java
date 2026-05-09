package com.dyrnq.utils;

import org.apache.commons.lang3.StringUtils;

public class X500NameConverter {

    /**
     * @param X500Name eg C=CN, ST=GD, L=SZ, O=vihoo, OU=dev, CN=reg.domain.com, emailAddress=yy@vivo.com
     * @return
     */
    public static String toOpenSSL(String X500Name) {
        String[] part = StringUtils.splitByWholeSeparator(X500Name, ",");

        // 使用 / 分隔符连接属性名称和值
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < part.length; i++) {
            if (StringUtils.isNoneBlank(part)) {
                sb.append(StringUtils.trim(part[i])).append("/");
            }
        }
        // 去除末尾的分隔符
        if (sb.charAt(sb.length() - 1) == '/') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * @param openssl eg C=CN/ST=GD/L=SZ/O=vihoo/OU=dev/CN=reg.domain.com/emailAddress=yy@vivo.com
     * @return
     */
    public static String toX500Name(String openssl) {
        String[] part = StringUtils.splitByWholeSeparator(openssl, "/");

        // 使用 / 分隔符连接属性名称和值
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < part.length; i++) {
            if (StringUtils.isNoneBlank(part)) {
                sb.append(StringUtils.trim(part[i])).append(",");
            }
        }
        // 去除末尾的分隔符
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }
}

package com.dyrnq.utils;

import com.dyrnq.distops.Constants;
import com.password4j.BcryptFunction;
import com.password4j.Password;
import com.password4j.types.Bcrypt;

import java.util.regex.Matcher;

public class BcryptUtils {
    public static String hashPw(String plainPassword) {
        return Password.hash(plainPassword).with(BcryptFunction.getInstance(Constants.BCRYPT_COST)).getResult();
    }

    public static boolean checkPw(String plainPassword, String hash) {
        int cost = Constants.BCRYPT_COST;
        Matcher matcher = Constants.BCRYPT_PATTERN.matcher(hash);
        Bcrypt ver = Bcrypt.B;
        if (matcher.find()) {
            cost = Integer.parseInt(matcher.group(1));
            String verStr = matcher.group(0);
            ver = switch (verStr) {
                case "2a" -> Bcrypt.A;
                case "2x" -> Bcrypt.X;
                case "2y" -> Bcrypt.Y;
                default -> Bcrypt.B;
            };
        }

        return Password.check(plainPassword, hash).with(BcryptFunction.getInstance(ver, cost));
    }
}

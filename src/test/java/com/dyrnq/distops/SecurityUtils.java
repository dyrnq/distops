package com.dyrnq.distops;

import com.dyrnq.utils.BcryptUtils;
import com.password4j.BcryptFunction;
import com.password4j.Password;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecurityUtils {
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[ayb]\\$(\\d{2})\\$.*");
    public static void main(String[] args) {
        String myPassword = "test";

        String hash = Password.hash(myPassword).with(BcryptFunction.getInstance(12)).getResult();
        System.out.println("生成的 Hash: " + hash);


        boolean isMatch = Password.check(myPassword, hash).with(BcryptFunction.getInstance(12));
        System.out.println("密码是否匹配: " + isMatch); // 输出 true
        isMatch = Password.check(myPassword, hash).withBcrypt();





        System.out.println("密码是否匹配: " + BcryptUtils.checkPw(myPassword,hash)); // 输出 false
    }
}

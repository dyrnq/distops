package com.dyrnq.utils;

import cn.hutool.crypto.digest.BCrypt;
import java.security.SecureRandom;

public class BCryptPasswordEncoder {
    public static final BCryptPasswordEncoder DEFAULT = new BCryptPasswordEncoder(12);

    /**
     * A BCrypt hash of "dummy" at cost 12, used as a constant-time fallback
     * when checking passwords for non-existent users. This ensures the BCrypt
     * verification always runs at the same cost, eliminating the timing
     * side-channel that would otherwise reveal whether an account exists.
     */
    public static final String DUMMY_HASH = "$2a$12$dKXBt0YDtvfUjRqu3s1xleSJR2PYKczIizFhYd7zzlX5t6Z09XxRq";

    private final SecureRandom random;
    private final int strength;

    public BCryptPasswordEncoder(int strength) {
        this.random = new SecureRandom();
        this.strength = strength;
    }

    public String encode(CharSequence rawPassword) {
        String salt = BCrypt.gensalt(strength, random);
        return BCrypt.hashpw(rawPassword.toString(), salt);
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}

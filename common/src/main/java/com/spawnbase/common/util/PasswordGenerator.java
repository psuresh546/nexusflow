package com.spawnbase.common.util;

import java.security.SecureRandom;


public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS    = "0123456789";
    private static final String SPECIAL   = "!@#$%^&*";

    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
        throw new UnsupportedOperationException(
                "Utility class — do not instantiate");
    }

    public static String generate(int length) {
        if (length < 8) {
            throw new IllegalArgumentException(
                    "Password length must be at least 8");
        }

        StringBuilder password = new StringBuilder(length);

        // Guarantee at least one from each category
        password.append(randomChar(UPPERCASE));
        password.append(randomChar(LOWERCASE));
        password.append(randomChar(DIGITS));
        password.append(randomChar(SPECIAL));

        // Fill the rest randomly
        for (int i = 4; i < length; i++) {
            password.append(randomChar(ALL_CHARS));
        }

        // Shuffle — don't want first 4 chars to always
        // follow the same pattern (Upper, Lower, Digit, Special)
        return shuffle(password.toString());
    }

    /**
     * Generate a default 16-character password.
     */
    public static String generate() {
        return generate(16);
    }

    private static char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }

    private static String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
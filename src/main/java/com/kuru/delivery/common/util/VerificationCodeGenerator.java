package com.kuru.delivery.common.util;

import java.security.SecureRandom;

public final class VerificationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCodeGenerator() {
    }

    /**
     * Generates a 6-digit numeric code as a String, zero-padded if necessary.
     */
    public static String generate6DigitCode() {
        int code = RANDOM.nextInt(1_000_000); // 0 - 999999
        return String.format("%06d", code);
    }
}



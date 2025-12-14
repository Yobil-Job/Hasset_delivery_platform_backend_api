package com.kuru.delivery.common.util;

import java.util.regex.Pattern;

/**
 * Utility class for password validation.
 * Enforces strong password requirements:
 * - At least 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private PasswordValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates if a password meets the security requirements.
     *
     * @param password the password to validate
     * @return true if password is valid, false otherwise
     */
    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        return UPPERCASE_PATTERN.matcher(password).find() &&
               LOWERCASE_PATTERN.matcher(password).find() &&
               DIGIT_PATTERN.matcher(password).find() &&
               SPECIAL_CHAR_PATTERN.matcher(password).find();
    }

    /**
     * Validates password and returns a detailed error message if invalid.
     *
     * @param password the password to validate
     * @return error message if invalid, null if valid
     */
    public static String validateWithMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }

        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long";
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one uppercase letter";
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one lowercase letter";
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            return "Password must contain at least one digit";
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\",./<>?)";
        }

        return null; // Password is valid
    }
}


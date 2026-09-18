package dev.hieplp.mci.core;

/**
 * Internal string helpers for decimal-number operands: input validation
 * and result normalization shared by {@link MyBigNumber}.
 *
 * <p>Package-private; not part of the public API.</p>
 *
 * @author HiepLP (hiepphuocly@gmail.com)
 * @version 0.0.1
 * @since 0.0.1
 */
final class NumberStrings {

    private static final AppLogger LOG = AppLogger.of(NumberStrings.class);

    private NumberStrings() {
    }

    /**
     * Ensures {@code s} is a non-empty string of ASCII digits.
     *
     * @param s    operand value to check
     * @param name operand name used in log and exception messages
     * @throws IllegalArgumentException if {@code s} is null, empty, or
     *         contains a non-ASCII-digit character
     */
    static void requireDigits(String s, String name) {
        if (s == null || s.isEmpty()) {
            LOG.warn("Invalid operand {0}: null or empty", name);
            throw new IllegalArgumentException(name + " must not be null or empty");
        }
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c < '0' || c > '9') {
                LOG.warn("Invalid operand {0}: non-digit character ''{1}'' at index {2}", name, c, k);
                throw new IllegalArgumentException(
                        name + " contains non-digit character '" + c + "' at index " + k);
            }
        }
    }
    /**
     * Removes leading zeros; {@code "000"} becomes {@code "0"}.
     *
     * @param s digit string (assumed already validated)
     * @return {@code s} without leading zeros, never empty
     */

    static String stripLeadingZeros(String s) {
        int k = 0;
        while (k < s.length() - 1 && s.charAt(k) == '0') {
            k++;
        }
        return s.substring(k);
    }
}

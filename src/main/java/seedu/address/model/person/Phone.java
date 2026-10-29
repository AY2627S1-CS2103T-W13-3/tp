package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's phone number in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}
 */
public class Phone {
    public static final String MESSAGE_CONSTRAINTS =
            "Phone numbers need at least 3 digits (0-9). An optional leading + and single spaces or hyphens between"
                    + " digit groups are allowed.";
    private static final int MIN_PHONE_DIGITS = 3;
    private static final int MIN_QUERY_DIGITS = 1;

    public final String value;
    private final String digits;

    /**
     * Constructs a {@code Phone}.
     *
     * @param phone A valid phone number.
     */
    public Phone(String phone) {
        requireNonNull(phone);
        checkArgument(isValidPhone(phone), MESSAGE_CONSTRAINTS);
        value = phone;
        digits = extractDigits(phone);
    }

    /**
     * Returns true if a given string is a valid phone number.
     */
    public static boolean isValidPhone(String test) {
        return hasValidFormat(test, MIN_PHONE_DIGITS);
    }

    /**
     * Returns whether a search query has valid phone formatting and at least one ASCII digit.
     * Search fragments may be shorter than stored phone numbers.
     */
    public static boolean isValidPhoneQuery(String query) {
        return hasValidFormat(query, MIN_QUERY_DIGITS);
    }

    private static boolean hasValidFormat(String test, int minimumDigits) {
        requireNonNull(test);
        int digitCount = 0;
        boolean wasPreviousDigit = false;
        for (int i = 0; i < test.length(); i++) {
            char character = test.charAt(i);
            if (character >= '0' && character <= '9') {
                digitCount++;
                wasPreviousDigit = true;
            } else if (i == 0 && character == '+') {
                wasPreviousDigit = false;
            } else if ((character == ' ' || character == '-') && wasPreviousDigit
                    && i + 1 < test.length() && test.charAt(i + 1) >= '0' && test.charAt(i + 1) <= '9') {
                wasPreviousDigit = false;
            } else {
                return false;
            }
        }
        // The look-ahead check ensures every accepted separator is followed by a digit.
        return digitCount >= minimumDigits;
    }

    /**
     * Removes the optional plus sign and display separators, retaining leading zeroes.
     */
    static String extractDigits(String value) {
        return value.replace("+", "").replace(" ", "").replace("-", "");
    }

    /**
     * Returns whether both phone numbers contain the same digits, regardless of display formatting.
     */
    boolean hasSameDigitsAs(Phone other) {
        return other != null && digits.equals(other.digits);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Phone otherPhone)) {
            return false;
        }

        return value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

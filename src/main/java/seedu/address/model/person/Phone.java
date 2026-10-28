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
        digits = phone.replace("+", "").replace(" ", "").replace("-", "");
    }

    /**
     * Returns true if a given string is a valid phone number.
     */
    public static boolean isValidPhone(String test) {
        requireNonNull(test);
        int digitCount = 0;
        boolean previousWasDigit = false;
        for (int i = 0; i < test.length(); i++) {
            char character = test.charAt(i);
            if (character >= '0' && character <= '9') {
                digitCount++;
                previousWasDigit = true;
            } else if (i == 0 && character == '+') {
                previousWasDigit = false;
            } else if ((character == ' ' || character == '-') && previousWasDigit
                    && i + 1 < test.length() && test.charAt(i + 1) >= '0' && test.charAt(i + 1) <= '9') {
                previousWasDigit = false;
            } else {
                return false;
            }
        }
        return digitCount >= 3 && previousWasDigit;
    }

    /** Returns whether both phone numbers contain the same digits, regardless of display formatting. */
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

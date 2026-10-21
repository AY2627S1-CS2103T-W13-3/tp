package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's email in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final String MESSAGE_CONSTRAINTS =
            "Email format is not supported. Example: rachel.lim+work@example.com.";
    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    /**
     * Returns true if a given string is a valid email.
     * Scans once without recursive regex matching, including for long input.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        int separator = test.indexOf('@');
        if (separator <= 0 || separator == test.length() - 1) {
            return false;
        }
        boolean previousAlphanumeric = false;
        for (int i = 0; i < separator; i++) {
            char character = test.charAt(i);
            boolean alphanumeric = isAsciiAlphanumeric(character);
            if (!alphanumeric && (!previousAlphanumeric || "+_.-".indexOf(character) < 0)) {
                return false;
            }
            previousAlphanumeric = alphanumeric;
        }
        if (!previousAlphanumeric) {
            return false;
        }

        // Each domain label starts/ends with an alphanumeric character; separators cannot repeat.
        // The last label also needs an adjacent alphanumeric pair, matching the existing grammar.
        previousAlphanumeric = false;
        boolean hasAdjacentPair = false;
        for (int i = separator + 1; i < test.length(); i++) {
            char character = test.charAt(i);
            if (isAsciiAlphanumeric(character)) {
                hasAdjacentPair |= previousAlphanumeric;
                previousAlphanumeric = true;
            } else if ((character == '-' || character == '.') && previousAlphanumeric) {
                previousAlphanumeric = false;
                if (character == '.') {
                    hasAdjacentPair = false;
                }
            } else {
                return false;
            }
        }
        return previousAlphanumeric && hasAdjacentPair;
    }

    private static boolean isAsciiAlphanumeric(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9';
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
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

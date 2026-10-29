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
    public static boolean isValidEmail(String email) {
        requireNonNull(email);
        int separator = email.indexOf('@');
        if (separator <= 0 || separator == email.length() - 1) {
            return false;
        }
        return isValidLocalPart(email, separator) && isValidDomain(email, separator + 1);
    }

    /** Validates the local part in place, preserving the supported punctuation grammar. */
    private static boolean isValidLocalPart(String email, int separator) {
        boolean wasPreviousCharacterAlphanumeric = false;
        for (int i = 0; i < separator; i++) {
            char character = email.charAt(i);
            boolean isAlphanumeric = isAsciiAlphanumeric(character);
            if (!isAlphanumeric && (i == 0 || "+_.-".indexOf(character) < 0
                    || email.charAt(i - 1) == '.' || character == '.' && !wasPreviousCharacterAlphanumeric)) {
                return false;
            }
            wasPreviousCharacterAlphanumeric = isAlphanumeric;
        }
        return wasPreviousCharacterAlphanumeric;
    }

    /** Validates domain labels in place; the final label requires adjacent alphanumeric characters. */
    private static boolean isValidDomain(String email, int domainStart) {
        boolean wasPreviousCharacterAlphanumeric = false;
        boolean hasAdjacentPair = false;
        for (int i = domainStart; i < email.length(); i++) {
            char character = email.charAt(i);
            if (isAsciiAlphanumeric(character)) {
                hasAdjacentPair |= wasPreviousCharacterAlphanumeric;
                wasPreviousCharacterAlphanumeric = true;
            } else if (character == '-' && (wasPreviousCharacterAlphanumeric || email.charAt(i - 1) == '-')) {
                wasPreviousCharacterAlphanumeric = false;
            } else if (character == '.' && wasPreviousCharacterAlphanumeric) {
                wasPreviousCharacterAlphanumeric = false;
                hasAdjacentPair = false;
            } else {
                return false;
            }
        }
        return wasPreviousCharacterAlphanumeric && hasAdjacentPair;
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

package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's address in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidAddress(String)}
 */
public class Address {

    public static final String MESSAGE_CONSTRAINTS =
            "Addresses must contain valid, non-blank single-line Unicode text without control characters.";

    // Single-line text may contain Unicode and punctuation, but not controls or line separators.
    public static final String VALIDATION_REGEX = "[^\\x00-\\x1f\\x7f-\\x9f\\u2028\\u2029]+";

    public final String value;

    /**
     * Constructs an {@code Address}.
     *
     * @param address A valid address.
     */
    public Address(String address) {
        requireNonNull(address);
        String trimmedAddress = address.replaceAll("^ +| +$", "");
        checkArgument(isValidAddress(trimmedAddress), MESSAGE_CONSTRAINTS);
        value = trimmedAddress;
    }

    /**
     * Returns true if a given string is a valid address.
     */
    public static boolean isValidAddress(String test) {
        String trimmedAddress = test.replaceAll("^ +| +$", "");
        return !trimmedAddress.isEmpty() && trimmedAddress.matches(VALIDATION_REGEX)
                && hasValidSurrogatePairs(trimmedAddress);
    }

    /** Returns whether every UTF-16 surrogate belongs to a complete high/low pair. */
    private static boolean hasValidSurrogatePairs(String text) {
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (index + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(index + 1))) {
                    return false;
                }
                index++;
            } else if (Character.isLowSurrogate(character)) {
                return false;
            }
        }
        return true;
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
        if (!(other instanceof Address otherAddress)) {
            return false;
        }

        return value.equals(otherAddress.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

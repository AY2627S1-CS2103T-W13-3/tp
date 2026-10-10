package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's address in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidAddress(String)}
 */
public class Address {

    public static final String MESSAGE_CONSTRAINTS = "Addresses must not be blank.";

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
        return !trimmedAddress.isEmpty() && trimmedAddress.matches(VALIDATION_REGEX);
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

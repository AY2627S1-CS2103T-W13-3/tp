package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.StringUtil.trimOrdinarySpaces;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names may contain letters from any language, digits 0-9, combining accents and ordinary spaces."
                    + " Names must not be blank.";

    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        String trimmedName = trimOrdinarySpaces(name);
        checkArgument(isValidName(trimmedName), MESSAGE_CONSTRAINTS);
        fullName = trimmedName;
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        String trimmedName = trimOrdinarySpaces(test);
        if (trimmedName.isEmpty()) {
            return false;
        }
        boolean canHaveCombiningMark = false;
        for (int offset = 0; offset < trimmedName.length();) {
            int codePoint = trimmedName.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                canHaveCombiningMark = true;
            } else if (codePoint >= '0' && codePoint <= '9' || codePoint == ' ') {
                canHaveCombiningMark = false;
            } else if (!canHaveCombiningMark || !isCombiningMark(codePoint)) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}

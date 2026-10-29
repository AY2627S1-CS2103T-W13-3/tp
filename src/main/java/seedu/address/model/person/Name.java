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
            "Names may contain letters from any language, digits 0-9, combining accents, ordinary spaces and / - ' @ ."
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
        int first = trimmedName.codePointAt(0);
        if (!Character.isLetter(first) && !(first >= '0' && first <= '9')) {
            return false;
        }
        boolean canHaveCombiningMark = false;
        boolean hasVisibleContent = false;
        for (int offset = 0; offset < trimmedName.length();) {
            int codePoint = trimmedName.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                canHaveCombiningMark = true;
                hasVisibleContent |= !isHangulFiller(codePoint);
            } else if (codePoint >= '0' && codePoint <= '9' || codePoint == ' '
                    || codePoint == '/' || codePoint == '-' || codePoint == '\''
                    || codePoint == '@' || codePoint == '.') {
                canHaveCombiningMark = false;
                hasVisibleContent |= codePoint >= '0' && codePoint <= '9';
            } else if (!canHaveCombiningMark || !isCombiningMark(codePoint)) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return hasVisibleContent;
    }

    /** These letters can accompany a name but cannot provide visible content on their own. */
    private static boolean isHangulFiller(int codePoint) {
        return codePoint == 0x115f || codePoint == 0x1160 || codePoint == 0x3164 || codePoint == 0xffa0;
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

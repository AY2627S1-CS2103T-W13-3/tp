package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.StringUtil.trimOrdinarySpaces;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Represents a client's single pending action. Instances are immutable and may have past due dates.
 */
public final class FollowUp {

    public static final String MESSAGE_INVALID_DATE =
            "Dates must exist and use YYYY-MM-DD format, with a year from 0001 to 9999.";
    public static final String MESSAGE_INVALID_DESCRIPTION =
            "Follow-up descriptions must contain 1 to 200 Unicode code points after trimming, "
                    + "including visible text and no controls, line breaks or invalid characters.";

    private final LocalDate dueDate;
    private final String description;

    /**
     * Constructs a follow-up with a valid date and description.
     * Past dates are allowed so that overdue actions can be restored from storage.
     *
     * @throws NullPointerException if either argument is null.
     * @throws IllegalArgumentException if the year or description is invalid.
     */
    public FollowUp(LocalDate dueDate, String description) {
        requireNonNull(dueDate);
        requireNonNull(description);
        checkArgument(isValidYear(dueDate), MESSAGE_INVALID_DATE);
        String trimmedDescription = trimOrdinarySpaces(description);
        checkArgument(isValidDescription(trimmedDescription), MESSAGE_INVALID_DESCRIPTION);
        this.dueDate = dueDate;
        this.description = trimmedDescription;
    }

    /**
     * Parses an exact YYYY-MM-DD date after removing outer ordinary spaces.
     *
     * @throws NullPointerException if {@code text} is null.
     * @throws IllegalArgumentException if the format, calendar date or year is invalid.
     */
    public static LocalDate parseDate(String text) {
        requireNonNull(text);
        String trimmedDate = trimOrdinarySpaces(text);
        checkArgument(trimmedDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"), MESSAGE_INVALID_DATE);
        try {
            LocalDate date = LocalDate.parse(trimmedDate);
            checkArgument(isValidYear(date), MESSAGE_INVALID_DATE);
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DATE, e);
        }
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the action's status relative to the supplied date, without consulting the system clock.
     */
    public FollowUpStatus getStatus(LocalDate today) {
        requireNonNull(today);
        if (dueDate.isBefore(today)) {
            return FollowUpStatus.OVERDUE;
        }
        if (dueDate.isEqual(today)) {
            return FollowUpStatus.DUE_TODAY;
        }
        return FollowUpStatus.UPCOMING;
    }

    private static boolean isValidYear(LocalDate date) {
        return date.getYear() >= 1 && date.getYear() <= 9999;
    }

    /**
     * Validates single-line Unicode text without changing its spelling or joining sequences.
     */
    private static boolean isValidDescription(String text) {
        int codePointCount = 0;
        boolean hasVisibleContent = false;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            int type = Character.getType(codePoint);
            codePointCount++;
            if (codePointCount > 200 || type == Character.CONTROL || type == Character.SURROGATE
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR
                    || type == Character.UNASSIGNED) {
                return false;
            }
            hasVisibleContent |= isVisibleDescriptionCharacter(codePoint);
            index += Character.charCount(codePoint);
        }
        return hasVisibleContent;
    }

    /**
     * Marks and format characters may accompany visible text but cannot form a note by themselves.
     */
    private static boolean isVisibleDescriptionCharacter(int codePoint) {
        if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
            return false;
        }
        return switch (Character.getType(codePoint)) {
            case Character.FORMAT, Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK,
                    Character.ENCLOSING_MARK -> false;
            // Hangul fillers and Braille blank have letter/symbol categories but do not provide visible content.
            default -> codePoint != 0x115f && codePoint != 0x1160 && codePoint != 0x3164
                    && codePoint != 0xffa0 && codePoint != 0x2800;
        };
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof FollowUp otherFollowUp)) {
            return false;
        }
        return dueDate.equals(otherFollowUp.dueDate) && description.equals(otherFollowUp.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dueDate, description);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("dueDate", dueDate)
                .add("description", description)
                .toString();
    }
}

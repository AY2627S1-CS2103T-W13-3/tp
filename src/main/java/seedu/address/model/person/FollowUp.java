package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

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
            "Follow-up descriptions must contain 1 to 200 printable English characters after trimming.";

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
        checkArgument(trimmedDescription.matches("[ -~]{1,200}"), MESSAGE_INVALID_DESCRIPTION);
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

    private static String trimOrdinarySpaces(String text) {
        return text.replaceAll("^ +| +$", "");
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

package seedu.address.model.person;

/**
 * Represents the status of a pending action relative to a supplied date.
 */
public enum FollowUpStatus {
    OVERDUE("OVERDUE"),
    DUE_TODAY("DUE TODAY"),
    UPCOMING("UPCOMING");

    private final String displayName;

    FollowUpStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

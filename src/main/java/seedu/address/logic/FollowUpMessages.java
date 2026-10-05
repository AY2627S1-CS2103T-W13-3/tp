package seedu.address.logic;

import seedu.address.model.person.FollowUp;

/**
 * Contains the shared user-facing messages for the follow-up workflow.
 */
public final class FollowUpMessages {

    public static final String MESSAGE_RECORDED = "Follow-up for %1$s recorded for %2$s.";
    public static final String MESSAGE_UPDATED = "Follow-up for %1$s updated to %2$s.";
    public static final String MESSAGE_CLEARED = "Follow-up for %1$s cleared.";
    public static final String MESSAGE_NO_FOLLOW_UP = "This client has no pending follow-up.";
    public static final String MESSAGE_PENDING = "Pending follow-ups";
    public static final String MESSAGE_NO_PENDING = "There are no pending follow-ups.";
    public static final String MESSAGE_NO_CLIENTS = "No clients to display.";
    public static final String MESSAGE_INVALID_INDEX = "Index must be a positive integer from 1 to 2147483647.";
    public static final String MESSAGE_INDEX_OUT_OF_RANGE = "The client index provided is invalid.";
    public static final String MESSAGE_INVALID_DATE = FollowUp.MESSAGE_INVALID_DATE;
    public static final String MESSAGE_PAST_DATE = "Follow-up date cannot be in the past.";
    public static final String MESSAGE_INVALID_DESCRIPTION = FollowUp.MESSAGE_INVALID_DESCRIPTION;
    public static final String MESSAGE_INVALID_CHARACTERS =
            "Use one command line and ordinary spaces. Control characters are not allowed.";
    public static final String MESSAGE_EMPTY_COMMAND = "Enter a command.";
    public static final String MESSAGE_DUPLICATE_PREFIXES =
            "Multiple values specified for the following single-valued field(s): ";
    public static final String MESSAGE_SAVE_FAILED =
            "Changes could not be saved. No changes were kept. Try the command again.";
    public static final String MESSAGE_LOAD_WARNING =
            "Saved data could not be loaded. Client changes are blocked to protect your saved data. "
            + "Close Policy Harbour and restore a valid saved copy before trying again.";
    public static final String MESSAGE_CHANGES_BLOCKED =
            "Client changes are blocked because saved data could not be loaded. "
            + "Close Policy Harbour and restore valid saved data.";
    public static final String MESSAGE_STARTUP = "Listed all clients.";
    public static final String MESSAGE_INVALID_FORMAT = "Invalid command format!\n"
            + "Usage: followup INDEX d/DATE m/DESCRIPTION\n"
            + "Or: followup INDEX clear";

    private FollowUpMessages() {}
}

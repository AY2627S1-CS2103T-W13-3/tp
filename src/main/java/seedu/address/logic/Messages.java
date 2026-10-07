package seedu.address.logic;

import java.util.LinkedHashSet;
import java.util.Set;

import seedu.address.logic.parser.Prefix;
import seedu.address.model.person.Person;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_PERSON_DISPLAYED_INDEX = "The client index provided is invalid.";
    public static final String MESSAGE_PERSONS_LISTED_OVERVIEW = "%1$d person(s) listed!";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";
    public static final String MESSAGE_INVALID_CHARACTERS =
            "Use one command line and ordinary spaces. Control characters are not allowed.";
    public static final String MESSAGE_EMPTY_COMMAND = "Enter a command.";
    public static final String MESSAGE_LOAD_WARNING = "Saved data could not be loaded. Client changes are blocked "
            + "to protect your saved data. Close Policy Harbour and restore a valid saved copy before trying again.";
    public static final String MESSAGE_CHANGES_BLOCKED = "Client changes are blocked because saved data could not be "
            + "loaded. Close Policy Harbour and restore valid saved data.";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields = new LinkedHashSet<>();
        for (Prefix duplicatePrefix : duplicatePrefixes) {
            duplicateFields.add(duplicatePrefix.toString());
        }

        return MESSAGE_DUPLICATE_FIELDS + String.join(", ", duplicateFields);
    }

    /**
     * Formats the {@code person} for display to the user.
     */
    public static String format(Person person) {
        final StringBuilder builder = new StringBuilder();
        builder.append(person.getName())
                .append("; Phone: ")
                .append(person.getPhone())
                .append("; Email: ")
                .append(person.getEmail())
                .append("; Address: ")
                .append(person.getAddress())
                .append("; Tags: ");
        person.getTags().forEach(builder::append);
        return builder.toString();
    }

}

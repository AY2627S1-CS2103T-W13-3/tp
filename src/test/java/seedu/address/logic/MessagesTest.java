package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DESCRIPTION;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;

import org.junit.jupiter.api.Test;

public class MessagesTest {

    @Test
    public void getErrorMessageForDuplicatePrefixes_preservesSuppliedOrderAndRemovesDuplicates() {
        assertEquals("Multiple values specified for the following single-valued field(s): d/, m/",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_FOLLOW_UP_DATE,
                        PREFIX_FOLLOW_UP_DESCRIPTION, PREFIX_FOLLOW_UP_DATE));
    }

    @Test
    public void getErrorMessageForDuplicatePrefixes_singlePrefix_hasNoSeparator() {
        assertEquals("Multiple values specified for the following single-valued field(s): n/",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));
    }
}

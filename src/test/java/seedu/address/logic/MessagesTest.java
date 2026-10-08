package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FOLLOW_UP_DESCRIPTION;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

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

    @Test
    public void format_contactDetailsAndTags_usesExactSortedClientText() {
        Person person = new PersonBuilder().withName("Rachel Lim").withPhone("001")
                .withEmail("Rachel@example.com").withAddress("Blk 123")
                .withTags("HEALTH", "active", "2026")
                .withFollowUp(new FollowUp(LocalDate.of(2026, 10, 9), "Call client")).build();
        assertEquals("Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; "
                + "Tags: [2026] [active] [health]", Messages.format(person));
        assertEquals("Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; Tags: None",
                Messages.format(new PersonBuilder(person).withTags().build()));
        assertEquals("0 client(s) listed!", String.format(Messages.MESSAGE_PERSONS_LISTED_OVERVIEW, 0));
    }

}

package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_CHANGES_BLOCKED;
import static seedu.address.logic.Messages.MESSAGE_EMPTY_COMMAND;
import static seedu.address.logic.Messages.MESSAGE_INVALID_CHARACTERS;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EditCommand.EditPersonDescriptor;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.SetFollowUpCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_PERSON), command);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Person person = new PersonBuilder().build();
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder(person).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_PERSON.getOneBased() + " " + PersonUtil.getEditPersonDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_PERSON, descriptor), command);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new NameContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD + " 3") instanceof ListCommand);
    }

    @Test
    public void parseCommand_emptyInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_EMPTY_COMMAND, () -> parser.parseCommand("   "));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }

    @Test
    public void parseCommand_followUps_ignoresTrailingText() throws Exception {
        assertTrue(parser.parseCommand("followups any trailing text") instanceof FollowUpsCommand);
    }

    @Test
    public void parseCommand_followUp_routesToSingularParser() throws Exception {
        assertEquals(new SetFollowUpCommand(INDEX_FIRST_PERSON, "2026-10-05", "Send quotation"),
                parser.parseCommand("followup 1 d/2026-10-05 m/Send quotation"));
    }

    @Test
    public void parseCommand_controlCharacter_throwsBeforeOtherChecks() {
        assertThrows(ParseException.class, MESSAGE_INVALID_CHARACTERS,
                () -> parser.parseCommand("unknown\u2028command", true));
    }

    @Test
    public void parseCommand_loadingBlocked_blocksMutationsButNotQueries() throws Exception {
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED,
                () -> parser.parseCommand("followup malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED,
                () -> parser.parseCommand("add malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED,
                () -> parser.parseCommand("edit malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED,
                () -> parser.parseCommand("delete malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED,
                () -> parser.parseCommand("clear", true));
        assertTrue(parser.parseCommand("followups", true) instanceof FollowUpsCommand);
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND,
                () -> parser.parseCommand("unknown", true));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND,
                () -> parser.parseCommand("FOLLOWUP 1 clear"));
    }
}

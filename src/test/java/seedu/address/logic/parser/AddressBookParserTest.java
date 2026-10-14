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
import seedu.address.logic.commands.ClearFollowUpCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.SetFollowUpCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_view_routesInNormalAndBlockedSessions() throws Exception {
        for (boolean blocked : List.of(false, true)) {
            assertEquals(new ViewCommand(INDEX_FIRST_PERSON), parser.parseCommand(" view 0001 ", blocked));
            assertThrows(ParseException.class, "Invalid command format!\nUsage: view INDEX", () ->
                    parser.parseCommand("view 0 extra", blocked));
            assertThrows(ParseException.class, "Index must be a positive integer from 1 to 2147483647.", () ->
                    parser.parseCommand("view 0", blocked));
            assertThrows(ParseException.class, "Unknown command.", () -> parser.parseCommand("VIEW 1", blocked));
        }
    }

    @Test
    public void parseCommand_view_invalidCharactersTakeGlobalPriority() {
        for (String character : List.of("\t", "\n", "\r", "\u0000", "\u007f", "\u0085", "\u2028", "\u2029")) {
            assertThrows(ParseException.class,
                    "Use one command line and ordinary spaces. Control characters are not allowed.", () ->
                    parser.parseCommand("view 0 extra" + character, true));
        }
    }

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_standaloneClear_isUnknownInNormalAndBlockedSessions() {
        assertUnsupportedRoute("clear");
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_PERSON.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_PERSON), command);
    }

    @Test
    public void parseCommand_edit_isUnknownInNormalAndBlockedSessions() {
        assertUnsupportedRoute("edit");
        assertThrows(ParseException.class, "Unknown command.", () ->
                parser.parseCommand("edit 1 n/Alice", true));
    }

    @Test
    public void parseCommand_exit_isUnknownInNormalAndBlockedSessions() {
        assertUnsupportedRoute("exit");
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new NameContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_find_validatesGloballyBeforeParsingInNormalAndBlockedSessions() throws Exception {
        for (boolean blocked : List.of(false, true)) {
            assertEquals(new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice", "Bob"))),
                    parser.parseCommand(" find   Alice  Bob ", blocked));
            assertThrows(ParseException.class, "Invalid command format!\nUsage: find KEYWORD [MORE_KEYWORDS]...", () ->
                    parser.parseCommand("find   ", blocked));
            assertThrows(ParseException.class, "Unknown command.", () -> parser.parseCommand("FIND Alice", blocked));
            for (String character : List.of("\t", "\n", "\r", "\u0000", "\u007f", "\u0085",
                    "\u2028", "\u2029")) {
                assertThrows(ParseException.class,
                        "Use one command line and ordinary spaces. Control characters are not allowed.", () ->
                        parser.parseCommand("find " + character, blocked));
            }
        }
    }

    @Test
    public void parseCommand_help_isUnknownInNormalAndBlockedSessions() {
        assertUnsupportedRoute("help");
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
        assertEquals(new ClearFollowUpCommand(INDEX_FIRST_PERSON), parser.parseCommand("followup 0001 clear"));
    }

    @Test
    public void parseCommand_unsupportedRoutes_invalidCharactersTakeGlobalPriority() {
        for (String commandWord : List.of("edit", "clear", "help", "exit")) {
            for (boolean blocked : List.of(false, true)) {
                assertThrows(ParseException.class,
                        "Use one command line and ordinary spaces. Control characters are not allowed.", () ->
                        parser.parseCommand(commandWord + "\t", blocked));
            }
        }
    }

    @Test
    public void parseCommand_controlCharacter_throwsBeforeOtherChecks() {
        assertThrows(ParseException.class, MESSAGE_INVALID_CHARACTERS, () ->
                parser.parseCommand("unknown\u2028command", true));
    }

    @Test
    public void parseCommand_loadingBlocked_blocksMutationsButNotQueries() throws Exception {
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                parser.parseCommand("followup malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                parser.parseCommand("add malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                parser.parseCommand("delete malformed", true));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                parser.parseCommand("followup 1 clear", true));
        assertTrue(parser.parseCommand("followups", true) instanceof FollowUpsCommand);
        assertTrue(parser.parseCommand("list", true) instanceof ListCommand);
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknown", true));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("FOLLOWUP 1 clear"));
    }

    private void assertUnsupportedRoute(String commandWord) {
        for (boolean blocked : List.of(false, true)) {
            for (String suffix : List.of("", " 1", " malformed extra")) {
                assertThrows(ParseException.class, "Unknown command.", () ->
                        parser.parseCommand(" " + commandWord + suffix + " ", blocked));
            }
        }
    }
}

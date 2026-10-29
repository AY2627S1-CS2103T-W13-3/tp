package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_CHANGES_BLOCKED;
import static seedu.address.logic.Messages.MESSAGE_EMPTY_COMMAND;
import static seedu.address.logic.Messages.MESSAGE_INVALID_CHARACTERS;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearFollowUpCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EditCommand.EditPersonDescriptor;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.SetFollowUpCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_view_routesInNormalAndBlockedSessions() throws Exception {
        for (boolean blocked : List.of(false, true)) {
            assertEquals(new ViewCommand(new ClientId(1)), parser.parseCommand(" view 0001 ", blocked));
            assertThrows(ParseException.class,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, ViewCommand.MESSAGE_USAGE), () ->
                    parser.parseCommand("view 0 extra", blocked));
            assertThrows(ParseException.class, ClientIdParserUtil.MESSAGE_INVALID_CLIENT_ID, () ->
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
                DeleteCommand.COMMAND_WORD + " " + 1);
        assertEquals(new DeleteCommand(new ClientId(1)), command);
    }

    @Test
    public void parseCommand_edit_routesAndProtectsLoading() throws Exception {
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        descriptor.setName(new Name("Alice"));
        assertEquals(new EditCommand(new ClientId(17), descriptor), parser.parseCommand("edit 0017 n/Alice"));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                parser.parseCommand("edit malformed", true));
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
        assertEquals(new FindCommand(new ClientContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_find_validatesGloballyBeforeParsingInNormalAndBlockedSessions() throws Exception {
        for (boolean blocked : List.of(false, true)) {
            assertEquals(new FindCommand(new ClientContainsKeywordsPredicate(List.of("Alice", "Bob"))),
                    parser.parseCommand(" find   Alice  Bob ", blocked));
            assertThrows(ParseException.class,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE), () ->
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
        assertTrue(parser.parseCommand("  list   ") instanceof ListCommand);
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
    public void parseCommand_followUps_acceptsOuterSpaces() throws Exception {
        assertTrue(parser.parseCommand("  followups   ") instanceof FollowUpsCommand);
    }

    @Test
    public void parseCommand_noArgumentCommands_rejectsExtraArguments() {
        for (String command : List.of("list", "followups")) {
            for (boolean blocked : List.of(false, true)) {
                for (String suffix : List.of(" words", " 123", " n/Rachel", " list", " \u00a0")) {
                    String usage = "Usage: " + command + " (no arguments)";
                    assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage), () ->
                            parser.parseCommand(" " + command + suffix + " ", blocked));
                }
                assertThrows(ParseException.class, MESSAGE_INVALID_CHARACTERS, () ->
                        parser.parseCommand(command + " words\n", blocked));
            }
        }
    }

    @Test
    public void parseCommand_targetedCommands_rejectsExtraTokens() {
        for (String command : List.of("view 1 words", "delete 1 words", "edit 1 words n/Rachel",
                "followup 1 clear words", "followup 1 words d/2099-12-31 m/Call client")) {
            assertThrows(ParseException.class, () -> parser.parseCommand(command));
        }
    }

    @Test
    public void parseCommand_followUp_routesToSingularParser() throws Exception {
        assertEquals(new SetFollowUpCommand(new ClientId(1), "2026-10-05", "Send quotation"),
                parser.parseCommand("followup 1 d/2026-10-05 m/Send quotation"));
        assertEquals(new ClearFollowUpCommand(new ClientId(1)), parser.parseCommand("followup 0001 clear"));
    }

    @Test
    public void parseCommand_supportedAndUnsupportedRoutes_invalidCharactersTakeGlobalPriority() {
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

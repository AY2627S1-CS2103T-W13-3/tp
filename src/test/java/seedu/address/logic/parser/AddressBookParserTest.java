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
import java.util.Locale;
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
        for (boolean isDataLoadingBlocked : List.of(false, true)) {
            assertEquals(new ViewCommand(new ClientId(1)), parser.parseCommand(" view 0001 ", isDataLoadingBlocked));
            assertThrows(ParseException.class,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, ViewCommand.MESSAGE_USAGE), () ->
                    parser.parseCommand("view 0 extra", isDataLoadingBlocked));
            assertThrows(ParseException.class, ClientIdParserUtil.MESSAGE_INVALID_CLIENT_ID, () ->
                    parser.parseCommand("view 0", isDataLoadingBlocked));
            assertEquals(new ViewCommand(new ClientId(1)), parser.parseCommand("VIEW 1", isDataLoadingBlocked));
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
        for (boolean isDataLoadingBlocked : List.of(false, true)) {
            assertEquals(new FindCommand(new ClientContainsKeywordsPredicate(List.of("Alice", "Bob"))),
                    parser.parseCommand(" find   Alice  Bob ", isDataLoadingBlocked));
            assertThrows(ParseException.class,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE), () ->
                    parser.parseCommand("find   ", isDataLoadingBlocked));
            assertEquals(new FindCommand(new ClientContainsKeywordsPredicate(List.of("Alice"))),
                    parser.parseCommand("FIND Alice", isDataLoadingBlocked));
            for (String character : List.of("\t", "\n", "\r", "\u0000", "\u007f", "\u0085",
                    "\u2028", "\u2029")) {
                assertThrows(ParseException.class,
                        "Use one command line and ordinary spaces. Control characters are not allowed.", () ->
                        parser.parseCommand("find " + character, isDataLoadingBlocked));
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
    public void parseCommand_caseVariants_preserveArgumentsAcrossLocales() throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            for (Locale locale : List.of(Locale.ROOT, Locale.forLanguageTag("tr-TR"))) {
                Locale.setDefault(locale);
                for (String input : List.of(
                        "add n/Rachel McKay p/91110001 e/Rachel.McKay@Example.com a/Blk 123 Queen Street t/Health",
                        "edit 1 n/Rachel McKay e/Rachel.McKay@Example.com",
                        "delete 1", "view 1", "find Rachel McKay",
                        "followup 1 d/2099-12-15 m/Call Rachel ASAP")) {
                    int boundary = input.indexOf(' ');
                    String word = input.substring(0, boundary);
                    String arguments = input.substring(boundary);
                    for (String variant : List.of(word.toUpperCase(Locale.ROOT),
                            word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))) {
                        assertEquals(parser.parseCommand(input), parser.parseCommand(" " + variant + arguments + " "));
                    }
                }
                for (boolean isDataLoadingBlocked : List.of(false, true)) {
                    assertTrue(parser.parseCommand("LIST", isDataLoadingBlocked) instanceof ListCommand);
                    assertTrue(parser.parseCommand("LiSt", isDataLoadingBlocked) instanceof ListCommand);
                    assertTrue(parser.parseCommand("FOLLOWUPS", isDataLoadingBlocked) instanceof FollowUpsCommand);
                    assertTrue(parser.parseCommand("FoLlOwUpS", isDataLoadingBlocked) instanceof FollowUpsCommand);
                }
                assertEquals(new ClearFollowUpCommand(new ClientId(1)), parser.parseCommand("FoLlOwUp 1 ClEaR"));
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void parseCommand_uppercasePrefixes_keepExistingGrammar() throws Exception {
        for (String input : List.of("ADD N/Rachel p/91110001 e/rachel@example.com a/Blk 1",
                "EDIT 1 N/Rachel", "FOLLOWUP 1 D/2099-12-15 m/Call", "FOLLOWUP 1 d/2099-12-15 M/Call")) {
            assertThrows(ParseException.class, () -> parser.parseCommand(input));
        }
        assertEquals(new FindCommand(new ClientContainsKeywordsPredicate(List.of("N/Rachel"))),
                parser.parseCommand("FIND N/Rachel"));
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
        for (String command : List.of("list", "LIST", "LiSt", "followups", "FOLLOWUPS", "FoLlOwUpS")) {
            for (boolean isDataLoadingBlocked : List.of(false, true)) {
                for (String suffix : List.of(" words", " 123", " n/Rachel", " list", " \u00a0")) {
                    String usage = "Usage: " + command.toLowerCase(Locale.ROOT) + " (no arguments)";
                    assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, usage), () ->
                            parser.parseCommand(" " + command + suffix + " ", isDataLoadingBlocked));
                }
                assertThrows(ParseException.class, MESSAGE_INVALID_CHARACTERS, () ->
                        parser.parseCommand(command + " words\n", isDataLoadingBlocked));
            }
        }
    }

    @Test
    public void parseCommand_targetedCommands_rejectsExtraTokens() {
        for (String command : List.of("view 1 words", "delete 1 words", "edit 1 words n/Rachel",
                "followup 1 clear words", "followup 1 words d/2099-12-31 m/Call client",
                "VIEW 1 words", "DELETE 1 words", "EDIT 1 words n/Rachel", "FOLLOWUP 1 CLEAR words")) {
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
        for (String commandWord : List.of("edit", "clear", "help", "exit", "EDIT", "CLEAR", "HELP", "EXIT")) {
            for (boolean isDataLoadingBlocked : List.of(false, true)) {
                assertThrows(ParseException.class,
                        "Use one command line and ordinary spaces. Control characters are not allowed.", () ->
                        parser.parseCommand(commandWord + "\t", isDataLoadingBlocked));
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
        assertEquals(new ClearFollowUpCommand(new ClientId(1)), parser.parseCommand("FOLLOWUP 1 clear"));
        for (String command : List.of("ADD malformed", "AdD malformed", "EDIT malformed", "EdIt malformed",
                "DELETE malformed", "DeLeTe malformed", "FOLLOWUP malformed", "FoLlOwUp 1 CLEAR")) {
            assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () -> parser.parseCommand(command, true));
        }
    }

    private void assertUnsupportedRoute(String commandWord) {
        for (String variant : List.of(commandWord, commandWord.toUpperCase(Locale.ROOT),
                commandWord.substring(0, 1).toUpperCase(Locale.ROOT) + commandWord.substring(1))) {
            for (boolean isDataLoadingBlocked : List.of(false, true)) {
                for (String suffix : List.of("", " 1", " malformed extra")) {
                    assertThrows(ParseException.class, "Unknown command.", () ->
                            parser.parseCommand(" " + variant + suffix + " ", isDataLoadingBlocked));
                }
            }
        }
    }
}

package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.ClientSearchPredicate;
import seedu.address.model.person.ClientSearchPredicate.Field;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", "Invalid command format!\n" + FindCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "     ", "Invalid command format!\n" + FindCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        // no leading and trailing whitespaces
        FindCommand expectedFindCommand =
                new FindCommand(new ClientContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "Alice Bob", expectedFindCommand);

        // Only ordinary spaces separate keywords after global character validation.
        assertParseSuccess(parser, "   Alice   Bob  ", expectedFindCommand);
    }

    @Test
    public void parse_phoneAndMixedKeywords_preservesDigitsAndIndependentTokens() {
        assertParseSuccess(parser, "00123456 Bob 81234562 81234562",
                new FindCommand(new ClientContainsKeywordsPredicate(
                        List.of("00123456", "Bob", "81234562", "81234562"))));
        assertParseSuccess(parser, "8123 4562",
                new FindCommand(new ClientContainsKeywordsPredicate(List.of("8123", "4562"))));
    }

    @Test
    public void parse_quotesPunctuationAndNonOrdinarySpaces_remainLiteral() {
        assertParseSuccess(parser, "\"Alice Bob\" Alice, Alice\u00a0Bob",
                new FindCommand(new ClientContainsKeywordsPredicate(List.of("\"Alice", "Bob\"",
                        "Alice,", "Alice\u00a0Bob"))));
    }

    @Test
    public void parse_explicitFields_buildsCorrespondingPredicates() {
        assertParseSuccess(parser, "n/  RaChel   Lim  ",
                new FindCommand(new ClientSearchPredicate(Field.NAME, "RaChel   Lim")));
        assertParseSuccess(parser, "p/+65 9123-4567",
                new FindCommand(new ClientSearchPredicate(Field.PHONE, "+65 9123-4567")));
        assertParseSuccess(parser, "t/ReNeWaL",
                new FindCommand(new ClientSearchPredicate(Field.TAG, "ReNeWaL")));
    }

    @Test
    public void parse_invalidExplicitFields_throwsParseException() {
        for (String input : List.of("n/", "p/---", "p/12x", "t/has space", "n/Rachel p/9123",
                "p/9123 t/renewal", "Rachel n/rac", "t/renewal n/rac")) {
            assertParseFailure(parser, input,
                    "Invalid command format!\n" + FindCommand.MESSAGE_USAGE);
        }
    }

}

package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.ClientContainsKeywordsPredicate;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "", "Invalid command format!\nUsage: find KEYWORD [MORE_KEYWORDS]...");
        assertParseFailure(parser, "     ", "Invalid command format!\nUsage: find KEYWORD [MORE_KEYWORDS]...");
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

}

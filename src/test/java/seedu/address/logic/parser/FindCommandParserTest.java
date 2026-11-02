package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.ClientSearchPredicate;
import seedu.address.model.person.ClientSearchPredicate.Field;
import seedu.address.model.tag.Tag;

public class FindCommandParserTest {

    private static final String PHONE_QUERY_CONSTRAINTS =
            "Phone search queries need at least 1 digit (0-9). An optional leading + and single spaces or hyphens"
                    + " between digit groups are allowed.";

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
    public void parse_shortPhoneQueries_acceptsFragmentsWithValidFormatting() {
        for (String query : List.of("0", "9", "12", "+0", "+12", "0-1", "0 1")) {
            assertParseSuccess(parser, "p/" + query,
                    new FindCommand(new ClientSearchPredicate(Field.PHONE, query)));
        }
    }

    @Test
    public void parse_malformedPhoneQueries_rejectsMisplacedSeparatorsAndNonAsciiDigits() {
        for (String query : List.of("+", "---", "12x", "-1", "+ 1", "1--2", "1  2", "9123  4567",
                "1- 2", "1 +2", "1-", "１２３", "١٢٣")) {
            assertParseFailure(parser, "p/" + query, PHONE_QUERY_CONSTRAINTS);
        }
    }

    @Test
    public void parse_segmentedTagQueries_acceptsMixedCase() {
        for (String query : List.of("life-insurance", "HIGH_PRIORITY", "A-2_b")) {
            assertParseSuccess(parser, "t/" + query,
                    new FindCommand(new ClientSearchPredicate(Field.TAG, query)));
        }
    }

    @Test
    public void parse_invalidTagQueries_reportsTagConstraints() {
        for (String query : List.of("life--insurance", "has space", "health!", "健康", "１２３")) {
            assertParseFailure(parser, "t/" + query, Tag.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_invalidExplicitFields_throwsParseException() {
        for (String input : List.of("n/", "p/", "t/", "n/   ", "p/   ", "t/   ", "n/Rachel p/9123",
                "p/9123 t/renewal", "Rachel n/rac", "t/renewal n/rac", "n/Rachel n/Bob", "p/9 p/1",
                "t/health t/renewal", "p/p/9", "t/t/health", "p/12x t/health", "t/life-insurance p/9")) {
            assertParseFailure(parser, input,
                    "Invalid command format!\n" + FindCommand.MESSAGE_USAGE);
        }
    }

}

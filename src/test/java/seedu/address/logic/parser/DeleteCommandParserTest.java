package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;

/** Tests delete argument shape before shared index syntax. */
public class DeleteCommandParserTest {

    private final DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        assertParseSuccess(parser, "1", new DeleteCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, " 0001 ", new DeleteCommand(INDEX_FIRST_PERSON));
    }

    @Test
    public void parse_missingOrExtraTokens_reportsUsageBeforeSyntax() {
        for (String invalid : List.of("", "  ", "1 extra", "0 extra", "abc 2")) {
            assertParseFailure(parser, invalid, "Invalid command format!\nUsage: delete INDEX");
        }
    }

    @Test
    public void parse_singleInvalidToken_preservesIndexError() {
        for (String invalid : List.of("a", "0", "-1", "1.5", "2147483648", "١")) {
            assertParseFailure(parser, invalid, "Index must be a positive integer from 1 to 2147483647.");
        }
    }
}

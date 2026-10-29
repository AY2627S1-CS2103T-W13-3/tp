package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;
import seedu.address.model.person.ClientId;

/** Tests delete argument shape before permanent Client ID syntax. */
public class DeleteCommandParserTest {

    private final DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        assertParseSuccess(parser, "1", new DeleteCommand(new ClientId(1)));
        assertParseSuccess(parser, " 0001 ", new DeleteCommand(new ClientId(1)));
    }

    @Test
    public void parse_missingOrExtraTokens_reportsUsageBeforeSyntax() {
        for (String invalid : List.of("", "  ", "1 extra", "0 extra", "abc 2")) {
            assertParseFailure(parser, invalid, "Invalid command format!\nUsage: delete ID");
        }
    }

    @Test
    public void parse_singleInvalidToken_preservesClientIdError() {
        for (String invalid : List.of("a", "0", "-1", "1.5", "2147483648", "١")) {
            assertParseFailure(parser, invalid, "Client ID must be a positive integer from 1 to 2147483647.");
        }
    }
}

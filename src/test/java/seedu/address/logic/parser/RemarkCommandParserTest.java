package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {
    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_validRemark_success() {
        assertParseSuccess(parser, " 1 r/  Likes coffee  ",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes coffee")));
    }

    @Test
    public void parse_emptyOrMissingRemark_clearsRemark() {
        assertParseSuccess(parser, "1 r/", new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")));
        assertParseSuccess(parser, "1", new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")));
    }

    @Test
    public void parse_invalidIndex_failure() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);
        for (String input : new String[] {"", "r/coffee", "0 r/coffee", "-1 r/coffee",
            "2147483648 r/coffee", "abc r/coffee"}) {
            assertParseFailure(parser, input, expectedMessage);
        }
    }

    @Test
    public void parse_repeatedRemark_failure() {
        assertParseFailure(parser, "1 r/coffee r/tea", Messages.getErrorMessageForDuplicatePrefixes(PREFIX_REMARK));
    }
}

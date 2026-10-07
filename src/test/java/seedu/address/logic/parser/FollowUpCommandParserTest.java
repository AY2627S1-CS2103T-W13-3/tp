package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_DUPLICATE_FIELDS;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ClearFollowUpCommand;
import seedu.address.logic.commands.SetFollowUpCommand;

public class FollowUpCommandParserTest {
    private static final String FORMAT_MESSAGE = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            SetFollowUpCommand.MESSAGE_USAGE);
    private final FollowUpCommandParser parser = new FollowUpCommandParser();

    @Test
    public void parse_recordAndClearForms_success() {
        assertParseSuccess(parser, " 1 d/2026-10-05 m/Send quotation",
                new SetFollowUpCommand(INDEX_FIRST_PERSON, "2026-10-05", "Send quotation"));
        assertParseSuccess(parser, " 1 m/Send quotation d/2026-10-05",
                new SetFollowUpCommand(INDEX_FIRST_PERSON, "2026-10-05", "Send quotation"));
        assertParseSuccess(parser, " 1 clear", new ClearFollowUpCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, " 3 clear", new ClearFollowUpCommand(Index.fromOneBased(3)));
        assertParseSuccess(parser, " 01 clear", new ClearFollowUpCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, " 000000000000000000001 clear", new ClearFollowUpCommand(INDEX_FIRST_PERSON));
        assertParseSuccess(parser, " 999 d/2026-10-05 m/Send quotation",
                new SetFollowUpCommand(Index.fromOneBased(999), "2026-10-05", "Send quotation"));
    }

    @Test
    public void parse_shapeErrors_takePriorityOverDuplicatesAndIndex() {
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06", FORMAT_MESSAGE);
        assertParseFailure(parser, " 1 clear extra", FORMAT_MESSAGE);
        assertParseFailure(parser, " d/2026-10-05 m/x", FORMAT_MESSAGE);
        assertParseFailure(parser, " 1 d/2026-10-05", FORMAT_MESSAGE);
    }

    @Test
    public void parse_duplicatePrefixes_takePriorityOverIndex() {
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06 m/x",
                MESSAGE_DUPLICATE_FIELDS + "d/");
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06 m/x m/y",
                MESSAGE_DUPLICATE_FIELDS + "d/, m/");
    }

    @Test
    public void parse_indexSyntax_failure() {
        assertParseFailure(parser, " 0 clear", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, " +1 clear", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, " 1.5 clear", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, " 2147483648 clear", MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, " 3000000000 clear", MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_descriptionClearIsTreatedAsDescription() {
        assertParseSuccess(parser, " 1 d/2026-10-05 m/clear",
                new SetFollowUpCommand(INDEX_FIRST_PERSON, "2026-10-05", "clear"));
    }

    @Test
    public void parseIndex_longLeadingZeroes_success() throws Exception {
        assertEquals(INDEX_FIRST_PERSON, FollowUpParserUtil.parseIndex("000000000000000000001"));
        assertEquals(Index.fromOneBased(Integer.MAX_VALUE), FollowUpParserUtil.parseIndex("00000000002147483647"));
        assertEquals(Index.fromOneBased(3), FollowUpParserUtil.parseIndex("3"));
        assertEquals(Index.fromOneBased(999), FollowUpParserUtil.parseIndex("999"));
    }
}

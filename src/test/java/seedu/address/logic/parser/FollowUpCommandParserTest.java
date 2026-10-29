package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_DUPLICATE_FIELDS;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.ClientIdParserUtil.MESSAGE_INVALID_CLIENT_ID;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ClearFollowUpCommand;
import seedu.address.logic.commands.SetFollowUpCommand;
import seedu.address.model.person.ClientId;

public class FollowUpCommandParserTest {
    private static final String FORMAT_MESSAGE = String.format(MESSAGE_INVALID_COMMAND_FORMAT,
            SetFollowUpCommand.MESSAGE_USAGE);
    private final FollowUpCommandParser parser = new FollowUpCommandParser();

    @Test
    public void parse_recordAndClearForms_success() {
        assertParseSuccess(parser, " 1 d/2026-10-05 m/Send quotation",
                new SetFollowUpCommand(new ClientId(1), "2026-10-05", "Send quotation"));
        assertParseSuccess(parser, " 1 m/Send quotation d/2026-10-05",
                new SetFollowUpCommand(new ClientId(1), "2026-10-05", "Send quotation"));
        assertParseSuccess(parser, " 1 clear", new ClearFollowUpCommand(new ClientId(1)));
        assertParseSuccess(parser, " 3 clear", new ClearFollowUpCommand(new ClientId(3)));
        assertParseSuccess(parser, " 01 clear", new ClearFollowUpCommand(new ClientId(1)));
        assertParseSuccess(parser, " 000000000000000000001 clear", new ClearFollowUpCommand(new ClientId(1)));
        assertParseSuccess(parser, " 999 d/2026-10-05 m/Send quotation",
                new SetFollowUpCommand(new ClientId(999), "2026-10-05", "Send quotation"));
    }

    @Test
    public void parse_longSpacesAndLeadingZeros_preservesDescription() {
        String spaces = " ".repeat(10000);
        assertParseSuccess(parser, " " + "0".repeat(10000) + "17" + spaces + "clear",
                new ClearFollowUpCommand(new ClientId(17)));
        assertParseSuccess(parser, " 17 d/2026-10-05 m/Call" + spaces + "Rachel",
                new SetFollowUpCommand(new ClientId(17), "2026-10-05", "Call" + spaces + "Rachel"));
    }

    @Test
    public void parse_shapeErrors_takePriorityOverDuplicatesAndClientId() {
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06", FORMAT_MESSAGE);
        assertParseFailure(parser, " 1 clear extra", FORMAT_MESSAGE);
        assertParseFailure(parser, " d/2026-10-05 m/x", FORMAT_MESSAGE);
        assertParseFailure(parser, " 1 d/2026-10-05", FORMAT_MESSAGE);
    }

    @Test
    public void parse_duplicatePrefixes_takePriorityOverClientId() {
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06 m/x",
                MESSAGE_DUPLICATE_FIELDS + "d/");
        assertParseFailure(parser, " 0 d/2026-10-05 d/2026-10-06 m/x m/y",
                MESSAGE_DUPLICATE_FIELDS + "d/, m/");
    }

    @Test
    public void parse_clientIdSyntax_failure() {
        assertParseFailure(parser, " 0 clear", MESSAGE_INVALID_CLIENT_ID);
        assertParseFailure(parser, " +1 clear", MESSAGE_INVALID_CLIENT_ID);
        assertParseFailure(parser, " 1.5 clear", MESSAGE_INVALID_CLIENT_ID);
        assertParseFailure(parser, " 2147483648 clear", MESSAGE_INVALID_CLIENT_ID);
        assertParseFailure(parser, " 3000000000 clear", MESSAGE_INVALID_CLIENT_ID);
    }

    @Test
    public void parse_descriptionClearIsTreatedAsDescription() {
        assertParseSuccess(parser, " 1 d/2026-10-05 m/clear",
                new SetFollowUpCommand(new ClientId(1), "2026-10-05", "clear"));
    }

    @Test
    public void parseClientId_longLeadingZeroes_success() throws Exception {
        assertEquals(new ClientId(1), ClientIdParserUtil.parseClientId("000000000000000000001"));
        assertEquals(new ClientId(Integer.MAX_VALUE), ClientIdParserUtil.parseClientId("00000000002147483647"));
        assertEquals(new ClientId(3), ClientIdParserUtil.parseClientId("3"));
        assertEquals(new ClientId(999), ClientIdParserUtil.parseClientId("999"));
    }
}

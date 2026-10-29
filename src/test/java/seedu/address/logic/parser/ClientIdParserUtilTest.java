package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.ClientId;

public class ClientIdParserUtilTest {
    @Test
    public void parseClientId_boundariesAndLeadingZeros_success() throws Exception {
        assertEquals(new ClientId(17), ClientIdParserUtil.parseClientId(" " + "0".repeat(10000) + "17 "));
        assertEquals(new ClientId(Integer.MAX_VALUE), ClientIdParserUtil.parseClientId("2147483647"));
    }

    @Test
    public void parseClientId_invalidValues_reportsIdDiagnostic() {
        for (String value : List.of("", "0", "-1", "+1", "2147483648", "9".repeat(10000),
                "١", "1\t", "1" + " ".repeat(10000) + "7")) {
            ParseException error = assertThrows(ParseException.class, () -> ClientIdParserUtil.parseClientId(value));
            assertEquals(ClientIdParserUtil.MESSAGE_INVALID_CLIENT_ID, error.getMessage());
        }
    }
}

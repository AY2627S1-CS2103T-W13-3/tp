package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.person.FollowUp;

public class JsonAdaptedFollowUpTest {
    @Test
    public void toModelType_unicodeJsonRoundTrip_preservesExactText() throws Exception {
        for (String description : new String[] {"续保电话", "வாடிக்கையாளரை அழைக்கவும்", "اتصال بشأن التجديد",
            "Cafe\u0301", "👩\u200d💻", "😀".repeat(200)}) {
            FollowUp action = new FollowUp(LocalDate.of(2099, 12, 15), description);
            String json = JsonUtil.toJsonString(new JsonAdaptedFollowUp(action));
            JsonAdaptedFollowUp restored = JsonUtil.fromJsonString(json, JsonAdaptedFollowUp.class);
            assertEquals(action, restored.toModelType());
            assertEquals(description, restored.toModelType().getDescription());
        }
    }

    @Test
    public void toModelType_invalidUnicodeNotes_rejectsStoredValue() throws Exception {
        for (String description : new String[] {"\ud800", "\udc00", "Call\ud800end", "\u200b", "\ufe0f",
            "\u034f", "Call\u0085", "Call\u2028", "Call\u2029", "😀".repeat(201)}) {
            String json = JsonUtil.toJsonString(new JsonAdaptedFollowUp("2099-12-15", description));
            JsonAdaptedFollowUp restored = JsonUtil.fromJsonString(json, JsonAdaptedFollowUp.class);
            assertThrows(IllegalValueException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, restored::toModelType);
        }
    }
}

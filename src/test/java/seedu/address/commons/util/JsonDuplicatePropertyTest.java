package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import seedu.address.model.UserPrefs;

/** Checks JSON property uniqueness independently of the client schema. */
public class JsonDuplicatePropertyTest {
    @TestFactory
    public Stream<DynamicTest> fromJsonString_duplicateProperties_rejectedAtEveryDepth() {
        return Stream.of(
                "{\"persons\":[1],\"persons\":[]}",
                "{\"person\":{\"name\":\"Ada\",\"name\":\"Bob\"}}",
                "{\"followUp\":{\"description\":\"Call\",\"description\":\"Email\"}}",
                "{\"name\":\"Ada\",\"na\\u006de\":\"Bob\"}",
                "{\"name\":\"Ada\",\"name\":\"Ada\"}").map(json -> DynamicTest.dynamicTest(json, () ->
                    assertThrows(IOException.class, () -> JsonUtil.fromJsonString(json, Map.class, true))));
    }

    @Test
    public void fromJsonString_samePropertyInSeparateObjects_allowed() throws IOException {
        String json = "{\"first\":{\"name\":\"Ada\"},\"second\":{\"name\":\"Bob\"}}";
        Map<?, ?> parsed = JsonUtil.fromJsonString(json, Map.class, true);
        assertEquals(Map.of("first", Map.of("name", "Ada"), "second", Map.of("name", "Bob")), parsed);
    }

    @Test
    public void fromJsonString_unknownProperties_remainAllowedButCannotRepeat() throws IOException {
        assertEquals(new UserPrefs(), JsonUtil.fromJsonString(
                "{\"unknown\":{\"note\":\"ignored\"}}", UserPrefs.class, true));
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString(
                "{\"unknown\":{\"note\":\"first\",\"note\":\"second\"}}", UserPrefs.class, true));
    }
}

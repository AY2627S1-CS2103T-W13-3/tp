package seedu.address.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/** Rejects non-string saved values before Jackson can coerce them into valid domain text. */
public class StoredTextDeserializer extends StdScalarDeserializer<String> {
    public StoredTextDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "Saved text fields must be JSON strings.");
        }
        return parser.getText();
    }
}

package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.testutil.LogCapturingHandler;

/** Tests privacy at the JSON loading diagnostic boundary. */
public class JsonDiagnosticPrivacyTest {
    private static final String CANARY = "PRIVATE_CLIENT_CANARY_7284";

    @TempDir
    public Path testFolder;

    private Logger logger;
    private LogCapturingHandler handler;

    @BeforeEach
    public void setUp() throws IOException {
        // Initialize JsonUtil first because obtaining a logger removes its existing handlers.
        JsonUtil.fromJsonString("{}", Object.class);
        logger = LogsCenter.getLogger(JsonUtil.class);
        handler = new LogCapturingHandler();
        logger.addHandler(handler);
    }

    @AfterEach
    public void tearDown() {
        logger.removeHandler(handler);
        handler.close();
    }

    @Test
    public void readJsonFile_malformedClientJson_doesNotLogClientContents() throws Exception {
        DataLoadingException failure = assertPrivateFailure("{\"persons\":[" + CANARY + "]}",
                file -> new JsonAddressBookStorage(file).readAddressBook());
        assertInstanceOf(JsonMappingException.class, failure.getCause());
        assertInstanceOf(JsonParseException.class, failure.getCause().getCause());
    }

    @Test
    public void readJsonFile_wrongClientType_doesNotLogClientContents() throws Exception {
        DataLoadingException failure = assertPrivateFailure("{\"persons\":\"" + CANARY + "\"}",
                file -> new JsonAddressBookStorage(file).readAddressBook());
        assertInstanceOf(JsonMappingException.class, failure.getCause());
    }

    @Test
    public void readJsonFile_invalidPreferenceType_doesNotLogClientContents() throws Exception {
        DataLoadingException failure = assertPrivateFailure(
                "{\"guiSettings\":{\"windowWidth\":\"" + CANARY + "\"}}",
                file -> JsonUtil.readJsonFile(file, UserPrefs.class));
        assertInstanceOf(JsonMappingException.class, failure.getCause());
    }

    @Test
    public void readJsonFile_nestedFailure_preservesCauseWithoutLoggingIt() throws Exception {
        DataLoadingException failure = assertPrivateFailure("{\"value\":\"" + CANARY + "\"}",
                file -> JsonUtil.readJsonFile(file, RejectingValue.class));
        assertInstanceOf(JsonMappingException.class, failure.getCause());
        assertTrue(getStackTrace(failure.getCause().getCause()).contains(CANARY));
    }

    @Test
    public void readJsonFile_directory_preservesIoFailureWithoutAttachingItToLog() throws Exception {
        Path directory = Files.createDirectory(testFolder.resolve("directory"));
        DataLoadingException failure = assertThrows(DataLoadingException.class, () ->
                JsonUtil.readJsonFile(directory, Object.class));
        assertInstanceOf(IOException.class, failure.getCause());
        assertPrivateDiagnostics(directory);
        assertTrue(Files.isDirectory(directory));
    }

    private DataLoadingException assertPrivateFailure(String json, LoadingOperation operation) throws Exception {
        Path file = testFolder.resolve("input.json");
        Files.writeString(file, json);
        byte[] original = Files.readAllBytes(file);

        DataLoadingException failure = assertThrows(DataLoadingException.class, () ->
                operation.load(file));
        assertTrue(getStackTrace(failure).contains(CANARY),
                "The fixture must exercise an exception containing untrusted contents");
        assertArrayEquals(original, Files.readAllBytes(file));
        assertPrivateDiagnostics(file);
        return failure;
    }

    private void assertPrivateDiagnostics(Path file) {
        assertTrue(handler.getRecords().stream().anyMatch(record -> record.getLevel().equals(Level.WARNING)
                && record.getMessage().contains(file.toString())), "Retain a useful warning with the file path");
        for (LogRecord record : handler.getRecords()) {
            assertFalse(record.getMessage().contains(CANARY), "Log message exposes client contents");
            assertNull(record.getThrown(), "Do not attach untrusted exception details to loading diagnostics");
            assertFalse(new SimpleFormatter().format(record).contains(CANARY),
                    "Formatted diagnostic exposes client contents");
        }
    }

    private static String getStackTrace(Throwable failure) {
        StringWriter result = new StringWriter();
        failure.printStackTrace(new PrintWriter(result));
        return result.toString();
    }

    /** Simulates a nested deserialization failure containing an untrusted value. */
    public static class RejectingValue {
        @JsonCreator
        public RejectingValue(@JsonProperty("value") String value) {
            throw new IllegalArgumentException("Invalid saved value", new IllegalStateException(value));
        }
    }

    @FunctionalInterface
    private interface LoadingOperation {
        void load(Path file) throws DataLoadingException;
    }
}

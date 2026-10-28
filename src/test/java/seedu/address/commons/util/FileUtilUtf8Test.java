package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;

/** Tests that saved text is decoded without silently replacing malformed bytes. */
public class FileUtilUtf8Test {
    @TempDir
    public Path folder;

    @Test
    public void readFromFile_malformedEncoding_throwsIoException() throws IOException {
        byte[][] malformed = {
            {(byte) 0x80}, // stray continuation byte
            {(byte) 0xC3}, // truncated two-byte sequence
            {(byte) 0xFF}, // invalid leading byte
            {(byte) 0xC0, (byte) 0xAF}, // overlong encoding of '/'
            {(byte) 0xED, (byte) 0xA0, (byte) 0x80} // encoded high surrogate
        };
        Path file = folder.resolve("malformed.txt");
        for (byte[] bytes : malformed) {
            Files.write(file, bytes);
            assertThrows(IOException.class, () -> FileUtil.readFromFile(file));
        }
    }

    @Test
    public void readFromFile_leadingBom_ignoresOneSignatureAndPreservesValue() throws Exception {
        Path file = folder.resolve("bom.json");
        Files.writeString(file, "\ufeff\"value\ufeff\" \n");
        assertEquals("\"value\ufeff\" \n", FileUtil.readFromFile(file));
        assertEquals("value\ufeff", JsonUtil.readJsonFile(file, String.class).orElseThrow());
        for (String invalid : new String[] {"\ufeff\ufeff{}", " \ufeff{}", "{}\ufeff", "\ufeff{}{}"}) {
            Files.writeString(file, invalid);
            assertThrows(DataLoadingException.class, () -> JsonUtil.readJsonFile(file, Object.class));
        }
    }

    @Test
    public void readFromFile_validUtf8_preservesUnicodeAndReplacementCharacter() throws IOException {
        String text = "Ada 新加坡 🏠 \uFFFD\n\t ";
        Path file = folder.resolve("valid.txt");
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));

        assertEquals(text, FileUtil.readFromFile(file));
    }
}

package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests that saved text is decoded without silently replacing malformed bytes. */
public class FileUtilUtf8Test {
    @TempDir
    public Path folder;

    @Test
    public void readFromFile_malformedEncoding_throwsIoException() throws IOException {
        byte[][] malformed = {
            {(byte) 0x80}, // stray continuation byte
            {(byte) 0xC3}, // truncated two-byte sequence
            {(byte) 0xFF} // invalid leading byte
        };
        Path file = folder.resolve("malformed.txt");
        for (byte[] bytes : malformed) {
            Files.write(file, bytes);
            assertThrows(IOException.class, () -> FileUtil.readFromFile(file));
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

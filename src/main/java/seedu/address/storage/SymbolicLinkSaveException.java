package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Path;

/** Indicates that saving would replace a symbolic link instead of updating its target. */
public class SymbolicLinkSaveException extends IOException {
    public SymbolicLinkSaveException(Path destination) {
        super("The client data file is a symbolic link: " + destination);
    }
}

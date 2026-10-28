package seedu.address.model.exceptions;

/** Signals that a new client cannot be allocated without reusing a permanent ID. */
public class ClientIdExhaustedException extends RuntimeException {
    public ClientIdExhaustedException() {
        super("No more client IDs are available. Existing clients can still be viewed, edited or deleted.");
    }
}

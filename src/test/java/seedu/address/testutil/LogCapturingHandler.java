package seedu.address.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Collects diagnostic records for assertions without writing them to an external destination.
 */
public final class LogCapturingHandler extends Handler {
    private final List<LogRecord> records = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    public List<LogRecord> getRecords() {
        return List.copyOf(records);
    }

    @Override
    public void flush() {
        // Records are already available in memory.
    }

    @Override
    public void close() {
        // There are no external resources to close.
    }
}

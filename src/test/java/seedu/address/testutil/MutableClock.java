package seedu.address.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** A clock whose instant can be advanced without waiting for wall-clock time. */
public class MutableClock extends Clock {
    private Instant instant;
    private final ZoneId zone;

    /** Creates a clock at the given instant in the given time zone. */
    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}

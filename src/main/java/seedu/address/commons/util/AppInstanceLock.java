package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/** Holds an operating-system lock while one application instance uses a data folder. */
public final class AppInstanceLock implements AutoCloseable {
    private static final Map<Path, AppInstanceLock> HELD_LOCKS = new HashMap<>();

    private final FileChannel channel;
    private final Path lockPath;
    private boolean isClosed;

    private AppInstanceLock(FileChannel channel, Path lockPath) {
        this.channel = channel;
        this.lockPath = lockPath;
    }

    /**
     * Acquires the exclusive lock without waiting for another instance to close.
     * The marker remains on disk after closing: deleting it could let another process
     * lock a different file while the original owner still holds its lock.
     *
     * @throws AlreadyRunningException if an instance already holds this folder's lock.
     * @throws IOException if the marker cannot be opened or locked.
     */
    public static synchronized AppInstanceLock acquire(Path lockFile) throws IOException {
        requireNonNull(lockFile);
        Path absolutePath = lockFile.toAbsolutePath().normalize();
        Path lockPath = absolutePath.getParent().toRealPath().resolve(absolutePath.getFileName());
        // Closing any descriptor for the same file can release a process's locks on some OSes.
        // Reject a same-JVM duplicate before opening another descriptor, including folder aliases.
        if (HELD_LOCKS.containsKey(lockPath)) {
            throw new AlreadyRunningException();
        }
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        boolean isAcquired = false;
        try {
            if (channel.tryLock() == null) {
                throw new AlreadyRunningException();
            }
            AppInstanceLock instanceLock = new AppInstanceLock(channel, lockPath);
            HELD_LOCKS.put(lockPath, instanceLock);
            isAcquired = true;
            return instanceLock;
        } catch (OverlappingFileLockException e) {
            throw new AlreadyRunningException();
        } finally {
            if (!isAcquired) {
                channel.close();
            }
        }
    }

    /** Releases the lock; repeated closes are harmless. The OS also releases it on process exit. */
    @Override
    public void close() throws IOException {
        synchronized (AppInstanceLock.class) {
            if (!isClosed) {
                channel.close();
                isClosed = true;
                HELD_LOCKS.remove(lockPath, this);
            }
        }
    }

    /** A second instance must not load a stale snapshot or write preferences in the same folder. */
    public static final class AlreadyRunningException extends IOException {
        private AlreadyRunningException() {
            super("Another PolicyHarbor instance is using this folder.");
        }
    }
}

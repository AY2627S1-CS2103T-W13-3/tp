package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.TypicalPersons;

/** Checks that an indeterminate file-existence result cannot enable destructive startup. */
public class InaccessibleDataRecoveryTest {
    @TempDir
    public Path testFolder;

    private Path dataDirectory;
    private Path clientsFile;
    private Storage storage;

    @BeforeEach
    public void setUp() throws Exception {
        dataDirectory = Files.createDirectory(testFolder.resolve("data"));
        clientsFile = dataDirectory.resolve("addressbook.json");
        JsonAddressBookStorage clients = new JsonAddressBookStorage(clientsFile);
        clients.saveAddressBook(TypicalPersons.getTypicalAddressBook());
        storage = new StorageManager(clients, new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
    }

    @Test
    public void readJsonFile_inaccessibleParent_throwsDataLoadingException() throws Exception {
        Set<PosixFilePermission> permissions = originalPermissions();
        try {
            denyDirectoryAccess();
            assertThrows(DataLoadingException.class, () -> JsonUtil.readJsonFile(clientsFile, Object.class));
        } finally {
            Files.setPosixFilePermissions(dataDirectory, permissions);
        }
    }

    @Test
    public void startup_inaccessibleParent_remainsProtectedAfterAccessReturnsUntilRestart() throws Exception {
        byte[] originalClients = Files.readAllBytes(clientsFile);
        Set<PosixFilePermission> permissions = originalPermissions();
        MainApp app = new MainApp();
        Model model;
        try {
            denyDirectoryAccess();
            model = app.initModelManager(storage, new UserPrefs());
        } finally {
            Files.setPosixFilePermissions(dataDirectory, permissions);
        }

        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("list");
        logic.execute("find Alice");
        logic.execute("followups");
        assertTrue(logic.isDataLoadingBlocked());
        String add = "add n/Recovery Client p/81234567 e/recovery@example.com a/Recovery Street";
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () -> logic.execute(add));
        assertArrayEquals(originalClients, Files.readAllBytes(clientsFile));

        Model restarted = app.initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertEquals(TypicalPersons.getTypicalAddressBook(), restarted.getAddressBook());
        new LogicManager(restarted, storage).execute(add);
        assertEquals(TypicalPersons.getTypicalAddressBook().getPersonList().size() + 1,
                restarted.getAddressBook().getPersonList().size());
        assertTrue(restarted.getAddressBook().getPersonList()
                .containsAll(TypicalPersons.getTypicalAddressBook().getPersonList()));
        assertEquals(restarted.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void startup_absentFileUnderInaccessibleParent_requiresAccessibleRestart() throws Exception {
        Set<PosixFilePermission> permissions = originalPermissions();
        Files.delete(clientsFile);
        assertTrue(Files.notExists(clientsFile));
        assertFalse(JsonUtil.readJsonFile(clientsFile, Object.class).isPresent());

        MainApp app = new MainApp();
        Model freshInstall = app.initModelManager(storage, new UserPrefs());
        assertFalse(freshInstall.isDataLoadingBlocked());
        assertTrue(freshInstall.getAddressBook().getPersonList().isEmpty());

        Model protectedSession;
        try {
            // The file is absent, but the application cannot confirm that without parent access.
            denyDirectoryAccess();
            assertThrows(DataLoadingException.class, () -> JsonUtil.readJsonFile(clientsFile, Object.class));
            protectedSession = app.initModelManager(storage, new UserPrefs());
            assertTrue(protectedSession.isDataLoadingBlocked());
            assertTrue(protectedSession.getAddressBook().getPersonList().isEmpty());
        } finally {
            Files.setPosixFilePermissions(dataDirectory, permissions);
        }

        String add = "add n/Recovery Client p/81234567 e/recovery@example.com a/Recovery Street";
        LogicManager blockedLogic = new LogicManager(protectedSession, storage);
        blockedLogic.execute("list");
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () -> blockedLogic.execute(add));
        assertTrue(Files.notExists(clientsFile));

        Model restarted = app.initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertTrue(restarted.getAddressBook().getPersonList().isEmpty());
        new LogicManager(restarted, storage).execute(add);
        assertEquals(1, restarted.getAddressBook().getPersonList().size());
        assertEquals(restarted.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    private Set<PosixFilePermission> originalPermissions() throws IOException {
        assumeTrue(Files.getFileStore(dataDirectory).supportsFileAttributeView(PosixFileAttributeView.class),
                "This regression requires POSIX directory permissions");
        return Files.getPosixFilePermissions(dataDirectory);
    }

    private void denyDirectoryAccess() throws IOException {
        Files.setPosixFilePermissions(dataDirectory, Set.of());
        assumeFalse(Files.exists(clientsFile) || Files.notExists(clientsFile),
                "This account can bypass the test directory's access restrictions");
    }
}

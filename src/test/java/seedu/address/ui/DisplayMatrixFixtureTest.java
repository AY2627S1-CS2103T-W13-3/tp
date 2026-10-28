package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.storage.JsonAddressBookStorage;

/** Checks that the packaged display fixture is reproducible and safe to generate. */
public class DisplayMatrixFixtureTest {
    @TempDir
    public Path folder;

    @Test
    public void main_explicitDate_createsReloadableFixtureAndRefusesOverwrite() throws Exception {
        Path destination = folder.resolve("fresh fixture");
        DisplayMatrixFixture.main(new String[] {destination.toString(), "2026-10-28"});
        Path data = destination.resolve("data/addressbook.json");
        ReadOnlyAddressBook clients = new JsonAddressBookStorage(data).readAddressBook().orElseThrow();
        assertEquals(1000, clients.getPersonList().size());
        assertEquals(29, count(clients, FollowUpStatus.OVERDUE));
        assertEquals(2, count(clients, FollowUpStatus.DUE_TODAY));
        assertEquals(70, count(clients, FollowUpStatus.UPCOMING));
        assertEquals(125, clients.getPersonList().getFirst().getEmail().value.length());
        assertEquals(153, clients.getPersonList().getFirst().getAddress().value.length());
        assertEquals("Client 1000", clients.getPersonList().getLast().getName().fullName);

        byte[] original = Files.readAllBytes(data);
        assertThrows(IOException.class, () ->
                DisplayMatrixFixture.main(new String[] {destination.toString(), "2026-10-29"}));
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    private static long count(ReadOnlyAddressBook clients, FollowUpStatus status) {
        return clients.getPersonList().stream().filter(person -> person.getFollowUp()
                .map(followUp -> followUp.getStatus(LocalDate.of(2026, 10, 28)) == status).orElse(false)).count();
    }
}

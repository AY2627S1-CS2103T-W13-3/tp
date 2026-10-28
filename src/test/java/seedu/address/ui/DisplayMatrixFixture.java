package seedu.address.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import seedu.address.model.AddressBook;
import seedu.address.model.person.FollowUp;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.testutil.PersonBuilder;

/**
 * Manual fixture: writes a data file of 1,000 fictional clients for the packaged-JAR display matrix.
 * Client 1 has long unbroken values; every tenth client has a follow-up, spread across all three statuses.
 * Run {@code main} with a fresh folder and, optionally, a reference date ({@code yyyy-MM-dd}, default today),
 * then launch the JAR from that folder.
 */
public class DisplayMatrixFixture {
    static final int CLIENT_COUNT = 1000;

    public static void main(String[] args) throws IOException {
        Path data = Paths.get(args.length > 0 ? args[0] : "display-matrix", "data", "addressbook.json");
        if (Files.exists(data)) {
            throw new IOException(data.toAbsolutePath() + " already exists; use a fresh folder.");
        }
        LocalDate today = args.length > 1 ? LocalDate.parse(args[1]) : LocalDate.now();
        new JsonAddressBookStorage(data).saveAddressBook(clients(today));
        System.out.println("Wrote " + CLIENT_COUNT + " fictional clients to " + data.toAbsolutePath());
        System.out.println("Reference date " + today + ": on that date 29 follow-ups are overdue, 2 are due today"
                + " and 70 are upcoming.");
    }

    /** Returns the fixture clients, with follow-up dates relative to {@code today}. */
    static AddressBook clients(LocalDate today) {
        AddressBook clients = new AddressBook();
        clients.addPerson(new PersonBuilder().withName("Abcdefghij".repeat(8)).withPhone("9".repeat(40))
                .withEmail("a".repeat(70) + "@" + "b".repeat(50) + ".com").withAddress("Blk" + "9".repeat(150))
                .withTags("t".repeat(60), "health").withFollowUp(new FollowUp(today, "x".repeat(200))).build());
        for (int i = 2; i <= CLIENT_COUNT; i++) {
            PersonBuilder client = new PersonBuilder().withName("Client " + i).withPhone(String.format("9%07d", i))
                    .withEmail("client" + i + "@example.com").withAddress("Blk " + i + " Example Street");
            if (i % 10 == 0) {
                // 29 overdue, 1 due today and 70 upcoming.
                client.withFollowUp(new FollowUp(today.plusDays(i / 10 - 30), "Review policy for client " + i));
            }
            clients.addPerson(client.build());
        }
        return clients;
    }
}

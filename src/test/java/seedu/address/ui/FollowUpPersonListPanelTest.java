package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FollowUpPersonListPanelTest {

    @Test
    public void getEmptyListMessage_ordinaryMode_showsNoClients() {
        assertEquals("No clients to display.", PersonListPanel.getEmptyListMessage(false));
    }

    @Test
    public void getEmptyListMessage_pendingMode_showsNoPendingFollowUps() {
        assertEquals("There are no pending follow-ups.", PersonListPanel.getEmptyListMessage(true));
    }
}

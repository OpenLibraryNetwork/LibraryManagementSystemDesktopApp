package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PopularPublicationsTest {

    private static BorrowDTO borrow(String publicationDocumentId, String title) {
        BorrowDTO b = new BorrowDTO();
        b.setStrapiPublicationDocumentId(publicationDocumentId);
        b.setPublicationTitle(title);
        return b;
    }

    @Test
    void countsPerPublicationNotPerTitle() {
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(borrow("b2", "Ταξίδι στο παρελθόν"));
        borrows.add(borrow("b2", "Ταξίδι στο παρελθόν"));
        borrows.add(borrow("b5", "Ταξίδι στο παρελθόν")); // another book with the same title
        borrows.add(borrow("b1", "Αλγόριθμοι της αντίστασης"));
        borrows.add(borrow(null, null));                  // unknown publication: ignored

        List<PopularPublications.Entry> top = PopularPublications.top(borrows, 10);

        assertEquals(List.of(
                new PopularPublications.Entry("b2", "Ταξίδι στο παρελθόν", 2),
                new PopularPublications.Entry("b1", "Αλγόριθμοι της αντίστασης", 1),
                new PopularPublications.Entry("b5", "Ταξίδι στο παρελθόν", 1)), top);
    }

    @Test
    void keepsOnlyTheLimit() {
        List<BorrowDTO> borrows = new ArrayList<>();
        for (int i = 1; i <= 12; i++) borrows.add(borrow("b" + i, "Τ" + i));
        assertEquals(10, PopularPublications.top(borrows, 10).size());
    }

    @Test
    void aBorrowFromAnOlderCatalogDoesNotMergeWithTheBookThatNowHasItsId() {
        // the local borrow kept id 1 and the old title; in today's catalog id 1 is another book
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(borrow("b1", "Ταξίδι στο παρελθόν"));
        borrows.add(borrow("b1", "Ταξίδι στο παρελθόν"));
        borrows.add(borrow("b1", "Αλγόριθμοι της αντίστασης"));

        assertEquals(List.of(
                new PopularPublications.Entry("b1", "Ταξίδι στο παρελθόν", 2),
                new PopularPublications.Entry("b1", "Αλγόριθμοι της αντίστασης", 1)), PopularPublications.top(borrows, 10));
    }

    @Test
    void coverOnlyWhenTheCatalogTitleIsTheBorrowedTitle() {
        assertTrue(PopularPublications.coverBelongsTo("Αλγόριθμοι της αντίστασης", "ΑΛΓΟΡΙΘΜΟΙ ΤΗΣ ΑΝΤΙΣΤΑΣΗΣ"));
        assertFalse(PopularPublications.coverBelongsTo("Ταξίδι στο παρελθόν", "Αλγόριθμοι της αντίστασης"));
        assertFalse(PopularPublications.coverBelongsTo(null, "Αλγόριθμοι της αντίστασης"));
    }
}

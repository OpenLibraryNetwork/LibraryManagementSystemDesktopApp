package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PopularPublicationsTest {

    private static BorrowDTO borrow(Long publicationId, String title) {
        BorrowDTO b = new BorrowDTO();
        b.setStrapiPublicationId(publicationId);
        b.setPublicationTitle(title);
        return b;
    }

    @Test
    void countsPerPublicationNotPerTitle() {
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(borrow(2L, "Ταξίδι στο παρελθόν"));
        borrows.add(borrow(2L, "Ταξίδι στο παρελθόν"));
        borrows.add(borrow(5L, "Ταξίδι στο παρελθόν")); // another book with the same title
        borrows.add(borrow(1L, "Αλγόριθμοι της αντίστασης"));
        borrows.add(borrow(null, null));                  // unknown publication: ignored

        List<PopularPublications.Entry> top = PopularPublications.top(borrows, 10);

        assertEquals(List.of(
                new PopularPublications.Entry(2L, "Ταξίδι στο παρελθόν", 2),
                new PopularPublications.Entry(1L, "Αλγόριθμοι της αντίστασης", 1),
                new PopularPublications.Entry(5L, "Ταξίδι στο παρελθόν", 1)), top);
    }

    @Test
    void keepsOnlyTheLimit() {
        List<BorrowDTO> borrows = new ArrayList<>();
        for (long id = 1; id <= 12; id++) borrows.add(borrow(id, "Τ" + id));
        assertEquals(10, PopularPublications.top(borrows, 10).size());
    }

    @Test
    void aBorrowFromAnOlderCatalogDoesNotMergeWithTheBookThatNowHasItsId() {
        // the local borrow kept id 1 and the old title; in today's catalog id 1 is another book
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(borrow(1L, "Ταξίδι στο παρελθόν"));
        borrows.add(borrow(1L, "Ταξίδι στο παρελθόν"));
        borrows.add(borrow(1L, "Αλγόριθμοι της αντίστασης"));

        assertEquals(List.of(
                new PopularPublications.Entry(1L, "Ταξίδι στο παρελθόν", 2),
                new PopularPublications.Entry(1L, "Αλγόριθμοι της αντίστασης", 1)), PopularPublications.top(borrows, 10));
    }

    @Test
    void coverOnlyWhenTheCatalogTitleIsTheBorrowedTitle() {
        assertTrue(PopularPublications.coverBelongsTo("Αλγόριθμοι της αντίστασης", "ΑΛΓΟΡΙΘΜΟΙ ΤΗΣ ΑΝΤΙΣΤΑΣΗΣ"));
        assertFalse(PopularPublications.coverBelongsTo("Ταξίδι στο παρελθόν", "Αλγόριθμοι της αντίστασης"));
        assertFalse(PopularPublications.coverBelongsTo(null, "Αλγόριθμοι της αντίστασης"));
    }
}

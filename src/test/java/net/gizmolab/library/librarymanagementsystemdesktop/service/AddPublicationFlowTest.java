package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddPublicationFlowTest {

    private CatalogService catalog;
    private AddPublicationFlow flow;
    private final PublicationDraft draft = new PublicationDraft();

    private static PublicationDTO pub(long id) {
        PublicationDTO p = new PublicationDTO();
        p.setId(id);
        p.setTitle("Τ" + id);
        return p;
    }

    @BeforeEach
    void setUp() {
        catalog = mock(CatalogService.class);
        flow = new AddPublicationFlow(catalog, 3L);
        draft.setType(PublicationDraft.BROCHURE);
        draft.setTitle("Νέα");
    }

    @Test
    void existingPublicationOnlyGetsCopies() {
        when(catalog.addCopies(8L, 3L, 2, "NEW")).thenReturn(new CatalogService.CopiesResult(2, 2, null));
        AddPublicationFlow.Outcome o = flow.addCopies(pub(8), 2, "NEW");
        assertEquals(AddPublicationFlow.Kind.DONE, o.kind());
        verifyNoMoreInteractions(ignoreStubs(catalog));
    }

    @Test
    void localPublicationIsCreatedThenCopies() throws Exception {
        when(catalog.createPublication(draft)).thenReturn(
                new CatalogService.PublicationCreateResult(CatalogService.Source.LOCAL, pub(12), List.of()));
        when(catalog.addCopies(12L, 3L, 1, "NEW")).thenReturn(new CatalogService.CopiesResult(1, 1, null));
        AddPublicationFlow.Outcome o = flow.createAndAddCopies(draft, 1, "NEW");
        assertEquals(AddPublicationFlow.Kind.DONE, o.kind());
        assertEquals(12L, o.publication().getId());
        assertFalse(o.foundElsewhere());
    }

    @Test
    void isbnFoundInBiblionetAtTheLastCheck() throws Exception {
        when(catalog.createPublication(draft)).thenReturn(
                new CatalogService.PublicationCreateResult(CatalogService.Source.BIBLIONET, pub(40), List.of()));
        when(catalog.addCopies(40L, 3L, 1, "NEW")).thenReturn(new CatalogService.CopiesResult(1, 1, null));
        AddPublicationFlow.Outcome o = flow.createAndAddCopies(draft, 1, "NEW");
        assertEquals(AddPublicationFlow.Kind.DONE, o.kind());
        assertTrue(o.foundElsewhere());
    }

    @Test
    void duplicateOffersExistingAndCreatesNoCopies() throws Exception { // Review Focus 2
        when(catalog.createPublication(draft)).thenReturn(
                new CatalogService.PublicationCreateResult(null, null, List.of(pub(5))));
        AddPublicationFlow.Outcome o = flow.createAndAddCopies(draft, 1, "NEW");
        assertEquals(AddPublicationFlow.Kind.DUPLICATES, o.kind());
        assertEquals(5L, o.duplicates().get(0).getId());
        verify(catalog, never()).addCopies(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void partialCopiesThenRetryAsksOnlyForTheRest() throws Exception { // Review Focus 3
        when(catalog.createPublication(draft)).thenReturn(
                new CatalogService.PublicationCreateResult(CatalogService.Source.LOCAL, pub(12), List.of()));
        when(catalog.addCopies(12L, 3L, 3, "FAIR")).thenReturn(new CatalogService.CopiesResult(1, 3, "Η ενέργεια απέτυχε."));
        AddPublicationFlow.Outcome partial = flow.createAndAddCopies(draft, 3, "FAIR");
        assertEquals(AddPublicationFlow.Kind.PARTIAL_COPIES, partial.kind());

        when(catalog.addCopies(12L, 3L, 2, "FAIR")).thenReturn(new CatalogService.CopiesResult(2, 2, null));
        AddPublicationFlow.Outcome retried = flow.retryCopies(partial, "FAIR");
        assertEquals(AddPublicationFlow.Kind.DONE, retried.kind());
        verify(catalog, times(1)).createPublication(any()); // Review Focus 5: created once, never again on retry
    }

    @Test
    void repeatedPartialRetriesKeepTheOriginalTotals() throws Exception { // final review minor
        when(catalog.createPublication(draft)).thenReturn(
                new CatalogService.PublicationCreateResult(CatalogService.Source.LOCAL, pub(12), List.of()));
        when(catalog.addCopies(12L, 3L, 3, "FAIR")).thenReturn(new CatalogService.CopiesResult(1, 3, "Η ενέργεια απέτυχε."));
        AddPublicationFlow.Outcome first = flow.createAndAddCopies(draft, 3, "FAIR");

        when(catalog.addCopies(12L, 3L, 2, "FAIR")).thenReturn(new CatalogService.CopiesResult(1, 2, "Η ενέργεια απέτυχε."));
        AddPublicationFlow.Outcome second = flow.retryCopies(first, "FAIR");
        assertEquals(AddPublicationFlow.Kind.PARTIAL_COPIES, second.kind());
        assertEquals(2, second.copies().created());   // 1 + 1 of the whole request
        assertEquals(3, second.copies().requested()); // the librarian asked for 3

        when(catalog.addCopies(12L, 3L, 1, "FAIR")).thenReturn(new CatalogService.CopiesResult(1, 1, null));
        AddPublicationFlow.Outcome third = flow.retryCopies(second, "FAIR");
        assertEquals(AddPublicationFlow.Kind.DONE, third.kind());
        assertEquals(3, third.copies().created());
        verify(catalog).addCopies(12L, 3L, 1, "FAIR");
    }
}

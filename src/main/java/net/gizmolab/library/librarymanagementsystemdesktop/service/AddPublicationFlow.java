package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;

import java.io.IOException;
import java.util.List;

/**
 * What "Ολοκλήρωση" does in the add wizards. A publication is created only in createAndAddCopies;
 * cancelling a wizard before that creates nothing.
 */
public class AddPublicationFlow {

    public enum Kind { DONE, DUPLICATES, PARTIAL_COPIES }

    /**
     * @param foundElsewhere the server answered with an existing Biblionet/catalog record instead of creating ours
     */
    public record Outcome(Kind kind, PublicationDTO publication, List<PublicationDTO> duplicates,
                          CatalogService.CopiesResult copies, boolean foundElsewhere) {}

    private final CatalogService catalog;
    private final String libraryDocumentId;

    public AddPublicationFlow(CatalogService catalog, String libraryDocumentId) {
        this.catalog = catalog;
        this.libraryDocumentId = libraryDocumentId;
    }

    /** The publication already exists (catalog, Biblionet import or a chosen duplicate): copies only. */
    public Outcome addCopies(PublicationDTO publication, int count, String condition) {
        return withCopies(publication, count, condition, false);
    }

    public Outcome createAndAddCopies(PublicationDraft draft, int count, String condition)
            throws IOException, InterruptedException {
        CatalogService.PublicationCreateResult created = catalog.createPublication(draft);
        if (created.isDuplicate()) {
            return new Outcome(Kind.DUPLICATES, null, created.duplicates(), null, false);
        }
        return withCopies(created.publication(), count, condition, created.source() != CatalogService.Source.LOCAL);
    }

    /**
     * After PARTIAL_COPIES: only the copies that were not created. The result counts the whole
     * original request, so repeated retries keep asking for the rest and report "X of Y" correctly.
     */
    public Outcome retryCopies(Outcome partial, String condition) {
        CatalogService.CopiesResult before = partial.copies();
        Outcome retried = withCopies(partial.publication(), before.requested() - before.created(),
                condition, partial.foundElsewhere());
        CatalogService.CopiesResult total = new CatalogService.CopiesResult(
                before.created() + retried.copies().created(), before.requested(), retried.copies().errorMessage());
        return new Outcome(total.isComplete() ? Kind.DONE : Kind.PARTIAL_COPIES,
                retried.publication(), List.of(), total, retried.foundElsewhere());
    }

    private Outcome withCopies(PublicationDTO publication, int count, String condition, boolean foundElsewhere) {
        CatalogService.CopiesResult copies = catalog.addCopies(publication.getDocumentId(), libraryDocumentId, count, condition);
        return new Outcome(copies.isComplete() ? Kind.DONE : Kind.PARTIAL_COPIES,
                publication, List.of(), copies, foundElsewhere);
    }
}

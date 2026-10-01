package net.gizmolab.library.librarymanagementsystemdesktop.service;

import tools.jackson.databind.JsonNode;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.MagazineDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PersonDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublisherDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.IssueOrder;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Catalog calls of the "add publication" flows, with plain result types.
 * A 409 from a local creation is not an error here: it comes back as "duplicate + candidates".
 */
@Service
public class CatalogService {

    public enum Source { CATALOG, BIBLIONET, NOT_FOUND, LOCAL }

    public record IsbnLookupResult(Source source, PublicationDTO publication) {}

    public record CreateResult<T>(T created, List<T> duplicates) {
        public boolean isDuplicate() { return created == null; }
    }

    public record PublicationCreateResult(Source source, PublicationDTO publication, List<PublicationDTO> duplicates) {
        public boolean isDuplicate() { return publication == null; }
    }

    public record CopiesResult(int created, int requested, String errorMessage) {
        public boolean isComplete() { return created == requested; }
    }

    public enum MagazineSource { CATALOG, NLG, NOT_FOUND, UNAVAILABLE }

    /** magazine is null for NOT_FOUND / UNAVAILABLE; issn is the normalized ISSN the server looked for. */
    public record MagazineLookupResult(MagazineSource source, MagazineDTO magazine, String issn) {}

    private final StrapiApiClient api;

    public CatalogService(StrapiApiClient api) {
        this.api = api;
    }

    public IsbnLookupResult lookupIsbn(String isbn) throws IOException, InterruptedException {
        JsonNode response = api.isbnLookup(isbn);
        JsonNode data = response.path("data");
        return new IsbnLookupResult(sourceOf(response), data.isObject() ? DTOConverter.publicationFromJson(data) : null);
    }

    public List<PublicationDTO> searchBrochures(String query) throws IOException, InterruptedException {
        return DTOConverter.publicationsFromJson(api.searchBrochures(query));
    }

    public List<PersonDTO> searchPersons(String query) throws IOException, InterruptedException {
        return DTOConverter.personsFromJson(api.searchPersons(query));
    }

    public List<PublisherDTO> searchPublishers(String query) throws IOException, InterruptedException {
        return DTOConverter.publishersFromJson(api.searchPublishers(query));
    }

    public List<ContributorRoleDTO> getRoles() throws IOException, InterruptedException {
        List<ContributorRoleDTO> roles = new ArrayList<>();
        for (JsonNode node : api.getContributorRoles().path("data")) {
            roles.add(DTOConverter.roleFromJson(node));
        }
        return roles;
    }

    /** All subjects, page by page (the server returns at most 100 per request). */
    public List<SubjectDTO> getSubjects() throws IOException, InterruptedException {
        List<SubjectDTO> subjects = new ArrayList<>();
        int pageCount = 1;
        for (int page = 1; page <= pageCount; page++) {
            JsonNode response = api.getSubjectsPage(page);
            subjects.addAll(DTOConverter.subjectsFromJson(response));
            pageCount = response.path("meta").path("pagination").path("pageCount").asInt(1);
        }
        return subjects;
    }

    public CreateResult<PersonDTO> createPerson(PersonDraft draft) throws IOException, InterruptedException {
        try {
            return new CreateResult<>(DTOConverter.personFromJson(api.createLocalPerson(draft.toPayload()).path("data")), List.of());
        } catch (StrapiApiClient.ConflictException e) {
            return new CreateResult<>(null, candidates(e, DTOConverter::personFromJson));
        }
    }

    public CreateResult<PublisherDTO> createPublisher(PublisherDraft draft) throws IOException, InterruptedException {
        try {
            return new CreateResult<>(DTOConverter.publisherFromJson(api.createLocalPublisher(draft.toPayload()).path("data")), List.of());
        } catch (StrapiApiClient.ConflictException e) {
            return new CreateResult<>(null, candidates(e, DTOConverter::publisherFromJson));
        }
    }

    /**
     * 201 → LOCAL; 200 → BIBLIONET/CATALOG (the ISBN turned out to exist; local data ignored by the server);
     * 409 → duplicates (same brochure, or the ISBN is already in the catalog).
     */
    public PublicationCreateResult createPublication(PublicationDraft draft) throws IOException, InterruptedException {
        try {
            JsonNode response = api.createLocalPublication(draft.toPayload());
            return new PublicationCreateResult(sourceOf(response), DTOConverter.publicationFromJson(response.path("data")), List.of());
        } catch (StrapiApiClient.ConflictException e) {
            return new PublicationCreateResult(null, null, candidates(e, DTOConverter::publicationFromJson));
        }
    }

    public MagazineLookupResult lookupIssn(String issnOrCode) throws IOException, InterruptedException {
        JsonNode response = api.magazineIssnLookup(issnOrCode);
        MagazineSource source = switch (response.path("source").asString("")) {
            case "catalog" -> MagazineSource.CATALOG;
            case "nlg" -> MagazineSource.NLG;
            case "not-found" -> MagazineSource.NOT_FOUND;
            case "unavailable" -> MagazineSource.UNAVAILABLE;
            default -> throw new IllegalStateException("Unknown source in response: " + response.path("source"));
        };
        JsonNode data = response.path("data");
        return new MagazineLookupResult(source, data.isObject() ? DTOConverter.magazineFromJson(data) : null,
                response.path("issn").asString(null));
    }

    public List<MagazineDTO> searchMagazines(String query) throws IOException, InterruptedException {
        return DTOConverter.magazinesFromJson(api.searchMagazines(query));
    }

    public CreateResult<MagazineDTO> createMagazine(MagazineDraft draft) throws IOException, InterruptedException {
        try {
            return new CreateResult<>(DTOConverter.magazineFromJson(api.createLocalMagazine(draft.toPayload()).path("data")), List.of());
        } catch (StrapiApiClient.ConflictException e) {
            return new CreateResult<>(null, candidates(e, DTOConverter::magazineFromJson));
        }
    }

    /**
     * Issues of the magazine in reading order, page by page (the server returns at most 100 per request);
     * only those with a copy in the library when a library is given.
     */
    public List<PublicationDTO> getIssues(String magazineDocumentId, String libraryDocumentId) throws IOException, InterruptedException {
        List<PublicationDTO> issues = new ArrayList<>();
        int pageCount = 1;
        for (int page = 1; page <= pageCount; page++) {
            JsonNode response = api.getIssues(magazineDocumentId, libraryDocumentId, page);
            issues.addAll(DTOConverter.publicationsFromJson(response));
            pageCount = response.path("meta").path("pagination").path("pageCount").asInt(1);
        }
        issues.sort(IssueOrder.NATURAL);
        return issues;
    }

    /**
     * Creates `count` copies numbered after the highest copy number this library already has.
     * Never throws: on failure it stops and reports how many were created.
     */
    public CopiesResult addCopies(String publicationDocumentId, String libraryDocumentId, int count, String condition) {
        int next;
        try {
            next = DTOConverter.copiesFromJson(api.getCopiesInLibrary(publicationDocumentId, libraryDocumentId)).stream()
                    .mapToInt(CopyDTO::getCopyNumber).max().orElse(0) + 1;
        } catch (Exception e) {
            return failed(0, count, e);
        }
        int created = 0;
        for (int i = 0; i < count; i++) {
            try {
                api.createCopy(publicationDocumentId, next + i, condition);
                created++;
            } catch (Exception e) {
                return failed(created, count, e);
            }
        }
        return new CopiesResult(created, count, null);
    }

    private static CopiesResult failed(int created, int requested, Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        return new CopiesResult(created, requested, UserMessages.describe(e));
    }

    private <T> List<T> candidates(StrapiApiClient.ConflictException e, Function<JsonNode, T> convert) {
        List<T> result = new ArrayList<>();
        try {
            for (JsonNode node : api.getObjectMapper().readTree(e.getBody()).path("candidates")) {
                T item = convert.apply(node);
                if (item != null) result.add(item);
            }
        } catch (RuntimeException parseError) { // Jackson 3: JacksonException is unchecked
            // No readable candidates: the caller still knows it is a duplicate
        }
        return result;
    }

    private static Source sourceOf(JsonNode response) {
        return switch (response.path("source").asString("")) {
            case "catalog" -> Source.CATALOG;
            case "biblionet" -> Source.BIBLIONET;
            case "not-found" -> Source.NOT_FOUND;
            case "local" -> Source.LOCAL;
            default -> throw new IllegalStateException("Unknown source in response: " + response.path("source"));
        };
    }
}

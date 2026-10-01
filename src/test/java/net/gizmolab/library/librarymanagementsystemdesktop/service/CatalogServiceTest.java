package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.MagazineDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PersonDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublicationDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.testsupport.FixtureServer;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.gizmolab.library.librarymanagementsystemdesktop.testsupport.FixtureServer.fixture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    private FixtureServer server;
    private CatalogService catalog;

    @BeforeEach
    void setUp() throws Exception {
        server = new FixtureServer();
        AuthService auth = mock(AuthService.class);
        when(auth.getStrapiBaseUrl()).thenReturn(server.url());
        when(auth.getJwt()).thenReturn("jwt");
        catalog = new CatalogService(new StrapiApiClient(auth));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static PublicationDraft brochureDraft() {
        PublicationDraft d = new PublicationDraft();
        d.setType(PublicationDraft.BROCHURE);
        d.setTitle("Μανιφέστο");
        return d;
    }

    @Test
    void isbnFoundInBiblionet() throws Exception {
        server.on("POST", "/api/books/isbn-lookup", 200, fixture("isbn-lookup-found.json"));
        CatalogService.IsbnLookupResult r = catalog.lookupIsbn("978-960-211-652-4");
        assertEquals(CatalogService.Source.BIBLIONET, r.source());
        assertEquals("Θεραπείας συνέχεια", r.publication().getTitle());
        assertEquals(2, r.publication().getContributors().size());
    }

    @Test
    void isbnNotFound() throws Exception {
        server.on("POST", "/api/books/isbn-lookup", 200, fixture("isbn-lookup-not-found.json"));
        CatalogService.IsbnLookupResult r = catalog.lookupIsbn("9791032305690");
        assertEquals(CatalogService.Source.NOT_FOUND, r.source());
        assertNull(r.publication());
    }

    @Test
    void personCreatedOrDuplicate() throws Exception {
        PersonDraft draft = new PersonDraft();
        draft.setFirstname("Νέο");
        server.on("POST", "/api/persons/local", 201,
                "{\"source\":\"local\",\"data\":{\"id\":9,\"documentId\":\"ps9\",\"name\":\"Νέο\",\"reviewed\":false}}");
        CatalogService.CreateResult<PersonDTO> created = catalog.createPerson(draft);
        assertFalse(created.isDuplicate());
        assertEquals("ps9", created.created().getDocumentId());

        server.on("POST", "/api/persons/local", 409, fixture("persons-local-duplicate-409.json"));
        CatalogService.CreateResult<PersonDTO> dup = catalog.createPerson(draft);
        assertTrue(dup.isDuplicate());
        assertEquals(List.of("Ομάδα Γειτονιάς"), dup.duplicates().stream().map(PersonDTO::getDisplayName).toList());
    }

    @Test
    void publicationLocalBiblionetOrDuplicate() throws Exception {
        server.on("POST", "/api/books/local", 201,
                "{\"source\":\"local\",\"data\":{\"id\":12,\"documentId\":\"bk12\",\"title\":\"Μανιφέστο\",\"type\":\"Μπροσούρα\"}}");
        CatalogService.PublicationCreateResult local = catalog.createPublication(brochureDraft());
        assertEquals(CatalogService.Source.LOCAL, local.source());
        assertEquals("bk12", local.publication().getDocumentId());

        server.on("POST", "/api/books/local", 200, fixture("isbn-lookup-found.json"));
        assertEquals(CatalogService.Source.BIBLIONET, catalog.createPublication(brochureDraft()).source());

        server.on("POST", "/api/books/local", 409, fixture("books-local-duplicate-409.json"));
        CatalogService.PublicationCreateResult dup = catalog.createPublication(brochureDraft());
        assertTrue(dup.isDuplicate());
        assertEquals("Μανιφέστο", dup.duplicates().get(0).getTitle());
    }

    @Test
    void searchesAndLists() throws Exception {
        server.on("GET", "/api/books/search", 200, fixture("brochures-search.json"));
        server.on("GET", "/api/persons/search", 200, fixture("persons-search.json"));
        server.on("GET", "/api/contributor-roles", 200, fixture("contributor-roles.json"));

        List<PublicationDTO> brochures = catalog.searchBrochures("μανιφεστο");
        assertEquals("Μανιφέστο", brochures.get(0).getTitle());
        assertEquals("Ομάδα Γειτονιάς", brochures.get(0).getAuthorNames());
        assertEquals(1, brochures.get(0).getTotalCopies()); // "Στη βιβλιοθήκη σας": copies come with the search
        assertEquals("Ομάδα Γειτονιάς", catalog.searchPersons("γειτονια").get(0).getDisplayName());
        List<ContributorRoleDTO> roles = catalog.getRoles();
        assertEquals(List.of("Συγγραφέας", "Μεταφραστής"), roles.stream().map(ContributorRoleDTO::getName).toList());
        assertTrue(roles.get(0).isAuthor());
    }

    @Test
    void subjectsLoadEveryPage() throws Exception { // final review: Strapi maxLimit is 100
        String page = "{\"data\":[{\"id\":%d,\"documentId\":\"s%d\",\"subjectTitle\":\"Θέμα %d\",\"subjectDDC\":\"%d\"}],"
                + "\"meta\":{\"pagination\":{\"page\":%d,\"pageSize\":100,\"pageCount\":2,\"total\":2}}}";
        server.onSequence("GET", "/api/subjects", List.of(
                new Object[]{200, String.format(page, 1, 1, 1, 100, 1)},
                new Object[]{200, String.format(page, 2, 2, 2, 200, 2)}));

        List<SubjectDTO> subjects = catalog.getSubjects();

        assertEquals(List.of("s1", "s2"), subjects.stream().map(SubjectDTO::getDocumentId).toList());
        assertEquals(2, server.requests().size());
        assertTrue(server.requests().get(1).contains("pagination[page]=2"), server.requests().get(1));
        assertTrue(server.requests().stream().allMatch(r -> r.contains("pagination[pageSize]=100")));
    }

    @Test
    void copiesContinueAfterTheHighestNumberEvenBeyond100() { // final review minor: only the highest is fetched
        server.on("GET", "/api/copies", 200, "{\"data\":[{\"id\":150,\"documentId\":\"c150\",\"copyNumber\":150}]}");
        server.on("POST", "/api/copies", 200, "{\"data\":{\"id\":151,\"documentId\":\"c151\",\"copyNumber\":151}}");

        assertTrue(catalog.addCopies("p5", "l3", 1, "NEW").isComplete());

        assertTrue(server.requests().get(0).contains("sort=copyNumber:desc&pagination[pageSize]=1"), server.requests().get(0));
        assertTrue(server.bodies().stream().anyMatch(b -> b.contains("\"copyNumber\":151")));
    }

    @Test
    void copiesContinueAfterExistingNumbers() { // Review Focus 3
        server.on("GET", "/api/copies", 200,
                "{\"data\":[{\"id\":1,\"documentId\":\"c1\",\"copyNumber\":1},{\"id\":2,\"documentId\":\"c2\",\"copyNumber\":2}]}");
        server.on("POST", "/api/copies", 200, "{\"data\":{\"id\":10,\"documentId\":\"c10\",\"copyNumber\":3}}");

        CatalogService.CopiesResult r = catalog.addCopies("p5", "l3", 2, "GOOD");

        assertTrue(r.isComplete());
        assertEquals(2, r.created());
        List<String> posted = server.bodies().stream().filter(b -> b.contains("copyNumber")).toList();
        assertEquals(2, posted.size());
        assertTrue(posted.get(0).contains("\"copyNumber\":3"));
        assertTrue(posted.get(1).contains("\"copyNumber\":4"));
        assertTrue(posted.get(0).contains("\"condition\":\"GOOD\""));
        assertTrue(posted.get(0).contains("\"publication\":\"p5\""), posted.get(0));
        assertTrue(server.requests().get(0).contains("filters[publication][documentId][$eq]=p5&filters[library][documentId][$eq]=l3"));
    }

    @Test
    void copiesStopAtFirstFailure() {
        server.on("GET", "/api/copies", 200, "{\"data\":[]}");
        server.onSequence("POST", "/api/copies", List.of(
                new Object[]{200, "{\"data\":{\"id\":10,\"documentId\":\"c10\"}}"},
                new Object[]{500, "Internal Server Error"}));

        CatalogService.CopiesResult r = catalog.addCopies("p5", "l3", 3, "NEW");

        assertFalse(r.isComplete());
        assertEquals(1, r.created());
        assertEquals(3, r.requested());
        assertEquals(UserMessages.GENERIC, r.errorMessage());
    }

    @Test
    void issnLookupKnowsEverySource() throws Exception {
        server.onSequence("POST", "/api/magazines/issn-lookup", List.of(
                new Object[]{200, fixture("magazine-issn-lookup-nlg.json")},
                new Object[]{200, fixture("magazine-issn-lookup-catalog.json")},
                new Object[]{200, fixture("magazine-issn-lookup-not-found.json")},
                new Object[]{200, fixture("magazine-issn-lookup-unavailable.json")}));

        CatalogService.MagazineLookupResult nlg = catalog.lookupIssn("2241-5580");
        assertEquals(CatalogService.MagazineSource.NLG, nlg.source());
        assertEquals("Κοινωνικός Αναρχισμός", nlg.magazine().getTitle());
        assertEquals(CatalogService.MagazineSource.CATALOG, catalog.lookupIssn("977224155800805").source());
        CatalogService.MagazineLookupResult notFound = catalog.lookupIssn("0317-8471");
        assertEquals(CatalogService.MagazineSource.NOT_FOUND, notFound.source());
        assertNull(notFound.magazine());
        assertEquals("0317-8471", notFound.issn());
        CatalogService.MagazineLookupResult down = catalog.lookupIssn("1108-2402");
        assertEquals(CatalogService.MagazineSource.UNAVAILABLE, down.source());
        assertEquals("1108-2402", down.issn());
    }

    @Test
    void magazineSearchAndDuplicate() throws Exception {
        server.on("GET", "/api/magazines/search", 200, fixture("magazines-search.json"));
        server.on("POST", "/api/magazines/local", 409, fixture("magazines-local-duplicate-409.json"));

        assertEquals(1, catalog.searchMagazines("αναρχισμος").get(0).getIssuesInLibrary());
        MagazineDraft draft = new MagazineDraft();
        draft.setTitle("ΚΟΙΝΩΝΙΚΟΣ ΑΝΑΡΧΙΣΜΟΣ");
        CatalogService.CreateResult<MagazineDTO> result = catalog.createMagazine(draft);
        assertTrue(result.isDuplicate());
        assertEquals("Κοινωνικός Αναρχισμός", result.duplicates().get(0).getTitle());
    }

    @Test
    void issuesComeSortedWithTheLibraryCopies() throws Exception {
        server.on("GET", "/api/books", 200, fixture("issues-of-magazine.json"));
        List<PublicationDTO> issues = catalog.getIssues("m1", null);
        assertEquals(List.of("5", "10", "Άνοιξη 2020"), issues.stream()
                .map(i -> i.getIssueNumber() != null ? i.getIssueNumber() : i.getPublicationMonthYear()).toList());
        assertEquals(1, issues.get(0).getTotalCopies());
        assertEquals("Αφιέρωμα", issues.get(0).getSubtitle());
    }

    @Test
    void issuesLoadEveryPage() throws Exception { // final review M-4: Strapi maxLimit is 100
        String page = "{\"data\":[{\"id\":%d,\"documentId\":\"i%<d\",\"title\":\"Π\",\"type\":\"Περιοδικό\",\"issueNumber\":\"%s\"}],"
                + "\"meta\":{\"pagination\":{\"page\":%d,\"pageSize\":100,\"pageCount\":2,\"total\":2}}}";
        server.onSequence("GET", "/api/books", List.of(
                new Object[]{200, String.format(page, 1, "101", 1)},
                new Object[]{200, String.format(page, 2, "7", 2)}));

        List<PublicationDTO> issues = catalog.getIssues("m9", null);

        assertEquals(List.of("7", "101"), issues.stream().map(PublicationDTO::getIssueNumber).toList());
        assertEquals(2, server.requests().size());
        assertTrue(server.requests().get(1).contains("pagination[page]=2"), server.requests().get(1));
    }

    @Test
    void duplicateIssueComesBackAsCandidates() throws Exception {
        server.on("POST", "/api/books/local", 409, fixture("issue-local-duplicate-409.json"));
        PublicationDraft draft = new PublicationDraft();
        draft.setType(PublicationDraft.PERIODICAL);
        draft.setTitle("Κοινωνικός Αναρχισμός");
        draft.setMagazineId("m1");
        draft.setIssueNumber("05");
        CatalogService.PublicationCreateResult result = catalog.createPublication(draft);
        assertTrue(result.isDuplicate());
        assertEquals("5", result.duplicates().get(0).getIssueNumber());
    }
}

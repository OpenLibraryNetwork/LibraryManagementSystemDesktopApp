package net.gizmolab.library.librarymanagementsystemdesktop.dto.draft;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorRoleDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DraftsTest {

    private static PersonDTO person(long id) {
        PersonDTO p = new PersonDTO();
        p.setId(id);
        p.setName("P" + id);
        return p;
    }

    private static final ContributorRoleDTO AUTHOR = new ContributorRoleDTO(1L, "Συγγραφέας", "1");

    @Test
    void brochurePayloadHasOnlyFilledFields() {
        PublicationDraft d = new PublicationDraft();
        d.setType("Μπροσούρα");
        d.setTitle("  Για την αυτοοργάνωση ");
        d.setSubtitle(" ");
        d.setYearText("2019");
        d.setPagesText("");
        d.setLanguage("Ελληνικά");
        d.setPublisherId(7L);
        d.setContributors(List.of(new ContributorDTO(person(3), AUTHOR)));
        d.setSubjectIds(List.of(20L));
        d.setIsbn("123"); // ignored for brochures

        assertTrue(d.validate().isEmpty());
        assertEquals(Map.of("data", Map.of(
                "type", "Μπροσούρα",
                "title", "Για την αυτοοργάνωση",
                "yearPublished", 2019,
                "language", "Ελληνικά",
                "publisher", 7L,
                "contributors", List.of(Map.of("person", 3L, "role", 1L)),
                "subjects", List.of(20L))), d.toPayload());
    }

    @Test
    void bookPayloadCarriesIsbn() {
        PublicationDraft d = new PublicationDraft();
        d.setType("Βιβλίο");
        d.setIsbn(" 978-0-306-40615-7 ");
        d.setTitle("Foreign Book");
        d.setOriginalLanguage("English");
        assertEquals(Map.of("data", Map.of(
                "type", "Βιβλίο", "isbn", "978-0-306-40615-7", "title", "Foreign Book", "originalLanguage", "English")),
                d.toPayload());
    }

    @Test
    void publicationValidation() {
        PublicationDraft d = new PublicationDraft();
        d.setType("Βιβλίο");
        d.setTitle(" ");
        d.setYearText("πέρσι");
        d.setPagesText("12α");
        d.setContributors(List.of(new ContributorDTO(person(3), null)));
        Map<String, String> errors = d.validate();
        assertEquals(List.of("title", "isbn", "year", "pages", "contributors"), List.copyOf(errors.keySet()));
        assertEquals("Ο τίτλος είναι υποχρεωτικός.", errors.get("title"));
    }

    @Test
    void personDraft() {
        PersonDraft p = new PersonDraft();
        assertEquals("Συμπληρώστε όνομα ή επώνυμο.", p.validate().get("name"));
        p.setFirstname(" Νίκη ");
        p.setLastname("Λοϊζίδη");
        p.setQualifier("1950-");
        p.setBornYear("");
        assertTrue(p.validate().isEmpty());
        assertEquals(Map.of("data", Map.of("name", "Νίκη Λοϊζίδη", "firstname", "Νίκη", "lastname", "Λοϊζίδη", "qualifier", "1950-")),
                p.toPayload());
    }

    @Test
    void publisherDraft() {
        PublisherDraft p = new PublisherDraft();
        assertEquals("Το όνομα είναι υποχρεωτικό.", p.validate().get("name"));
        p.setName(" Εκδόσεις Γειτονιάς ");
        p.setEmail("info@example.org");
        assertEquals(Map.of("data", Map.of("name", "Εκδόσεις Γειτονιάς", "email", "info@example.org")), p.toPayload());
    }

    @Test
    void issueDraft() {
        PublicationDraft d = new PublicationDraft();
        d.setType(PublicationDraft.PERIODICAL);
        d.setTitle("Κοινωνικός Αναρχισμός");
        assertEquals(java.util.Set.of("magazine", "issue"), d.validate().keySet());

        d.setMagazineId(3L);
        d.setPeriod(" Άνοιξη 2020 ");
        assertTrue(d.validate().isEmpty());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) d.toPayload().get("data");
        assertEquals("Περιοδικό", data.get("type"));
        assertEquals(3L, data.get("magazine"));
        assertEquals("Άνοιξη 2020", data.get("publicationMonthYear"));
        assertFalse(data.containsKey("issueNumber"));
        assertFalse(data.containsKey("isbn"));
    }

    @Test
    void magazineDraft() {
        MagazineDraft d = new MagazineDraft();
        d.setIssn("2241-5581");
        assertEquals(java.util.Set.of("title", "issn"), d.validate().keySet());

        d.setTitle(" Κοινωνικός Αναρχισμός ");
        d.setIssn("22415580");
        d.setPlace("Θεσσαλονίκη");
        d.setPublisherId(4L);
        assertTrue(d.validate().isEmpty());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) d.toPayload().get("data");
        assertEquals(java.util.Map.of("title", "Κοινωνικός Αναρχισμός", "issn", "2241-5580", "place", "Θεσσαλονίκη", "publisher", 4L), data);
    }
}

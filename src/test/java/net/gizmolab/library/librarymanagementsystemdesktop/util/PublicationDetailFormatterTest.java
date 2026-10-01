package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicationDetailFormatterTest {

    private static PersonDTO person(String name, String qualifier) {
        PersonDTO p = new PersonDTO();
        p.setName(name);
        p.setQualifier(qualifier);
        return p;
    }

    private static PublicationDTO book() {
        PublicationDTO pub = new PublicationDTO();
        pub.setTitle("Θεραπείας συνέχεια");
        pub.setType("Βιβλίο");
        pub.setIsbn("9789602116524");
        pub.setYearPublished(2002);
        pub.setPages(119);
        pub.setLanguage("");
        pub.setBinding("Μαλακό εξώφυλλο");
        PublisherDTO publisher = new PublisherDTO();
        publisher.setName("Νεφέλη");
        pub.setPublisher(publisher);
        pub.setContributors(List.of(
                new ContributorDTO(person("Νίκη Λοϊζίδη", null), new ContributorRoleDTO(1L, "Συγγραφέας", "1")),
                new ContributorDTO(person("Παναγιώτης Σκόνδρας", "1960-"), new ContributorRoleDTO(2L, "Μεταφραστής", "2"))));
        pub.setSubjects(List.of(new SubjectDTO(1L, "Νεοελληνική πεζογραφία", "889.3", "20"),
                new SubjectDTO(2L, "Χωρίς κωδικό", null, null)));
        return pub;
    }

    @Test
    void contributorLinesInOrder() {
        assertEquals(List.of("Νίκη Λοϊζίδη — Συγγραφέας", "Παναγιώτης Σκόνδρας (1960-) — Μεταφραστής"),
                PublicationDetailFormatter.contributorLines(book()));
    }

    @Test
    void subjectLines() {
        assertEquals(List.of("889.3 Νεοελληνική πεζογραφία", "Χωρίς κωδικό"),
                PublicationDetailFormatter.subjectLines(book()));
    }

    @Test
    void originLabel() {
        PublicationDTO pub = book();
        pub.setBiblionetId("72584");
        pub.setReviewed(true);
        assertEquals("Biblionet", PublicationDetailFormatter.originLabel(pub));
        pub.setBiblionetId(null);
        pub.setReviewed(false);
        assertEquals("Τοπική εγγραφή — εκκρεμεί έλεγχος από τον καταλογογράφο",
                PublicationDetailFormatter.originLabel(pub));
    }

    @Test
    void detailRowsSkipBlanksAndKeepOrder() {
        List<String[]> rows = PublicationDetailFormatter.detailRows(book());
        assertEquals(List.of("Τύπος", "ISBN", "Εκδότης", "Έτος", "Σελίδες", "Βιβλιοδεσία"),
                rows.stream().map(r -> r[0]).toList());
        assertEquals("119", rows.get(4)[1]);
    }

    @Test
    void coverUrl() {
        assertEquals("http://host:1337/uploads/c.jpg",
                PublicationDetailFormatter.resolveCoverUrl("/uploads/c.jpg", "http://host:1337"));
        assertEquals("https://biblionet.gr/x.jpg",
                PublicationDetailFormatter.resolveCoverUrl("https://biblionet.gr/x.jpg", "http://host:1337"));
        assertNull(PublicationDetailFormatter.resolveCoverUrl(null, "http://host:1337"));
        assertNull(PublicationDetailFormatter.resolveCoverUrl(" ", "http://host:1337"));
    }

    @Test
    void borrowedCopyCannotBeDeleted() {
        CopyDTO copy = new CopyDTO();
        copy.setAvailable(false);
        assertFalse(PublicationDetailFormatter.canDeleteCopy(copy));
        copy.setAvailable(true);
        assertTrue(PublicationDetailFormatter.canDeleteCopy(copy));
    }

    @Test
    void authorSuffixIsEmptyWithoutAuthors() { // review #4: no orphan " — " in the borrow form
        assertEquals(" — Νίκη Λοϊζίδη", PublicationDetailFormatter.authorSuffix(book()));
        PublicationDTO brochure = new PublicationDTO();
        brochure.setTitle("Χωρίς συγγραφέα");
        assertEquals("", PublicationDetailFormatter.authorSuffix(brochure));
    }

    private static PublicationDTO issue(String number, String period) {
        PublicationDTO pub = new PublicationDTO();
        pub.setTitle("Κοινωνικός Αναρχισμός");
        pub.setType("Περιοδικό");
        pub.setIssueNumber(number);
        pub.setPublicationMonthYear(period);
        return pub;
    }

    @Test
    void issueLabelAndDisplayTitle() {
        assertEquals("τεύχ. 5 (Δεκέμβριος 2016)", PublicationDetailFormatter.issueLabel("5", "Δεκέμβριος 2016"));
        assertEquals("τεύχ. 12-13", PublicationDetailFormatter.issueLabel(" 12-13 ", null));
        assertEquals("Άνοιξη 2020", PublicationDetailFormatter.issueLabel("", "Άνοιξη 2020"));
        assertEquals("", PublicationDetailFormatter.issueLabel(null, " "));
        assertEquals("Κοινωνικός Αναρχισμός — τεύχ. 5", PublicationDetailFormatter.displayTitle(issue("5", null)));
        assertEquals("Θεραπείας συνέχεια", PublicationDetailFormatter.displayTitle(book()));
    }

    @Test
    void detailRowsOfAnIssue() {
        List<String[]> rows = PublicationDetailFormatter.detailRows(issue("5", "Δεκέμβριος 2016"));
        assertEquals(List.of("Τύπος", "Περιοδικό", "Αριθμός", "Περίοδος"),
                rows.stream().map(r -> r[0]).toList().subList(0, 4));
        assertEquals("Κοινωνικός Αναρχισμός", rows.get(1)[1]);
    }
}

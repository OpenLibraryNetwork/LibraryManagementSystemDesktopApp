package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.gizmolab.library.librarymanagementsystemdesktop.testsupport.FixtureServer.fixture;
import static org.junit.jupiter.api.Assertions.*;

class MagazineConverterTest {

    private static JsonNode json(String text) throws Exception {
        return new ObjectMapper().readTree(text);
    }

    @Test
    void magazineFromNationalLibraryLookup() throws Exception {
        MagazineDTO m = DTOConverter.magazineFromJson(json(fixture("magazine-issn-lookup-nlg.json")).path("data"));
        assertEquals("Κοινωνικός Αναρχισμός", m.getTitle());
        assertEquals("2241-5580", m.getIssn());
        assertEquals("Θεσσαλονίκη", m.getPlace());
        assertEquals("633300", m.getNlgBiblionumber());
        assertTrue(m.isFromNlg());
        assertEquals("Ελευθεριακές Εκδόσεις Κουρσάλ", m.getPublisher().getName());
        assertEquals("Κοινωνικός Αναρχισμός", m.getDisplayName());
    }

    @Test
    void searchAndInLibraryCarryTheLibraryIssueCount() throws Exception {
        assertEquals(1, DTOConverter.magazinesFromJson(json(fixture("magazines-search.json"))).get(0).getIssuesInLibrary());
        assertEquals(1, DTOConverter.magazinesFromJson(json(fixture("magazines-in-library.json"))).get(0).getIssuesInLibrary());
    }

    @Test
    void displayNameShowsTheQualifier() {
        MagazineDTO m = new MagazineDTO();
        m.setTitle("Αναρχία");
        m.setQualifier("Θεσσαλονίκη");
        assertEquals("Αναρχία (Θεσσαλονίκη)", m.getDisplayName());
    }

    @Test
    void issueNumberIsText() throws Exception {
        List<PublicationDTO> issues = DTOConverter.publicationsFromJson(json(fixture("issues-of-magazine.json")));
        assertTrue(issues.stream().anyMatch(i -> "5".equals(i.getIssueNumber()) && "Δεκέμβριος 2016".equals(i.getPublicationMonthYear())));
        assertTrue(issues.stream().anyMatch(i -> i.getIssueNumber() == null && "Άνοιξη 2020".equals(i.getPublicationMonthYear())));
    }

    @Test
    void copyOfAnIssueShowsWhichIssue() throws Exception {
        CopyDTO copy = DTOConverter.copyFromJson(json("{\"id\":4,\"documentId\":\"c4\",\"copyNumber\":1,\"isAvailable\":true,"
                + "\"publication\":{\"id\":9,\"documentId\":\"i9\",\"title\":\"Κοινωνικός Αναρχισμός\",\"type\":\"Περιοδικό\","
                + "\"issueNumber\":\"5\",\"publicationMonthYear\":\"Δεκέμβριος 2016\"}}"));
        assertEquals("Κοινωνικός Αναρχισμός — τεύχ. 5 (Δεκέμβριος 2016)", copy.getPublicationTitle());
        assertEquals("c4", copy.getDocumentId());
        assertEquals("i9", copy.getPublicationDocumentId());
    }
}

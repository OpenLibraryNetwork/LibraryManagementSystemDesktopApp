package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import tools.jackson.databind.ObjectMapper;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlatFormatTest {

    private static final String BOOK = "{\"data\":{\"id\":7,\"documentId\":\"bk1\",\"title\":\"Τ\",\"type\":\"Βιβλίο\","
            + "\"publisher\":{\"id\":3,\"documentId\":\"pb1\",\"name\":\"Ε\"},"
            + "\"copies\":[{\"documentId\":\"c1\",\"isAvailable\":true,\"library\":{\"documentId\":\"libA\"}},"
            + "{\"documentId\":\"c2\",\"isAvailable\":false,\"library\":{\"documentId\":\"libA\"}},"
            + "{\"documentId\":\"c3\",\"isAvailable\":true,\"library\":{\"documentId\":\"libB\"}}]}}";

    @Test
    void readsTheFlatStrapi5Format() throws Exception {
        PublicationDTO pub = DTOConverter.publicationFromJson(new ObjectMapper().readTree(BOOK), "libA");
        assertEquals("bk1", pub.getDocumentId());
        assertEquals("Τ", pub.getTitle());
        assertEquals("pb1", pub.getPublisher().getDocumentId());
        assertEquals("Ε", pub.getPublisher().getName());
    }

    @Test
    void countsOnlyThisLibrarysCopies() throws Exception { // Review Focus 5
        PublicationDTO pub = DTOConverter.publicationFromJson(new ObjectMapper().readTree(BOOK), "libA");
        assertEquals(2, pub.getTotalCopies());
        assertEquals(1, pub.getAvailableCopies());
    }

    @Test
    void nonScalarOrMalformedFieldsDoNotBreakTheWholeList() throws Exception { // final review: Jackson 3 converts strictly
        String json = "{\"data\":[{\"id\":1,\"documentId\":\"bk1\",\"title\":\"Α\",\"summary\":[{\"type\":\"paragraph\"}],"
                + "\"pages\":\"abc\",\"price\":\"n/a\"},{\"id\":2,\"documentId\":\"bk2\",\"title\":\"Β\"}]}";
        java.util.List<PublicationDTO> list = DTOConverter.publicationsFromJson(new ObjectMapper().readTree(json));
        assertEquals(java.util.List.of("Α", "Β"), list.stream().map(PublicationDTO::getTitle).toList());
        assertNull(list.get(0).getSummary());
        assertNull(list.get(0).getPages());
        assertNull(list.get(0).getPrice());
    }
}

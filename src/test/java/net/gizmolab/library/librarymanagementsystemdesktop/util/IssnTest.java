package net.gizmolab.library.librarymanagementsystemdesktop.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IssnTest {

    @Test
    void normalizes() {
        assertEquals(Optional.of("2241-5580"), Issn.normalize("22415580"));
        assertEquals(Optional.of("2241-5580"), Issn.normalize(" 2241 5580 "));
        assertEquals(Optional.of("0000-006X"), Issn.normalize("0000-006x")); // Review Focus 3
        assertTrue(Issn.normalize("2241-5581").isEmpty());
        assertTrue(Issn.normalize(null).isEmpty());
    }

    @Test
    void barcodes() {
        assertEquals(Optional.of("2241-5580"), Issn.parseCode("9772241558008"));
        assertEquals(Optional.of("2241-5580"), Issn.parseCode("977224155800805"));    // Review Focus 3: 2-digit add-on
        assertEquals(Optional.of("2241-5580"), Issn.parseCode("977224155800812345")); // 5-digit add-on
        assertEquals(Optional.of("1108-2402"), Issn.parseCode("1108-2402"));
        assertTrue(Issn.parseCode("9772241558009").isEmpty()); // wrong EAN check digit
        assertTrue(Issn.parseCode("9789602116524").isEmpty()); // ISBN barcode
    }

    @Test
    void tellsCodesFromTitles() {
        assertTrue(Issn.looksLikeCode("2241-5580"));
        assertTrue(Issn.looksLikeCode("2241-5581"));      // invalid, but still a code: the user gets "invalid ISSN"
        assertTrue(Issn.looksLikeCode("9789602116524"));  // a barcode, rejected later
        assertFalse(Issn.looksLikeCode("Κοινωνικός Αναρχισμός"));
        assertFalse(Issn.looksLikeCode("1984"));
        assertFalse(Issn.looksLikeCode(""));
    }
}

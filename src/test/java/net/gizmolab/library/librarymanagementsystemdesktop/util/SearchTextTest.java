package net.gizmolab.library.librarymanagementsystemdesktop.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchTextTest {

    @Test
    void normalizeLikeTheServer() {
        assertEquals("λοιζιδησ", SearchText.normalize("ΛΟΪΖΊΔΗΣ"));
        assertEquals("889 3 νεοελληνικη πεζογραφια", SearchText.normalize("889.3 Νεοελληνική πεζογραφία"));
        assertEquals("", SearchText.normalize(null));
    }

    @Test
    void onlyRealTextIsSearchable() { // Review Focus 4
        assertFalse(SearchText.isSearchable("Ν."));
        assertFalse(SearchText.isSearchable("--"));
        assertFalse(SearchText.isSearchable("  "));
        assertFalse(SearchText.isSearchable(null));
        assertTrue(SearchText.isSearchable("Λο"));
        assertTrue(SearchText.isSearchable("1 9"));
    }

    @Test
    void everyWordMustMatch() {
        assertTrue(SearchText.matches("889.3 Νεοελληνική πεζογραφία", "ΠΕΖΟ 889"));
        assertFalse(SearchText.matches("889.3 Νεοελληνική πεζογραφία", "πεζο ποιηση"));
        assertTrue(SearchText.matches("οτιδήποτε", "  "));
    }
}

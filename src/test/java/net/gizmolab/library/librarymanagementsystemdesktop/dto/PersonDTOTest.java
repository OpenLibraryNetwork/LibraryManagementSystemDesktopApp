package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonDTOTest {

    private static PersonDTO person(String name, String qualifier, String born, String death) {
        PersonDTO p = new PersonDTO();
        p.setName(name);
        p.setQualifier(qualifier);
        p.setBornYear(born);
        p.setDeathYear(death);
        return p;
    }

    @Test
    void displayNameAddsQualifier() {
        assertEquals("Γιάννης Παπαδόπουλος (1950-)", person("Γιάννης Παπαδόπουλος", "1950-", null, null).getDisplayName());
        assertEquals("Γιάννης Παπαδόπουλος", person("Γιάννης Παπαδόπουλος", " ", null, null).getDisplayName());
        assertEquals("", person(null, null, null, null).getDisplayName());
    }

    @Test
    void lifespan() {
        assertEquals("1950–2010", person("x", null, "1950", "2010").getLifespan());
        assertEquals("1950–", person("x", null, "1950", "").getLifespan());
        assertEquals("–2010", person("x", null, null, "2010").getLifespan());
        assertEquals("", person("x", null, "", null).getLifespan());
    }

    @Test
    void origin() {
        PersonDTO p = person("x", null, null, null);
        assertFalse(p.isFromBiblionet());
        p.setBiblionetPersonId("15521");
        assertTrue(p.isFromBiblionet());
    }
}

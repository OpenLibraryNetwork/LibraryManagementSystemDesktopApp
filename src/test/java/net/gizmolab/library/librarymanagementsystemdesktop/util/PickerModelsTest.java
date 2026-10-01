package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorRoleDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.SubjectDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PickerModelsTest {

    private static final ContributorRoleDTO AUTHOR = new ContributorRoleDTO(1L, "Συγγραφέας", "1");
    private static final ContributorRoleDTO TRANSLATOR = new ContributorRoleDTO(2L, "Μεταφραστής", "2");

    private static PersonDTO person(long id, String name) {
        PersonDTO p = new PersonDTO();
        p.setId(id);
        p.setName(name);
        return p;
    }

    @Test
    void contributorSelectionKeepsOrderAndRejectsDuplicates() {
        ContributorSelection s = new ContributorSelection();
        PersonDTO niki = person(1, "Νίκη");
        PersonDTO panos = person(2, "Πάνος");
        assertTrue(s.add(niki, AUTHOR));
        assertTrue(s.add(panos, TRANSLATOR));
        assertTrue(s.add(niki, TRANSLATOR));      // same person, other role: allowed
        assertFalse(s.add(niki, AUTHOR));         // same pair: rejected
        assertFalse(s.add(null, AUTHOR));
        assertFalse(s.add(panos, null));

        s.moveUp(1);
        assertEquals(List.of("Πάνος — Μεταφραστής", "Νίκη — Συγγραφέας", "Νίκη — Μεταφραστής"),
                s.items().stream().map(ContributorSelection::label).toList());
        s.moveDown(2); // already last: no change
        s.moveUp(0);   // already first: no change
        s.remove(1);
        assertEquals(List.of("Πάνος — Μεταφραστής", "Νίκη — Μεταφραστής"),
                s.items().stream().map(ContributorSelection::label).toList());
    }

    @Test
    void onlyTheLatestSearchMayUpdateTheList() { // Review Focus 1
        LatestRequestGate gate = new LatestRequestGate();
        long first = gate.next();  // "Λο"
        long second = gate.next(); // "Λοϊζ"
        assertFalse(gate.isLatest(first));
        assertTrue(gate.isLatest(second));
    }

    @Test
    void subjectFilterIgnoresAccentsAndMatchesCode() {
        List<SubjectDTO> all = List.of(
                new SubjectDTO(1L, "Νεοελληνική πεζογραφία", "889.3", "20"),
                new SubjectDTO(2L, "Ποίηση", null, "21"));
        assertEquals("889.3 Νεοελληνική πεζογραφία", SubjectFilter.label(all.get(0)));
        assertEquals("Ποίηση", SubjectFilter.label(all.get(1)));
        assertEquals(List.of(1L), SubjectFilter.filter(all, "ΠΕΖΟΓΡΑΦΙΑ").stream().map(SubjectDTO::getId).toList());
        assertEquals(List.of(1L), SubjectFilter.filter(all, "889").stream().map(SubjectDTO::getId).toList());
        assertEquals(2, SubjectFilter.filter(all, "").size());
    }
}

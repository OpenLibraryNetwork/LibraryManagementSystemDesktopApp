package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorRoleDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Ordered contributors of the local cataloguing form; the same (person, role) pair only once. */
public class ContributorSelection {

    private final List<ContributorDTO> items = new ArrayList<>();

    public boolean add(PersonDTO person, ContributorRoleDTO role) {
        if (person == null || role == null) return false;
        boolean exists = items.stream().anyMatch(c ->
                Objects.equals(c.getPerson().getDocumentId(), person.getDocumentId()) && Objects.equals(c.getRole().getDocumentId(), role.getDocumentId()));
        if (exists) return false;
        items.add(new ContributorDTO(person, role));
        return true;
    }

    public void moveUp(int index) {
        if (index > 0 && index < items.size()) Collections.swap(items, index, index - 1);
    }

    public void moveDown(int index) {
        if (index >= 0 && index < items.size() - 1) Collections.swap(items, index, index + 1);
    }

    public void remove(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    public List<ContributorDTO> items() {
        return List.copyOf(items);
    }

    public static String label(ContributorDTO c) {
        return c.getPerson().getDisplayName() + " — " + c.getRole().getName();
    }
}

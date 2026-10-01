package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorWorksFilterTest {

    private static final ContributorRoleDTO AUTHOR = new ContributorRoleDTO(1L, "Συγγραφέας", "1");
    private static final ContributorRoleDTO TRANSLATOR = new ContributorRoleDTO(2L, "Μεταφραστής", "2");

    private static PersonDTO person(long id) {
        PersonDTO p = new PersonDTO();
        p.setId(id);
        p.setName("P" + id);
        return p;
    }

    private static PublicationDTO pub(String title, ContributorDTO... contributors) {
        PublicationDTO p = new PublicationDTO();
        p.setTitle(title);
        p.setContributors(List.of(contributors));
        return p;
    }

    @Test
    void keepsOnlyWorksWhereThePersonIsAnAuthor() { // Review Focus 1
        PersonDTO niki = person(10);
        PersonDTO other = person(11);
        List<PublicationDTO> fromServer = List.of(
                pub("Συγγραφέας", new ContributorDTO(niki, AUTHOR)),
                pub("Μόνο μεταφράστρια", new ContributorDTO(other, AUTHOR), new ContributorDTO(niki, TRANSLATOR)),
                pub("Και τα δύο", new ContributorDTO(niki, TRANSLATOR), new ContributorDTO(niki, AUTHOR)),
                pub("Άλλου", new ContributorDTO(other, AUTHOR)));

        assertEquals(List.of("Συγγραφέας", "Και τα δύο"),
                AuthorWorksFilter.authoredBy(fromServer, 10L).stream().map(PublicationDTO::getTitle).toList());
    }
}

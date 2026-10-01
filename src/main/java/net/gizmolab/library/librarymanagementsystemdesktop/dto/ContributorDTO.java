package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/** A person with a role on a publication, in presentation order. */
public class ContributorDTO {
    private final PersonDTO person;
    private final ContributorRoleDTO role;

    public ContributorDTO(PersonDTO person, ContributorRoleDTO role) {
        this.person = person;
        this.role = role;
    }

    public PersonDTO getPerson() { return person; }
    public ContributorRoleDTO getRole() { return role; }
}

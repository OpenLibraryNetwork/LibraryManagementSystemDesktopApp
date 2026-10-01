package net.gizmolab.library.librarymanagementsystemdesktop.dto;

public class ContributorRoleDTO {
    /** Biblionet ContributorTypeID of "Συγγραφέας". The role is recognised by this id, never by name. */
    public static final String AUTHOR_TYPE_ID = "1";

    private Long id;
    private String name;
    private String biblionetTypeId;

    public ContributorRoleDTO() {}

    public ContributorRoleDTO(Long id, String name, String biblionetTypeId) {
        this.id = id;
        this.name = name;
        this.biblionetTypeId = biblionetTypeId;
    }

    public boolean isAuthor() { return AUTHOR_TYPE_ID.equals(biblionetTypeId); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBiblionetTypeId() { return biblionetTypeId; }
    public void setBiblionetTypeId(String biblionetTypeId) { this.biblionetTypeId = biblionetTypeId; }

    @Override
    public String toString() { return name; }
}

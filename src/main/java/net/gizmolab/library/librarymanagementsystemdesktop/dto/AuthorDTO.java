package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for Strapi Author (Συγγραφείς) content type.
 */
public class AuthorDTO {
    private Long id;              // Strapi ID (was authorId)
    private String name;          // Display name (e.g., "Νίκη Λοϊζίδη")
    private String firstname;
    private String lastname;
    private String biblionetPersonId;
    private int bookCount;

    public AuthorDTO() {}

    public AuthorDTO(Long id, String name, String firstname, String lastname) {
        this.id = id;
        this.name = name;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getBiblionetPersonId() { return biblionetPersonId; }
    public void setBiblionetPersonId(String biblionetPersonId) { this.biblionetPersonId = biblionetPersonId; }
    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    /** Display name for UI */
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        return ((firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "")).trim();
    }

    @Override
    public String toString() { return getDisplayName(); }
}

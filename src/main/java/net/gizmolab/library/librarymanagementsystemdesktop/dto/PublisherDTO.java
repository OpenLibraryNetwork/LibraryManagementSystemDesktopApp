package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for Strapi Publisher (Εκδότες) content type.
 */
public class PublisherDTO {
    private Long id;              // Strapi ID (was publisherId)
    private String name;
    private String biblionetCompanyId;
    private String address;
    private String phone;
    private String email;
    private String website;
    private int bookCount;

    public PublisherDTO() {}

    public PublisherDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBiblionetCompanyId() { return biblionetCompanyId; }
    public void setBiblionetCompanyId(String biblionetCompanyId) { this.biblionetCompanyId = biblionetCompanyId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public int getBookCount() { return bookCount; }
    public void setBookCount(int bookCount) { this.bookCount = bookCount; }

    @Override
    public String toString() { return getDisplayName(); }

    private String alternativeName;
    private String qualifier;
    private boolean reviewed;

    public String getAlternativeName() { return alternativeName; }
    public void setAlternativeName(String alternativeName) { this.alternativeName = alternativeName; }
    public String getQualifier() { return qualifier; }
    public void setQualifier(String qualifier) { this.qualifier = qualifier; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    /** "Name (qualifier)" when a qualifier exists. */
    public String getDisplayName() {
        String base = name != null ? name : "";
        return (qualifier == null || qualifier.trim().isEmpty()) ? base : base + " (" + qualifier.trim() + ")";
    }

    public boolean isFromBiblionet() { return biblionetCompanyId != null; }
}

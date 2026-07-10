package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import java.util.List;

/**
 * Unified DTO for all publication types (Βιβλίο, Μπροσούρα, Περιοδικό).
 * Maps to Strapi Book (Έντυπα) content type.
 */
public class PublicationDTO {
    private Long id;                  // Strapi ID
    private String title;
    private String type;              // "Βιβλίο", "Μπροσούρα", "Περιοδικό"
    private String isbn;              // only for Βιβλίο
    private String subtitle;
    private Integer yearPublished;
    private String description;
    private String summary;
    private Integer pages;
    private String language;
    private String coverImageUrl;
    private String binding;
    private String edition;
    private String category;

    // Periodical-specific
    private Integer issueNumber;
    private String publicationMonthYear;
    private Long magazineId;
    private String magazineTitle;

    // Relations (flattened for display)
    private String authorNames;       // Comma-separated
    private List<AuthorDTO> authors;
    private PublisherDTO publisher;

    // Copy stats (aggregated from Strapi)
    private int totalCopies;
    private int availableCopies;

    public PublicationDTO() {}

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public Integer getYearPublished() { return yearPublished; }
    public void setYearPublished(Integer yearPublished) { this.yearPublished = yearPublished; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getBinding() { return binding; }
    public void setBinding(String binding) { this.binding = binding; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getIssueNumber() { return issueNumber; }
    public void setIssueNumber(Integer issueNumber) { this.issueNumber = issueNumber; }
    public String getPublicationMonthYear() { return publicationMonthYear; }
    public void setPublicationMonthYear(String publicationMonthYear) { this.publicationMonthYear = publicationMonthYear; }
    public Long getMagazineId() { return magazineId; }
    public void setMagazineId(Long magazineId) { this.magazineId = magazineId; }
    public String getMagazineTitle() { return magazineTitle; }
    public void setMagazineTitle(String magazineTitle) { this.magazineTitle = magazineTitle; }
    public String getAuthorNames() { return authorNames; }
    public void setAuthorNames(String authorNames) { this.authorNames = authorNames; }
    public List<AuthorDTO> getAuthors() { return authors; }
    public void setAuthors(List<AuthorDTO> authors) { this.authors = authors; }
    public PublisherDTO getPublisher() { return publisher; }
    public void setPublisher(PublisherDTO publisher) { this.publisher = publisher; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
}

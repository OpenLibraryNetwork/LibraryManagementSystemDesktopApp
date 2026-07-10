package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * DTO for Strapi Magazine (Περιοδικά) content type.
 */
public class MagazineDTO {
    private Long id;              // Strapi ID
    private String title;
    private String issn;
    private PublisherDTO publisher;
    private int issuesCount;

    public MagazineDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public PublisherDTO getPublisher() { return publisher; }
    public void setPublisher(PublisherDTO publisher) { this.publisher = publisher; }
    public int getIssuesCount() { return issuesCount; }
    public void setIssuesCount(int issuesCount) { this.issuesCount = issuesCount; }

    @Override
    public String toString() { return title; }
}

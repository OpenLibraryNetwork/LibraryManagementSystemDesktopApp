package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.*;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts between Strapi JSON responses and DTOs.
 *
 * Strapi v4 response format:
 * - Single: { "data": { "id": N, "attributes": { ... } } }
 * - List:   { "data": [ { "id": N, "attributes": { ... } }, ... ] }
 * - Relations: { "data": { "id": N, "attributes": { ... } } } nested
 */
public class DTOConverter {

    // ═══════════════════════════════════════════════════════
    // Publication (Έντυπα) — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    /**
     * Parse a Strapi Book response into PublicationDTO.
     * Accepts either the wrapper { "data": { "id", "attributes" } }
     * or the inner { "id", "attributes" } directly.
     */
    public static PublicationDTO publicationFromJson(JsonNode node) {
        if (node == null) return null;

        // Handle wrapper: { "data": { ... } }
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        PublicationDTO dto = new PublicationDTO();
        dto.setId(node.path("id").asLong());

        JsonNode attr = node.has("attributes") ? node.get("attributes") : node;

        dto.setTitle(textOrNull(attr, "title"));
        dto.setType(textOrNull(attr, "type"));
        dto.setIsbn(textOrNull(attr, "isbn"));
        dto.setSubtitle(textOrNull(attr, "subtitle"));
        dto.setYearPublished(intOrNull(attr, "yearPublished"));
        dto.setDescription(textOrNull(attr, "description"));
        dto.setSummary(textOrNull(attr, "summary"));
        dto.setPages(intOrNull(attr, "pages"));
        dto.setLanguage(textOrNull(attr, "language"));
        dto.setCoverImageUrl(textOrNull(attr, "coverImageUrl"));
        dto.setBinding(textOrNull(attr, "binding"));
        dto.setEdition(textOrNull(attr, "edition"));
        dto.setCategory(textOrNull(attr, "category"));
        dto.setIssueNumber(intOrNull(attr, "issueNumber"));
        dto.setPublicationMonthYear(textOrNull(attr, "publicationMonthYear"));

        // Parse authors relation
        JsonNode authorsNode = attr.path("authors").path("data");
        if (authorsNode.isArray()) {
            List<AuthorDTO> authors = new ArrayList<>();
            for (JsonNode authorNode : authorsNode) {
                authors.add(authorFromJson(authorNode));
            }
            dto.setAuthors(authors);
            dto.setAuthorNames(authors.stream()
                    .map(AuthorDTO::getDisplayName)
                    .collect(Collectors.joining(", ")));
        }

        // Parse publisher relation
        JsonNode publisherNode = attr.path("publisher").path("data");
        if (!publisherNode.isNull() && !publisherNode.isMissingNode()) {
            dto.setPublisher(publisherFromJson(publisherNode));
        }

        // Parse copies relation (count only)
        JsonNode copiesNode = attr.path("copies").path("data");
        if (copiesNode.isArray()) {
            dto.setTotalCopies(copiesNode.size());
            int available = 0;
            for (JsonNode copyNode : copiesNode) {
                JsonNode copyAttr = copyNode.has("attributes") ? copyNode.get("attributes") : copyNode;
                if (copyAttr.path("isAvailable").asBoolean(false)) {
                    available++;
                }
            }
            dto.setAvailableCopies(available);
        }

        // Parse magazine relation (for periodicals)
        JsonNode magNode = attr.path("magazine").path("data");
        if (!magNode.isNull() && !magNode.isMissingNode()) {
            dto.setMagazineId(magNode.path("id").asLong());
            JsonNode magAttr = magNode.has("attributes") ? magNode.get("attributes") : magNode;
            dto.setMagazineTitle(textOrNull(magAttr, "title"));
        }

        return dto;
    }

    /**
     * Parse a Strapi list response into List<PublicationDTO>.
     */
    public static List<PublicationDTO> publicationsFromJson(JsonNode response) {
        List<PublicationDTO> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                PublicationDTO dto = publicationFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Copy (Αντίτυπα) — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static CopyDTO copyFromJson(JsonNode node) {
        if (node == null) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        CopyDTO dto = new CopyDTO();
        dto.setId(node.path("id").asLong());

        JsonNode attr = node.has("attributes") ? node.get("attributes") : node;
        dto.setCopyNumber(attr.path("copyNumber").asInt(1));
        dto.setAvailable(attr.path("isAvailable").asBoolean(true));
        dto.setCondition(textOrNull(attr, "condition"));

        // Parse publication relation
        JsonNode pubNode = attr.path("publication").path("data");
        if (!pubNode.isNull() && !pubNode.isMissingNode()) {
            dto.setPublicationId(pubNode.path("id").asLong());
            JsonNode pubAttr = pubNode.has("attributes") ? pubNode.get("attributes") : pubNode;
            dto.setPublicationTitle(textOrNull(pubAttr, "title"));
        }

        // Parse library relation
        JsonNode libNode = attr.path("library").path("data");
        if (!libNode.isNull() && !libNode.isMissingNode()) {
            dto.setLibraryId(libNode.path("id").asLong());
            JsonNode libAttr = libNode.has("attributes") ? libNode.get("attributes") : libNode;
            dto.setLibraryName(textOrNull(libAttr, "name"));
        }

        return dto;
    }

    public static List<CopyDTO> copiesFromJson(JsonNode response) {
        List<CopyDTO> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                CopyDTO dto = copyFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Author — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static AuthorDTO authorFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        AuthorDTO dto = new AuthorDTO();
        dto.setId(node.path("id").asLong());

        JsonNode attr = node.has("attributes") ? node.get("attributes") : node;
        dto.setName(textOrNull(attr, "name"));
        dto.setFirstname(textOrNull(attr, "firstname"));
        dto.setLastname(textOrNull(attr, "lastname"));
        dto.setBiblionetPersonId(textOrNull(attr, "biblionetPersonId"));

        JsonNode booksNode = attr.path("books").path("data");
        if (booksNode.isArray()) {
            dto.setBookCount(booksNode.size());
        }

        return dto;
    }

    public static List<AuthorDTO> authorsFromJson(JsonNode response) {
        List<AuthorDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                AuthorDTO dto = authorFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Publisher — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static PublisherDTO publisherFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        PublisherDTO dto = new PublisherDTO();
        dto.setId(node.path("id").asLong());

        JsonNode attr = node.has("attributes") ? node.get("attributes") : node;
        dto.setName(textOrNull(attr, "name"));
        dto.setBiblionetCompanyId(textOrNull(attr, "biblionetCompanyId"));
        dto.setAddress(textOrNull(attr, "address"));
        dto.setPhone(textOrNull(attr, "phone"));
        dto.setEmail(textOrNull(attr, "email"));
        dto.setWebsite(textOrNull(attr, "website"));

        JsonNode booksNode = attr.path("books").path("data");
        if (booksNode.isArray()) {
            dto.setBookCount(booksNode.size());
        }

        return dto;
    }

    public static List<PublisherDTO> publishersFromJson(JsonNode response) {
        List<PublisherDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                PublisherDTO dto = publisherFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Magazine — from Strapi JSON
    // ═══════════════════════════════════════════════════════

    public static MagazineDTO magazineFromJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.has("data") && !node.get("data").isArray()) {
            node = node.get("data");
        }
        if (node == null || node.isNull()) return null;

        MagazineDTO dto = new MagazineDTO();
        dto.setId(node.path("id").asLong());

        JsonNode attr = node.has("attributes") ? node.get("attributes") : node;
        dto.setTitle(textOrNull(attr, "title"));
        dto.setIssn(textOrNull(attr, "issn"));

        JsonNode pubNode = attr.path("publisher").path("data");
        if (!pubNode.isNull() && !pubNode.isMissingNode()) {
            dto.setPublisher(publisherFromJson(pubNode));
        }

        JsonNode issuesNode = attr.path("issues").path("data");
        if (issuesNode.isArray()) {
            dto.setIssuesCount(issuesNode.size());
        }

        return dto;
    }

    public static List<MagazineDTO> magazinesFromJson(JsonNode response) {
        List<MagazineDTO> result = new ArrayList<>();
        if (response == null) return result;
        JsonNode dataArray = response.path("data");
        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                MagazineDTO dto = magazineFromJson(item);
                if (dto != null) result.add(dto);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    // Borrow — from local H2 entity
    // ═══════════════════════════════════════════════════════

    /**
     * Convert local Borrow entity to BorrowDTO.
     * No Strapi call needed — all display fields are cached in Borrow.
     */
    public static BorrowDTO convertBorrowToDTO(Borrow borrow) {
        if (borrow == null) return null;

        BorrowDTO dto = new BorrowDTO();
        dto.setId(borrow.getId());
        dto.setStrapiCopyId(borrow.getStrapiCopyId());
        dto.setStrapiPublicationId(borrow.getStrapiPublicationId());
        dto.setCopyNumber(borrow.getCopyNumber());

        // Cached publication info
        dto.setPublicationTitle(borrow.getPublicationTitle());
        dto.setPublicationType(borrow.getPublicationType());
        dto.setIsbn(borrow.getIsbn());
        dto.setAuthorName(borrow.getAuthorName());

        // User info (local JPA)
        if (borrow.getUser() != null) {
            dto.setUserId(borrow.getUser().getUserId());
            dto.setUserFirstName(borrow.getUser().getFirstname());
            dto.setUserLastName(borrow.getUser().getLastname());
        }

        // Dates
        dto.setBorrowDate(borrow.getBorrowDate());
        dto.setDueDate(borrow.getDueDate());
        dto.setReturnDate(borrow.getReturnDate());
        dto.setReturned(borrow.isReturned());

        // Calculated status
        dto.setOverdue(calculateOverdue(borrow));
        dto.setStatus(calculateStatus(borrow));

        return dto;
    }

    private static boolean calculateOverdue(Borrow borrow) {
        if (borrow.isReturned()) return false;
        if (borrow.getDueDate() == null) return false;
        return LocalDate.now().isAfter(borrow.getDueDate());
    }

    private static String calculateStatus(Borrow borrow) {
        if (borrow.isReturned()) return "Returned";
        if (calculateOverdue(borrow)) return "Overdue";
        return "Active";
    }

    // ═══════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        return value.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        return value.asInt();
    }
}

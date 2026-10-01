package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import java.util.List;

/**
 * Wraps a page of results from a Strapi v4 paginated API response.
 *
 * Strapi v4 pagination response format:
 * <pre>
 * {
 *   "data": [...],
 *   "meta": {
 *     "pagination": {
 *       "page": 1,
 *       "pageSize": 15,
 *       "pageCount": 10,
 *       "total": 150
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> The DTO type contained in the page
 */
public class StrapiPageResponse<T> {

    private final List<T> data;
    private final int page;
    private final int pageSize;
    private final int pageCount;
    private final int total;
    private String errorMessage; // set when the page could not be loaded

    public StrapiPageResponse(List<T> data, int page, int pageSize, int pageCount, int total) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.total = total;
    }

    /**
     * Creates an empty response (no results).
     */
    /**
     * An empty page for a failed load, carrying a Greek message for the user
     * (so a 403 or an unreachable server is not mistaken for "no items").
     */
    public static <T> StrapiPageResponse<T> failed(Throwable error) {
        StrapiPageResponse<T> page = empty();
        page.errorMessage = net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages.describe(error);
        return page;
    }

    /** Null when the page loaded normally. */
    public String getErrorMessage() {
        return errorMessage;
    }

    public static <T> StrapiPageResponse<T> empty() {
        return new StrapiPageResponse<>(List.of(), 1, 15, 1, 0);
    }

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "StrapiPageResponse{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", pageCount=" + pageCount +
                ", total=" + total +
                ", dataSize=" + (data != null ? data.size() : 0) +
                '}';
    }
}

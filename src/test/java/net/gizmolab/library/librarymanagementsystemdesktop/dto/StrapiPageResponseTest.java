package net.gizmolab.library.librarymanagementsystemdesktop.dto;

import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrapiPageResponseTest {

    @Test
    void failedPageCarriesAUserMessageInsteadOfLookingEmpty() { // review Important #1
        StrapiPageResponse<PersonDTO> page = StrapiPageResponse.failed(
                new RuntimeException(new StrapiApiClient.ForbiddenException("{}", "Forbidden")));
        assertTrue(page.getData().isEmpty());
        assertEquals(0, page.getTotal());
        assertEquals(UserMessages.FORBIDDEN, page.getErrorMessage());
    }

    @Test
    void emptyPageHasNoError() {
        assertNull(StrapiPageResponse.empty().getErrorMessage());
    }
}

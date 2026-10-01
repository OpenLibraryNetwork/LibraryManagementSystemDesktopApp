package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueOrderTest {

    private static PublicationDTO issue(String number, String period) {
        PublicationDTO p = new PublicationDTO();
        p.setIssueNumber(number);
        p.setPublicationMonthYear(period);
        return p;
    }

    @Test
    void numbersNaturallyThenIssuesWithoutNumberByPeriod() {
        List<PublicationDTO> issues = new ArrayList<>(List.of(
                issue(null, "Χειμώνας 2019"), issue("10", null), issue("9", null), issue("12-13", null),
                issue(null, "Άνοιξη 2020"), issue("τεύχ. 2", null), issue("1", null)));
        issues.sort(IssueOrder.NATURAL);
        assertEquals(List.of("1", "τεύχ. 2", "9", "10", "12-13", "Άνοιξη 2020", "Χειμώνας 2019"),
                issues.stream().map(i -> i.getIssueNumber() != null ? i.getIssueNumber() : i.getPublicationMonthYear()).toList());
    }
}

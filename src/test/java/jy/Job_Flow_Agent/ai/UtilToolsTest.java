package jy.Job_Flow_Agent.ai;

import jy.Job_Flow_Agent.AI.Tools.UtilTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UtilTools 단위 테스트")
class UtilToolsTest {

    private final UtilTools utilTools = new UtilTools();

    // ─────────────────────────────────────────────────
    //  UT-01: getTodayDate() - 오늘 날짜 반환
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("UT-01: getTodayDate() - 오늘 날짜 반환, LocalDate.now() 일치")
    void getTodayDate_returnsToday() {
        LocalDate before = LocalDate.now();
        LocalDate result = utilTools.getTodayDate();
        LocalDate after = LocalDate.now();

        assertThat(result).isNotNull();
        assertThat(result).isAfterOrEqualTo(before);
        assertThat(result).isBeforeOrEqualTo(after);
    }
}

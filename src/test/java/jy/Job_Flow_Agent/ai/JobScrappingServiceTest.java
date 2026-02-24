package jy.Job_Flow_Agent.ai;

import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.AI.Service.JobAnalyzer;
import jy.Job_Flow_Agent.AI.Service.JobScrappingService;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobScrappingService 단위 테스트")
class JobScrappingServiceTest {

    @Mock
    private JobAnalyzer jobAnalyzer;

    @InjectMocks
    private JobScrappingService jobScrappingService;

    private JobPostingInfo sampleJobInfo() {
        return new JobPostingInfo(
                "테스트컴퍼니",
                "백엔드 개발자 (Java/Spring)",
                List.of("서버 개발", "API 설계"),
                List.of("Java 3년 이상"),
                List.of("AWS 경험자 우대"),
                List.of("Java", "Spring Boot", "MySQL"),
                "2026-03-31",
                "회사 내규에 따름",
                "서울 강남구"
        );
    }

    // ─────────────────────────────────────────────────
    //  JS-02: URL null/빈 문자열 → GlobalException
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JS-02: URL이 null이면 GlobalException(NEED_URL_LINK, 400) 발생")
    void jobScrapping_nullUrl_throwsGlobalException() {
        assertThatThrownBy(() -> jobScrappingService.jobScrapping(null))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> {
                    GlobalException ge = (GlobalException) ex;
                    assertThat(ge.getErrorCode()).isEqualTo("NEED_URL_LINK");
                    assertThat(ge.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("JS-02: URL이 빈 문자열이면 GlobalException(NEED_URL_LINK) 발생")
    void jobScrapping_emptyUrl_throwsGlobalException() {
        assertThatThrownBy(() -> jobScrappingService.jobScrapping(""))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("NEED_URL_LINK"));
    }

    // ─────────────────────────────────────────────────
    //  JS-03: AI 분석 실패 → GlobalException 전파
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JS-03: Jsoup 연결 실패(네트워크 오류) → GlobalException(JOB_SCRAPPING_ERROR) 발생")
    void jobScrapping_invalidUrl_throwsGlobalException() {
        // given - 존재하지 않는 URL → Jsoup 내부에서 예외 발생
        String invalidUrl = "http://this.url.does.not.exist.invalid/job/123";

        // when & then
        assertThatThrownBy(() -> jobScrappingService.jobScrapping(invalidUrl))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("JOB_SCRAPPING_ERROR"));
    }
}

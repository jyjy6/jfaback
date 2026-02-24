package jy.Job_Flow_Agent.ai;

import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.AI.Service.JobScrappingService;
import jy.Job_Flow_Agent.AI.Tools.JobScrappingTools;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobScrappingTools 단위 테스트")
class JobScrappingToolsTest {

    @Mock
    private JobScrappingService jobScrappingService;

    @InjectMocks
    private JobScrappingTools jobScrappingTools;

    private JobPostingInfo sampleInfo() {
        return new JobPostingInfo(
                "테스트컴퍼니",
                "백엔드 개발자",
                List.of("서버 개발"),
                List.of("Java 3년 이상"),
                List.of("AWS 우대"),
                List.of("Java", "Spring Boot"),
                "2026-03-31",
                "협의",
                "서울"
        );
    }

    // ─────────────────────────────────────────────────
    //  JST-01: scrapeJobPosting() - 포맷된 문자열 반환
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JST-01: scrapeJobPosting() - 회사명, 기술스택 포함 포맷 문자열 반환")
    void scrapeJobPosting_returnsFormattedString() {
        // given
        given(jobScrappingService.jobScrapping("https://example.com/job/1")).willReturn(sampleInfo());

        // when
        String result = jobScrappingTools.scrapeJobPosting("https://example.com/job/1");

        // then
        assertThat(result).contains("테스트컴퍼니");
        assertThat(result).contains("백엔드 개발자");
        assertThat(result).contains("Java");
        assertThat(result).contains("채용 공고 분석 결과");
        then(jobScrappingService).should().jobScrapping("https://example.com/job/1");
    }

    @Test
    @DisplayName("JST-01: scrapeJobPosting() 서비스 예외 시 오류 메시지 문자열 반환 (예외 전파 안 됨)")
    void scrapeJobPosting_serviceThrows_returnsErrorString() {
        // given
        given(jobScrappingService.jobScrapping("https://bad.url"))
                .willThrow(new GlobalException("오류", "JOB_SCRAPPING_ERROR"));

        // when
        String result = jobScrappingTools.scrapeJobPosting("https://bad.url");

        // then - 예외를 잡아서 오류 메시지 문자열 반환
        assertThat(result).contains("오류가 발생했습니다");
    }

    // ─────────────────────────────────────────────────
    //  JST-02: returnJobInfo() - JobPostingInfo DTO 반환
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("JST-02: returnJobInfo() - JobPostingInfo DTO 그대로 반환")
    void returnJobInfo_returnsJobPostingInfoDto() {
        // given
        JobPostingInfo expected = sampleInfo();
        given(jobScrappingService.jobScrapping("https://example.com/job/2")).willReturn(expected);

        // when
        JobPostingInfo result = jobScrappingTools.returnJobInfo("https://example.com/job/2");

        // then
        assertThat(result.companyName()).isEqualTo("테스트컴퍼니");
        assertThat(result.techStack()).contains("Java", "Spring Boot");
    }
}

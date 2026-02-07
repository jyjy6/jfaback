package jy.Job_Flow_Agent.AI.Service;

import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScrappingService {

    private final JobAnalyzer jobAnalyzer;

    /**
     * URL에서 채용 공고를 스크래핑하고 구조화된 데이터로 변환합니다.
     */
    public JobPostingInfo jobScrapping(String URL){
        if(URL == null || URL.isEmpty()){
            throw new GlobalException("URL을 입력하세요", "NEED_URL_LINK", HttpStatus.BAD_REQUEST);
        }
        try{
            log.info("🌐 스크래핑 시작: {}", URL);
            
            // 1. Jsoup으로 HTML 가져오기 (User-Agent 설정 필수)
            Document doc = Jsoup.connect(URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000) // 타임아웃 10초로 증가
                    .get();

            String title = doc.title();
            String bodyText = doc.body().text(); // 태그 제거된 순수 텍스트

            log.info("✅ HTML 다운로드 완료 (제목: {}, 길이: {}자). AI 분석 시작...", title, bodyText.length());

            // 2. AI를 통해 텍스트 구조화 (Structured Extraction)
            // 제목 정보도 함께 넘겨주면 분석에 도움이 됨
            String contentToAnalyze = "제목: " + title + "\n\n본문:\n" + bodyText;
            
            JobPostingInfo info = jobAnalyzer.analyze(contentToAnalyze);
            
            log.info("✨ AI 분석 완료: {} (기술스택: {})", info.companyName(), info.techStack());

            return info;

        } catch (Exception e){
            log.error("❌ 크롤링 및 분석 실패: {}", e.getMessage(), e);
            throw new GlobalException("JOB_SCRAPPING_ERROR", "채용 공고 분석 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
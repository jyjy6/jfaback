package jy.Job_Flow_Agent.AI.Tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import jy.Job_Flow_Agent.AI.Service.JobScrappingService;
import jy.Job_Flow_Agent.AI.Service.JobScrappingService.jobScrappingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LangChain4j 툴: 채용 공고 웹 스크래핑 기능
 * 
 * AI 어시스턴트가 사용자가 제공한 채용 공고 URL을 스크래핑하여
 * 실시간으로 공고 내용을 분석할 수 있도록 하는 툴입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScrappingTools {
    
    private final JobScrappingService jobScrappingService;

    /**
     * 채용 공고 URL을 스크래핑하여 제목과 본문 내용을 추출합니다.
     * 
     * 사용 시나리오:
     * - 사용자가 "이 공고 분석해줘: https://..." 같은 요청을 할 때
     * - "이 링크의 채용 공고를 내 이력서와 비교해줘" 같은 요청이 들어올 때
     * - "이 공고에 어떤 자격 요건이 있는지 알려줘" 같은 요청이 들어올 때
     * 
     * @param url 스크래핑할 채용 공고 URL (잡코리아, 사람인 등)
     * @return 스크래핑된 채용 공고 내용 (제목과 본문)
     */
    @Tool("사용자가 제공한 채용 공고 URL을 스크래핑하여 내용을 가져옵니다. " +
          "사용자가 채용 공고 링크를 공유하거나, 특정 공고에 대한 분석을 요청할 때 이 도구를 사용하세요. " +
          "URL이 메시지에 포함되어 있으면 자동으로 스크래핑하여 분석합니다. " +
          "예: '이 공고 분석해줘:', '이 링크 봐줄래?', '이 채용 공고 어때?' 등")
    public String scrapeJobPosting(@P("스크래핑할 채용 공고 URL") String url) {
        
        log.info("🌐 Job Scraping Tool 호출 - URL: '{}'", url);
        
        try {
            // JobScrappingService를 통해 스크래핑
            jobScrappingDTO result = jobScrappingService.jobScrapping(url);
            
            // AI가 읽기 쉬운 형태로 변환
            StringBuilder formattedResult = new StringBuilder();
            formattedResult.append("【채용 공고 정보】\n\n");
            formattedResult.append("제목: ").append(result.getTitle()).append("\n\n");
            formattedResult.append("본문 내용:\n");
            formattedResult.append(result.getBodyText());
            formattedResult.append("\n\n【출처】\n");
            formattedResult.append("URL: ").append(url);
            
            log.info("✅ 스크래핑 성공 - 제목: {}, 본문 길이: {}자", 
                    result.getTitle(), result.getBodyText().length());
            
            return formattedResult.toString();
            
        } catch (Exception e) {
            log.error("❌ 채용 공고 스크래핑 실패 - URL: {}", url, e);
            return "채용 공고 스크래핑에 실패했습니다. URL을 확인해주세요: " + e.getMessage();
        }
    }
}
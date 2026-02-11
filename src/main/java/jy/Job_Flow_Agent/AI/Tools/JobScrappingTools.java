package jy.Job_Flow_Agent.AI.Tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.AI.Service.JobScrappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j 툴: 채용 공고 웹 스크래핑 및 구조화 기능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScrappingTools {
    
    private final JobScrappingService jobScrappingService;

    /**
     * 채용 공고 URL을 스크래핑하고 구조화된 정보로 변환합니다.
     */
    @Tool("""
          사용자가 제공한 채용 공고 URL을 스크래핑하여 핵심 정보를 분석합니다. 
          사용자가 채용 공고 링크를 공유하거나 분석을 요청할 때 사용하세요. 
          반환된 정보는 이미 구조화되어 있으므로, 이를 바탕으로 바로 답변하면 됩니다.
          """)
    public String scrapeJobPosting(@P("스크래핑할 채용 공고 URL") String url) {
        
        log.info("🌐 Job Scraping Tool 호출 - URL: '{}'", url);
        
        try {
            // 1. 서비스 호출 (스크래핑 + AI 구조화)
            JobPostingInfo info = jobScrappingService.jobScrapping(url);
            
            // 2. AI(Chat Model)에게 전달할 깔끔한 포맷 생성
            StringBuilder sb = new StringBuilder();
            sb.append("【채용 공고 분석 결과】\n");
            sb.append("--------------------------------------------------\n");
            sb.append("■ 회사명: ").append(info.companyName()).append("\n");
            sb.append("■ 공고명: ").append(info.title()).append("\n");
            sb.append("■ 위치: ").append(info.location()).append("\n");
            sb.append("■ 마감일: ").append(info.deadline()).append("\n");
            sb.append("■ 연봉: ").append(info.salary()).append("\n");
            
            sb.append("\n[기술 스택]\n");
            if (info.techStack() != null) {
                info.techStack().forEach(stack -> sb.append("- ").append(stack).append("\n"));
            }

            sb.append("\n[주요 업무]\n");
            if (info.majorTasks() != null) {
                info.majorTasks().forEach(task -> sb.append("- ").append(task).append("\n"));
            }

            sb.append("\n[자격 요건]\n");
            if (info.requirements() != null) {
                info.requirements().forEach(req -> sb.append("- ").append(req).append("\n"));
            }

            sb.append("\n[우대 사항]\n");
            if (info.preferredSkills() != null) {
                info.preferredSkills().forEach(pref -> sb.append("- ").append(pref).append("\n"));
            }
            sb.append("--------------------------------------------------\n");
            sb.append("출처: ").append(url);
            
            log.info("✅ 분석 완료 및 반환 - 회사: {}, 기술스택 수: {}", 
                    info.companyName(), (info.techStack() != null ? info.techStack().size() : 0));
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("❌ 채용 공고 처리 실패 - URL: {}", url, e);
            return "채용 공고를 분석하는 중 오류가 발생했습니다. (원인: " + e.getMessage() + ")";
        }
    }

    /**
     * 채용 공고 URL을 스크래핑하고 구조화된 정보로 변환합니다.
     */
    @Tool("""
          사용자가 제공한 채용 공고 URL을 스크래핑하여 핵심 정보의 DTO를 반환합니다.
          """)
    public JobPostingInfo returnJobInfo(@P("스크래핑할 채용 공고 URL") String url) {
        return jobScrappingService.jobScrapping(url);
    }


}

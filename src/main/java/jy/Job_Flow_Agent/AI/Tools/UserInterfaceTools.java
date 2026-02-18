package jy.Job_Flow_Agent.AI.Tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.MemoryId;
import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.AI.Event.UIEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * UI 렌더링을 위한 전용 도구
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInterfaceTools {

    private final ApplicationEventPublisher eventPublisher;

    @Tool("사용자에게 채용 공고 정보를 시각적인 카드 형태로 보여줍니다. 채용 공고를 분석했거나 정보를 공유할 때 사용하세요.")
    public String displayJobPostingCard(
            @MemoryId String username,
            @P("렌더링할 채용 공고 정보 DTO") JobPostingInfo jobInfo
    ) {
        log.info("🖥️ UI Tool 호출 - 사용자: {}, 회사: {}", username, jobInfo.companyName());
        
        // Spring ApplicationEvent를 사용하여 비동기적으로 처리하거나 
        // 컨트롤러에서 구독 중인 Sink로 전달하도록 설계
        eventPublisher.publishEvent(new UIEventWrapper(username, "JOB_POSTING", jobInfo));
        
        return "화면에 채용 공고 카드가 성공적으로 표시되었습니다.";
    }

    /**
     * 이벤트를 감싸는 래퍼 (사용자 ID 포함)
     */
    public record UIEventWrapper(String username, String type, Object data) {}
}

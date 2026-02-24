package jy.Job_Flow_Agent.ai;

import jy.Job_Flow_Agent.AI.DTO.JobPostingInfo;
import jy.Job_Flow_Agent.AI.Tools.UserInterfaceTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserInterfaceTools 단위 테스트")
class UserInterfaceToolsTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserInterfaceTools userInterfaceTools;

    // ─────────────────────────────────────────────────
    //  UIT-01: displayJobPostingCard() - 이벤트 발행 확인
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("UIT-01: displayJobPostingCard() - publishEvent 호출 확인, UIEventWrapper 타입·데이터 검증")
    void displayJobPostingCard_publishesUIEventWrapper() {
        // given
        JobPostingInfo jobInfo = new JobPostingInfo(
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

        // when
        String result = userInterfaceTools.displayJobPostingCard("testuser", jobInfo);

        // then - 반환 문자열 확인
        assertThat(result).contains("성공적으로 표시");

        // ApplicationEventPublisher.publishEvent() 호출 확인
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should().publishEvent(captor.capture());

        Object event = captor.getValue();
        assertThat(event).isInstanceOf(UserInterfaceTools.UIEventWrapper.class);

        UserInterfaceTools.UIEventWrapper wrapper = (UserInterfaceTools.UIEventWrapper) event;
        assertThat(wrapper.username()).isEqualTo("testuser");
        assertThat(wrapper.type()).isEqualTo("JOB_POSTING");
        assertThat(wrapper.data()).isEqualTo(jobInfo);
    }
}

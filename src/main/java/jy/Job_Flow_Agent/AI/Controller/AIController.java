package jy.Job_Flow_Agent.AI.Controller;



import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    @Qualifier("assistant")
    private final Assistant assistant;



    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String message = request.get("message");
        log.info("Chat message from {}: {}", customUserDetails.getUsername(), message);

        return assistant.chat(customUserDetails.getUsername(), message);
    }
}

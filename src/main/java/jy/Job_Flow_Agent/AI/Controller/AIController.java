package jy.Job_Flow_Agent.AI.Controller;



import jy.Job_Flow_Agent.AI.AssistantModels.StreamingAssistant;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final StreamingAssistant streamingAssistant;



    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody Map<String, String> request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String message = request.get("message");
        String username = customUserDetails.getUsername();
        log.info("Streaming Chat message from {}: {}", username, message);

        return Flux.create(sink -> {
            streamingAssistant.chat(username, message)
                    .onPartialResponse(sink::next) // 기존 onNext 대신 사용
                    .onCompleteResponse(response -> sink.complete()) // 기존 onComplete 대신 사용
                    .onError(sink::error)
                    .start();
        });
    }


}


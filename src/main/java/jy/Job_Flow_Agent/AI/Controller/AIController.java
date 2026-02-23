package jy.Job_Flow_Agent.AI.Controller;



import jy.Job_Flow_Agent.AI.AssistantModels.StreamingAssistant;
import jy.Job_Flow_Agent.AI.Event.UIEvent;
import jy.Job_Flow_Agent.AI.Event.UIEventService;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final StreamingAssistant streamingAssistant;
    private final UIEventService uiEventService;



    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chat(
            @RequestBody Map<String, String> request, 
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        String message = request.get("message");
        String username = (customUserDetails != null) ? customUserDetails.getUsername() : "anonymous";
        log.info("Streaming Chat message from {}: {}", username, message);

        // 초기 버퍼링 방지용 공백
        Flux<ServerSentEvent<Object>> initialBurst = Flux.just(
                ServerSentEvent.builder().comment(" ".repeat(1024)).build()
        );

        // 종료 신호를 위한 Sink
        Sinks.Empty<Void> completionSink = Sinks.empty();

        // 1. UI 이벤트 스트림
        Flux<ServerSentEvent<Object>> uiStream = uiEventService.subscribe(username)
                .map(event -> ServerSentEvent.builder()
                        .event("ui_render")
                        .data(event)
                        .build());

        // 2. 텍스트 스트리밍
        Flux<ServerSentEvent<Object>> textStream = Flux.create(sink -> {
            try {
                streamingAssistant.chat(username, message)
                        .onPartialResponse(token -> {
                            sink.next(ServerSentEvent.builder()
                                    .event("message")
                                    .data(token)
                                    .build());
                        })
                        .onCompleteResponse(responseObj -> {
                            sink.complete();
                            completionSink.tryEmitEmpty(); // 답변 종료 시 신호 발생
                        })
                        .onError(e -> {
                            log.error("AI Assistant Error: ", e);
                            sink.next(ServerSentEvent.builder()
                                    .event("error")
                                    .data(Map.of("message", "AI 응답 생성 중 오류가 발생했습니다."))
                                    .build());
                            sink.complete();
                            completionSink.tryEmitEmpty();
                        })
                        .start();
            } catch (Exception e) {
                sink.error(e);
                completionSink.tryEmitEmpty();
            }
        });

        return Flux.concat(initialBurst, Flux.merge(textStream, uiStream))
                .takeUntilOther(completionSink.asMono()) // 종료 신호 수신 시 전체 스트림 종료
                .onErrorResume(e -> {
                    log.error("SSE Stream Error: ", e);
                    return Flux.just(ServerSentEvent.builder()
                            .event("error")
                            .data(Map.of("message", "시스템 오류가 발생했습니다."))
                            .build());
                })
                .doOnCancel(() -> uiEventService.unsubscribe(username))
                .doOnTerminate(() -> {
                    uiEventService.unsubscribe(username);
                    log.info("🏁 Streaming Finished for {}", username);
                });
    }


}


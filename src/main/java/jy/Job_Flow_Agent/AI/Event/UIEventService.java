package jy.Job_Flow_Agent.AI.Event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jy.Job_Flow_Agent.AI.Tools.UserInterfaceTools.UIEventWrapper;

@Slf4j
@Service
public class UIEventService {

    private final Map<String, Sinks.Many<UIEvent>> userSinks = new ConcurrentHashMap<>();

    public Flux<UIEvent> subscribe(String username) {
        Sinks.Many<UIEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.put(username, sink);
        return sink.asFlux().doFinally(signalType -> userSinks.remove(username));
    }

    public void unsubscribe(String username) {
        log.info("🔌 UI Event 구독 해제 - 사용자: {}", username);
        userSinks.remove(username);
    }

    @EventListener
    public void handleUIEvent(UIEventWrapper eventWrapper) {
        String username = eventWrapper.username();
        Sinks.Many<UIEvent> sink = userSinks.get(username);
        if (sink != null) {
            log.info("📢 UI Event 전달 - 사용자: {}, 타입: {}", username, eventWrapper.type());
            sink.tryEmitNext(new UIEvent(eventWrapper.type(), eventWrapper.data()));
        } else {
            log.warn("⚠️ 구독 중인 Sink가 없습니다. - 사용자: {}", username);
        }

    }
}

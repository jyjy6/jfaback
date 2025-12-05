package jy.Job_Flow_Agent.Redis;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final String prefix = "chat:memory:";

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = prefix + memoryId;
        
        try {
            List<String> jsonMessages = redisTemplate.opsForList().range(key, 0, -1);
            
            if (jsonMessages == null || jsonMessages.isEmpty()) {
                return new ArrayList<>();
            }
            
            return jsonMessages.stream()
                    .map(json -> {
                        try {
                            return ChatMessageDeserializer.messageFromJson(json);
                        } catch (Exception e) {
                            log.error("Failed to deserialize chat message: {}", json, e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get messages for memoryId: {}", memoryId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = prefix + memoryId;
        
        try {
            // 기존 메시지 삭제
            redisTemplate.delete(key);
            
            if (messages != null && !messages.isEmpty()) {
                // ChatMessage를 JSON 문자열로 변환
                List<String> jsonMessages = messages.stream()
                        .map(msg -> {
                            try {
                                return ChatMessageSerializer.messageToJson(msg);
                            } catch (Exception e) {
                                log.error("Failed to serialize chat message", e);
                                return null;
                            }
                        })
                        .filter(json -> json != null)
                        .collect(Collectors.toList());
                
                if (!jsonMessages.isEmpty()) {
                    redisTemplate.opsForList().rightPushAll(key, jsonMessages);
                    // TTL 설정
                    redisTemplate.expire(key, Duration.ofHours(1));
                }
            }
        } catch (Exception e) {
            log.error("Failed to update messages for memoryId: {}", memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = prefix + memoryId;
        redisTemplate.delete(key);
    }
}


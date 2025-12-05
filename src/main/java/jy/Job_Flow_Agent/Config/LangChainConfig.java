package jy.Job_Flow_Agent.Config;


import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;

import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.AI.AssistantModels.RagAssistant;
import jy.Job_Flow_Agent.AI.AssistantModels.StreamingAssistant;
import jy.Job_Flow_Agent.AI.Tools.MemberSearchTools;
import jy.Job_Flow_Agent.AI.Tools.UtilTools;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Redis.RedisChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class LangChainConfig {
    @Value("${google.gemini.api.key}")
    String apiKey;

    private final StringRedisTemplate stringRedisTemplate;



    /**
     * Phase 2.2: 도구(Tools) 및 함수 호출용 Assistant
     * - Calculator.java 도구를 AI가 사용할 수 있도록 설정
     * - AI가 계산이 필요하다고 판단하면 Calculator의 메서드를 자동으로 호출
     */
    @Bean("assistantWithTools")
    public Assistant assistantWithTools(MemberSearchTools memberSearchTools,
                                        UtilTools utilTools) {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR","GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RedisChatMemoryStore store = new RedisChatMemoryStore(stringRedisTemplate);
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.5)
                .build();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(memberSearchTools, utilTools)
                .chatMemoryProvider(userId -> MessageWindowChatMemory.builder()
                        .id(userId)
                        .maxMessages(20)
                        .chatMemoryStore(store)
                        .build())
                .build();
    }
    /**
     * Phase 2.2: 도구(Tools) 및 함수 호출용 Assistant
     * - Calculator.java 도구를 AI가 사용할 수 있도록 설정
     * - AI가 계산이 필요하다고 판단하면 Calculator의 메서드를 자동으로 호출
     */

    @Bean("assistantWithToolsForAdmin")
    public Assistant assistantWithToolsForAdmin(MemberSearchTools memberSearchTools,
                                                UtilTools utilTools) {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR","GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RedisChatMemoryStore store = new RedisChatMemoryStore(stringRedisTemplate);
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.5)
                .build();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools( memberSearchTools, utilTools)
                .chatMemoryProvider(userId -> MessageWindowChatMemory.builder()
                        .id(userId)
                        .maxMessages(20)
                        .chatMemoryStore(store)
                        .build())
                .build();
    }



    /**
     * - 스트리밍 응답 Assistant
     * - GoogleAiGeminiStreamingChatModel
     * - TokenStream을 통해 실시간 토큰 생성
     */
    @Bean
    public StreamingAssistant streamingAssistant() {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR","GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RedisChatMemoryStore store = new RedisChatMemoryStore(stringRedisTemplate);
        GoogleAiGeminiStreamingChatModel streamingModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();

        return AiServices.builder(StreamingAssistant.class)
                .streamingChatLanguageModel(streamingModel)
                .chatMemoryProvider(userId -> MessageWindowChatMemory.builder()
                        .id(userId)  // 사용자/세션 ID
                        .maxMessages(20)  // 최근 20개 메시지만 유지
                        .chatMemoryStore(store)
                        .build())
                .build();
    }


    // ==================== Phase 2.1: RAG (Retrieval Augmented Generation) ====================
    /**
     * Phase 2.1-A: Embedding Model Bean
     *
     * EmbeddingModel: 텍스트를 벡터(숫자 배열)로 변환하는 모델
     * 작동 원리:
     * - "퇴직금 계산" → [0.123, -0.456, 0.789, ...] (384개 숫자)
     * - "퇴직금 산정" → [0.125, -0.450, 0.791, ...] (유사한 벡터)
     * - "날씨 정보"   → [0.891, 0.234, -0.567, ...] (다른 벡터)
     *
     * 모델:
     * - OpenAI text-embedding-ada-002 (유료, 1536차원, 더 정확)
     * *****현재 모델***** Google PaLM Embeddings (유료, 768차원)
     * - Cohere Embeddings (유료, 1024차원)
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR","GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String modelName = "text-embedding-004";
        log.info("🧠 Embedding Model 초기화 - Google AI ({})", modelName);

        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey) // API 키 설정
                .modelName(modelName)
                .build();
    }
    /**
     * Phase 2.1-C: RAG Assistant Bean
     *
     * RagAssistant: 문서 기반 질의응답을 수행하는 AI 어시스턴트
     *
     * 특징:
     * - 검색된 문서만을 기반으로 답변 생성
     * - 환각(Hallucination) 최소화
     * - 답변의 출처 추적 가능
     *
     * 작동 방식:
     * 1. RagService에서 관련 문서 검색
     * 2. 검색된 문서를 @SystemMessage의 {{information}}에 주입
     * 3. 사용자 질문을 @UserMessage의 {{question}}에 주입
     * 4. AI가 문서 기반으로 답변 생성
     *
     * @return RAG 전용 AI 어시스턴트
     */
    @Bean
    public RagAssistant ragAssistant() {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR","GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("🤖 RAG Assistant 초기화 - Gemini 2.5 pro");

        // RAG에는 일반 Chat Model 사용 (Streaming 불필요)
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.4)  // RAG는 정확성이 중요하므로 낮은 temperature 사용
                .build();

        return AiServices.create(RagAssistant.class, model);
    }






}

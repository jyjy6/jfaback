package jy.Job_Flow_Agent.Config;


import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.AI.AssistantModels.StreamingAssistant;
import jy.Job_Flow_Agent.AI.Tools.JobScrappingTools;
import jy.Job_Flow_Agent.AI.Tools.MemberSearchTools;
import jy.Job_Flow_Agent.AI.Tools.RagTools;
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

    @Value("${pinecone.api.key}")
    private String pineconeApiKey;

    @Value("${pinecone.index.name:jfa}")
    private String pineconeIndexName;

    @Value("${pinecone.namespace:default}")
    private String pineconeNamespace;

    @Value("${pinecone.project.id:}")
    private String pineconeProjectId;

    @Value("${pinecone.environment:}")
    private String pineconeEnvironment;


    /**
     * 통합 AI Assistant
     * - Tools: 회원 정보 조회, 유틸리티, RAG 문서 검색 (사용자별 필터링)
     * - ChatMemory: 대화 문맥 유지 (Redis)
     */
    @Bean("assistant")
    public Assistant assistant(MemberSearchTools memberSearchTools,
                               UtilTools utilTools,
                               RagTools ragTools,
                               JobScrappingTools jobScrappingTools) {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR", "GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RedisChatMemoryStore store = new RedisChatMemoryStore(stringRedisTemplate);
        

        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.4)
                .build();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(memberSearchTools, utilTools, ragTools, jobScrappingTools) // 도구 등록 (RagTools 추가)
                .chatMemoryProvider(username -> MessageWindowChatMemory.builder()
                        .id(username)
                        .maxMessages(20)
                        .chatMemoryStore(store)
                        .build())
                .build();
    }


    /**
     * 스트리밍 응답 Assistant
     * (RAG나 Tool 없이 빠른 대화가 필요할 때 사용)
     */
    @Bean
    public StreamingAssistant streamingAssistant() {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR", "GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        RedisChatMemoryStore store = new RedisChatMemoryStore(stringRedisTemplate);
        GoogleAiGeminiStreamingChatModel streamingModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash") // 빠른 응답용 Flash 모델
                .temperature(0.7)
                .build();

        return AiServices.builder(StreamingAssistant.class)
                .streamingChatLanguageModel(streamingModel)
                .chatMemoryProvider(username -> MessageWindowChatMemory.builder()
                        .id(username)
                        .maxMessages(20)
                        .chatMemoryStore(store)
                        .build())
                .build();
    }


    // ==================== RAG Components ====================

    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null) {
            throw new GlobalException("GEMINI_API_KEY_ERROR", "GEMINI_API_KEY not set in environment variables", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Gemini text-embedding-004 모델은 기본 768차원입니다. 
        // Pinecone 인덱스를 생성할 때 반드시 Dimensions를 768로 설정해야 합니다. gemini-embedding-001이건 3072차원
        // (1024차원은 지원하지 않으므로 인덱스 재생성이 필요합니다.)
        String modelName = "text-embedding-004";
        log.info("🧠 Embedding Model 초기화 - Google AI ({})", modelName);

        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing Pinecone Embedding Store - Index: {}, Namespace: {}, Environment: {}",
                pineconeIndexName, pineconeNamespace, pineconeEnvironment);

        return PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .index(pineconeIndexName)
                .nameSpace(pineconeNamespace) // 1.0.0-beta1 호환성 확인
                .build();
    }

    /**
     * RAG 검색기 (ContentRetriever)
     * - 사용자의 질문을 임베딩하여 Vector Store에서 유사한 문서를 찾아오는 역할
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)   // 상위 3개 문서 검색
                .minScore(0.6)   // 유사도 0.6 이상인 것만 (너무 관련 없는 것 제외)
                .build();
    }
    
    // RagAssistant 인터페이스는 더 이상 Bean으로 등록하지 않지만, 
    // 기존 코드 호환성을 위해 남겨두거나 삭제할 수 있음. 
    // 여기서는 통합 Assistant를 사용하므로 RagAssistant Bean 정의는 제거함.
}

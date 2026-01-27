package jy.Job_Flow_Agent.AI.Tools;


import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG(Retrieval-Augmented Generation) 도구
 * 
 * AI가 사용자의 업로드된 문서를 검색하고 관련 정보를 제공받을 수 있는 도구입니다.
 * AI는 이 도구를 통해 사용자별 문서 데이터베이스에 접근할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTools {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 사용자가 업로드한 문서에서 관련 정보를 검색합니다.
     * 
     * 사용 시나리오:
     * - 사용자가 "내가 업로드한 문서에 XX가 있어?" 같은 질문을 할 때
     * - "내 이력서에서 경력 알려줘" 같은 요청이 들어올 때
     * - "저장된 채용공고 중에 XX 회사 정보 찾아줘" 같은 요청이 들어올 때
     * 
     * @param query 검색할 질문 또는 키워드
     * @param userId 현재 사용자 ID (해당 유저의 문서만 검색)
     * @return 검색된 관련 문서 내용과 출처 정보
     */
    @Tool("사용자가 업로드한 문서(이력서, 채용공고, 메모 등)에서 관련 정보를 검색합니다. " +
          "사용자가 자신의 문서, 과거 업로드한 정보, 저장된 내용에 대해 질문할 때 이 도구를 사용하세요. " +
          "예: '내 이력서에...', '업로드한 문서에서...', '저장된 공고 중...' 등")
    public String searchUserDocuments(
            @P("검색할 질문 또는 키워드") String query,
            @P("현재 사용자 ID") String userId) {
        
        log.info("🔍 RAG Tool 호출 - Query: '{}', User: '{}'", query, userId);
        
        try {
            // 1. 질문을 임베딩으로 변환
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 사용자별 문서 검색 (Metadata Filter 적용)
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)      // 상위 5개 문서 검색
                    .minScore(0.6)      // 유사도 60% 이상만 반환
                    .filter(MetadataFilterBuilder.metadataKey("userId").isEqualTo(userId))
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> relevantMatches = searchResult.matches();
            
            log.info("✅ 검색 결과: {}개 문서 조각 발견", relevantMatches.size());

            // 3. 검색 결과가 없으면 안내 메시지 반환
            if (relevantMatches.isEmpty()) {
                return "검색 결과가 없습니다. 사용자가 업로드한 문서에서 관련 정보를 찾을 수 없습니다. " +
                       "사용자에게 문서를 먼저 업로드하도록 안내해주세요.";
            }

            // 4. 검색된 내용을 하나의 문자열로 결합
            String documentContent = relevantMatches.stream()
                    .map(EmbeddingMatch::embedded)
                    .map(TextSegment::text)
                    .collect(Collectors.joining("\n\n---\n\n"));

            // 5. 출처 정보 추출 (중복 제거)
            List<String> sources = relevantMatches.stream()
                    .map(EmbeddingMatch::embedded)
                    .map(segment -> segment.metadata().getString("document_name"))
                    .distinct()
                    .collect(Collectors.toList());

            // 6. AI가 사용할 수 있는 형태로 반환
            StringBuilder result = new StringBuilder();
            result.append("【검색된 문서 내용】\n\n");
            result.append(documentContent);
            result.append("\n\n【출처】\n");
            sources.forEach(source -> result.append("- ").append(source).append("\n"));
            
            log.info("📄 출처 문서: {}", sources);
            
            return result.toString();

        } catch (Exception e) {
            log.error("❌ RAG 검색 중 오류 발생", e);
            return "문서 검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}

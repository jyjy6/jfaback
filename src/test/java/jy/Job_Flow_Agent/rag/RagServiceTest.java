package jy.Job_Flow_Agent.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.AI.RAG.DTO.RagDTO;
import jy.Job_Flow_Agent.AI.RAG.Entity.DocumentMetadata;
import jy.Job_Flow_Agent.AI.RAG.Repository.DocumentRepository;
import jy.Job_Flow_Agent.AI.RAG.Service.RagService;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Entity.Member;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagService 단위 테스트")
class RagServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ContentRetriever contentRetriever;

    @Mock
    private Assistant assistant;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private RagService ragService;

    private CustomUserDetails testUser() {
        Member member = Member.builder()
                .id(1L)
                .username("testuser")
                .password("pw")
                .email("testuser@test.com")
                .displayName("테스터")
                .roles(Set.of("ROLE_USER"))
                .build();
        return new CustomUserDetails(member);
    }

    private DocumentMetadata savedDoc(Long id, String username, String name) {
        return DocumentMetadata.builder()
                .id(id)
                .username(username)
                .documentName(name)
                .documentType("txt")
                .fileSize(100L)
                .status(DocumentMetadata.DocumentStatus.PROCESSING)
                .build();
    }

    // ─────────────────────────────────────────────────
    //  RS-01: 텍스트 파일 업로드 임베딩 처리 정상
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-01: txt 파일 ingestDocument() - COMPLETED 상태로 2회 save 호출 확인")
    void ingestDocument_txtFile_savesCompleted() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Java Spring Boot 개발자입니다. 경력 3년입니다.".getBytes()
        );
        DocumentMetadata firstSaved = savedDoc(10L, "testuser", "resume.txt");
        given(documentRepository.save(any(DocumentMetadata.class)))
                .willReturn(firstSaved)           // 1st save: PROCESSING
                .willReturn(firstSaved);           // 2nd save: COMPLETED

        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f, 0.2f});
        given(embeddingModel.embedAll(anyList())).willReturn(Response.from(List.of(dummyEmbedding)));

        // when
        RagDTO.IngestResponse response = ragService.ingestDocument(file, testUser());

        // then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getDocumentName()).isEqualTo("resume.txt");

        // save 2회: PROCESSING 저장 + COMPLETED 업데이트
        then(documentRepository).should(times(2)).save(any(DocumentMetadata.class));
        then(embeddingStore).should().addAll(anyList(), anyList());
    }

    // ─────────────────────────────────────────────────
    //  RS-02: 텍스트 직접 입력 임베딩 정상
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-02: ingestText() - COMPLETED 상태로 2회 save 호출 확인")
    void ingestText_success() {
        // given
        RagDTO.IngestTextRequest request = new RagDTO.IngestTextRequest(
                "Java 개발자로서 5년 경력이 있습니다.", "자기소개", "테스트 설명"
        );
        DocumentMetadata saved = savedDoc(20L, "testuser", "자기소개");
        given(documentRepository.save(any(DocumentMetadata.class))).willReturn(saved);

        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f, 0.2f});
        given(embeddingModel.embedAll(anyList())).willReturn(Response.from(List.of(dummyEmbedding)));

        // when
        RagDTO.IngestResponse response = ragService.ingestText(request, testUser());

        // then
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        then(documentRepository).should(times(2)).save(any(DocumentMetadata.class));
        then(embeddingStore).should().addAll(anyList(), anyList());
    }

    // ─────────────────────────────────────────────────
    //  RS-03: 임베딩 처리 중 예외 발생 → GlobalException 전파
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-03: embedAll() 예외 발생 시 GlobalException(INGEST_TEXT_ERROR) 발생")
    void ingestText_embeddingFails_throwsGlobalException() {
        // given
        RagDTO.IngestTextRequest request = new RagDTO.IngestTextRequest(
                "테스트 텍스트", "테스트문서", null
        );
        DocumentMetadata saved = savedDoc(30L, "testuser", "테스트문서");
        given(documentRepository.save(any(DocumentMetadata.class))).willReturn(saved);
        given(embeddingModel.embedAll(anyList())).willThrow(new RuntimeException("Pinecone connection failed"));

        // when & then
        assertThatThrownBy(() -> ragService.ingestText(request, testUser()))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("INGEST_TEXT_ERROR"));
    }

    // ─────────────────────────────────────────────────
    //  RS-04: 문서 검색 정상
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-04: search() - embeddingStore.search() 호출 확인, SearchResponse 반환")
    void search_returnsSearchResponse() {
        // given
        RagDTO.SearchRequest request = new RagDTO.SearchRequest("Java 개발자", 5, 0.5);
        Embedding queryEmbedding = Embedding.from(new float[]{0.1f});
        given(embeddingModel.embed(anyString())).willReturn(Response.from(queryEmbedding));

        EmbeddingSearchResult<TextSegment> emptyResult = new EmbeddingSearchResult<>(Collections.emptyList());
        given(embeddingStore.search(any())).willReturn(emptyResult);

        // when
        RagDTO.SearchResponse response = ragService.search(request, testUser());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQuery()).isEqualTo("Java 개발자");
        then(embeddingStore).should().search(any());
    }

    // ─────────────────────────────────────────────────
    //  RS-05: 문서 삭제 정상 - DB삭제 + Pinecone 벡터 삭제
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-05: deleteDocument() - embeddingStore.removeAll() + documentRepository.delete() 호출 확인")
    void deleteDocument_success_callsStoreRemoveAndRepoDelete() {
        // given
        DocumentMetadata doc = savedDoc(50L, "testuser", "my-doc.txt");
        doc.setStatus(DocumentMetadata.DocumentStatus.COMPLETED);
        given(documentRepository.findById(50L)).willReturn(Optional.of(doc));

        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f});
        given(embeddingModel.embed("delete")).willReturn(Response.from(dummyEmbedding));

        // 삭제 대상 벡터 1개 검색 결과 반환
        TextSegment segment = TextSegment.from("some text");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "vector-id-1", dummyEmbedding, segment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));
        given(embeddingStore.search(any())).willReturn(searchResult);

        // when
        RagDTO.DeleteResponse response = ragService.deleteDocument(50L, testUser());

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getDocumentId()).isEqualTo(50L);
        then(embeddingStore).should().removeAll(List.of("vector-id-1"));
        then(documentRepository).should().delete(doc);
    }

    // ─────────────────────────────────────────────────
    //  RS-06: 타인 문서 삭제 시도 → GlobalException
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-06: 소유자가 다른 문서 삭제 시도 → GlobalException 발생, delete() 미호출")
    void deleteDocument_notOwner_throwsGlobalException() {
        // given - 문서 소유자: "anotheruser", 요청자: "testuser"
        DocumentMetadata doc = savedDoc(60L, "anotheruser", "their-doc.txt");
        given(documentRepository.findById(60L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> ragService.deleteDocument(60L, testUser()))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getErrorCode()).isEqualTo("UNAUTHORIZED_DOCUMENT_DELETE"));

        then(documentRepository).should(never()).delete(any());
        then(embeddingStore).should(never()).removeAll(anyList());
    }

    // ─────────────────────────────────────────────────
    //  RS-07: 사용자별 문서 목록 조회
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("RS-07: getDocumentsByUser() - findByUsernameOrderByCreatedAtDesc 호출 확인")
    void getDocumentsByUser_callsRepositoryWithUsername() {
        // given
        DocumentMetadata doc1 = savedDoc(1L, "testuser", "doc1.txt");
        doc1.setStatus(DocumentMetadata.DocumentStatus.COMPLETED);
        doc1.setChunkCount(5);
        given(documentRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .willReturn(List.of(doc1));

        // when
        RagDTO.DocumentListResponse response = ragService.getDocumentsByUser(testUser());

        // then
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getDocuments().get(0).getDocumentName()).isEqualTo("doc1.txt");
        then(documentRepository).should().findByUsernameOrderByCreatedAtDesc("testuser");
    }
}

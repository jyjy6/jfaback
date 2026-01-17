package jy.Job_Flow_Agent.AI.RAG.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jy.Job_Flow_Agent.AI.RAG.DTO.RagDTO;
import jy.Job_Flow_Agent.AI.RAG.Entity.DocumentMetadata;
import jy.Job_Flow_Agent.AI.AssistantModels.Assistant;
import jy.Job_Flow_Agent.AI.RAG.Repository.DocumentRepository;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 시스템의 핵심 비즈니스 로직 서비스
 * 
 * 주요 기능:
 * 1. 문서 업로드 및 임베딩 처리
 * 2. 벡터 검색
 * 3. RAG 기반 질의응답
 * 4. 문서 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ContentRetriever contentRetriever;
    private final Assistant assistant; // 통합 Assistant 사용
    private final DocumentRepository documentRepository;

    /**
     * 파일 업로드 및 임베딩 처리
     * 
     * 프로세스:
     * 1. 파일 메타데이터를 MySQL에 저장
     * 2. 문서를 파싱하여 텍스트 추출
     * 3. 텍스트를 작은 청크로 분할
     * 4. 각 청크를 임베딩으로 변환
     * 5. 임베딩을 Pinecone에 저장
     */
    @Transactional
    public RagDTO.IngestResponse ingestDocument(MultipartFile file, CustomUserDetails user) {
        log.info("Starting document ingestion: {} by user: {}", file.getOriginalFilename(), user.getUsername());

        try {
            // 1. 문서 메타데이터 저장
            DocumentMetadata documentEntity = createDocumentEntity(file, user.getUsername());
            documentEntity = documentRepository.save(documentEntity);
            final Long documentId = documentEntity.getId();
            final String documentName = documentEntity.getDocumentName();
            final String userId = user.getUsername();
            log.info("Document metadata saved with ID: {}", documentId);

            // 2. 문서 파싱
            DocumentParser parser = getDocumentParser(file.getOriginalFilename());
            Document document = parser.parse(file.getInputStream());
            log.info("Document parsed successfully");

            // 3. 문서 분할 (Chunking)
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    500,  // maxSegmentSizeInChars: 각 청크의 최대 크기
                    100   // maxOverlapSizeInChars: 청크 간 겹치는 부분 크기
            );
            List<TextSegment> segments = splitter.split(document);
            log.info("Document split into {} segments", segments.size());

            // 4. 메타데이터 추가 (문서명, 사용자 ID를 각 세그먼트에 태그)
            List<TextSegment> segmentsWithMetadata = segments.stream()
                    .map(segment -> TextSegment.from(
                            segment.text(),
                            segment.metadata()
                                    .put("document_id", documentId)
                                    .put("document_name", documentName)
                                    .put("userId", userId) // 사용자 식별을 위한 메타데이터 추가
                    ))
                    .toList();

            // 5. 임베딩 생성 및 저장
            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMetadata).content();
            embeddingStore.addAll(embeddings, segmentsWithMetadata);
            log.info("Embeddings stored in Pinecone");

            // 6. 문서 상태 업데이트
            documentEntity.setChunkCount(segments.size());
            documentEntity.setStatus(DocumentMetadata.DocumentStatus.COMPLETED);
            documentRepository.save(documentEntity);

            return RagDTO.IngestResponse.builder()
                    .documentId(documentEntity.getId())
                    .documentName(documentEntity.getDocumentName())
                    .chunkCount(segments.size())
                    .status("COMPLETED")
                    .message("문서가 성공적으로 처리되었습니다.")
                    .uploadedAt(documentEntity.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error during document ingestion", e);
            throw new GlobalException("INGEST_DOCUMENT_ERROR", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 텍스트 직접 입력하여 임베딩
     */
    @Transactional
    public RagDTO.IngestResponse ingestText(RagDTO.IngestTextRequest request, CustomUserDetails user) {
        log.info("Starting text ingestion: {} by user: {}", request.getDocumentName(), user.getUsername());

        try {
            // 1. 문서 메타데이터 저장
            DocumentMetadata documentEntity = DocumentMetadata.builder()
                    .documentName(request.getDocumentName())
                    .documentType("TEXT")
                    .fileSize((long) request.getText().length())
                    .description(request.getDescription())
                    .status(DocumentMetadata.DocumentStatus.PROCESSING)
                    .userId(user.getUsername())
                    .build();
            documentEntity = documentRepository.save(documentEntity);
            final Long documentId = documentEntity.getId();
            final String documentName = documentEntity.getDocumentName();
            final String userId = user.getUsername();

            // 2. 텍스트를 Document로 변환
            Document document = Document.from(request.getText());

            // 3. 문서 분할
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
            List<TextSegment> segments = splitter.split(document);
            log.info("Text split into {} segments", segments.size());

            // 4. 메타데이터 추가
            List<TextSegment> segmentsWithMetadata = segments.stream()
                    .map(segment -> TextSegment.from(
                            segment.text(),
                            segment.metadata()
                                    .put("document_id", documentId)
                                    .put("document_name", documentName)
                                    .put("userId", userId)
                    ))
                    .toList();

            // 5. 임베딩 생성 및 저장
            List<Embedding> embeddings = embeddingModel.embedAll(segmentsWithMetadata).content();
            embeddingStore.addAll(embeddings, segmentsWithMetadata);

            // 6. 문서 상태 업데이트
            documentEntity.setChunkCount(segments.size());
            documentEntity.setStatus(DocumentMetadata.DocumentStatus.COMPLETED);
            documentRepository.save(documentEntity);

            return RagDTO.IngestResponse.builder()
                    .documentId(documentEntity.getId())
                    .documentName(documentEntity.getDocumentName())
                    .chunkCount(segments.size())
                    .status("COMPLETED")
                    .message("텍스트가 성공적으로 처리되었습니다.")
                    .uploadedAt(documentEntity.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error during text ingestion", e);
            throw new GlobalException("INGEST_TEXT_ERROR", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * RAG 기반 질의응답
     * 
     * 프로세스:
     * 1. 사용자 질문을 임베딩으로 변환
     * 2. Pinecone에서 유사한 문서 검색 (사용자 ID 필터링)
     * 3. 검색된 문서와 질문을 함께 AI에 전달
     * 4. AI가 문서 기반 답변 생성
     */
    public RagDTO.AskResponse ask(RagDTO.AskRequest request, CustomUserDetails user) {
        log.info("Processing question: {} for user: {}", request.getQuestion(), user.getUsername());

        try {
            // 1. 관련 문서 검색 (Metadata Filter 적용)
            Embedding queryEmbedding = embeddingModel.embed(request.getQuestion()).content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5) // maxResults (기본값 설정)
                    .minScore(0.6) // minScore (유사도 임계값)
                    .filter(MetadataFilterBuilder.metadataKey("userId").isEqualTo(user.getUsername()))
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> relevantMatches = searchResult.matches();
            
            log.info("Found {} relevant content pieces", relevantMatches.size());

            // 2. 검색된 내용을 하나의 문자열로 결합
            String information = relevantMatches.stream()
                    .map(EmbeddingMatch::embedded)
                    .map(TextSegment::text)
                    .collect(Collectors.joining("\n\n---\n\n"));

            // 3. 출처 정보 추출
            List<String> sources = relevantMatches.stream()
                    .map(EmbeddingMatch::embedded)
                    .map(segment -> segment.metadata().getString("document_name"))
                    .distinct()
                    .collect(Collectors.toList());

            // 4. Assistant를 통해 답변 생성
            String answer = assistant.answer(request.getQuestion(), information);
            log.info("Answer generated successfully");

            return RagDTO.AskResponse.builder()
                    .question(request.getQuestion())
                    .answer(answer)
                    .sources(sources)
                    .sourceCount(sources.size())
                    .answeredAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error during question answering", e);
            throw new GlobalException("ASK_RESPONSE_ERROR", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 문서 검색 (답변 생성 없이 관련 문서만 검색)
     */
    public RagDTO.SearchResponse search(RagDTO.SearchRequest request, CustomUserDetails user) {
        log.info("Searching documents for query: {} user: {}", request.getQuery(), user.getUsername());

        try {
            Embedding queryEmbedding = embeddingModel.embed(request.getQuery()).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(10) // maxResults
                    .minScore(0.5) // minScore
                    .filter(MetadataFilterBuilder.metadataKey("userId").isEqualTo(user.getUsername()))
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> relevantMatches = searchResult.matches();

            List<RagDTO.SearchResult> results = relevantMatches.stream()
                    .map(match -> {
                        TextSegment segment = match.embedded();
                        return RagDTO.SearchResult.builder()
                                .content(segment.text())
                                .score(match.score())
                                .source(segment.metadata().getString("document_name"))
                                .build();
                    })
                    .collect(Collectors.toList());

            return RagDTO.SearchResponse.builder()
                    .query(request.getQuery())
                    .results(results)
                    .totalResults(results.size())
                    .build();

        } catch (Exception e) {
            log.error("Error during document search", e);
            throw new GlobalException("SEARCH_DOCUMENT_ERROR", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 전체 문서 목록 조회
     */
    public RagDTO.DocumentListResponse getAllDocuments() {
        List<DocumentMetadata> documents = documentRepository.findAll();
        Long totalChunks = documentRepository.getTotalChunkCount();

        List<RagDTO.DocumentInfo> documentInfos = documents.stream()
                .map(this::toDocumentInfo)
                .collect(Collectors.toList());

        return RagDTO.DocumentListResponse.builder()
                .documents(documentInfos)
                .totalCount(documentInfos.size())
                .totalChunks(totalChunks)
                .build();
    }

    /**
     * 사용자별 문서 목록 조회
     */
    public RagDTO.DocumentListResponse getDocumentsByUser(CustomUserDetails user) {
        String userId = user.getUsername();
        List<DocumentMetadata> documents = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        Long totalChunks = documents.stream()
                .filter(doc -> doc.getStatus() == DocumentMetadata.DocumentStatus.COMPLETED)
                .mapToLong(doc -> doc.getChunkCount() != null ? doc.getChunkCount() : 0L)
                .sum();

        List<RagDTO.DocumentInfo> documentInfos = documents.stream()
                .map(this::toDocumentInfo)
                .collect(Collectors.toList());

        return RagDTO.DocumentListResponse.builder()
                .documents(documentInfos)
                .totalCount(documentInfos.size())
                .totalChunks(totalChunks)
                .build();
    }

    /**
     * 특정 문서 조회
     */
    public RagDTO.DocumentInfo getDocument(Long documentId) {
        DocumentMetadata document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GlobalException("DOCUMENT_NOT_FOUND","Document not found with id: " + documentId));
        return toDocumentInfo(document);
    }

    /**
     * 문서 삭제 (메타데이터만 삭제, Pinecone 벡터는 수동 관리 필요)
     * 
     * 주의: Pinecone에서 벡터를 삭제하려면 별도의 삭제 로직이 필요합니다.
     */
    @Transactional
    public RagDTO.DeleteResponse deleteDocument(Long documentId, CustomUserDetails customUserDetails) {
        DocumentMetadata document = documentRepository.findById(documentId)
                .orElseThrow(() -> new GlobalException("DOCUMENT_NOT_FOUND","Document not found with id: " + documentId));

        // 권한 체크: 문서 업로드한 유저가 아니면 예외 발생
        if(!document.getUserId().equals(customUserDetails.getUsername())){
            throw new GlobalException("유저_권한없음","파일을 업로드 한 유저만 삭제할 수 있습니다.",HttpStatus.UNAUTHORIZED);
        }

        String documentName = document.getDocumentName();
        
        // Pinecone에서 벡터 삭제 (document_id 메타데이터 기반)
        // 1단계: 먼저 해당 문서의 모든 벡터를 검색해서 ID를 수집
        try {
            log.info("Searching vectors to delete for document_id: {}", documentId);
            
            // 임의의 임베딩으로 검색 (결과는 필터로 제한됨)
            Embedding dummyEmbedding = embeddingModel.embed("delete").content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(dummyEmbedding)
                    .maxResults(1000) // 최대한 많이 가져오기
                    .minScore(0.0) // 모든 결과 포함
                    .filter(MetadataFilterBuilder.metadataKey("document_id").isEqualTo(documentId))
                    .build();
            
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            // 2단계: 검색된 벡터들의 ID를 수집
            List<String> vectorIdsToDelete = searchResult.matches().stream()
                    .map(match -> match.embeddingId())
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            
            log.info("Found {} vectors to delete for document: {}", vectorIdsToDelete.size(), documentName);
            
            // 3단계: ID 리스트로 삭제
            if (!vectorIdsToDelete.isEmpty()) {
                embeddingStore.removeAll(vectorIdsToDelete);
                log.info("Successfully deleted {} vectors from Pinecone", vectorIdsToDelete.size());
            } else {
                log.warn("No vectors found to delete for document_id: {}", documentId);
            }
            
        } catch (Exception e) {
            log.error("Error deleting vectors from Pinecone for document_id: {}", documentId, e);
            // Pinecone 삭제 실패해도 DB는 삭제 (벡터는 수동으로 정리 필요)
            log.warn("Continuing with database deletion despite Pinecone error");
        }
        
        // DB에서 메타데이터 삭제
        documentRepository.delete(document);
        log.info("Document metadata deleted from database: {}", documentName);

        return RagDTO.DeleteResponse.builder()
                .documentId(documentId)
                .documentName(documentName)
                .message("문서 메타데이터와 Pinecone 벡터가 성공적으로 삭제되었습니다.")
                .success(true)
                .build();
    }

    // ========== Private Helper Methods ==========

    private DocumentMetadata createDocumentEntity(MultipartFile file, String userId) {
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);

        return DocumentMetadata.builder()
                .documentName(fileName)
                .documentType(fileType)
                .fileSize(file.getSize())
                .status(DocumentMetadata.DocumentStatus.PROCESSING)
                .userId(userId)
                .build();
    }

    private DocumentParser getDocumentParser(String fileName) {
        String extension = getFileExtension(fileName);
        
        // TXT 파일은 TextDocumentParser, 그 외는 Apache Tika
        if ("txt".equalsIgnoreCase(extension)) {
            return new TextDocumentParser();
        } else {
            return new ApacheTikaDocumentParser();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "unknown";
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "unknown";
    }

    private RagDTO.DocumentInfo toDocumentInfo(DocumentMetadata document) {
        return RagDTO.DocumentInfo.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .documentType(document.getDocumentType())
                .fileSize(document.getFileSize())
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}


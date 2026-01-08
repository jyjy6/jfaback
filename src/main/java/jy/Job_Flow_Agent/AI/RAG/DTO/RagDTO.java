package jy.Job_Flow_Agent.AI.RAG.DTO;


import jy.Job_Flow_Agent.AI.RAG.Entity.DocumentMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 시스템 관련 DTO 모음
 */
public class RagDTO {

    /**
     * 문서 업로드 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestResponse {
        private Long documentId;
        private String documentName;
        private Integer chunkCount;
        private String status;
        private String message;
        private LocalDateTime uploadedAt;
    }

    /**
     * 질의응답 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AskRequest {
        private String question;
    }

    /**
     * 질의응답 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AskResponse {
        private String question;
        private String answer;
        private List<String> sources;  // 참조된 문서 출처
        private Integer sourceCount;
        private LocalDateTime answeredAt;
    }

    /**
     * 문서 검색 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String query;
        private Integer maxResults = 5;
        private Double minScore = 0.6;
    }

    /**
     * 문서 검색 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse {
        private String query;
        private List<SearchResult> results;
        private Integer totalResults;
    }

    /**
     * 검색 결과 개별 항목
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String content;
        private Double score;
        private String source;
    }

    /**
     * 문서 정보 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private Long id;
        private String documentName;
        private String documentType;
        private Long fileSize;
        private Integer chunkCount;
        private DocumentMetadata.DocumentStatus status;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 문서 목록 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentListResponse {
        private List<DocumentInfo> documents;
        private Integer totalCount;
        private Long totalChunks;
    }

    /**
     * 문서 삭제 응답 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteResponse {
        private Long documentId;
        private String documentName;
        private String message;
        private boolean success;
    }

    /**
     * 텍스트 직접 입력 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngestTextRequest {
        private String text;
        private String documentName;
        private String description;
    }
}

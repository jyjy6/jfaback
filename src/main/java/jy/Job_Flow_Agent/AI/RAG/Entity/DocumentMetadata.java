package jy.Job_Flow_Agent.AI.RAG.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 문서 메타데이터 엔티티
 *
 * Pinecone에 저장되는 벡터와 연결되는 문서의 메타정보를 MySQL에 저장
 * - Pinecone: 벡터 데이터 (임베딩) + 텍스트 내용
 * - MySQL: 문서 메타데이터 (파일명, 업로드 시간, 크기, 상태 등)
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_name", columnList = "document_name"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 문서 소유자 ID (사용자 식별용)
     */
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * 문서 이름 (파일명)
     */
    @Column(name = "document_name", nullable = false, length = 500)
    private String documentName;

    /**
     * 문서 타입 (PDF, TXT, DOCX 등)
     */
    @Column(name = "document_type", length = 50)
    private String documentType;

    /**
     * 원본 파일 크기 (bytes)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 파일 저장 경로 (로컬 또는 클라우드 스토리지 경로)
     */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    /**
     * 문서가 분할된 청크(Segment) 개수
     * 하나의 문서가 여러 벡터로 분할되어 Pinecone에 저장됨
     */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /**
     * 문서 처리 상태
     * PENDING: 업로드 대기
     * PROCESSING: 임베딩 처리 중
     * COMPLETED: 처리 완료
     * FAILED: 처리 실패
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    /**
     * 문서 설명 또는 요약
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 에러 메시지 (처리 실패 시)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 생성 일시 (자동 설정)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시 (자동 갱신)
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 문서 상태 Enum
     */
    public enum DocumentStatus {
        PENDING,    // 업로드 대기
        PROCESSING, // 처리 중
        COMPLETED,  // 완료
        FAILED      // 실패
    }
}


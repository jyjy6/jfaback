package jy.Job_Flow_Agent.AI.RAG.Repository;

import jy.Job_Flow_Agent.AI.RAG.Entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 문서 메타데이터 Repository
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {

    /**
     * 문서명으로 문서 조회
     */
    Optional<DocumentMetadata> findByDocumentName(String documentName);

    /**
     * 문서명 존재 여부 확인
     */
    boolean existsByDocumentName(String documentName);

    /**
     * 상태별 문서 목록 조회
     */
    List<DocumentMetadata> findByStatus(DocumentMetadata.DocumentStatus status);

    /**
     * 특정 기간 이후 생성된 문서 목록 조회
     */
    @Query("SELECT d FROM DocumentMetadata d WHERE d.createdAt >= :startDate ORDER BY d.createdAt DESC")
    List<DocumentMetadata> findDocumentsCreatedAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * 문서 타입별 조회
     */
    List<DocumentMetadata> findByDocumentType(String documentType);

    /**
     * 최근 업로드된 문서 조회 (상위 N개)
     */
    @Query("SELECT d FROM DocumentMetadata d ORDER BY d.createdAt DESC")
    List<DocumentMetadata> findRecentDocuments();

    /**
     * 처리 완료된 문서만 조회
     */
    @Query("SELECT d FROM DocumentMetadata d WHERE d.status = 'COMPLETED' ORDER BY d.createdAt DESC")
    List<DocumentMetadata> findCompletedDocuments();

    /**
     * 총 청크 수 집계
     */
    @Query("SELECT COALESCE(SUM(d.chunkCount), 0) FROM DocumentMetadata d WHERE d.status = 'COMPLETED'")
    Long getTotalChunkCount();

    /**
     * 사용자별 문서 목록 조회
     */
    List<DocumentMetadata> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * 사용자별 완료된 문서 목록 조회
     */
    @Query("SELECT d FROM DocumentMetadata d WHERE d.username = :username AND d.status = 'COMPLETED' ORDER BY d.createdAt DESC")
    List<DocumentMetadata> findCompletedDocumentsByUsername(@Param("username") String username);
}

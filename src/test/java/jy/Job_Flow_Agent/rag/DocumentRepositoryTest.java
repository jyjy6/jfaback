package jy.Job_Flow_Agent.rag;

import jy.Job_Flow_Agent.AI.RAG.Entity.DocumentMetadata;
import jy.Job_Flow_Agent.AI.RAG.Repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("DocumentRepository 단위 테스트 (TestContainers MySQL)")
class DocumentRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
    }

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    private DocumentMetadata buildDoc(String username, String name, DocumentMetadata.DocumentStatus status, Integer chunkCount) {
        return DocumentMetadata.builder()
                .username(username)
                .documentName(name)
                .documentType("txt")
                .fileSize(1000L)
                .status(status)
                .chunkCount(chunkCount)
                .build();
    }

    // ─────────────────────────────────────────────────
    //  DR-01: findByUsernameOrderByCreatedAtDesc() - 최신순 정렬
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("DR-01: findByUsernameOrderByCreatedAtDesc() - 최신순 정렬 확인")
    void findByUsernameOrderByCreatedAtDesc_sortedByCreatedAt() throws InterruptedException {
        documentRepository.save(buildDoc("user1", "first.txt", DocumentMetadata.DocumentStatus.COMPLETED, 5));
        Thread.sleep(10); // createdAt 차이 보장
        documentRepository.save(buildDoc("user1", "second.txt", DocumentMetadata.DocumentStatus.COMPLETED, 3));

        List<DocumentMetadata> results = documentRepository.findByUsernameOrderByCreatedAtDesc("user1");

        assertThat(results).hasSize(2);
        // 최신 문서가 첫 번째
        assertThat(results.get(0).getDocumentName()).isEqualTo("second.txt");
        assertThat(results.get(1).getDocumentName()).isEqualTo("first.txt");
    }

    @Test
    @DisplayName("DR-01: findByUsernameOrderByCreatedAtDesc() - 다른 사용자 문서 미포함")
    void findByUsernameOrderByCreatedAtDesc_onlyCurrentUser() {
        documentRepository.save(buildDoc("user1", "mine.txt", DocumentMetadata.DocumentStatus.COMPLETED, 5));
        documentRepository.save(buildDoc("user2", "theirs.txt", DocumentMetadata.DocumentStatus.COMPLETED, 3));

        List<DocumentMetadata> results = documentRepository.findByUsernameOrderByCreatedAtDesc("user1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocumentName()).isEqualTo("mine.txt");
    }

    // ─────────────────────────────────────────────────
    //  DR-02: getTotalChunkCount() - COMPLETED 청크 합산
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("DR-02: getTotalChunkCount() - COMPLETED 문서 청크 합산 정확히 반환")
    void getTotalChunkCount_sumsCompletedChunks() {
        documentRepository.save(buildDoc("user1", "doc1.txt", DocumentMetadata.DocumentStatus.COMPLETED, 10));
        documentRepository.save(buildDoc("user1", "doc2.txt", DocumentMetadata.DocumentStatus.COMPLETED, 5));
        documentRepository.save(buildDoc("user1", "doc3.txt", DocumentMetadata.DocumentStatus.FAILED, 3));   // FAILED - 제외

        Long total = documentRepository.getTotalChunkCount();

        assertThat(total).isEqualTo(15L); // 10 + 5 (FAILED는 제외)
    }

    @Test
    @DisplayName("DR-02: getTotalChunkCount() - COMPLETED 없으면 0 반환")
    void getTotalChunkCount_noCompleted_returnsZero() {
        documentRepository.save(buildDoc("user1", "doc1.txt", DocumentMetadata.DocumentStatus.FAILED, 5));

        Long total = documentRepository.getTotalChunkCount();

        assertThat(total).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────────
    //  DR-03: findCompletedDocumentsByUsername() - COMPLETED 상태만
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("DR-03: findCompletedDocumentsByUsername() - COMPLETED 문서만 반환, FAILED 제외")
    void findCompletedDocumentsByUsername_onlyCompleted() {
        documentRepository.save(buildDoc("user1", "complete.txt", DocumentMetadata.DocumentStatus.COMPLETED, 5));
        documentRepository.save(buildDoc("user1", "failed.txt", DocumentMetadata.DocumentStatus.FAILED, 3));
        documentRepository.save(buildDoc("user1", "processing.txt", DocumentMetadata.DocumentStatus.PROCESSING, 0));

        List<DocumentMetadata> results = documentRepository.findCompletedDocumentsByUsername("user1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocumentName()).isEqualTo("complete.txt");
        assertThat(results.get(0).getStatus()).isEqualTo(DocumentMetadata.DocumentStatus.COMPLETED);
    }
}

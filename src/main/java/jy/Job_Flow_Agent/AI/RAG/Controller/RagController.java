package jy.Job_Flow_Agent.AI.RAG.Controller;


import jy.Job_Flow_Agent.AI.RAG.DTO.RagDTO;
import jy.Job_Flow_Agent.AI.RAG.Service.RagService;
import jy.Job_Flow_Agent.Auth.Util.AuthUtils;
import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import jy.Job_Flow_Agent.Member.Service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 시스템 REST API 컨트롤러
 * <p>
 * 제공 API:
 * 1. POST /api/rag/ingest - 파일 업로드 및 임베딩
 * 2. POST /api/rag/ingest/text - 텍스트 직접 입력
 * 3. POST /api/rag/ask - RAG 기반 질의응답
 * 4. POST /api/rag/search - 문서 검색
 * 5. GET /api/rag/documents - 문서 목록 조회
 * 6. GET /api/rag/documents/{id} - 특정 문서 조회
 * 7. DELETE /api/rag/documents/{id} - 문서 삭제
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final RagService ragService;

    /**
     * 파일 업로드 및 임베딩 처리
     */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RagDTO.IngestResponse> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("Received file upload request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new GlobalException("파일이 없습니다.", "EMPTY_FILE", HttpStatus.BAD_REQUEST);

        }

        RagDTO.IngestResponse response = ragService.ingestDocument(file, customUserDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 텍스트 직접 입력하여 임베딩
     */
    @PostMapping("/ingest/text")
    public ResponseEntity<RagDTO.IngestResponse> ingestText(
            @RequestBody RagDTO.IngestTextRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("Received text ingestion request: {}", request.getDocumentName());

        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new GlobalException("텍스트가 비어있습니다.", "EMPTY_QUESTION", HttpStatus.BAD_REQUEST);
        }

        RagDTO.IngestResponse response = ragService.ingestText(request, customUserDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RAG 기반 질의응답 (Deprecated: AIController.chat 사용 권장)
     */
//    @PostMapping("/ask")
//    public ResponseEntity<RagDTO.AskResponse> ask(
//            @RequestBody RagDTO.AskRequest request) {
//        log.info("Received question: {}", request.getQuestion());
//
//        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
//            throw new GlobalException("질문이 비어있습니다.");
//        }
//
//        RagDTO.AskResponse response = ragService.ask(request);
//        return ResponseEntity.ok(response);
//    }

    /**
     * 문서 검색 (답변 생성 없이 관련 문서만 검색)
     */
    @PostMapping("/search")
    public ResponseEntity<RagDTO.SearchResponse> search(
            @RequestBody RagDTO.SearchRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("Received search request: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new GlobalException("텍스트가 비어있습니다.", "EMPTY_QUESTION", HttpStatus.BAD_REQUEST);
        }

        RagDTO.SearchResponse response = ragService.search(request, customUserDetails);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 문서 목록 조회
     */
    @GetMapping("/documents")
    public ResponseEntity<RagDTO.DocumentListResponse> getAllDocuments() {
        log.info("Fetching all documents");
        RagDTO.DocumentListResponse response = ragService.getAllDocuments();
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 문서 목록 조회
     */
    @GetMapping("/documents/my")
    public ResponseEntity<RagDTO.DocumentListResponse> getMyDocuments(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("Fetching documents for user: {}", customUserDetails.getUsername());
        RagDTO.DocumentListResponse response = ragService.getDocumentsByUser(customUserDetails);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 문서 조회
     */
    @GetMapping("/documents/{id}")
    public ResponseEntity<RagDTO.DocumentInfo> getDocument(@PathVariable Long id) {
        log.info("Fetching document with id: {}", id);
        RagDTO.DocumentInfo documentInfo = ragService.getDocument(id);
        return ResponseEntity.ok(documentInfo);
    }

    /**
     * 문서 삭제
     */
    @DeleteMapping("/documents/delete/{id}")
    public ResponseEntity<RagDTO.DeleteResponse> deleteDocument(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                @PathVariable("id") Long documentId) {

        log.info("Deleting document with id: {}", documentId);
        RagDTO.DeleteResponse response = ragService.deleteDocument(documentId, customUserDetails);
        return ResponseEntity.ok(response);
    }

    /**
     * Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("RAG System is running");
    }
}

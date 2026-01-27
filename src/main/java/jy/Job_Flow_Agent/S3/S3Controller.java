package jy.Job_Flow_Agent.S3;


import jy.Job_Flow_Agent.GlobalErrorHandler.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RequestMapping("/api/v1/s3")
@RequiredArgsConstructor
@RestController
public class S3Controller {

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    private final S3Service s3Service;

    // Presigned URL 생성 (음악 파일 및 이미지)
    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponseDto> getPresignedUrl(
            @RequestParam String filename,
            @RequestParam String filetype
    ) {
        try {
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            String randomFilename = UUID.randomUUID().toString() + "_" + decodedFilename;

            // 파일 타입에 따른 폴더 구분
            String folder = filetype.startsWith("audio/") ? "music" : "docs";
            String fullPath = folder + "/" + randomFilename;

            String presignedUrl = s3Service.createPresignedUrl(fullPath);
            String usableUrl = "https://" + bucket + ".s3.amazonaws.com/" + fullPath;

            PresignedUrlResponseDto response = PresignedUrlResponseDto.builder()
                    .presignedUrl(presignedUrl)
                    .usableUrl(usableUrl)
                    .build();

            log.info("Presigned URL 생성 완료: {}", filename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage());
            throw new GlobalException("Presigned URL 생성에 실패했습니다.", "PRESIGNED_URL_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}

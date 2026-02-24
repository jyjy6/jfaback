package jy.Job_Flow_Agent.s3;

import jy.Job_Flow_Agent.S3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 단위 테스트")
class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
    }

    // ─────────────────────────────────────────────────
    //  S3-01: Presigned URL 생성 정상
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("S3-01: createPresignedUrl() - 유효한 URL 문자열 반환")
    void createPresignedUrl_success_returnsUrl() throws Exception {
        // given
        URL fakeUrl = new URL("https://test-bucket.s3.amazonaws.com/images/test.jpg?X-Amz-Signature=abc");
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(fakeUrl);

        // when
        String result = (String) org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(s3Service, "createPresignedUrl", "images/test.jpg");

        // then
        assertThat(result).isNotBlank();
        assertThat(result).contains("test-bucket");
    }

    // ─────────────────────────────────────────────────
    //  S3-02: S3 예외 발생 시 예외 전파
    // ─────────────────────────────────────────────────
    @Test
    @DisplayName("S3-02: s3Presigner 예외 발생 시 예외 전파 확인")
    void createPresignedUrl_s3Throws_propagatesException() {
        // given
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willThrow(new RuntimeException("S3 연결 실패"));

        // when & then
        assertThatThrownBy(() ->
                org.springframework.test.util.ReflectionTestUtils
                        .invokeMethod(s3Service, "createPresignedUrl", "images/test.jpg")
        ).hasCauseInstanceOf(RuntimeException.class)
         .hasMessageContaining("S3 연결 실패");
    }
}

package jy.Job_Flow_Agent.S3;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {


    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Presigner s3Presigner;

    String createPresignedUrl(String path) {
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket) //올릴 버킷명
                .key(path) //경로
                .build();
        var preSignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) //URL 유효기간
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(preSignRequest).url().toString();
        return presignedUrl;
    }

}

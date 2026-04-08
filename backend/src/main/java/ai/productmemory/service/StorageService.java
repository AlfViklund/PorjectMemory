package ai.productmemory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * StorageService — S3/MinIO abstraction layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void upload(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.debug("Uploaded {} bytes to s3://{}/{}", data.length, bucketName, key);
    }

    public byte[] download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    public String getPresignedUrl(String key) {
        // TODO: implement presigned URL for evidence download
        return "s3://" + bucketName + "/" + key;
    }
}

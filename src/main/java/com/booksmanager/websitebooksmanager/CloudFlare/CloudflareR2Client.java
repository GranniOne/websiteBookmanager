package com.booksmanager.websitebooksmanager.CloudFlare;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Client for interacting with Cloudflare R2 Storage using AWS SDK S3 compatibility
 */

@Service
public class CloudflareR2Client {
    private final S3Client s3Client;
    private final S3Presigner presigner;




    /**
     * Creates a new CloudflareR2Client with the provided configuration
     */
    public CloudflareR2Client(@Value("${cloudflare.accountId}") String accountId,
                              @Value("${cloudflare.accessKey}") String accessKey,
                              @Value("${cloudflare.secretKey}") String secretKey) {


        S3Config config = new S3Config(
                accountId,
                accessKey,
                secretKey
        );
        this.s3Client = buildS3Client(config);
        this.presigner = buildS3Presigner(config);
    }
    @PostConstruct
    public void init() {
        System.out.println("CloudflareR2Client init");
    }
    public void hello(){
        System.out.println("hello from CloudflareR2Client");
    }


    private static S3Presigner buildS3Presigner(S3Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        return S3Presigner.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto")) // Required by SDK but not used by R2
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    public String generatePresignedUploadUrl(String bucketName, String objectKey, Duration expiration) {
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(builder -> builder
                        .bucket(bucketName)
                        .key(objectKey)
                        .build())
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }
    public String generatePresignedDownloadUrl(String bucketName, String objectKey, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
    /**
     * Configuration class for R2 credentials and endpoint
     * - accountId: Your Cloudflare account ID
     * - accessKey: Your R2 Access Key ID (see: https://developers.cloudflare.com/r2/api/tokens)
     * - secretKey: Your R2 Secret Access Key (see: https://developers.cloudflare.com/r2/api/tokens)
     */
    public static class S3Config {
        private final String accountId;
        private final String accessKey;
        private final String secretKey;
        private final String endpoint;

        public S3Config(String accountId, String accessKey, String secretKey) {
            this.accountId = accountId;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
        }

        public String getAccessKey() { return accessKey; }
        public String getSecretKey() { return secretKey; }
        public String getEndpoint() { return endpoint; }
    }

    /**
     * Builds and configures the S3 client with R2-specific settings
     */
    private static S3Client buildS3Client(S3Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto")) // Required by SDK but not used by R2
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    /**
     * Lists all buckets in the R2 storage
     */
    public List<Bucket> listBuckets() {
        try {
            return s3Client.listBuckets().buckets();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list buckets: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all objects in the specified bucket
     */
    public List<S3Object> listObjects(String bucketName) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            return s3Client.listObjectsV2(request).contents();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list objects in bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    public List<S3Object> listObjectsFromDirectory(String bucketName, String directoryName) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(directoryName+ "/") // only objects under this folder
                    .build();
            return s3Client.listObjectsV2(request).contents();
        }
        catch (S3Exception e) {
            throw new RuntimeException("Failed to list objects in bucket " + bucketName + ": " + directoryName+ e.getMessage(), e);
        }
    }
    public ResponseInputStream<GetObjectResponse> getObjectFromR2(String directoryName) {
        try {
           GetObjectRequest request = GetObjectRequest.builder()
                   .bucket("bookmanager")
                   .key(directoryName)
                   .build();
            System.out.println(request.toString());
            return s3Client.getObject(request);
        }
        catch (S3Exception e) {
            throw new RuntimeException("Failed to list objects in bucket " + "bookmanager" + ": " + directoryName+ e.getMessage(), e);
        }
    }
    public InputStream getObjectInputStream(String key) {
        // This returns the stream directly from R2
        // We don't call .readAllBytes() here!
        return getObjectFromR2(key);
    }
    /**
     * Uploads an object to the specified bucket
     */
    // 1. For the Thumbnail (byte array)
    public void putObject(String bucketName, String key, byte[] data) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("image/jpeg") // Tell Cloudflare it's an image
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    // 2. For the PDF (Stream from Path - Best for RAM)
    public void putObject(String bucketName, String key, File file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(request, file.toPath());
            // Note: The SDK can take a Path directly and handle the streaming for you!
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload PDF: " + e.getMessage(), e);
        }
    }
    public void putObject(String bucketName, String key, String content) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(content));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to put object " + key + " in bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }
}
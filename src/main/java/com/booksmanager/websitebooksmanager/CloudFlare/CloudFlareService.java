package com.booksmanager.websitebooksmanager.CloudFlare;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudFlareService {

    public Map<String, DataStruct> signedUrls = new HashMap<String, DataStruct>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    public void createPresignedurlBooks(String path, DataTypes type, String directory) {
        Instant now = Instant.now();
        DataStruct struct = signedUrls.computeIfAbsent(directory, k -> new DataStruct());

        switch (type) {
            case MetaData -> {
                struct.setPresignedMetaData(generatePresignedDownloadUrl(path));
                struct.setMetaExpiry(now.plus(Duration.ofMinutes(15)));
            }
            case PDF -> {
                struct.setPresignedPdf(generatePresignedDownloadUrl(path));
                struct.setPdfExpiry(now.plus(Duration.ofMinutes(15)));
            }
            case COVER -> {
                struct.setPresignedCover(generatePresignedDownloadUrl(path));
                struct.setCoverExpiry(now.plus(Duration.ofMinutes(15)));
            }
            default -> throw new IllegalArgumentException("Unknown DataType: " + type);
        }
        signedUrls.putIfAbsent(directory, struct); // ensures it's in the map
    }

    private String generatePresignedDownloadUrl(String parameters) {
        return cloudflareR2Client.generatePresignedDownloadUrl(
                "bookmanager",
                parameters,
                Duration.ofMinutes(2)
        );
    }

    public String getSignedUrl(String directory, DataTypes type) {
        DataStruct struct = null;
        if(signedUrls.containsKey(directory)) {
            struct = signedUrls.get(directory);
            System.out.println(struct.getPresignedCover());
        }
        Instant now = Instant.now();
        return switch (type) {
            case COVER -> {
                assert struct != null;
                if (struct.getCoverExpiry() == null || now.isAfter(struct.getCoverExpiry())) {
                    struct.setPresignedCover(generatePresignedDownloadUrl(directory));
                    struct.setCoverExpiry(now.plus(Duration.ofMinutes(2)));
                }
                yield struct.getPresignedCover();
            }
            case PDF -> {
                assert struct != null;
                if (struct.getPdfExpiry() == null || now.isAfter(struct.getPdfExpiry())) {
                    struct.setPresignedPdf(generatePresignedDownloadUrl(directory));
                    struct.setPdfExpiry(now.plus(Duration.ofMinutes(2)));
                }
                yield struct.getPresignedPdf();
            }
            case MetaData -> {
                assert struct != null;
                if (struct.getMetaExpiry() == null || now.isAfter(struct.getMetaExpiry())) {
                    struct.setPresignedMetaData(generatePresignedDownloadUrl(directory));
                    struct.setMetaExpiry(now.plus(Duration.ofMinutes(2)));
                }
                yield struct.getPresignedMetaData();
            }
            default -> throw new IllegalArgumentException("Unknown DataType: " + type);
        };
    }




}

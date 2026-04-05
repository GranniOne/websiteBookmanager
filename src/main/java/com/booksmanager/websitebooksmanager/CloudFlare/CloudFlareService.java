package com.booksmanager.websitebooksmanager.CloudFlare;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CloudFlareService {
    public Map<String, DataStruct> signedUrls = new HashMap<String, DataStruct>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    public void createPresignedurlBooks(String path, DataTypes type, String directory) {
        Instant now = Instant.now();

        AtomicBoolean isNew = new AtomicBoolean(false);

        DataStruct struct = signedUrls.computeIfAbsent(directory, k -> {
            isNew.set(true); // mark that a new one is being created
            System.out.println("Creating new DataStruct for directory: " + k);
            return new DataStruct();
        });
        if(isNew.get()) {
            System.out.println("why does it run");
            switch (type) {
                case MetaData -> {
                    struct.setPresignedMetaData(generatePresignedDownloadUrl(path));
                    struct.setMetaExpiry(now.plus(Duration.ofMinutes(2)));
                }
                case PDF -> {
                    struct.setPresignedPdf(generatePresignedDownloadUrl(path));
                    struct.setPdfExpiry(now.plus(Duration.ofMinutes(2)));
                }
                case COVER -> {
                    struct.setPresignedCover(generatePresignedDownloadUrl(path));
                    struct.setCoverExpiry(now.plus(Duration.ofMinutes(2)));
                }
                default -> throw new IllegalArgumentException("Unknown DataType: " + type);
            }
            signedUrls.putIfAbsent(directory, struct); // ensures it's in the map
        }

    }

    private String generatePresignedDownloadUrl(String parameters) {
        return cloudflareR2Client.generatePresignedDownloadUrl(
                "bookmanager",
                parameters,
                Duration.ofMinutes(2)
        );
    }

    public String getSignedUrl(String directory, DataTypes type, String path) {
        DataStruct struct = null;
        if(signedUrls.containsKey(directory)) {
            struct = signedUrls.get(directory);
            System.out.println(struct.getPresignedCover());
        }
        Instant now = Instant.now();

        boolean expired = switch (type) {
            case COVER -> struct == null || struct.getCoverExpiry() == null || now.isAfter(struct.getCoverExpiry());
            case PDF -> struct == null || struct.getPdfExpiry() == null || now.isAfter(struct.getPdfExpiry());
            case MetaData -> struct == null || struct.getMetaExpiry() == null || now.isAfter(struct.getMetaExpiry());
            default -> true;
        };

        return switch (type) {
            case COVER -> {
                assert struct != null;
                if (expired) {
                    signedUrls.remove(directory);
                    createPresignedurlBooks(directory, DataTypes.COVER, path);

                }
                yield signedUrls.get(path).getPresignedCover();
            }
            case PDF -> {
                assert struct != null;
                if (expired) {
                    signedUrls.remove(directory);
                    createPresignedurlBooks(directory, DataTypes.PDF, path);

                }
                yield signedUrls.get(path).getPresignedPdf();
            }
            case MetaData -> {
                assert struct != null;
                if (expired) {
                    signedUrls.remove(directory);
                    createPresignedurlBooks(directory, DataTypes.MetaData, path);

                }
                yield signedUrls.get(path).getPresignedMetaData();
            }
            default -> throw new IllegalArgumentException("Unknown DataType: " + type);
        };
    }




}

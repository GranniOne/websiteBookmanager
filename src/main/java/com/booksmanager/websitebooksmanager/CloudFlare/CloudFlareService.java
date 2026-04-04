package com.booksmanager.websitebooksmanager.CloudFlare;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudFlareService {

    public Map<String,String> signedUrls = new HashMap<String,String>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    public void createPresignedurlBooks(String parameters) {
        // Generate a pre-signed upload URL valid for 15 minutes
        String uploadUrl = cloudflareR2Client.generatePresignedDownloadUrl(
                "bookmanager",
                parameters,
                Duration.ofMinutes(15)
        );
        signedUrls.put(parameters, uploadUrl);
    }




}

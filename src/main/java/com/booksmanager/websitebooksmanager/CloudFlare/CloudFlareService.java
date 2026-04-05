package com.booksmanager.websitebooksmanager.CloudFlare;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CloudFlareService {
    public Map<String,byte[]> signedUrls = new HashMap<String,byte[]>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

}




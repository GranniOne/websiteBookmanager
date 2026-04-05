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
    public Map<String, String> signedUrls = new HashMap<String, String>();

    private final CloudflareR2Client cloudflareR2Client;

    public CloudFlareService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

}




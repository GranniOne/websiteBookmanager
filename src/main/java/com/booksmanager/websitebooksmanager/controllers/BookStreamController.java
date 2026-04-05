package com.booksmanager.websitebooksmanager.controllers;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;

@RestController
public class BookStreamController {

    private final CloudflareR2Client cloudflareR2Client;

    public BookStreamController(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    // Use a dynamic path variable
    @GetMapping("/r2/stream/{directory}/{book}")
    public ResponseEntity<InputStreamResource> streamBook(
            @PathVariable String directory,
            @PathVariable String book
    ) {
        try {
            String key = "books/" + directory + "/" + book;
            ResponseInputStream<?> stream = cloudflareR2Client.getObjectFromR2(key);
            InputStreamResource resource = new InputStreamResource(stream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + book + "\"")
                    .contentType(MediaType.APPLICATION_PDF) // adapt if needed
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
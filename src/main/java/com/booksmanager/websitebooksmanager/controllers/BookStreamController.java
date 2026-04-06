package com.booksmanager.websitebooksmanager.controllers;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class BookStreamController {

    private final CloudflareR2Client cloudflareR2Client;

    public BookStreamController(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    @GetMapping("/api/books/cover")
    public void getCover(@RequestParam String key, HttpServletResponse response) throws IOException {
        response.setContentType("image/jpeg");
        // Browsers will cache this locally, making the second visit "instant"
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream inputStream = cloudflareR2Client.getObjectInputStream(key)) {
            org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    @GetMapping("/api/pdf/{directory}/{filename}")
    public void streamPdf(@PathVariable String directory,
                          @PathVariable String filename,
                          HttpServletResponse response) throws IOException {

        // Reconstruct the R2 Key (adjust "books/" prefix if needed)
        String key = "books/" + directory + "/" + filename;
        System.out.println(key);
        response.setContentType("application/pdf");
        // 'inline' tells the browser to show it in the iframe, not download it
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");

        try (InputStream inputStream = cloudflareR2Client.getObjectFromR2(key)) {
            org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
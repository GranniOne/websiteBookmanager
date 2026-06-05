package com.booksmanager.websitebooksmanager.controllers;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
@PreAuthorize("isAuthenticated()")
public class BookStreamController {

    private final CloudflareR2Client cloudflareR2Client;

    public BookStreamController(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    @GetMapping("/api/cover/**")
    public void getCover(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        System.out.println("getCover");
        String fullPath = request.getRequestURI();

        // remove "/api/epubs/"
        String relativePath = fullPath.substring("/api/cover/".length());

        String key = "epubs/" + relativePath;

        System.out.println("key: " + key);

        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream inputStream =
                     cloudflareR2Client.getObjectInputStream(key)) {

            StreamUtils.copy(inputStream, response.getOutputStream());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/api/books/{bookFormat}/cover")
    public void getEpubCover(@PathVariable String bookFormat ,HttpServletResponse response) throws IOException {
        String key = "books/" + bookFormat + "/cover.jpg";
        System.out.println(key);
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
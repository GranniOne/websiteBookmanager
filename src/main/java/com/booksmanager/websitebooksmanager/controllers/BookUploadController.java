package com.booksmanager.websitebooksmanager.controllers;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
public class BookUploadController {


    private final CloudflareR2Client cloudflareR2Client;

    public BookUploadController(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;

    }

    @GetMapping("api/upload/{filename}")
    public void UploadBook(@PathVariable String filename, HttpServletResponse response) {
        response.setContentType("application/pdf");
        // 'inline' tells the browser to show it in the iframe, not download it
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");


        //org.springframework.util.StreamUtils.copy(inputStream, response.getOutputStream());

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    }
}

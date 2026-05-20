package com.booksmanager.websitebooksmanager.controllers;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;

@RestController
@RequestMapping("/api/epub")
public class EpubUploadController {

    private final CloudflareR2Client cloudflareR2Client;

    public EpubUploadController(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    // Handles: /api/epub/book-of-vaadin-vaadin7/OEBPS/bk01-toc.html
    @GetMapping("/{bookKey}/**")
    public void serveEpubFile(
            @PathVariable String bookKey,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 1. Snatch the whole request path (e.g., "/api/epub/book-of-vaadin-vaadin7/OEBPS/bk01-toc.html")
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        // 2. Isolate the relative path after the bookKey (yields: "OEBPS/bk01-toc.html")
        String relativeFilePath = fullPath.substring(fullPath.indexOf(bookKey) + bookKey.length() + 1);

        // 3. Prepend the "epubs/" directory path prefix required by your bucket setup
        // This yields exactly: "epubs/book-of-vaadin-vaadin7/OEBPS/bk01-toc.html"
        // Or for images: "epubs/book-of-vaadin-vaadin7/OEBPS/img/addons/cval-pro-licenses-3.png"
        String r2ObjectKey = "epubs/" + bookKey + "/" + relativeFilePath;

        try {
            // 4. Retrieve the live byte stream from your existing client bean
            try (ResponseInputStream<GetObjectResponse> s3Stream = cloudflareR2Client.getObjectFromR2(r2ObjectKey)) {

                // 5. Send the correct browser context headers
                response.setContentType(determineMimeType(relativeFilePath));
                response.setContentLengthLong(s3Stream.response().contentLength());

                // 6. Direct memory pipe stream transfer out to the client
                StreamUtils.copy(s3Stream, response.getOutputStream());
            }
        } catch (NoSuchKeyException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested book file does not exist.");
        }
    }

    private String determineMimeType(String path) {
        String lowerPath = path.toLowerCase();

        // EPUB text content can use either extension depending on how it was packaged
        if (lowerPath.endsWith(".html") || lowerPath.endsWith(".xhtml")) return "application/xhtml+xml";

        if (lowerPath.endsWith(".css")) return "text/css";
        if (lowerPath.endsWith(".js")) return "application/javascript";

        // Handles your .png image pathing seamlessly
        if (lowerPath.endsWith(".png")) return "image/png";
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerPath.endsWith(".gif")) return "image/gif";
        if (lowerPath.endsWith(".svg")) return "image/svg+xml";

        // Embedded asset typography files
        if (lowerPath.endsWith(".woff")) return "font/woff";
        if (lowerPath.endsWith(".woff2")) return "font/woff2";
        if (lowerPath.endsWith(".otf")) return "font/otf";
        if (lowerPath.endsWith(".ttf")) return "font/ttf";

        // Structural book package indexing files
        if (lowerPath.endsWith(".opf")) return "application/oebps-package+xml";
        if (lowerPath.endsWith(".ncx")) return "application/x-dtbncx+xml";

        return "application/octet-stream";
    }
}
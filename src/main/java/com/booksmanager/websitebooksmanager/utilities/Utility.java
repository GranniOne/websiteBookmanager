package com.booksmanager.websitebooksmanager.utilities;

import com.booksmanager.websitebooksmanager.pdf.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import org.springframework.stereotype.Service;

@Service
public class Utility {

    private static CloudStorageService  cloudStorageService;
    private static CloudflareR2Client cloudflareR2Client;

    public Utility(CloudStorageService cloudStorageService, CloudflareR2Client cloudflareR2Client) {
        Utility.cloudStorageService = cloudStorageService;
        Utility.cloudflareR2Client = cloudflareR2Client;
    }


    public static String determineMimeType(String path) {
        if (path == null) return "application/octet-stream";

        // We only care about the extension. 
        // This handles cases where a filename might contain a dot in the middle
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) return "application/octet-stream";

        String ext = path.substring(lastDot).toLowerCase();

        // XHTML/HTML
        if (ext.equals(".html") || ext.equals(".xhtml") || ext.equals(".htm")) {
            return "application/xhtml+xml;charset=UTF-8";
        }

        // Styles and Scripts
        if (ext.endsWith(".css")) return "text/css;charset=UTF-8";
        if (ext.endsWith(".js")) return "application/javascript;charset=UTF-8";

        // Images
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".svg")) return "image/svg+xml";

        // Fonts
        if (ext.endsWith(".woff")) return "font/woff";
        if (ext.endsWith(".woff2")) return "font/woff2";
        if (ext.endsWith(".otf")) return "font/otf";
        if (ext.endsWith(".ttf")) return "font/ttf";

        // EPUB Structural Files
        if (ext.endsWith(".opf")) return "application/oebps-package+xml";
        if (ext.endsWith(".ncx")) return "application/x-dtbncx+xml";

        // Fallback
        return "application/octet-stream";
    }



}

package com.booksmanager.websitebooksmanager.epub;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Entities.BookType;
import com.booksmanager.websitebooksmanager.Entities.EpubBook;
import com.booksmanager.websitebooksmanager.Service.EpubService;
import com.booksmanager.websitebooksmanager.utilities.Utility;
import com.vaadin.flow.server.streams.UploadMetadata;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UnZipEpub {

    private final CloudflareR2Client cloudflareR2Client;
    private final EpubService epubService;

    public UnZipEpub(CloudflareR2Client cloudflareR2Client, EpubService epubService) {
        this.cloudflareR2Client = cloudflareR2Client;
        this.epubService = epubService;
    }

    public void unzip(UploadMetadata metadata, File file, Consumer<String> progressListener) throws Exception {
        File destDir = Files.createTempDirectory(metadata.fileName()).toFile();
        System.out.println("Creating temporary directory: " + destDir.getAbsolutePath());

        // Fully local thread-safe entity instance
        EpubBook epubBook = new EpubBook();
        progressListener.accept("Processing Epub...");
        byte[] buffer = new byte[1024];

        // 1. Unzip archive contents securely
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = resolveAndValidateFile(destDir, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException("Failed parsing ZIP archive container structures", e);
        }

        // 2. Parse the OCF structure to locate the OPF document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Path containerPath = Files.walk(destDir.toPath())
                .filter(p -> p.getFileName().toString().equals("container.xml"))
                .findFirst()
                .orElseThrow(() -> new IOException("Invalid EPUB: OCF container.xml missing."));

        Document containerDoc = builder.parse(containerPath.toFile());
        containerDoc.getDocumentElement().normalize();

        Element rootfile = (Element) containerDoc.getElementsByTagName("rootfile").item(0);
        String opfRelativePath = rootfile.getAttribute("full-path");
        Path opfPath = destDir.toPath().resolve(opfRelativePath);

        Document opfDoc = builder.parse(opfPath.toFile());
        opfDoc.getDocumentElement().normalize();

        // 3. Automated Strategy Detector Matrix
        NodeList packageList = opfDoc.getElementsByTagName("package");
        if (packageList.getLength() == 0) {
            throw new IOException("Invalid EPUB: The OPF file is missing the mandatory <package> element.");
        }

        Element packageElement = (Element) packageList.item(0);
        String version = packageElement.getAttribute("version");
        EpubVersionProcessor processor;

        if (version != null && version.startsWith("3")) {
            System.out.println("Automated Router: Routing to EPUB 3 Strategy Chain (Detected Version: " + version + ")");
            processor = new Epub3Processor();
        } else {
            System.out.println("Automated Router: Routing to Legacy EPUB 2 Strategy Chain (Detected Version: " + version + ")");
            processor = new Epub2Processor();
        }

// =========================================================================
// THE MISSING LINK: EXECUTE THE EXTRACTION PIPELINE
// =========================================================================
        progressListener.accept("Extracting book metadata metrics...");
        processor.extractTitle(opfDoc, epubBook);
        processor.extractAuthor(opfDoc, epubBook);
        processor.extractPublisher(opfDoc, epubBook);
        processor.extractDate(opfDoc, epubBook);
        processor.extractIdentifier(opfDoc, epubBook);
        processor.extractLanguage(opfDoc, epubBook); // Populates your language field

        progressListener.accept("Extracting book cover asset details...");
        processor.extractCover(destDir.toPath(), opfPath, opfDoc, epubBook);

        // 4. Parallelize heavy network-bound I/O transfers out to Cloudflare R2
        long totalFiles;
        try (var stream = Files.walk(destDir.toPath())) {
            totalFiles = stream.filter(Files::isRegularFile).count();
        }

        java.util.concurrent.atomic.AtomicInteger fileCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        try (var stream = Files.walk(destDir.toPath())) {
            stream.filter(Files::isRegularFile)
                    .parallel()
                    .forEach(currentFile -> {
                        try {
                            upload(currentFile, destDir, metadata);
                            int processedCount = fileCounter.incrementAndGet();
                            if (processedCount % 5 == 0 || processedCount == totalFiles) {
                                progressListener.accept(String.format("Uploading: file %d of %d...", processedCount, totalFiles));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        progressListener.accept("All files uploaded successfully!");

        // 5. Build Object Reference Key Profiles
        String baseName = metadata.fileName().replace(" ", "-");
        int dot = baseName.lastIndexOf('.');
        if (dot != -1) {
            baseName = baseName.substring(0, dot);
        }
        epubBook.setBookType(BookType.EPUB);
        epubBook.setTitle(baseName);
        epubBook.setR2Key("epubs/" + baseName);

        epubService.saveEpub(epubBook);
    }

    private void upload(Path path, File destDir, UploadMetadata metadata) throws IOException {
        Path relativePath = destDir.toPath().relativize(path);
        String baseName = metadata.fileName().replace(" ", "-");
        int dot = baseName.lastIndexOf('.');
        if (dot != -1) {
            baseName = baseName.substring(0, dot);
        }

        String r2Key = baseName + "/" + relativePath.toString().replace('\\', '/');
        String contentType = Utility.determineMimeType(path.toString());

        cloudflareR2Client.putObject(
                "bookmanager",
                "epubs/" + r2Key,
                path.toFile(),
                contentType
        );
    }

    private static File resolveAndValidateFile(File destinationDir, String zipEntryName) throws IOException {
        File targetFile = new File(destinationDir, zipEntryName);
        String destDirPath = destinationDir.getCanonicalPath();
        String targetFilePath = targetFile.getCanonicalPath();

        if (!targetFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target directory (Zip Slip vulnerability): " + zipEntryName);
        }
        return targetFile;
    }

    // =========================================================================
    // TRATEGY PATTERN HOOKS & IMPLEMENTATIONS (Inner Component Matrix)
    // =========================================================================

    private interface EpubVersionProcessor {
        void extractCover(Path destDir, Path opfPath, Document opfDoc, EpubBook epubBook);
        void extractAuthor(Document opfDoc, EpubBook epubBook);
        void extractPublisher(Document opfDoc, EpubBook epubBook);
        void extractTitle(Document opfDoc, EpubBook epubBook);
        void extractDate(Document opfDoc, EpubBook epubBook);
        void extractIdentifier(Document opfDoc, EpubBook epubBook);
        void extractLanguage(Document opfDoc, EpubBook epubBook);

    }

    /**
     * Modern Strict EPUB 3.x System Strategy
     */
    private static class Epub3Processor implements EpubVersionProcessor {
        @Override
        public void extractCover(Path destDir, Path opfPath, Document opfDoc, EpubBook epubBook) {
            NodeList itemList = opfDoc.getElementsByTagName("item");
            String coverPath = null;

            // Target 1: Pure Manifest Semantic Lookup (Recommended spec)
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                if ("cover-image".equals(item.getAttribute("properties"))) {
                    coverPath = item.getAttribute("href");
                    System.out.println("[EPUB 3 Strategy] Cover targeted via manifest property: " + coverPath);
                    break;
                }
            }

            // Target 2: Fallback lookups for poor EPUB 3 exports (Handling optional constraints)
            if (coverPath == null) {
                NodeList metaList = opfDoc.getElementsByTagName("meta");
                for (int i = 0; i < metaList.getLength(); i++) {
                    Element meta = (Element) metaList.item(i);
                    if ("cover".equals(meta.getAttribute("name"))) {
                        String targetId = meta.getAttribute("content");
                        for (int j = 0; j < itemList.getLength(); j++) {
                            Element item = (Element) itemList.item(j);
                            if (targetId.equals(item.getAttribute("id"))) {
                                coverPath = item.getAttribute("href");
                                System.out.println("[EPUB 3 Strategy Fallback] Found cover pointer via metadata: " + coverPath);
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            // Target 3: Scraping heuristic if everything else was skipped
            if (coverPath == null) {
                for (int i = 0; i < itemList.getLength(); i++) {
                    Element item = (Element) itemList.item(i);
                    String id = item.getAttribute("id").toLowerCase();
                    String href = item.getAttribute("href").toLowerCase();
                    if (id.contains("cover") || href.contains("cover")) {
                        coverPath = item.getAttribute("href");
                        System.out.println("[EPUB 3 Heuristic Fallback] Guessed cover via manifest strings: " + coverPath);
                        break;
                    }
                }
            }

            if (coverPath != null) {
                Path coverSource = opfPath.getParent().resolve(coverPath);
                epubBook.setCoverHref(destDir.relativize(coverSource).toString().replace("\\", "/"));
            } else {
                System.out.println("[EPUB 3 Strategy] No valid cover extracted.");
            }
        }
        @Override
        public void extractTitle(Document opfDoc, EpubBook epubBook) {
            NodeList titles = opfDoc.getElementsByTagName("dc:title");
            if (titles.getLength() > 0) {
                // Grab the first declared title as the primary entry
                epubBook.setOfficialTitle(titles.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractAuthor(Document opfDoc, EpubBook epubBook) {
            NodeList creators = opfDoc.getElementsByTagName("dc:creator");
            if (creators.getLength() > 0) {
                epubBook.setAuthor(creators.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractPublisher(Document opfDoc, EpubBook epubBook) {
            NodeList publishers = opfDoc.getElementsByTagName("dc:publisher");
            if (publishers.getLength() > 0) {
                epubBook.setPublisher(publishers.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractDate(Document opfDoc, EpubBook epubBook) {
            // EPUB 3 favors the refined dcterms:modified property over a plain dc:date
            NodeList metaList = opfDoc.getElementsByTagName("meta");
            for (int i = 0; i < metaList.getLength(); i++) {
                Element meta = (Element) metaList.item(i);
                if ("dcterms:modified".equals(meta.getAttribute("property")) ||
                        "dcterms:date".equals(meta.getAttribute("property"))) {
                    epubBook.setPublicationDate(meta.getTextContent().trim());
                    return; // Exit early once found
                }
            }

            // Fallback if no specialized meta properties exist
            NodeList dates = opfDoc.getElementsByTagName("dc:date");
            if (dates.getLength() > 0) {
                epubBook.setPublicationDate(dates.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractIdentifier(Document opfDoc, EpubBook epubBook) {
            // Look for the pageBreakSource ISBN first for textbook synchronization
            NodeList metaList = opfDoc.getElementsByTagName("meta");
            for (int i = 0; i < metaList.getLength(); i++) {
                Element meta = (Element) metaList.item(i);
                if ("pageBreakSource".equals(meta.getAttribute("property"))) {
                    String rawIsbn = meta.getTextContent().trim();
                    epubBook.setIsbn(rawIsbn.replaceAll("[^0-9]", "")); // Strip to pure digits
                    return;
                }
            }

            // Fallback to standard package unique-identifier mapping
            NodeList identifiers = opfDoc.getElementsByTagName("dc:identifier");
            if (identifiers.getLength() > 0) {
                epubBook.setIsbn(identifiers.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractLanguage(Document opfDoc, EpubBook epubBook) {
            NodeList languages = opfDoc.getElementsByTagName("dc:language");
            if (languages.getLength() > 0) {
                epubBook.setLanguage(languages.item(0).getTextContent().trim());
                System.out.println("[EPUB 3 Strategy] Extracted Language: " + epubBook.getLanguage());
            } else {
                epubBook.setLanguage("unknown");
            }
        }
    }

    /**
     * Legacy-Compliant EPUB 2.x System Strategy
     */
    private static class Epub2Processor implements EpubVersionProcessor {
        @Override
        public void extractCover(Path destDir, Path opfPath, Document opfDoc, EpubBook epubBook) {
            String metaContent = null;

            // 1. Get the raw string from the cover meta tag
            NodeList metaList = opfDoc.getElementsByTagName("meta");
            for (int i = 0; i < metaList.getLength(); i++) {
                Element meta = (Element) metaList.item(i);
                if ("cover".equals(meta.getAttribute("name"))) {
                    metaContent = meta.getAttribute("content").trim();
                    break;
                }
            }

            if (metaContent == null || metaContent.isEmpty()) {
                System.out.println("No cover meta tag found.");
                return;
            }

            // Calculate the folder where the OPF sits relative to the unzipped root
            // For example, if opfPath is "C:/temp/unzip/OEBPS/content.opf", opfParentRel is "OEBPS"
            Path opfParentRel = destDir.relativize(opfPath.getParent());
            String finalHref = null;

            // 2. Test Path A: Assume it's a direct path relative to the OPF directory
            Path directTestPath = destDir.resolve(opfParentRel).resolve(metaContent).normalize();

            if (Files.exists(directTestPath) && !Files.isDirectory(directTestPath)) {
                System.out.println("[Filesystem Match] Direct path exists: " + directTestPath);
                finalHref = metaContent;
            } else {
                // 3. Test Path B: It's a Manifest ID, look up the real href
                System.out.println("[Filesystem Check] Direct path not found. Checking manifest ID for: " + metaContent);

                NodeList itemList = opfDoc.getElementsByTagName("item");
                for (int i = 0; i < itemList.getLength(); i++) {
                    Element item = (Element) itemList.item(i);
                    String id = item.getAttribute("id");

                    if (metaContent.equals(id)) {
                        finalHref = item.getAttribute("href");
                        System.out.println("[Manifest Match] Resolved ID '" + id + "' to href: " + finalHref);
                        break;
                    }
                }
            }

            // 4. Final Verification and Save
            if (finalHref != null) {
                // Reconstruct the true path within destDir
                Path finalCoverPath = destDir.resolve(opfParentRel).resolve(finalHref).normalize();

                if (Files.exists(finalCoverPath)) {
                    // Get the clean internal path relative to the unzip root to store in DB/R2
                    // This safely preserves "OEBPS/Images/978-0-9974011-1-0_frontcover.jpg"
                    String internalRootPath = destDir.relativize(finalCoverPath).toString().replace("\\", "/");

                    epubBook.setCoverHref(internalRootPath);
                    System.out.println("[Cover Pipeline Locked] Verified full path: " + internalRootPath);
                } else {
                    System.out.println("[Error] Resolved manifest path does not exist: " + finalCoverPath);
                }
            } else {
                System.out.println("[Error] Could not resolve cover asset via path or ID.");
            }
        }
        @Override
        public void extractTitle(Document opfDoc, EpubBook epubBook) {
            NodeList titles = opfDoc.getElementsByTagName("dc:title");
            if (titles.getLength() == 0) titles = opfDoc.getElementsByTagName("title"); // Fallback

            if (titles.getLength() > 0) {
                epubBook.setOfficialTitle(titles.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractAuthor(Document opfDoc, EpubBook epubBook) {
            NodeList creators = opfDoc.getElementsByTagName("dc:creator");
            if (creators.getLength() == 0) creators = opfDoc.getElementsByTagName("creator");

            if (creators.getLength() > 0) {
                epubBook.setAuthor(creators.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractPublisher(Document opfDoc, EpubBook epubBook) {
            NodeList publishers = opfDoc.getElementsByTagName("dc:publisher");
            if (publishers.getLength() == 0) publishers = opfDoc.getElementsByTagName("publisher");

            if (publishers.getLength() > 0) {
                epubBook.setPublisher(publishers.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractDate(Document opfDoc, EpubBook epubBook) {
            NodeList dates = opfDoc.getElementsByTagName("dc:date");
            if (dates.getLength() == 0) dates = opfDoc.getElementsByTagName("date");

            if (dates.getLength() > 0) {
                epubBook.setPublicationDate(dates.item(0).getTextContent().trim());
            }
        }

        @Override
        public void extractIdentifier(Document opfDoc, EpubBook epubBook) {
            NodeList identifiers = opfDoc.getElementsByTagName("dc:identifier");
            if (identifiers.getLength() == 0) identifiers = opfDoc.getElementsByTagName("identifier");

            if (identifiers.getLength() > 0) {
                // Clean up common prefix noise often found in legacy files
                String rawId = identifiers.item(0).getTextContent().trim();
                epubBook.setIsbn(rawId.replace("urn:isbn:", "").replace("-", ""));
            }
        }

        @Override
        public void extractLanguage(Document opfDoc, EpubBook epubBook) {
            NodeList languages = opfDoc.getElementsByTagName("dc:language");
            if (languages.getLength() == 0) {
                languages = opfDoc.getElementsByTagName("language"); // Namespace fallback
            }

            if (languages.getLength() > 0) {
                epubBook.setLanguage(languages.item(0).getTextContent().trim());
                System.out.println("[EPUB 2 Strategy] Extracted Language: " + epubBook.getLanguage());
            } else {
                epubBook.setLanguage("unknown");
            }
        }
    }
}
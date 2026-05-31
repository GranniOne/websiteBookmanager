package com.booksmanager.websitebooksmanager.epub;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Entities.BookType;
import com.booksmanager.websitebooksmanager.Entities.EpubBook;
import com.booksmanager.websitebooksmanager.Layout.ProgressBarLabel;
import com.booksmanager.websitebooksmanager.Service.EpubService;
import com.booksmanager.websitebooksmanager.utilities.Utility;
import com.vaadin.flow.component.UI;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UnZipEpub {


    private static CloudflareR2Client cloudflareR2Client;
    private static EpubService epubService;
    private static EpubBook epubBook = new EpubBook();
    public UnZipEpub(CloudflareR2Client cloudflareR2Client, EpubService  epubService) {
        UnZipEpub.cloudflareR2Client = cloudflareR2Client;
        UnZipEpub.epubService = epubService;
    }

    public static void unzip(ProgressBarLabel pb, UploadMetadata metadata, File file, UI ui) throws IOException, Exception {
        File destDir = Files.createTempDirectory(metadata.fileName()).toFile();
        System.out.println("Creating temporary directory: " + destDir.getAbsolutePath());

        byte[] buffer = new byte[1024];

        // Use try-with-resources to ensure streams close automatically if an error occurs
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                // Securely resolve the file path
                File newFile = resolveAndValidateFile(destDir, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Fix for Windows-created archives and missing parent directories
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // Write file content securely
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
            throw new RuntimeException(e);
        }

        extractCover(destDir.toPath());



        Files.walk(destDir.toPath()).filter(Files::isRegularFile).parallel().forEach(files -> {
            try {

                upload(files,destDir,metadata);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });



        String baseName = metadata.fileName().replace(" ","-");

        int dot = baseName.lastIndexOf('.');
        if (dot != -1) {
            baseName = baseName.substring(0, dot);
        }
        epubBook.setBookType(BookType.EPUB);
        epubBook.setTitle(baseName);
        epubBook.setR2Key("epubs/"+baseName);

        epubService.saveEpub(epubBook);

    }




    public static void extractCover(Path destDir) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 1. Find container.xml
            Path containerPath = Files.walk(destDir)
                    .filter(p -> p.getFileName().toString().equals("container.xml"))
                    .findFirst()
                    .orElseThrow();

            Document containerDoc = builder.parse(containerPath.toFile());
            containerDoc.getDocumentElement().normalize();

            // 2. Get OPF path
            Element rootfile = (Element) containerDoc
                    .getElementsByTagName("rootfile")
                    .item(0);

            String opfRelativePath = rootfile.getAttribute("full-path");
            Path opfPath = destDir.resolve(opfRelativePath);

            // 3. Parse OPF
            Document opfDoc = builder.parse(opfPath.toFile());
            opfDoc.getDocumentElement().normalize();

            // 4. Extract cover id (meta property="cover" or name="cover")
            String coverId = null;

            NodeList metaList = opfDoc.getElementsByTagName("meta");
            for (int i = 0; i < metaList.getLength(); i++) {
                Element meta = (Element) metaList.item(i);

                // EPUB3 style: property="cover"
                if ("cover".equals(meta.getAttribute("property"))) {
                    coverId = meta.getAttribute("content");
                    break;
                }

                // EPUB2 style: name="cover"
                if ("cover".equals(meta.getAttribute("name"))) {
                    coverId = meta.getAttribute("content");
                    break;
                }
            }

            if (coverId == null) {
                System.out.println("No cover found in OPF");
                return;
            }

            // 5. Build manifest map (id -> href)
            NodeList itemList = opfDoc.getElementsByTagName("item");
            String coverHref = null;

            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);

                if (coverId.equals(item.getAttribute("id"))) {
                    coverHref = item.getAttribute("href");
                    break;
                }
            }

            if (coverHref == null) {
                System.out.println("Cover href not found in manifest");
                return;
            }
            // 6. Resolve actual file path
            Path coverSource = opfPath.getParent().resolve(coverHref);
            epubBook.setCoverhref(destDir.relativize(coverSource).toString().replace("\\","/"));
            Path coverTarget = destDir.resolve("cover.jpg");

            // 7. Copy cover into root
            Files.copy(coverSource, coverTarget);

            System.out.println("Cover extracted: " + coverTarget);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void upload(Path path, File destDir, UploadMetadata metadata) throws IOException {

        Path relativePath = destDir.toPath().relativize(path);

        String baseName = metadata.fileName().replace(" ","-");
        int dot = baseName.lastIndexOf('.');
        if (dot != -1) {
            baseName = baseName.substring(0, dot);
        }

        String r2Key = baseName + "/" +
                relativePath.toString().replace('\\', '/');

        String contentType = Utility.determineMimeType(path.toString());



        cloudflareR2Client.putObject(
                "bookmanager",
                "epubs/" + r2Key,
                path.toFile(),
                contentType
        );



    }



    /**
     * Guard against Zip Slip attacks by verifying the destination file
     * stays within the target directory.
     */
    private static File resolveAndValidateFile(File destinationDir, String zipEntryName) throws IOException {
        File targetFile = new File(destinationDir, zipEntryName);

        // Canonical paths resolve all "./" and "../" relative elements
        String destDirPath = destinationDir.getCanonicalPath();
        String targetFilePath = targetFile.getCanonicalPath();

        if (!targetFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target directory (Zip Slip vulnerability): " + zipEntryName);
        }

        return targetFile;
    }
}
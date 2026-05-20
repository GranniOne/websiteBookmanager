package com.booksmanager.websitebooksmanager.epub;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.ProgressBarLabel;
import com.booksmanager.websitebooksmanager.utilities.Utility;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.streams.UploadMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

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

    public UnZipEpub( CloudflareR2Client cloudflareR2Client) {
        UnZipEpub.cloudflareR2Client = cloudflareR2Client;
    }

    public static void unzip(ProgressBarLabel pb, UploadMetadata metadata, File file, UI ui) throws IOException {
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
        Files.walk(destDir.toPath()).filter(Files::isRegularFile).parallel().forEach(files -> {
            try {
                upload(files,destDir,metadata);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


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

        String contentType = Files.probeContentType(path);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }


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
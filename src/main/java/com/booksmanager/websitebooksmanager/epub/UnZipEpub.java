package com.booksmanager.websitebooksmanager.epub;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UnZipEpub {

    public UnZipEpub() {

    }

    public static void UnzipFile(File file) throws IOException {
        File destDir = Files.createTempDirectory("tmpDirPrefix").toFile();
        System.out.println(destDir.toPath());

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
        }
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
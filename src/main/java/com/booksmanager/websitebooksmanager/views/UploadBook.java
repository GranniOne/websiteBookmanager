package com.booksmanager.websitebooksmanager.views;


import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Value;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Route("upload")
public class UploadBook extends Div implements HasUrlParameter<String> {
    final CloudStorageService cloudStorageService;
    final CloudflareR2Client cloudflareR2Client;
    final String uploadDir;

    UploadBook(CloudStorageService cloudStorageService, CloudflareR2Client cloudflareR2Client, @Value("${app.upload.dir}") String uploadDir) {
        this.cloudStorageService = cloudStorageService;
        this.cloudflareR2Client = cloudflareR2Client;
        this.uploadDir = uploadDir;

    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        this.removeAll();

        List<String> pathList = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .get("file");

        Path pathFromUrl = Paths.get(pathList.get(0)).normalize();
        Path baseDirectory = Paths.get(uploadDir);
        Path securePath = baseDirectory.resolve(pathFromUrl).normalize();

        if (!securePath.startsWith(baseDirectory)) {
            throw new SecurityException("Invalid file path detected!");
        }

        Map<String, Object> metadataMap = cloudStorageService.createMetaDataMap(securePath,"programming","intermediate");

        // 2. IDENTITY: Get the title the service just discovered
        String discoveredTitle = (String) metadataMap.get("title");
        String originalFileName = (String) metadataMap.get("filename");

        // 3. PATHING: Define the cloud folder based on discovery
        String folderKey = "books/" + discoveredTitle + "/";
        String bucket = "bookmanager";


        // 4. GENERATION
        String jsonMetadata = cloudStorageService.convertMapToJson(metadataMap);
        byte[] thumbnail = cloudStorageService.generateThumbnailFromPath(securePath);

        // 5.UPLOADS
        //PDF: Key uses the discovered identity
        this.cloudflareR2Client.putObject(bucket, folderKey + originalFileName, securePath);

        //JSON: metadata.json
        this.cloudflareR2Client.putObject(bucket, folderKey + "meta.json", jsonMetadata);

        //Image: thumb.jpg
        if (thumbnail != null) {
            this.cloudflareR2Client.putObject(bucket, folderKey + "cover.jpg", thumbnail);
        }

        // 6. CLEANUP
        try { Files.deleteIfExists(securePath); } catch (IOException ignored) {}





        // Now the UI knows!
        Notification.show("Archived: " + metadataMap.get("title"));

    }
}

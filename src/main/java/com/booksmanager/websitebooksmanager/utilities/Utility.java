package com.booksmanager.websitebooksmanager.utilities;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.ProgressBarLabel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadMetadata;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
@Service
public class Utility {

    private static CloudStorageService  cloudStorageService;
    private static CloudflareR2Client cloudflareR2Client;

    public Utility(CloudStorageService cloudStorageService, CloudflareR2Client cloudflareR2Client) {
        Utility.cloudStorageService = cloudStorageService;
        Utility.cloudflareR2Client = cloudflareR2Client;
    }

    public static void SuccessForFileUpload(ProgressBarLabel pb, UploadMetadata metadata, File file, UI ui){
        try {
            String bucket = "bookmanager";

            // 1. DISCOVERY: Extract metadata from the file
            // We do this while the user is still on the original page
            Map<String, Object> metadataMap = cloudStorageService.createMetaDataMap(
                    file, "programming", "intermediate"
            );

            // 2. GENERATION: Prepare assets
            byte[] thumbnail = cloudStorageService.generateThumbnailFromPath(file);
            String jsonMetadata = cloudStorageService.convertMapToJson(metadataMap);
            String folderKey = "books/" + metadataMap.get("folderName") + "/";

            // 3. UPLOAD: Send everything to Cloudflare R2
            cloudflareR2Client.putObject(bucket, folderKey + metadataMap.get("filename"), file);
            cloudflareR2Client.putObject(bucket, folderKey + "meta.json", jsonMetadata);

            if (thumbnail != null) {
                cloudflareR2Client.putObject(bucket, folderKey + "cover.jpg", thumbnail);
            }

            // 5. SESSION HANDOFF: Store the map so the next view can edit it
            ui.access(() -> {
                VaadinSession.getCurrent().setAttribute("pendingMetadata", metadataMap);


                pb.getProgressBar().setIndeterminate(false);
                //ui.navigate(UploadBook.class);
            });
            pb.getProgressBar().setIndeterminate(false);
            // 6. FINISH: Navigate to the editor
            //getUI().ifPresent(ui -> ui.navigate(UploadBook.class));
        }catch (Exception e) {
            ui.access(() -> {
                pb.getProgressBar().setIndeterminate(false);
                pb.getProgressBarLabelText().setText("Upload failed: " + e.getMessage());
            });
        }
    }

}

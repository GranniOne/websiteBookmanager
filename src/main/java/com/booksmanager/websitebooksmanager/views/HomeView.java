package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Utilities.MyTransferProgressListener;
import com.booksmanager.websitebooksmanager.Utilities.ProgressBarLabel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.*;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@StyleSheet("styles.css")
@Route("")
public class HomeView extends Div {
    final CloudFlareService  cloudFlareService;
    final CloudStorageService cloudStorageService;
    final CloudflareR2Client  cloudflareR2Client;

    public HomeView(CloudFlareService  cloudFlareService, CloudStorageService  cloudStorageService, CloudflareR2Client  cloudflareR2Client) {
        this.cloudFlareService = cloudFlareService;
        this.cloudStorageService = cloudStorageService;
        this.cloudflareR2Client = cloudflareR2Client;

        // Force the view to fill the browser window
        setSizeFull();
        addClassName("home-page-wrapper");

        VerticalLayout welcomeIsland = new VerticalLayout();
        welcomeIsland.addClassName("home-island-container");
        welcomeIsland.addClassName("home-island");

        // Rest of your logic...
        H1 title = new H1("THE DIGITAL VOLUME ARCHIVE");
        title.addClassName("home-title");

        Paragraph subTitle = new Paragraph("A high-fidelity management system for your technical library.");
        subTitle.addClassName("home-subtitle");

        Div divider = new Div();
        divider.addClassName("home-divider");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.addClassName("home-action-row");

        Button exploreBtn = createMenuButton("Explore Collection", VaadinIcon.BOOK, "primary");
        exploreBtn.addClickListener(e -> UI.getCurrent().navigate(BookRoot.class));

        Button uploadBtn = createMenuButton("Upload Book", VaadinIcon.UPLOAD, "secondary");

        Button manageArchiveBtn = createMenuButton("Manage Archive", VaadinIcon.TRASH, "danger");
        Upload upload = getUpload(uploadBtn);

        actionRow.add(exploreBtn,upload,manageArchiveBtn);

        welcomeIsland.add(title, subTitle, divider, actionRow);
        add(welcomeIsland);

    }
    private @NonNull Upload getUpload(Button uploadBtn) {
        ProgressBarLabel pb = new ProgressBarLabel("");
        pb.setVisible(true);

        add(pb);


        // 1. Initialize constants and state before the handler
        final long bytesPerSecondLimit = 250 * 102400; // 250 KB/s
        // We use a 1-element array so we can 'reset' it inside whenStart if needed
        final long[] startTime = {0L};
        FileUploadCallback successHandler = (metadata, file) -> {
            pb.getProgressBar().setValue(0);
            pb.getProgressBar().setIndeterminate(true);
            pb.getProgressBarLabelText().setText("Archiving to cloud...");
            System.out.println("Archiving to cloud...");

            UI ui = UI.getCurrent();


            CompletableFuture.runAsync(() -> {
                SuccesForFileUpload(pb, metadata, file, ui);
            });
            //UI.getCurrent().navigate(UploadBook.class,QueryParameters.simple(Map.of("file", file.getAbsolutePath())));

        };


        TemporaryFileUploadHandler temporaryFileHandler = UploadHandler.toTempFile(successHandler);

        temporaryFileHandler
                .whenStart((starthandler) -> {
                    pb.getProgressBarLabelText().setText(starthandler.fileName());
                    pb.setVisible(true);
                    System.out.println("it ran");
                    startTime[0] = System.currentTimeMillis();

                })
                .onProgress((context, transferred, total) -> {

                    // 2. THE BRAKE: Force the thread to wait if it's too fast
                    long elapsedMs = System.currentTimeMillis() - startTime[0];

                    if (elapsedMs > 0) {
                        // How many ms SHOULD have passed for this many bytes?
                        long expectedMs = (transferred * 1000) / bytesPerSecondLimit;
                        long sleepTime = expectedMs - elapsedMs;

                        if (sleepTime > 0) {
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    System.out.println("progress " + transferred + "/" + total);
                    // 3. THE UI: Update the progress bar

                    if (total > 0) {
                        double progress = (double) transferred / total;

                        // Use the context UI to ensure thread safety
                        context.getUI().access(() -> {
                            pb.getProgressBar().setValue(progress); // Use the 0.0-1.0 range
                        });

                        System.out.println("Progress: " + (int)(progress * 100) + "%");
                    }


                })
                .whenComplete((context,success) -> {
                    System.out.println("success");

                    //pb.setVisible(false);
                });


        Upload upload = new Upload(temporaryFileHandler);
        upload.setDropAllowed(false);
        upload.setUploadButton(uploadBtn);
        return upload;
    }

    private void SuccesForFileUpload(ProgressBarLabel pb,UploadMetadata metadata, File file, UI ui){
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
            System.out.println(thumbnail.length);

            // 5. SESSION HANDOFF: Store the map so the next view can edit it
            ui.access(() -> {
                VaadinSession.getCurrent().setAttribute("pendingMetadata", metadataMap);
                VaadinSession.getCurrent().setAttribute("pendingThumbnail", thumbnail);

                pb.getProgressBar().setIndeterminate(false);
                ui.navigate(UploadBook.class);
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

    private Button createMenuButton(String text, VaadinIcon icon, String theme) {
        Button btn = new Button(text, icon.create());
        btn.addClassName("home-btn");
        btn.addClassName("btn-" + theme);
        return btn;
    }
}
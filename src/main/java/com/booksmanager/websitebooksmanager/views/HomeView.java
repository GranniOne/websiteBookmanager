package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Utilities.ProgressBarLabel;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadFormat;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.*;
import jakarta.annotation.security.PermitAll;
import org.apache.tomcat.util.http.fileupload.UploadContext;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@PermitAll
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
        final long bytesPerSecondLimit = 250 * 1024000; // 250 KB/s

        // We use a 1-element array so we can 'reset' it inside whenStart if needed
        final long[] startTime = {0L};
        FileUploadCallback successHandler = (metadata, file) -> {


            if(metadata.contentType().equals("application/pdf")){

            }

            pb.getProgressBar().setValue(0);
            pb.getProgressBar().setIndeterminate(true);
            pb.getProgressBarLabelText().setText("Archiving to cloud...");
            System.out.println("Archiving to cloud...");

            UI ui = UI.getCurrent();


            CompletableFuture.runAsync(() -> {
                SuccessForFileUpload(pb, metadata, file, ui);
            });
            //



        };


        TemporaryFileUploadHandler temporaryFileHandler = UploadHandler.toTempFile(successHandler);


        temporaryFileHandler
                .whenStart((starthandler) -> {
                    pb.getProgressBarLabelText().setText(starthandler.fileName());
                    pb.setVisible(true);
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
                    if (total > 0) {
                        double progress = (double) transferred / total;

                        // Use the context UI to ensure thread safety
                        context.getUI().access(() -> {
                            pb.getProgressBar().setValue(progress); // Use the 0.0-1.0 range
                        });

                        System.out.println("Progress: " + (int)(progress * 100) + "%");
                    }


                },bytesPerSecondLimit / 50)
                .whenComplete((context,success) -> {
                    Notification notification = success ? createSubmitSuccess() : createReportError();
                    notification.open();


                });


        Upload upload = new Upload(temporaryFileHandler);
        upload.setAcceptedFileTypes("application/pdf", ".pdf");
        upload.setDropAllowed(false);
        upload.setUploadButton(uploadBtn);
        return upload;
    }

    private void SuccessForFileUpload(ProgressBarLabel pb, UploadMetadata metadata, File file, UI ui){
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

    private Button createMenuButton(String text, VaadinIcon icon, String theme) {
        Button btn = new Button(text, icon.create());
        btn.addClassName("home-btn");
        btn.addClassName("btn-" + theme);
        return btn;
    }
    public Notification createSubmitSuccess() {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.SUCCESS);

        Icon icon = VaadinIcon.CHECK_CIRCLE.create();

        Button viewBtn = new Button("View");
        Button closeButton = new CloseButton();
        HorizontalLayout layout = new HorizontalLayout(icon,
                new Text("Application submitted!"));
        layout.addToEnd(viewBtn, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setMinWidth("350px");

        notification.add(layout);

        notification.setPosition(Notification.Position.TOP_CENTER);

        return notification;
    }
    public Notification createReportError() {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.ERROR);

        Icon icon = VaadinIcon.WARNING.create();
        Button retryBtn = new Button("Retry");
        Button closeButton = new CloseButton();

        HorizontalLayout layout = new HorizontalLayout(icon,
                new Text("Failed to generate report!"));
        layout.addToEnd(retryBtn, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setMinWidth("350px");

        notification.add(layout);
        notification.setPosition(Notification.Position.TOP_CENTER);

        return notification;
    }

    public class CloseButton extends Button {
        public CloseButton() {
            super(new Icon("lumo", "cross"));
            setAriaLabel("Close");
            addClickListener(e -> findAncestor(Notification.class).close());
        }
    }
}
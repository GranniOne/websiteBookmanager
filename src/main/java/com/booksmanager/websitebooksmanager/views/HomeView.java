package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.Utilities.MyTransferProgressListener;
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
import com.vaadin.flow.server.streams.*;
import org.jspecify.annotations.NonNull;

import java.util.Map;

@StyleSheet("styles.css")
@Route("")
public class HomeView extends Div {

    public HomeView() {
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
        ProgressBar pb = new ProgressBar();
        pb.setVisible(true);
        add(pb); // Add to layout

        // 1. Initialize constants and state before the handler
        final long bytesPerSecondLimit = 250 * 102400; // 250 KB/s
        // We use a 1-element array so we can 'reset' it inside whenStart if needed
        final long[] startTime = {0L};
        FileUploadCallback successHandler = (metadata, file) -> {

            System.out.printf("File saved to: %s%n", file.getAbsolutePath());
            UI.getCurrent().navigate(UploadBook.class,QueryParameters.simple(Map.of("file", file.getAbsolutePath())));

        };


        TemporaryFileUploadHandler temporaryFileHandler = UploadHandler.toTempFile(successHandler);

        temporaryFileHandler
                .whenStart((starthandler) -> {
                    pb.setMin(0);
                    pb.setMax(starthandler.contentLength());
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

                    double progress = (double) transferred / total;
                    pb.setValue(transferred);
                    System.out.println("Progress: " + (int)(progress * 100) + "% (" + transferred + " bytes)");


                })
                .whenComplete((context,success) -> {
                    System.out.println("success");
                    pb.setVisible(false);
                });


        Upload upload = new Upload(temporaryFileHandler);
        upload.setDropAllowed(false);
        upload.setUploadButton(uploadBtn);
        return upload;
    }

    private Button createMenuButton(String text, VaadinIcon icon, String theme) {
        Button btn = new Button(text, icon.create());
        btn.addClassName("home-btn");
        btn.addClassName("btn-" + theme);
        return btn;
    }
}
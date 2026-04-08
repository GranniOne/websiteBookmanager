package com.booksmanager.websitebooksmanager.views;


import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.*;
import org.springframework.beans.factory.annotation.Value;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.time.ZoneId;
import java.util.*;

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

        Div div = new Div();
        div.getStyle().setFlexDirection(Style.FlexDirection.COLUMN).setDisplay(Style.Display.FLEX).setBackground("#ffeaea");

        // Container for our inputs so we can access them later
        Map<String, HasValue<?, ?>> fieldRegistry = new HashMap<>();

        metadataMap.forEach((key, value) -> {
            if (key.equals("outline")) return; // Don't make the TOC editable in a text field

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Component inputField;

            // Determine the type of input based on the key or value type
            if (value instanceof Calendar || key.contains("Date")) {
                DatePicker datePicker = new DatePicker(key);
                if (value instanceof Calendar) {
                    // Convert Calendar to LocalDate for the DatePicker
                    datePicker.setValue(((Calendar) value).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate());
                }
                inputField = datePicker;
            } else if (value instanceof Integer || key.equals("pages")) {
                NumberField numberField = new NumberField(key);
                numberField.setValue(value != null ? ((Integer) value).doubleValue() : 0.0);
                inputField = numberField;
            } else if (value instanceof String[]) {
                TextField textField = new TextField(key);
                String joined = String.join(", ", (String[]) value);
                textField.setValue(joined);
                textField.setPlaceholder("Separate with commas...");
                inputField = textField;


            } else {
                TextField textField = new TextField(key);
                textField.setValue(value != null ? value.toString() : "");
                textField.setWidthFull();
                inputField = textField;
            }

            fieldRegistry.put(key, (HasValue<?, ?>) inputField);
            div.add(inputField);
        });

        add(div);


        // 3. PATHING: Define the cloud folder based on discovery
        String folderKey = "books/" + (String) metadataMap.get("title") + "/";
        String bucket = "bookmanager";


        // 4. GENERATION
        String jsonMetadata = cloudStorageService.convertMapToJson(metadataMap);
        byte[] thumbnail = cloudStorageService.generateThumbnailFromPath(securePath);

        Image image  = new Image(thumbnail,"cover image");
        add(image);






        /*        // 5.UPLOADS
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


         */




        // Now the UI knows!
        Notification.show("Archived: " + metadataMap.get("title"));

    }
}

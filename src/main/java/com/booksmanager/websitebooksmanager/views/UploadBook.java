package com.booksmanager.websitebooksmanager.views;


import com.booksmanager.websitebooksmanager.CloudFlare.CloudStorageService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.*;


import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Route("upload")
public class UploadBook extends Div implements HasUrlParameter<String> {
    final CloudStorageService cloudStorageService;
    UploadBook(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;

    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        this.removeAll();

        List<String> pathList = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .get("file");

        Path pathFromUrl = Paths.get(pathList.get(0));

        // The UI doesn't know the title yet!
        Map<String, Object> result = cloudStorageService.archiveBookSuite(
                pathFromUrl,
                "programming",
                "intermediate"
        );

        // Now the UI knows!
        Notification.show("Archived: " + result.get("title"));

    }
}

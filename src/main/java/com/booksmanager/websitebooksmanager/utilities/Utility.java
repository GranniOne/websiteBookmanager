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



}

package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.EmbedPdfViewer;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import software.amazon.awssdk.core.ResponseInputStream;

@PermitAll
@Route("/books/:bookDirectory/:book")
public class SelectedBookView extends Div implements BeforeEnterObserver {

    final CloudflareR2Client cloudflareR2Client;
    final CloudFlareService cloudflareService;

    SelectedBookView(CloudflareR2Client cloudflareR2Client, CloudFlareService cloudflareService) {
        this.getStyle().setHeight("100%").setWidth("100%");
        this.cloudflareR2Client = cloudflareR2Client;
        this.cloudflareService = cloudflareService;
        // Fill the full viewport
        this.setWidthFull();
        this.setHeightFull();

        // Remove borders and scrolling
        this.getStyle()
                .set("border", "none")
                .set("overflow", "hidden")
                .set("display", "flex").setJustifyContent(Style.JustifyContent.CENTER); // ensures iframe behaves like a block elemen
    }



    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        RouteParameters bookId= beforeEnterEvent.getRouteParameters();
        String book = bookId.get("book").orElse("");
        String directory = bookId.get("bookDirectory").orElse("");
        // Point to our NEW Spring Controller endpoint
        // We encode the parts so spaces ("%20") don't break the URL

        String src = "/api/pdf/" + directory + "/" + book;
        add(new EmbedPdfViewer(src));




    }



}

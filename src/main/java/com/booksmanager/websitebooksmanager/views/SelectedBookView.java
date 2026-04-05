package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import software.amazon.awssdk.core.ResponseInputStream;


@Route("/books/:bookDirectory/:book")
public class SelectedBookView extends IFrame implements BeforeEnterObserver {

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
                .set("display", "block"); // ensures iframe behaves like a block elemen
    }



    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        RouteParameters bookId= beforeEnterEvent.getRouteParameters();
        String book = bookId.get("book").orElse("");
        String directory = bookId.get("bookDirectory").orElse("");
        String bookKey = ("books" + "/" + directory + "/" + book).replace("%20"," ");
        ResponseInputStream test = cloudflareR2Client.getObjectFromR2(bookKey);
        // Build iframe pointing to Spring REST endpoint
        String src = "/r2/stream/" + directory + "/" + book;
        this.setSrc(src);










    }


}

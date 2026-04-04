package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;


@Route("/:bookRoot/:bookDirectory/:book")
public class SelectedBookView extends Div implements BeforeEnterObserver {

    final CloudflareR2Client cloudflareR2Client;
    final CloudFlareService cloudflareService;

    SelectedBookView(CloudflareR2Client cloudflareR2Client, CloudFlareService cloudflareService) {
        this.getStyle().setHeight("100%").setWidth("100%");
        this.cloudflareR2Client = cloudflareR2Client;
        this.cloudflareService = cloudflareService;
    }



    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        RouteParameters bookId= beforeEnterEvent.getRouteParameters();
        String book = bookId.get("book").orElse("");
        String directory = bookId.get("bookDirectory").orElse("");
        String bookRoot = bookId.get("bookRoot").orElse("");
        String bookKey = bookRoot + "/" + directory + "/" + book;
        if(!cloudflareService.signedUrls.containsKey(bookKey)){
            cloudflareService.createPresignedurlBooks(bookKey);
            System.out.println(cloudflareService.signedUrls.get(bookKey));
        }

        System.out.println("did not create a new key \n"+cloudflareService.signedUrls.get(bookKey));
        IFrame pdfFrame = new IFrame();
        pdfFrame.setWidth("100%");
        pdfFrame.setHeight("100%");
        pdfFrame.getElement().setAttribute("src", cloudflareService.signedUrls.get(bookKey));
        add(pdfFrame);
    }
}

package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.Map;

@Route("/books/:bookDirectory")
public class BookDirectory extends Div implements BeforeEnterObserver {
    final CloudflareR2Client cloudflareR2Client;
    BookDirectory(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        RouteParameters bookId= beforeEnterEvent.getRouteParameters();

        String directory = bookId.get("bookDirectory").orElse("");

        add(new Button("view pdf", event -> {
            RouteParameters bookParameter = new RouteParameters(
                    Map.of("bookDirectory", directory,"book",directory + ".pdf")
            );
            UI.getCurrent().navigate(SelectedBookView.class,bookParameter);
        }));



    }
}

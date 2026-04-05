package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.io.IOException;
import java.util.Map;
@StyleSheet("cardstyle.css")
@Route("/books")
public class BookRoot extends Div {

    private final CloudflareR2Client cloudflareR2Client;
    private final CloudFlareService cloudflareService;
    private final Div cardHolder = new Div();
    BookRoot(CloudflareR2Client cloudflareR2Client, CloudFlareService  cloudflareService) {
        this.cloudflareR2Client = cloudflareR2Client;
        this.cloudflareService = cloudflareService;
        setClassName("page");
        cardHolder.setClassName("booksview");
        cloudflareR2Client.listObjects("bookmanager").forEach(book -> {


            if(book.key().contains(".pdf")){

                String directory = book.key().substring(0, book.key().lastIndexOf("/"));
                cloudflareR2Client.listObjectsFromDirectory("bookmanager",directory).forEach(practical -> {
                    if(practical.key().contains(".jpg")){
                        CardLayout card = null;
                        try {
                            card = new CardLayout(practical.key().substring(practical.key().indexOf("/") + 1,practical.key().lastIndexOf("/")),cloudflareR2Client.getObjectFromR2(practical.key()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }


                        //ui navigation
                        card.getElement().addEventListener("click", event -> {
                            RouteParameters bookParameter = new RouteParameters(
                                    Map.of("bookDirectory", directory.substring(directory.indexOf("/") + 1))
                            );
                            UI.getCurrent().navigate(BookDirectory.class,bookParameter);
                        });
                        cardHolder.add(card);

                    }


                });

            }

        });
        add(cardHolder);


    }


}

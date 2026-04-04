package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.CloudFlare.DataTypes;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.Arrays;
import java.util.Map;
@StyleSheet("cardstyle.css")
@Route("/books")
public class BookRoot extends Div {

    BookRoot(CloudflareR2Client cloudflareR2Client, CloudFlareService  cloudflareService) {
        setClassName("page");
        Div cardHolder = new Div();
        cardHolder.setClassName("booksview");

        cloudflareR2Client.listObjectsFromDirectory("bookmanager","books/Practical Forensic Imaging Securing Digital Evidence").forEach(s3Object -> System.out.println(s3Object.key()));



        cloudflareR2Client.listObjects("bookmanager").forEach(book -> {
            if(book.key().contains(".pdf")){
                String directory = book.key().substring(0, book.key().lastIndexOf("/"));
                cloudflareR2Client.listObjectsFromDirectory("bookmanager",directory).forEach(practical -> {
                    if(practical.key().contains(".jpg")){
                        String[] directoryArray = practical.key().split("/");
                        cloudflareService.createPresignedurlBooks(directoryArray, DataTypes.COVER,directory);
                        CardLayout card = new CardLayout(practical.key().substring(practical.key().indexOf("/") + 1,practical.key().lastIndexOf("/")),cloudflareService.signedUrls.get(directory).getPresignedCover());
                        card.getElement().addEventListener("click", event -> {
                            RouteParameters bookParameter = new RouteParameters(
                                    //Map.of("bookRoot", test[0],"bookDirectory", test[1])
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

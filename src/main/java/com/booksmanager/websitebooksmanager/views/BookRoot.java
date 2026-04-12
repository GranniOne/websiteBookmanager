package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudFlareService;
import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.vaadin.flow.component.InputEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import org.jspecify.annotations.NonNull;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@StyleSheet("cardstyle.css")
@Route("/books")
public class BookRoot extends Div {

    private final CloudflareR2Client cloudflareR2Client;
    private final Div cardHolder = new Div();
    private Map<String, CardLayout> cardMap = new  HashMap<>();
    public BookRoot(CloudflareR2Client cloudflareR2Client, CloudFlareService cloudflareService) {
        this.cloudflareR2Client = cloudflareR2Client;

        setClassName("gallery-page-wrapper");
        cardHolder.setClassName("gallery-island");


        // 1. Single call to list the whole bucket
        var allObjects = cloudflareR2Client.listObjects("bookmanager");

        Map<String, String> directoryToCover = new HashMap<>();
        List<String> pdfPaths = new ArrayList<>();

        allObjects.forEach(obj -> {
            String key = obj.key();
            if (key.endsWith(".jpg")) {
                String dir = key.substring(0, key.lastIndexOf("/") + 1);
                directoryToCover.put(dir, key);
            } else if (key.endsWith(".pdf")) {
                String dir = key.substring(0, key.lastIndexOf("/") + 1);
                pdfPaths.add(dir);
            }
        });

        // 2. Build the shelf using image URLs
        for (String dir : pdfPaths) {
            String coverKey = directoryToCover.get(dir);

            if (coverKey != null) {
                String bookName = dir.substring(dir.indexOf("/") + 1).replace("/", "");

                // Create the URL to our proxy controller
                String imageUrl = "/api/books/cover?key=" + java.net.URLEncoder.encode(coverKey, java.nio.charset.StandardCharsets.UTF_8);

                // Pass the URL string to the card
                CardLayout card = new CardLayout(bookName, imageUrl);
                cardMap.put(bookName,card);
                System.out.println("hellooo");
                card.getElement().addEventListener("click", event -> {
                    System.out.println("Clicked on " + coverKey);
                    String routePath = dir.substring(dir.indexOf("/") + 1);
                    if(routePath.endsWith("/")) routePath = routePath.substring(0, routePath.length()-1);
                    UI.getCurrent().navigate(BookDirectory.class, new RouteParameters("bookDirectory", routePath));
                });

                cardHolder.add(card);
            }
        }
        TextField field = getTextField();
        add(field);
        add(cardHolder);
    }

    private @NonNull TextField getTextField() {
        TextField field = new TextField();
        field.setValueChangeMode(ValueChangeMode.TIMEOUT);
        field.setValueChangeTimeout(300);
        field.setClassName("card-gallery-search-field");
        field.setMaxHeight("30px");
        field.addValueChangeListener(event -> {
            cardMap.forEach((key, card) -> {
                card.setVisible(key.toLowerCase().contains(field.getValue().toLowerCase()));
            });
        });
        return field;
    }
}

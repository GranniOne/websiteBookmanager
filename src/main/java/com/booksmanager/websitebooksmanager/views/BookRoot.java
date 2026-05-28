package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.booksmanager.websitebooksmanager.Service.BookService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import org.jsoup.helper.Regex;
import org.jspecify.annotations.NonNull;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@PermitAll
@StyleSheet("cardstyle.css")
@Route("/books")
public class BookRoot extends VerticalLayout {

    private final CloudflareR2Client cloudflareR2Client;

    private final HorizontalLayout cardHolder = new HorizontalLayout();
    private final Map<String, CardLayout> cardMap = new HashMap<>();
    private final BookService bookService;

    private final Pattern BOOK_PATTERN =
            Pattern.compile("(epubs|books)/([^/]+)/");

    private final Map<String, String> coverMap = new HashMap<>();

    public BookRoot(CloudflareR2Client cloudflareR2Client, BookService  bookService) {
        this.cloudflareR2Client = cloudflareR2Client;
        this.bookService = bookService;

        setClassName("gallery-page-wrapper");
        cardHolder.setClassName("gallery-island");

        List<BookInterface> books = bookService.getAllBooks();

        books.forEach(book -> {


        });
        /*
        // 1. Load everything once
        List<S3Object> allObjects = cloudflareR2Client.listObjects("bookmanager");

        // 2. Extract unique book prefixes (identity layer)
        Set<String> books = new HashSet<>();

        for (S3Object obj : allObjects) {
            Matcher matcher = BOOK_PATTERN.matcher(obj.key());

            if (matcher.find()) {
                books.add(matcher.group()); // full prefix: epubs/book/
            }
        }

        // 3. Build cover map (prefix → cover image)
        for (S3Object obj : allObjects) {
            String key = obj.key();

            if (key.endsWith(".jpg") || key.endsWith(".png")) {

                Matcher matcher = BOOK_PATTERN.matcher(key);

                if (matcher.find()) {
                    String bookPrefix = matcher.group(); // epubs/book/
                    coverMap.putIfAbsent(bookPrefix, key);
                }
            }
        }

         */
        /*

        // 4. Build UI cards from book prefixes
        for (String bookPrefix : books) {

            String coverKey = coverMap.get(bookPrefix);

            // normalize book name
            String bookName = bookPrefix
                    .replace("epubs/", "")
                    .replace("books/", "")
                    .replace("/", "");
            String imageUrl = "";

            if(bookPrefix.contains("epubs")) {
                imageUrl = "/api/epubs/" + bookName + "/cover";
            }else if(bookPrefix.contains("books")) {
                imageUrl = "/api/books/" + bookName + "/cover";
            }


            CardLayout card = new CardLayout(bookName, imageUrl);
            cardMap.put(bookName, card);

            if(bookPrefix.contains("epubs")) {
                // navigation
                card.getElement().addEventListener("click", event -> {
                    UI.getCurrent().navigate(
                            UploadBook.class,
                            new RouteParameters("BookId", bookName)
                    );
                });
            }
            if(bookPrefix.contains("books")) {
                // navigation
                card.getElement().addEventListener("click", event -> {
                    UI.getCurrent().navigate(
                            BookDirectory.class,
                            new RouteParameters("bookDirectory", bookName)
                    );
                });
            }


            cardHolder.add(card);
        }

        // 5. Optional UI elements
        TextField field = getTextField();

        add(field);
        add(cardHolder);

         */


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




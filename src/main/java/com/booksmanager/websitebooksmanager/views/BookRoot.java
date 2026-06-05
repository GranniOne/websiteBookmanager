package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import com.booksmanager.websitebooksmanager.Entities.BookType;
import com.booksmanager.websitebooksmanager.Entities.EpubBook;
import com.booksmanager.websitebooksmanager.Entities.PdfBook;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.booksmanager.websitebooksmanager.Service.BookService;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.security.PermitAll;
import org.antlr.v4.runtime.misc.Triple;
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
    Map<CardLayout,BookInterface> searchableList = new HashMap<>();
    private final HorizontalLayout cardHolder = new HorizontalLayout();
    VerticalLayout mainLayout = new VerticalLayout();
    CheckboxGroup<BookType> checkboxGroup = new CheckboxGroup<>();
    TextField field = new TextField();
    private final Pattern BOOK_PATTERN =
            Pattern.compile("(epubs|books)/([^/]+)/");

    private final Map<String, String> coverMap = new HashMap<>();

    public BookRoot(CloudflareR2Client cloudflareR2Client, BookService  bookService) {
        this.cloudflareR2Client = cloudflareR2Client;

        setClassName("gallery-page-wrapper");
        cardHolder.setClassName("gallery-island");
        mainLayout.setClassName("gallery-island-main-layout");
        List<BookInterface> books = bookService.getAllBooks();
        try{
        books.forEach(book -> {

            String imageUrl = "";


            if(book.getBookType().equals(BookType.EPUB)) {
                if(((EpubBook)book).getCoverhref() != null){
                    imageUrl = "/api/cover/" + book.StripFileName() + "/" + ((EpubBook)book).getCoverhref();
                }else{
                    imageUrl = "/images/placeholder BookCover.jpg";
                }
            }else if(book.getBookType().equals(BookType.PDF)) {
                imageUrl = "/api/books/" + book.StripFileName() + "/cover";
            }


            CardLayout card = new CardLayout(book.StripFileName(), imageUrl);


            if(book.getBookType().equals(BookType.EPUB)) {
                // navigation
                card.getElement().addEventListener("click", event -> {
                    UI.getCurrent().navigate(
                            UploadBook.class,
                            new RouteParameters("BookId", book.getTitle())
                    );
                });
            }
            if(book.getBookType().equals(BookType.PDF)) {
                // navigation
                card.getElement().addEventListener("click", event -> {
                    UI.getCurrent().navigate(
                            BookDirectory.class,
                            new RouteParameters("bookDirectory", book.StripFileName())
                    );
                });
            }
            card.ChangeVisibility(true);
            searchableList.put(card,book);
            cardHolder.add(card);

        });} catch (Exception e) {
            throw new RuntimeException(e);
        }



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
        */
        // 5. Optional UI elements
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setClassName("gallery-page-filtering-layout");
        CheckboxGroup<BookType> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setItems(BookType.PDF, BookType.EPUB);
        checkboxGroup.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);
        checkboxGroup.addValueChangeListener(event -> {
            applyFilters(
                    event.getValue(),
                    field.getValue().toLowerCase()
            );
        });
        checkboxGroup.getStyle().setMinWidth("145px");
        TextField field = getTextField();

        horizontalLayout.add(field,checkboxGroup);
        mainLayout.add(horizontalLayout,cardHolder);
        add(mainLayout);






    }
    private void applyFilters(Set<BookType> selectedTypes,final String nameFilter) {

        searchableList.forEach((card, book) -> {
            System.out.println(book.getBookType());
            boolean matchesType =
                    selectedTypes.isEmpty() ||
                            selectedTypes.contains(book.getBookType());

            boolean matchesName =
                    nameFilter.isBlank() ||
                            book.getTitle().toLowerCase().contains(nameFilter);

            card.ChangeVisibility(matchesType && matchesName);
        });
    }
    private @NonNull TextField getTextField() {
        field.setValueChangeMode(ValueChangeMode.TIMEOUT);
        field.setValueChangeTimeout(300);
        field.setClassName("card-gallery-search-field");
        field.addValueChangeListener(event -> {
            applyFilters(
                    checkboxGroup.getValue(),
                    event.getValue().toLowerCase()
            );
        });
        return field;
    }


}




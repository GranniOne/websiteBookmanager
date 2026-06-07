package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import com.booksmanager.websitebooksmanager.Entities.BookType;
import com.booksmanager.websitebooksmanager.Entities.EpubBook;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.booksmanager.websitebooksmanager.Service.BookService;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@PermitAll
@StyleSheet("cardstyle.css")
@Route("/books")
public class BookRoot extends VerticalLayout {

    private final CloudflareR2Client cloudflareR2Client;
    private final BookService bookService;

    // View Components & State Variables
    private final Div cardHolder = new Div();
    private final Div scrollAnchor = new Div();
    private final VerticalLayout mainLayout = new VerticalLayout();
    private final CheckboxGroup<BookType> checkboxGroup = new CheckboxGroup<>();
    private final TextField field = new TextField();

    private int currentOffset = 0;
    private final int PAGE_SIZE = 24;
    private boolean isFilterActive = false;

    public BookRoot(CloudflareR2Client cloudflareR2Client, BookService bookService) {
        this.cloudflareR2Client = cloudflareR2Client;
        this.bookService = bookService;

        setClassName("gallery-page-wrapper");
        cardHolder.setClassName("gallery-island");
        mainLayout.setClassName("gallery-island-main-layout");

        // Set up filtering inputs layout
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setClassName("gallery-page-filtering-layout");

        checkboxGroup.setItems(BookType.PDF, BookType.EPUB);
        checkboxGroup.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);
        checkboxGroup.addValueChangeListener(event -> executeFilteringReload());
        checkboxGroup.getStyle().setMinWidth("145px");

        setupSearchTextField();

        horizontalLayout.add(field, checkboxGroup);
        mainLayout.add(horizontalLayout, cardHolder);
        add(mainLayout);

        // Load Initial Batch of Books
        loadNextChunk();

        // Setup Intersection Scroll Anchor
        scrollAnchor.setId("infinite-scroll-anchor");
        scrollAnchor.getStyle().set("height", "10px").set("width", "100%").set("clear", "both");
        mainLayout.add(scrollAnchor);

        // Bind JavaScript intersection observer
        setupInfiniteScrollListener();
    }

    private void loadNextChunk() {
        List<BookInterface> chunk;

        if (isFilterActive) {
            chunk = getFilteredBackendData(currentOffset, PAGE_SIZE);
        } else {
            chunk = bookService.getBooksPaged(currentOffset, PAGE_SIZE);
        }

        if (chunk.isEmpty()) {
            return;
        }

        try {
            for (BookInterface book : chunk) {
                String imageUrl = "";

                if (book.getBookType().equals(BookType.EPUB)) {
                    if (((EpubBook) book).getCoverHref() != null) {
                        imageUrl = "/api/cover/" + book.StripFileName() + "/" + ((EpubBook) book).getCoverHref();
                    } else {
                        imageUrl = "/images/placeholder BookCover.jpg";
                    }
                } else if (book.getBookType().equals(BookType.PDF)) {
                    imageUrl = "/api/books/" + book.StripFileName() + "/cover";
                }

                String cardTitle = book.StripFileName();
                if (book.getBookType().equals(BookType.EPUB) && ((EpubBook) book).getOfficialTitle() != null) {
                    cardTitle = ((EpubBook) book).getOfficialTitle();
                }

                CardLayout card = new CardLayout(cardTitle, imageUrl);

                if (book.getBookType().equals(BookType.EPUB)) {
                    card.getElement().addEventListener("click", event -> {
                        UI.getCurrent().navigate(UploadBook.class, new RouteParameters("BookId", book.getTitle()));
                    });
                } else if (book.getBookType().equals(BookType.PDF)) {
                    card.getElement().addEventListener("click", event -> {
                        UI.getCurrent().navigate(BookDirectory.class, new RouteParameters("bookDirectory", book.StripFileName()));
                    });
                }

                card.ChangeVisibility(true);
                cardHolder.add(card);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        currentOffset += PAGE_SIZE;
    }

    private void setupInfiniteScrollListener() {
        UI.getCurrent().getPage().executeJs(
                "const observer = new IntersectionObserver((entries) => {" +
                        "  entries.forEach(entry => {" +
                        "    if (entry.isIntersecting) {" +
                        "      $0.$server.onScrollToBottom();" +
                        "    }" +
                        "  });" +
                        "}, { rootMargin: '300px' });" + // Start loading 300px before user arrives at bottom
                        "const anchor = document.getElementById('infinite-scroll-anchor');" +
                        "if (anchor) { observer.observe(anchor); }"
                , getElement()
        );
    }

    @ClientCallable
    public void onScrollToBottom() {
        long totalCount = isFilterActive ? getTotalFilteredCount() : bookService.getTotalBookCount();
        if (currentOffset < totalCount) {
            loadNextChunk();
        }
    }

    private void executeFilteringReload() {
        Set<BookType> selectedTypes = checkboxGroup.getValue();
        String nameFilter = field.getValue().toLowerCase();

        // Check if a filter criteria is actively running
        isFilterActive = !selectedTypes.isEmpty() || !nameFilter.isBlank();

        // Reset view states
        cardHolder.removeAll();
        currentOffset = 0;

        // Populate filtered entries first chunk
        loadNextChunk();
    }

    private List<BookInterface> getFilteredBackendData(int offset, int limit) {
        Set<BookType> selectedTypes = checkboxGroup.getValue();
        String nameFilter = field.getValue().toLowerCase();

        List<BookInterface> allFiltered = getCombinedFilteredCollection(selectedTypes, nameFilter);

        if (offset >= allFiltered.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(offset + limit, allFiltered.size());
        return allFiltered.subList(offset, toIndex);
    }

    private long getTotalFilteredCount() {
        return getCombinedFilteredCollection(checkboxGroup.getValue(), field.getValue().toLowerCase()).size();
    }

    private List<BookInterface> getCombinedFilteredCollection(Set<BookType> selectedTypes, String nameFilter) {
        // Fall back on base getters
        List<BookInterface> allBooks = new ArrayList<>(bookService.getBooksPaged(0, bookService.getTotalBookCount()));

        return allBooks.stream().filter(book -> {
            boolean matchesType = selectedTypes.isEmpty() || selectedTypes.contains(book.getBookType());
            boolean matchesName = nameFilter.isBlank() || book.getTitle().toLowerCase().contains(nameFilter);
            return matchesType && matchesName;
        }).collect(Collectors.toList());
    }

    private void setupSearchTextField() {
        field.setValueChangeMode(ValueChangeMode.TIMEOUT);
        field.setValueChangeTimeout(300);
        field.setClassName("card-gallery-search-field");
        field.addValueChangeListener(event -> executeFilteringReload());
    }
}
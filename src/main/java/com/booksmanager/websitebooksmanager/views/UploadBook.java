package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.epub.EpubMetadataService;
import com.booksmanager.websitebooksmanager.epub.EpubMetadataService.EpubPageEntry;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@StyleSheet("EpubReader.css")
@PermitAll
@Route("read-book/:BookId")
public class UploadBook extends VerticalLayout implements HasUrlParameter<String> {

    private static final Logger log = LoggerFactory.getLogger(UploadBook.class);

    private final CloudflareR2Client cloudflareR2Client;
    private final EpubMetadataService epubMetadataService;

    private String currentActiveFullPath = "";
    private List<EpubPageEntry> cachedSpinePages;

    public UploadBook(CloudflareR2Client cloudflareR2Client, EpubMetadataService epubMetadataService) {
        this.epubMetadataService = epubMetadataService;
        this.cloudflareR2Client = cloudflareR2Client;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        System.out.println(event.getRouteParameters().get("BookId").orElse(null));
        removeAll();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        //String bookKey = "book-of-vaadin-vaadin7";
        String bookKey = event.getRouteParameters().get("BookId").orElse(null);
        String expectedPrefix = "epubs/" + bookKey + "/";

        // 1. Setup UI Elements
        IFrame iframe = new IFrame();
        iframe.setSizeFull();
        iframe.getElement().setAttribute("frameborder", "0");

        // 1b. Setup a Clean Navigation Style Grid
        Grid<EpubPageEntry> grid = new Grid<>();
        grid.setHeightFull();
        grid.setWidth("280px");

        // Add our custom class name matching our CSS stylesheet rules
        grid.addClassName("epub-reader-sidebar");

        // Strip the standard borders and row dividers via built-in theme variants
        grid.addThemeVariants(
                GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_NO_ROW_BORDERS
        );

        // Completely removes the header container row structure
        grid.getElement().setAttribute("theme", "no-header");

        // 2. Single Component Column acting as a Sidebar Item
        grid.addComponentColumn(page -> {
            String visibleTitle = page.getId();
            if (visibleTitle.startsWith("idp") || visibleTitle.equals("htmltoc")) {
                visibleTitle = "Chapter " + (cachedSpinePages.indexOf(page) + 1);
            }

            Span itemLabel = new Span(visibleTitle);
            itemLabel.addClassName("sidebar-pill"); // Binds directly to our rounded CSS rules

            return itemLabel;
        }).setFlexGrow(1);

        // 2. DIRECTION 1: Grid Click -> Updates IFrame
        grid.addSelectionListener(selectionEvent -> {
            selectionEvent.getFirstSelectedItem().ifPresent(selectedPage -> {
                if (selectedPage.getFullPath().equals(currentActiveFullPath)) {
                    return;
                }

                currentActiveFullPath = selectedPage.getFullPath();
                String relativePathInsideBook = currentActiveFullPath.substring(expectedPrefix.length());
                String apiRoute = "/api/epub/" + bookKey + "/" + relativePathInsideBook;

                log.info("Grid selection changed -> Navigating iframe to: {}", apiRoute);
                iframe.setSrc(apiRoute);
            });
        });

        // 3. DIRECTION 2: Safe Attach-Guarded Listener Execution
        iframe.addAttachListener(attachEvent -> {
            iframe.getElement().executeJs(
                    "const iframe = this; " +
                            "iframe.addEventListener('load', function() { " +
                            "    try { " +
                            "        const currentPath = iframe.contentWindow.location.pathname; " +
                            "        if (currentPath && currentPath.startsWith('/api/epub/')) { " +
                            "            iframe.dispatchEvent(new CustomEvent('internal-nav', { " +
                            "                detail: { path: currentPath } " +
                            "            })); " +
                            "        } " +
                            "    } catch(e) { " +
                            "        console.warn('Iframe tracking restricted:', e); " +
                            "    } " +
                            "});"
            );
        });

        // Register the DOM communication back-channel
        iframe.getElement().addEventListener("internal-nav", domEvent -> {
            String rawApiPath = domEvent.getEventData().get("event.detail.path").asString();
            String targetLookupPrefix = "/api/epub/" + bookKey + "/";

            if (rawApiPath.startsWith(targetLookupPrefix)) {
                String foundFullPath = expectedPrefix + rawApiPath.substring(targetLookupPrefix.length());

                if (!foundFullPath.equals(currentActiveFullPath)) {
                    this.currentActiveFullPath = foundFullPath;
                    log.info("Internal iframe navigation detected! Syncing grid layout to: {}", foundFullPath);

                    syncGridSelectionToPath(foundFullPath, grid);
                }
            }
        }).addEventData("event.detail.path");

        // 4. Assemble Layout
        HorizontalLayout workspace = new HorizontalLayout(grid, iframe);
        workspace.setSizeFull();
        workspace.setFlexGrow(1, iframe);
        add(workspace);

        // 5. Initial Data Binding
        try {
            cachedSpinePages = epubMetadataService.findTableOfContentsPath(bookKey);
            grid.setItems(cachedSpinePages);

            if (!cachedSpinePages.isEmpty()) {
                EpubPageEntry initialPage = cachedSpinePages.get(0);

                this.currentActiveFullPath = initialPage.getFullPath();
                grid.select(initialPage);

                String relativePathInsideBook = currentActiveFullPath.substring(expectedPrefix.length());
                iframe.setSrc("/api/epub/" + bookKey + "/" + relativePathInsideBook);
            }
        } catch (Exception e) {
            log.error("Failed to compile layout sync mapping tracking engines", e);
        }
    }

    private void syncGridSelectionToPath(String path, Grid<EpubPageEntry> grid) {
        if (cachedSpinePages == null) return;

        Optional<EpubPageEntry> match = cachedSpinePages.stream()
                .filter(page -> page.getFullPath().equals(path))
                .findFirst();

        match.ifPresent(page -> {
            grid.select(page);
            grid.scrollToIndex(cachedSpinePages.indexOf(page));
        });
    }
}
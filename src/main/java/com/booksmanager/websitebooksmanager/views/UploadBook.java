package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.epub.EpubHandler;
import com.booksmanager.websitebooksmanager.epub.EpubPage;
import com.vaadin.flow.component.combobox.ComboBox;
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

    // Reader State Variables (Defaults)
    private int readerFontSize = 16;
    private String readerBgColor = "#ffffff";
    private String readerTextColor = "#111111";
    private String selectedThemeName = "Light";

    private final CloudflareR2Client cloudflareR2Client;
    private EpubHandler epubHandler;

    private String currentActiveFullPath = "";
    private List<EpubPage> cachedSpinePages;

    IFrame iframe = new IFrame();

    public UploadBook(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        String bookKey = event.getRouteParameters().get("BookId").orElse(null);
        this.epubHandler = new EpubHandler(cloudflareR2Client, bookKey);

        try {
            // Initialize the handler (parses Container and OPF)
            epubHandler.initialize();
            // Fetch the data using your new dedicated tools
            cachedSpinePages = epubHandler.getTableOfContents();
            System.out.println("epubHandler.cachedSpinePages = " + cachedSpinePages);
        } catch (Exception e) {
            log.error("Failed to initialize book handler", e);
        }

        removeAll();

        // 1. Initialize Control Menu & Selectors First (To prevent forward-reference compilation issues)
        HorizontalLayout controlsMenu = new HorizontalLayout();
        controlsMenu.setPadding(true);
        controlsMenu.setSpacing(true);
        controlsMenu.setWidthFull();
        controlsMenu.addClassName("reader-top-toolbar");

        // Font Size Selector
        ComboBox<Integer> fontSizeSelect = new ComboBox<>("Font Size");
        fontSizeSelect.setItems(14, 16, 18, 20, 22, 24, 26);
        fontSizeSelect.setValue(this.readerFontSize); // Bind to global state
        fontSizeSelect.addValueChangeListener(events -> {
            if (events.getValue() != null) {
                this.readerFontSize = events.getValue();
                // Push update to the active DOM frame right away
                iframe.getElement().executeJs(
                        "this.contentDocument.documentElement.style.setProperty('--reader-font-size', '" + this.readerFontSize + "px');"
                );
            }
        });

        // Theme Selector
        ComboBox<String> themeSelect = new ComboBox<>("Theme");
        themeSelect.setItems("Light", "Sepia", "Dark");
        themeSelect.setValue(this.selectedThemeName); // Bind to global state
        themeSelect.addValueChangeListener(events -> {
            if (events.getValue() != null) {
                this.selectedThemeName = events.getValue();

                // Map the theme string to colors and save to state
                if ("Sepia".equals(selectedThemeName)) {
                    this.readerBgColor = "#f4ecd8";
                    this.readerTextColor = "#5b4636";
                } else if ("Dark".equals(selectedThemeName)) {
                    this.readerBgColor = "#121212";
                    this.readerTextColor = "#e0e0e0";
                } else { // Light
                    this.readerBgColor = "#ffffff";
                    this.readerTextColor = "#111111";
                }

                // Force the active document style properties to change
                iframe.getElement().executeJs(
                        "this.contentDocument.documentElement.style.setProperty('--reader-bg-color', '" + this.readerBgColor + "');" +
                                "this.contentDocument.documentElement.style.setProperty('--reader-text-color', '" + this.readerTextColor + "');"
                );
            }
        });

        controlsMenu.add(fontSizeSelect, themeSelect);

        // 2. Set Up IFrame Behavior & Event Handling
        String expectedPrefix = "epubs/" + bookKey + "/";

        iframe.getElement().addEventListener("load", e -> {
            String jsPath = "/api/epub/" + bookKey + "/js/kobo.js";

            String jsCode =
                    // Load pagination script
                    "var s = document.createElement('script');" +
                            "s.src = '" + jsPath + "';" +
                            "s.onload = function() { if(typeof paginate === 'function') paginate(); };" +
                            "this.contentDocument.head.appendChild(s);" +

                            // Inject Dynamic Style Engine using current Java state values
                            "var style = document.createElement('style');" +
                            "style.textContent = `" +
                            ":root {" +
                            "  --reader-font-size: " + readerFontSize + "px;" +
                            "  --reader-line-height: 1.6;" +
                            "  --reader-bg-color: " + readerBgColor + ";" +
                            "  --reader-text-color: " + readerTextColor + ";" +
                            "}" +
                            "html, body, body *, p, span, div, section, article {" +
                            "  font-size: var(--reader-font-size) !important;" +
                            "  line-height: var(--reader-line-height) !important;" +
                            "  background-color: var(--reader-bg-color) !important;" +
                            "  color: var(--reader-text-color) !important;" +
                            "}" +
                            "img { max-width: 100% !important; height: auto !important; }" +
                            "`;" +
                            "this.contentDocument.head.appendChild(style);";

            iframe.getElement().executeJs(jsCode);
        });

        // Safe Attach-Guarded Listener Execution for tracking location
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

        // 3. Setup Content Navigation Sidebar (Grid)
        Grid<EpubPage> grid = new Grid<>();
        grid.setWidth("280px");
        grid.setMinWidth("280px");
        grid.setMaxWidth("280px");
        grid.addClassName("epub-reader-sidebar");

        grid.addThemeVariants(
                GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_NO_ROW_BORDERS
        );
        grid.getElement().setAttribute("theme", "no-header");

        grid.addComponentColumn(page -> {
            Span itemLabel = new Span(page.getTitle());
            itemLabel.addClassName("sidebar-pill");
            return itemLabel;
        }).setFlexGrow(1);

        // Grid Selection Handler -> Updates IFrame
        grid.addSelectionListener(selectionEvent -> {
            selectionEvent.getFirstSelectedItem().ifPresent(selectedPage -> {
                if (selectedPage.getSrc().equals(currentActiveFullPath)) {
                    return;
                }

                currentActiveFullPath = selectedPage.getSrc();
                String relativePathInsideBook = currentActiveFullPath.substring(expectedPrefix.length());
                String apiRoute = "/api/epub/" + bookKey + "/" + relativePathInsideBook;

                log.info("Grid selection changed -> Navigating iframe to: {}", apiRoute);
                iframe.setSrc(apiRoute);
            });
        });

        // Register the DOM communication back-channel for synced selections
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

        // 4. Configure Layout Centering Containers
        HorizontalLayout iframeCenteringWrapper = new HorizontalLayout(iframe);
        iframeCenteringWrapper.setSizeFull();
        iframeCenteringWrapper.setJustifyContentMode(JustifyContentMode.CENTER);
        iframeCenteringWrapper.setAlignItems(Alignment.CENTER);
        iframeCenteringWrapper.addClassName("reader-frame-viewport");

        iframe.setWidth("100%");
        iframe.setMaxWidth("800px"); // Locked down text container boundary lines
        iframe.setHeightFull();
        iframe.getStyle().set("border", "none");

        // 5. Final Root Component Assembly
        this.addClassName("reader-main-layout");
        add(controlsMenu, grid, iframeCenteringWrapper);

        // 6. Initial Book Data Binding Context
        try {
            cachedSpinePages = epubHandler.getTableOfContents();
            grid.setItems(cachedSpinePages);

            if (!cachedSpinePages.isEmpty()) {
                EpubPage initialPage = cachedSpinePages.get(0);
                this.currentActiveFullPath = initialPage.getSrc();
                grid.select(initialPage);

                String relativePathInsideBook = currentActiveFullPath.substring(expectedPrefix.length());
                iframe.setSrc("/api/epub/" + bookKey + "/" + relativePathInsideBook);
            }
        } catch (Exception e) {
            log.error("Failed to compile layout sync mapping tracking engines", e);
        }
    }

    private void syncGridSelectionToPath(String path, Grid<EpubPage> grid) {
        if (cachedSpinePages == null) return;

        Optional<EpubPage> match = cachedSpinePages.stream()
                .filter(page -> page.getSrc().equals(path))
                .findFirst();

        match.ifPresent(page -> {
            grid.select(page);
            grid.scrollToIndex(cachedSpinePages.indexOf(page));
        });
    }
}
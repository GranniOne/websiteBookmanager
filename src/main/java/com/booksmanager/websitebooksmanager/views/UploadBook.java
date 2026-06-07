package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.epub.EpubHandler;
import com.booksmanager.websitebooksmanager.epub.EpubPage;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.Slider;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.Lumo;
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
    private final ValueSignal<Double> iFrameWidth = new ValueSignal<>(800.);


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

        // Ensure the root VerticalLayout fills the screen perfectly without weird margins
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // --- 1. Font Size & Theme Dropdowns ---
        ComboBox<Integer> fontSizeSelect = new ComboBox<>("Font Size");
        fontSizeSelect.setItems(14, 16, 18, 20, 22, 24, 26);
        fontSizeSelect.setValue(this.readerFontSize);
        fontSizeSelect.addValueChangeListener(events -> {
            if (events.getValue() != null) {
                this.readerFontSize = events.getValue();
                iframe.getElement().executeJs(
                        "this.contentDocument.documentElement.style.setProperty('--reader-font-size', '" + this.readerFontSize + "px');"
                );
            }
        });

        ComboBox<String> themeSelect = new ComboBox<>("Theme");
        themeSelect.setItems("Light", "Sepia", "Dark");
        themeSelect.setValue(this.selectedThemeName);
        themeSelect.addValueChangeListener(events -> {
            if (events.getValue() != null) {
                this.selectedThemeName = events.getValue();

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

                iframe.getElement().executeJs(
                        "this.contentDocument.documentElement.style.setProperty('--reader-bg-color', '" + this.readerBgColor + "');" +
                                "this.contentDocument.documentElement.style.setProperty('--reader-text-color', '" + this.readerTextColor + "');"
                );
            }
        });

        // --- 2. Set Up IFrame Behavior ---
        String expectedPrefix = "epubs/" + bookKey + "/";

        iframe.getElement().addEventListener("load", e -> {
            String jsPath = "/api/epub/" + bookKey + "/js/kobo.js";
            String jsCode =
                    "var style = document.createElement('style');" +
                            "style.textContent = `" +
                            "  :root {" +
                            "    --reader-font-size: " + readerFontSize + "px;" +
                            "    --reader-line-height: 1.6;" +
                            "    --reader-bg-color: " + readerBgColor + ";" +
                            "    --reader-text-color: " + readerTextColor + ";" +
                            "  }" +
                            "  /* Target only the top-level body framework */" +
                            "  html, body {" +
                            "    font-size: var(--reader-font-size) !important;" +
                            "    line-height: var(--reader-line-height) !important;" +
                            "    background-color: var(--reader-bg-color) !important;" +
                            "    color: var(--reader-text-color) !important;" +
                            "  }" +
                            "  /* Protect responsive structural spacing styles */" +
                            "  img { max-width: 100% !important; height: auto !important; }" +
                            "`;" +
                            "this.contentDocument.head.appendChild(style);";

            iframe.getElement().executeJs(jsCode);
        });

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

        // --- 3. Setup Sidebar Content Navigation (Grid) ---
        Grid<EpubPage> grid = new Grid<>();
        grid.setWidth("280px");
        grid.setMinWidth("280px");
        grid.setMaxWidth("280px");
        grid.setHeightFull();
        grid.addClassName("epub-reader-sidebar");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.getElement().setAttribute("theme", "no-header");

        grid.addComponentColumn(page -> {
            Span itemLabel = new Span(page.getTitle());
            itemLabel.addClassName("sidebar-pill");
            return itemLabel;
        }).setFlexGrow(1);

        grid.addSelectionListener(selectionEvent -> {
            selectionEvent.getFirstSelectedItem().ifPresent(selectedPage -> {
                if (selectedPage.getSrc().equals(currentActiveFullPath)) {
                    return;
                }
                currentActiveFullPath = selectedPage.getSrc();
                String relativePathInsideBook = currentActiveFullPath.substring(expectedPrefix.length());
                String apiRoute = "/api/epub/" + bookKey + "/" + relativePathInsideBook;
                iframe.setSrc(apiRoute);
            });
        });

        iframe.getElement().addEventListener("internal-nav", domEvent -> {
            String rawApiPath = domEvent.getEventData().get("event.detail.path").asString();
            String targetLookupPrefix = "/api/epub/" + bookKey + "/";

            if (rawApiPath.startsWith(targetLookupPrefix)) {
                String foundFullPath = expectedPrefix + rawApiPath.substring(targetLookupPrefix.length());
                if (!foundFullPath.equals(currentActiveFullPath)) {
                    this.currentActiveFullPath = foundFullPath;
                    syncGridSelectionToPath(foundFullPath, grid);
                }
            }
        }).addEventData("event.detail.path");

// --- 4. Draggable Splitter with Fixed IFrame Canvas ---

        // Primary Component (Left side): The book reading workspace canvas
        HorizontalLayout iframeCenteringWrapper = new HorizontalLayout(iframe);
        iframeCenteringWrapper.setSizeFull();
        iframeCenteringWrapper.setPadding(true);
        iframeCenteringWrapper.addClassName("reader-frame-viewport");

        iframe.setWidth("100%");
        iframe.setHeightFull();
        iframe.getStyle().set("border", "none");

        // Secondary Component (Right side): The custom menu panel drawer
        Div controlsMenu = new Div();
        controlsMenu.addClassName("reader-right-sidebar-drawer");
        controlsMenu.setSizeFull();
        controlsMenu.setMinWidth("0px");
        controlsMenu.setMaxWidth("280px");

        // Inner content wrapper holding the dropdown elements cleanly
        Div innerMenuContent = new Div();
        innerMenuContent.addClassName("drawer-inner-content");

        // Apply clean dark styles directly to the Vaadin ComboBox elements
        for (ComboBox<?> select : new ComboBox<?>[]{fontSizeSelect, themeSelect}) {
            select.setWidthFull();
            select.getElement().getStyle().set("--vaadin-combo-box-overlay-background", "#1e1f22");
            select.getElement().executeJs(
                    "const label = this.shadowRoot.querySelector('label');" +
                            "if (label) label.style.color = '#b9bbbe';" +
                            "const input = this.querySelector('input');" +
                            "if (input) { input.style.color = '#ffffff'; input.style.backgroundColor = '#2b2d31'; }"
            );
        }

        // --- Slider Initialization ---
        Slider iframeScaleSlider = new Slider("Reader Width");
        iframeScaleSlider.setMin(400);
        iframeScaleSlider.setMax(1200); // Dynamic fallback
        iframeScaleSlider.setWidthFull();

        // Bidirectional State Synchronization via Signal Peek & Set
        iframeScaleSlider.setValue(iFrameWidth.peek());

        iframeScaleSlider.addValueChangeListener(events -> {
            if (events.isFromClient() && events.getValue() != null) {
                iFrameWidth.set(events.getValue());
            }
        });
        iframeScaleSlider.setValueChangeMode(ValueChangeMode.EAGER);

        // Use the simplified Signal effect to scale the frame; Flexbox handles centering automatically
        Signal.effect(iframeScaleSlider, () -> {
            double currentWidth = iFrameWidth.get();

            iframe.setWidth(currentWidth + "px");
            iframe.getStyle().set("max-width", currentWidth + "px");
            iframe.getStyle().remove("margin-left");
        });

        // THE CORRECT RECENTERING CAP: Fetch browser dimension directly on component attach
        iframeCenteringWrapper.addAttachListener(events -> {
            events.getUI().getPage().executeJs("return window.innerWidth;")
                    .then(Double.class, totalWidth -> {
                        if (totalWidth != null) {
                            // Available workspace: subtract navigation grid (280) and drawer panel (280)
                            double safeMax = totalWidth - 280 - 280 - 48; // 48px breathing padding

                            // Bound the max range dynamically based on display size
                            double finalMaxBound = Math.max(400.0, safeMax);
                            iframeScaleSlider.setMax(finalMaxBound);

                            // Clamp active iframe width down if it initialized outside boundaries
                            if (iFrameWidth.peek() > finalMaxBound) {
                                iFrameWidth.set(finalMaxBound);
                            }
                        }
                    });
        });

        // Quick theme text alignment adjustments for labels
        iframeScaleSlider.getElement().executeJs(
                "const label = this.shadowRoot.querySelector('label');" +
                        "if (label) { label.style.color = '#b9bbbe'; label.style.fontWeight = '500'; label.style.fontSize = '13px'; }"
        );

// Clean layout insertion
        innerMenuContent.add(fontSizeSelect, themeSelect, iframeScaleSlider);
        controlsMenu.add(innerMenuContent);

        // Instantiate the SplitLayout normally
        SplitLayout rightSplitLayout = new SplitLayout(iframeCenteringWrapper, controlsMenu);
        rightSplitLayout.setSizeFull();
        rightSplitLayout.setSplitterPosition(85);

        // Change the splitter track color and customize the native handle look via inline style execution
        rightSplitLayout.getElement().getStyle().set("--lumo-contrast-10pct", "#2e3035");
        rightSplitLayout.getElement().executeJs(
                "const splitter = this.shadowRoot.querySelector('[part=\"splitter\"]');" +
                        "if (splitter) {" +
                        "  splitter.style.backgroundColor = '#2e3035';" +
                        "  splitter.style.width = '4px';" +
                        "  const handle = this.shadowRoot.querySelector('[part=\"handle\"]');" +
                        "  if (handle) { handle.style.backgroundColor = '#4e5058'; handle.style.borderRadius = '2px'; }" +
                        "}"
        );

        // The master workspace layout combining your left grid navigation and your splitter
        HorizontalLayout workspace = new HorizontalLayout(grid, rightSplitLayout);
        workspace.setSizeFull();
        workspace.setSpacing(false);
        workspace.setPadding(false);
        workspace.setFlexGrow(1, rightSplitLayout);

        add(workspace);

        // --- 5. Initial Book Data Binding ---
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
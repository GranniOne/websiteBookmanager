package com.booksmanager.websitebooksmanager.Layout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.component.orderedlayout.*;


public class EmbedPdfViewer extends Div {
    private final String src;
    public EmbedPdfViewer(String src) {
        this.src = src;

        // Main Container: Only structure, no aesthetics
        getStyle()
                .setWidth("100%")
                .setHeight("100%")
                .setDisplay(Style.Display.FLEX)
                .setFlexDirection(Style.FlexDirection.COLUMN);

        // Toolbar Container
        Div toolBar = new Div();
        toolBar.setId("toolbar-id"); // All styling will happen in JS/CSS

        // Row 1: Brand Colors
        Div brandRow = new Div();
        brandRow.getStyle().setDisplay(Style.Display.INLINE_FLEX).setAlignItems(Style.AlignItems.CENTER).setGap("12px");

        Span brandLabel = new Span("Choose brand color:");
        brandLabel.setClassName("text-sm font-medium"); // Tailwind-ready classes

        Div colorButtons = new Div();
        colorButtons.setId("color-buttons");

        Span selectedColorName = new Span();
        selectedColorName.setId("selected-color-name");
        selectedColorName.setClassName("text-sm");

        brandRow.add(brandLabel, colorButtons, selectedColorName);

        // Row 2: Theme Modes
        Div themeRow = new Div();
        themeRow.getStyle().setDisplay(Style.Display.FLEX).setAlignItems(Style.AlignItems.CENTER).setGap("12px");

        Span themeLabel = new Span("Theme mode:");
        themeLabel.setClassName("text-sm font-medium");

        Div themeButtons = new Div();
        themeButtons.setId("theme-buttons");

        Span selectedThemeMode = new Span();
        selectedThemeMode.setId("selected-theme-mode");
        selectedThemeMode.setClassName("text-sm");

        themeRow.add(themeLabel, themeButtons, selectedThemeMode);

        toolBar.add(brandRow, themeRow);

        // Viewer
        Div viewer = new Div();
        viewer.setId("pdf-viewer");
        viewer.getStyle().setFlexGrow("1");
        viewer.getStyle().setOverflow(Style.Overflow.AUTO);

        add(toolBar, viewer);



         /*
        setSizeFull();
        getStyle().set("padding", "0");
        getStyle().set("gap", "0");

        // ===== Toolbar =====
        Div toolbar = new Div();
        toolbar.setWidthFull();

        // --- Color section ---
        Span colorLabel = new Span("Choose brand color:");

        Div colorButtons = new Div();
        colorButtons.setId("color-buttons");
        colorButtons.getStyle()
                .set("display", "flex")
                .set("gap", "8px");

        Span selectedColorTextLabel = new Span("Selected:");

        Span selectedColorName = new Span("Purple");
        selectedColorName.setId("selected-color-name");

        Div colorRow = new Div(colorLabel, colorButtons, selectedColorTextLabel, selectedColorName);
        colorRow.getStyle().set("display", "flex").set("gap", "10px").set("align-items", "center");

        // --- Theme section ---
        Span themeLabel = new Span("Theme mode:");

        Div themeButtons = new Div();
        themeButtons.setId("theme-buttons");
        themeButtons.getStyle()
                .set("display", "flex")
                .set("gap", "8px");

        Span activeModeLabel = new Span("Active mode:");

        Span selectedThemeMode = new Span("system");
        selectedThemeMode.setId("selected-theme-mode");

        Div themeRow = new Div(themeLabel, themeButtons, activeModeLabel, selectedThemeMode);
        themeRow.getStyle().set("display", "flex").set("gap", "10px").set("align-items", "center");

        toolbar.add(colorRow, themeRow);

        // ===== PDF Viewer =====
        Div viewer = new Div();
        viewer.setId("pdf-viewer");
        viewer.setSizeFull();

        add(toolbar, viewer);

          */

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        //getElement().executeJs("import('/frontend/embedpdf/pdfLoader.js').then(m => m.initEmbedPdf(this, $0));", src);


        attachEvent.getUI().beforeClientResponse(this, context -> {
            getElement().executeJs("""
            (async () => {
                const m = await import('/frontend/embedpdf/pdfLoader.js');
                m.init(this,$0);
            })($0);
        """,src);
        });
    }
}

/*
@Tag("div")
public class EmbedPdfViewer extends VerticalLayout {

    public EmbedPdfViewer(String src) {







        getElement().executeJs("import('/frontend/embedpdf/pdfLoader.js').then(m => m.initEmbedPdf(this, $0));", src);
        /*
        getElement().executeJs("""
        (async (pdfSrc) => {
            const container = this;

            const EmbedPDF = (await import(
                '/frontend/embedpdf/embedpdf.js'
            )).default;

            EmbedPDF.init({
                type: 'container',
                target: container,
                src: pdfSrc,
                theme: { preference: 'system' }
            });
        })($0);
    """, src);


    }

}
*/
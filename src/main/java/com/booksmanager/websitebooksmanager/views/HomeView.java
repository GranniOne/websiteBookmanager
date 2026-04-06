package com.booksmanager.websitebooksmanager.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@StyleSheet("styles.css")
@Route("")
public class HomeView extends Div {

    public HomeView() {
        // Force the view to fill the browser window
        setSizeFull();
        addClassName("home-page-wrapper");

        VerticalLayout welcomeIsland = new VerticalLayout();
        welcomeIsland.addClassName("home-island-container");
        welcomeIsland.addClassName("home-island");

        // Rest of your logic...
        H1 title = new H1("THE DIGITAL VOLUME ARCHIVE");
        title.addClassName("home-title");

        Paragraph subTitle = new Paragraph("A high-fidelity management system for your technical library.");
        subTitle.addClassName("home-subtitle");

        Div divider = new Div();
        divider.addClassName("home-divider");

        HorizontalLayout actionRow = new HorizontalLayout();
        actionRow.addClassName("home-action-row");

        Button exploreBtn = createMenuButton("Explore Collection", VaadinIcon.BOOK, "primary");
        exploreBtn.addClickListener(e -> UI.getCurrent().navigate(BookRoot.class));

        actionRow.add(exploreBtn,
                createMenuButton("Upload Book", VaadinIcon.UPLOAD, "secondary"),
                createMenuButton("Manage Archive", VaadinIcon.TRASH, "danger"));

        welcomeIsland.add(title, subTitle, divider, actionRow);
        add(welcomeIsland);
    }

    private Button createMenuButton(String text, VaadinIcon icon, String theme) {
        Button btn = new Button(text, icon.create());
        btn.addClassName("home-btn");
        btn.addClassName("btn-" + theme);
        return btn;
    }
}
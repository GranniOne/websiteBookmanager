package com.booksmanager.websitebooksmanager.Layout;

import com.booksmanager.websitebooksmanager.views.BookRoot;
import com.booksmanager.websitebooksmanager.views.HomeView;
import com.booksmanager.websitebooksmanager.views.LoginView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;

@PermitAll
@Layout("")
public class AppLayoutNavbar extends AppLayout {

    public AppLayoutNavbar() {
        // 1. Style the navbar container itself (Dark background, white text)
        // Lumo variables ensure colors stay crisp and match the system design.

        H1 title = new H1("Books Manager");
        title.getStyle()
                .set("font-size", "1.125rem")
                .set("left", "var(--vaadin-padding-l)")
                .set("margin", "0")
                .set("position", "absolute")
                .set("color", "var(--lumo-base-color)"); // Ensures the title stays white

        HorizontalLayout navigation = getNavigation();

        addToNavbar(title, navigation);
    }

    private HorizontalLayout getNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setWidthFull();
        navigation.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        navigation.setHeight("100%"); // Match the full height of the navbar
        navigation.getStyle().set("gap", "0.5rem");

        navigation.add(
                createLink("Dashboard",HomeView.class),
                createLink("Book collection", BookRoot.class)

        );
        return navigation;
    }

    private RouterLink createLink(String viewName, Class<? extends Component> classes) {
        RouterLink link = new RouterLink();
        link.add(viewName);
        link.setRoute(classes);



        // 2. Style the links (White text, rounded corners, transitions for smooth hover)
        // 2. Style the links (Base background should be explicitly set, e.g., transparent)
        link.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("padding", "0 1rem")
                .set("font-weight", "500")
                .set("text-decoration", "none")
                .set("color", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "transparent") // 1. Start with an explicit base color
                .set("transition", "background-color 0.3s ease"); // 2. Adjusted to 0.3s so you can really see the fade

// 3. Add hover effects using explicit values so the transition works both ways
        link.getElement().addEventListener("mouseover", e -> {
            link.getStyle().set("background-color", "var(--lumo-tint-10pct)");
        });

        link.getElement().addEventListener("mouseout", e -> {
            // 3. Change this from .remove() to .set("transparent")
            link.getStyle().set("background-color", "transparent");
        });

        return link;
    }
}
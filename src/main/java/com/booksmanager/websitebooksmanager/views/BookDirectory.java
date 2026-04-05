package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import software.amazon.awssdk.core.ResponseInputStream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@StyleSheet("pdfprofilestyle.css")
@Route("/books/:bookDirectory")
public class BookDirectory extends Div implements BeforeEnterObserver {
    final CloudflareR2Client cloudflareR2Client;
    BookDirectory(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
        this.setClassName("book-page-container");
    }


    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        RouteParameters bookId= beforeEnterEvent.getRouteParameters();

        String directory = bookId.get("bookDirectory").orElse("");


        try (ResponseInputStream metaStream =  cloudflareR2Client.getObjectFromR2("books" + "/" + directory.replace("%20", " ") + "/" + "meta.json")) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode bookNode = mapper.readTree(metaStream);

            // Create the "Paper" container
            Div paper = new Div();
            paper.setClassName("paper-sheet");

            // Extract fields
            String title = bookNode.get("title").asText();
            String filename = bookNode.get("filename").asText();
            String category = bookNode.get("category").asText();
            String level = bookNode.get("level").asText();
            String type = bookNode.get("type").asText();
            String format = bookNode.get("format").asText();

            JsonNode topicsNode = bookNode.get("topics");
            String topics = topicsNode.isArray() && topicsNode.size() > 0
                    ? String.join(", ", mapper.convertValue(topicsNode, String[].class))
                    : "None";

            // Title section - mimicking a title page
            com.vaadin.flow.component.html.H1 titleHeader = new com.vaadin.flow.component.html.H1(title);
            titleHeader.setClassName("book-title");
            paper.add(titleHeader);

            Div divider = new Div();
            divider.setClassName("title-divider");
            paper.add(divider);

            // Metadata section
            Div details = new Div();
            details.setClassName("book-details");

            details.add(createDetailRow("Category", category));
            details.add(createDetailRow("Level", level));
            details.add(createDetailRow("Type", type));
            details.add(createDetailRow("Format", format));
            details.add(createDetailRow("Topics", topics));
            paper.add(details);

            // View Action
            Button viewButton = new Button("Open Volume", e -> {
                UI.getCurrent().navigate(
                        SelectedBookView.class,
                        new RouteParameters(Map.of(
                                "bookDirectory", directory,"book",filename
                        ))
                );
            });
            viewButton.setClassName("book-button");

            paper.add(viewButton);
            add(paper);

        } catch (IOException e) {
            add(new Div(new com.vaadin.flow.component.html.Span("Metadata unavailable: " + e.getMessage())));
        }


//        add(new Button("view pdf", event -> {
//            RouteParameters bookParameter = new RouteParameters(
//                    Map.of("bookDirectory", directory,"book",directory + ".pdf")
//            );
//            UI.getCurrent().navigate(SelectedBookView.class,bookParameter);
//        }));



    }
    private Div createDetailRow(String label, String value) {
        Div row = new Div();
        row.setClassName("detail-row");
        com.vaadin.flow.component.html.Span sLabel = new com.vaadin.flow.component.html.Span(label + ": ");
        sLabel.setClassName("detail-label");
        com.vaadin.flow.component.html.Span sValue = new com.vaadin.flow.component.html.Span(value);
        sValue.setClassName("detail-value");
        row.add(sLabel, sValue);
        return row;
    }
}

package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;


@Route("")
public class HomeView extends VerticalLayout {

    public HomeView(CloudflareR2Client cloudflareR2Client) {

        add(new H1("Welcome to your new application"));
        add(new Paragraph("This is the home view"));

        add(new Paragraph("You can edit this view in src\\main\\java\\com\\booksmanager\\websitebooksmanager\\views\\HomeView.java"));

        add(new Button("hello" ,buttonClickEvent -> {
            ResponseInputStream<GetObjectResponse> test =  cloudflareR2Client.getObjectFromR2("books/10 led Projects For Geeks/cover.jpg");
            try {
                byte[] works = test.readAllBytes();
                Image image = new Image(works,"test");
                add(image);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

    }
}

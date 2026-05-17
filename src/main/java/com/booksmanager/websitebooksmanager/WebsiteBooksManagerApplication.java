package com.booksmanager.websitebooksmanager;

import com.booksmanager.websitebooksmanager.epub.UnZipEpub;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;

import java.io.IOException;

@Push
@SpringBootApplication
@StyleSheet("styles.css")
public class WebsiteBooksManagerApplication implements AppShellConfigurator {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(WebsiteBooksManagerApplication.class, args);

    }



}

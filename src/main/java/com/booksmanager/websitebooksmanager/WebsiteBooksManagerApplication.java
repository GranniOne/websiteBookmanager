package com.booksmanager.websitebooksmanager;

import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
@Push
@SpringBootApplication
@StyleSheet("styles.css")
public class WebsiteBooksManagerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(WebsiteBooksManagerApplication.class, args);
    }



}

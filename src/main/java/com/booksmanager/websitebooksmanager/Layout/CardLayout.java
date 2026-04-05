package com.booksmanager.websitebooksmanager.Layout;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import software.amazon.awssdk.core.ResponseInputStream;

import java.io.IOException;


public class CardLayout extends Card {


    public CardLayout(String book, byte[] response) throws IOException {
        Div div = new Div();
        div.setClassName("inner-card");
        addClassName("cardlayout");
        Image cover = new Image(response,"CoverImage");
        cover.addClassName("cardlayout-image");
        div.add(cover);
        Div cardname = new Div();
        Span span = new Span(book);
        span.addClassName("cardlayout-span");
        cardname.setClassName("cardlayout-cardname");
        cardname.add(span);
        div.add(cardname);
        add(div);

    }
}

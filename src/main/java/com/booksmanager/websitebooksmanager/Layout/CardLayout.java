package com.booksmanager.websitebooksmanager.Layout;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import software.amazon.awssdk.core.ResponseInputStream;

import java.io.IOException;


public class CardLayout extends Composite<Div> {


    public CardLayout(String book, String url)  {
        // Outer Container: <div class="cardlayout">
        getContent().setClassName("Maincardlayout: " + book);
        // Inner Wrapper: <div class="inner-card">
        Div innerCard = new Div();
        innerCard.setClassName("inner-card");

        // Image: <img class="cardlayout-image">
        Image img = new Image();
        img.setSrc(url);
        img.setClassName("cardlayout-image");

        // Text Container: <div class="cardlayout-cardname">
        Div cardNameDiv = new Div();
        cardNameDiv.setClassName("cardlayout-cardname");

        // Text: <span class="cardlayout-span">
        Span titleSpan = new Span(book);
        titleSpan.setClassName("cardlayout-span");

        // Assemble like the React JSX
        cardNameDiv.add(titleSpan);
        innerCard.add(img, cardNameDiv);
        getContent().add(innerCard);

    }
}

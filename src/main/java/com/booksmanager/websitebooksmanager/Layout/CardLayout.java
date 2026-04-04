package com.booksmanager.websitebooksmanager.Layout;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;


public class CardLayout extends Card {


    public CardLayout(String book, String Cover) {
        Div div = new Div();
        div.setClassName("inner-card");
        addClassName("cardlayout");
        Image cover = new Image();
        cover.addClassName("cardlayout-image");
        cover.setSrc(Cover);
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

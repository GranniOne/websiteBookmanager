package com.booksmanager.websitebooksmanager.views;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.booksmanager.websitebooksmanager.Layout.CardLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.Map;
@StyleSheet("cardstyle.css")
@Route("/books")
public class BooksView extends Div {
    BooksView(CloudflareR2Client cloudflareR2Client) {
        setClassName("page");
        Div cardHolder = new Div();
        cardHolder.setClassName("booksview");


        cloudflareR2Client.listObjects("bookmanager").forEach(book -> {
            String bookName = book.key().substring(book.key().indexOf("/") + 1, book.key().length());
            if(!bookName.isEmpty()){
                CardLayout card = new CardLayout(bookName);
                card.getElement().addEventListener("click", event -> {
                    RouteParameters bookParameter = new RouteParameters(
                            Map.of("bookId", bookName,"bookDirectory", book.key().substring(0,book.key().indexOf("/")))
                    );

                    UI.getCurrent().navigate(SelectedBookView.class,bookParameter);
                });
                cardHolder.add(card);





                /*add(new Button(bookName, e -> {

                    RouteParameters bookParameter = new RouteParameters(
                            Map.of("bookId", bookName,"bookDirectory", book.key().substring(0,book.key().indexOf("/")))
                    );

                    UI.getCurrent().navigate(SelectedBookView.class,bookParameter);

                }));

                 */
            }
        });
        add(cardHolder);
    }
}

package com.booksmanager.websitebooksmanager.views;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.security.PermitAll;


@Route("login")
@StyleSheet("LoginStyle.css")
@StyleSheet(Lumo.STYLESHEET)
public class LoginView extends Div {

    LoginView(){
        getStyle()
                .set("display", "flex")
                .setJustifyContent(Style.JustifyContent.CENTER)
                .setAlignItems(Style.AlignItems.CENTER)
                .setHeight("100%")
                .setWidth("100%");
        LoginForm loginForm = new LoginForm();
        loginForm.getElement().getThemeList().add("dark");
        loginForm.setAction("login");
        loginForm.setError(false);
        add(loginForm);
    }
}

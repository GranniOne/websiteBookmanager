package com.booksmanager.websitebooksmanager.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.Route;

@Route("login")
public class LoginView extends Div {

    LoginView(){
        LoginForm loginForm = new LoginForm();
        loginForm.setAction("login");
        add(loginForm);
    }
}

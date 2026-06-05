package com.booksmanager.websitebooksmanager.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ErrorHandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomErrorHandler implements ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorHandler.class);

    @Override
    public void error(ErrorEvent errorEvent) {
        boolean redirected = ErrorHandlerUtil
                .handleErrorByRedirectingToErrorView(errorEvent.getThrowable());
        if (!redirected) {
            // We did not have a matching error view, logging and showing notification.
            logger.error("Something wrong happened", errorEvent.getThrowable());
            if(UI.getCurrent() != null) {
                UI.getCurrent().access(() -> {
                    Notification.show("An internal error has occurred." +
                            "Contact support for assistance.");
                });
            }
        }
    }
}
package com.booksmanager.websitebooksmanager;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApplicationServiceInitListener implements VaadinServiceInitListener {

    private final CloudflareR2Client r2Client;

    public ApplicationServiceInitListener(CloudflareR2Client r2Client) {
        this.r2Client = r2Client;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            // IndexHtmlRequestListener to change the bootstrap page
        });

        event.addDependencyFilter((dependencies, filterContext) -> {
            // DependencyFilter to add/remove/change dependencies sent to
            // the client
            return dependencies;
        });

        event.addRequestHandler((session, request, response) -> {
            // RequestHandler to change how responses are handled
            return false;
        });

        event.getSource().addSessionInitListener(sessionInitEvent -> {
            LoggerFactory.getLogger(getClass()).info("A new Session has been initialized! " + "ip: " + sessionInitEvent.getSession().getBrowser().getAddress() + " Browser:  " + sessionInitEvent.getSession().getBrowser().getUserAgent());


        });
    }

}
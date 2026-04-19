package com.booksmanager.websitebooksmanager.Utilities;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import jakarta.annotation.security.PermitAll;

import java.io.IOException;


@PermitAll
public class MyTransferProgressListener implements TransferProgressListener {
    private final ProgressBar progressBar;

    public MyTransferProgressListener(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    public void onStart(TransferContext context) {
        UI ui = context.getUI();
        ui.access(() -> {
            progressBar.setValue(0);
            progressBar.setVisible(true);
        });
    }

    @Override
    public void onProgress(TransferContext context, long transferredBytes, long totalBytes) {
        // Safety check to avoid division by zero or NaN if totalBytes is unknown (-1)
        if (totalBytes <= 0) {
            return;
        }

        double progress = (double) transferredBytes / totalBytes;
        context.getUI().access(() -> progressBar.setValue(progress));
    }

    @Override
    public void onComplete(TransferContext context, long transferredBytes) {
        context.getUI().access(() -> {
            progressBar.setValue(1.0);
            progressBar.setVisible(false);
        });
    }

    @Override
    public void onError(TransferContext context, IOException reason) {
        context.getUI().access(() -> {
            progressBar.setVisible(false);
            // Optional: You could show a Notification here using context.getUI()
        });
    }
}
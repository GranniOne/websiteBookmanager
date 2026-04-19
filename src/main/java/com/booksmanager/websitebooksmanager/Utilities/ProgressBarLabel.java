package com.booksmanager.websitebooksmanager.Utilities;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

@PermitAll
@StyleSheet(Lumo.STYLESHEET)
public class ProgressBarLabel extends Div {
    private final ProgressBar progressBar;
    private final NativeLabel progressBarLabelText;

    public ProgressBarLabel(String label) {
        this.progressBar = new ProgressBar();
        this.progressBarLabelText = new NativeLabel(label); // FIXED: No "NativeLabel" prefix


        setVisible(false);
        getStyle().setWidth("60%");

        progressBarLabelText.setId("pblabel");
        progressBar.getElement().setAttribute("aria-labelledby", "pblabel");

        add(progressBarLabelText, progressBar);
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public NativeLabel getProgressBarLabelText() { return progressBarLabelText; }
}

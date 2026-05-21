package com.booksmanager.websitebooksmanager.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EpubContainer {
    public String resolveOpfPath(Document containerDoc) {
        return ((Element) containerDoc.getElementsByTagName("rootfile").item(0))
                .getAttribute("full-path");
    }
}
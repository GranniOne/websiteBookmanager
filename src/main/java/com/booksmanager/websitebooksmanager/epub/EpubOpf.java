package com.booksmanager.websitebooksmanager.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EpubOpf {
    // Pass the document as a parameter, not as a class field
    public String resolveOpfPath(Document containerDoc) {
        return ((Element) containerDoc.getElementsByTagName("rootfile").item(0))
                .getAttribute("full-path");
    }

    public String findNcxPath(Document opfDoc) {
        NodeList items = opfDoc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            if ("application/x-dtbncx+xml".equals(item.getAttribute("media-type"))) {
                return item.getAttribute("href");
            }
        }
        return null;
    }

}
package com.booksmanager.websitebooksmanager.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EpubNcx {

    public List<EpubPage> parseNcxDocument(Document ncxDoc, String basePrefix, String opfParentFolder) {
        List<EpubPage> nav = new ArrayList<>();
        NodeList navPoints = ncxDoc.getElementsByTagName("navPoint");

        for (int i = 0; i < navPoints.getLength(); i++) {
            Element np = (Element) navPoints.item(i);

            // Extract the title
            String title = np.getElementsByTagName("text").item(0).getTextContent().trim();

            // Extract the src (the actual file path inside the OEBPS folder)
            String src = ((Element) np.getElementsByTagName("content").item(0)).getAttribute("src");

            // Extract the play order
            int order = Integer.parseInt(np.getAttribute("playOrder"));

            // Calculate the full path by combining the base path with the relative path
            String fullPath = basePrefix + opfParentFolder + src;

            nav.add(new EpubPage(i, order, title, fullPath));
        }
        System.out.println("parseNcxDocument: " + nav);
        // Ensure the table of contents is in the correct reading order
        nav.sort(Comparator.comparingInt(EpubPage::getPlayOrder));
        return nav;
    }
}
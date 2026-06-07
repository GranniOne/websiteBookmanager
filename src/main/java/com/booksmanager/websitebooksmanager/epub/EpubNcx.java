package com.booksmanager.websitebooksmanager.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EpubNcx {

    public List<EpubPage> parseNcxDocument(Document ncxDoc, String basePrefix, String opfParentFolder) {
        List<EpubPage> nav = new ArrayList<>();

        // Find the root <navMap> element to begin our clean top-down descent
        NodeList navMaps = ncxDoc.getElementsByTagName("navMap");
        if (navMaps.getLength() == 0) return nav;

        Element navMap = (Element) navMaps.item(0);
        NodeList topLevelPoints = navMap.getChildNodes();

        // An atomic integer counter to safely act as a fallback playOrder sequence
        AtomicInteger fallbackOrder = new AtomicInteger(1);

        // Start recursively walking the tree from the top layer down
        walkingChildrenRecursively(basePrefix, opfParentFolder, nav, topLevelPoints, fallbackOrder);

        // Sort cleanly. If playOrder was missing globally, fallbackOrder preserves the exact tree layout sequence
        nav.sort(Comparator.comparingInt(EpubPage::getPlayOrder));
        return nav;
    }

    private void walkingChildrenRecursively(String basePrefix, String opfParentFolder, List<EpubPage> nav, NodeList topLevelPoints, AtomicInteger fallbackOrder) {
        for (int i = 0; i < topLevelPoints.getLength(); i++) {
            if (topLevelPoints.item(i) instanceof Element childElement) {
                if ("navPoint".equals(childElement.getNodeName())) {
                    parseNavPointRecursive(childElement, nav, basePrefix, opfParentFolder, fallbackOrder);
                }
            }
        }
    }

    private void parseNavPointRecursive(Element navPoint, List<EpubPage> nav, String basePrefix, String opfParentFolder, AtomicInteger fallbackOrder) {
        try {
            // 1. Extract the text safely by targetting ONLY the immediate child <navLabel>
            String title = "Untitled Section";
            NodeList navLabels = navPoint.getElementsByTagName("navLabel");
            if (navLabels.getLength() > 0) {
                Element label = (Element) navLabels.item(0); // This belongs to THIS navPoint
                NodeList texts = label.getElementsByTagName("text");
                if (texts.getLength() > 0) {
                    title = texts.item(0).getTextContent().trim();
                }
            }

            // 2. Extract the content src safely
            String src = "";
            NodeList contents = navPoint.getElementsByTagName("content");
            if (contents.getLength() > 0) {
                src = ((Element) contents.item(0)).getAttribute("src");
            }

            // 3. Defensive playOrder check
            int order;
            String playOrderAttr = navPoint.getAttribute("playOrder");
            if (playOrderAttr != null && !playOrderAttr.isEmpty()) {
                order = Integer.parseInt(playOrderAttr);
            } else {
                order = fallbackOrder.getAndIncrement(); // Use our fallback counter if the publisher left it out!
            }

            String fullPath = basePrefix + opfParentFolder + src;

            // Add this page to our flat list representation
            nav.add(new EpubPage(nav.size(), order, title, fullPath));

        } catch (Exception e) {
            System.out.println("Skipping malformed navPoint node due to parsing exception: " + e.getMessage());
        }

        // 4. DEEP TRAVERSAL: Look for nested <navPoint> layers inside this one
        NodeList children = navPoint.getChildNodes();
        walkingChildrenRecursively(basePrefix, opfParentFolder, nav, children, fallbackOrder);
    }
}
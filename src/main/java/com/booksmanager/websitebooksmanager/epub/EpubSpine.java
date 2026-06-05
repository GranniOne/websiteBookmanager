package com.booksmanager.websitebooksmanager.epub;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpubSpine {

    public static List<EpubPage> getSpine(Document doc, String basePrefix, String opfParentFolder) {
        List<EpubPage> spinePages = new ArrayList<>();

        if (doc == null) {
            return spinePages;
        }

        // 1. Get the <manifest> element explicitly to isolate its children
        Element manifestElement = (Element) doc.getElementsByTagName("manifest").item(0);
        Map<String, String> manifestLookup = new HashMap<>();

        if (manifestElement != null) {
            // ONLY scan <item> tags that live inside the manifest block
            NodeList items = manifestElement.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String manifestId = item.getAttribute("id"); // The target ID
                String href = item.getAttribute("href");

                if (!manifestId.isEmpty() && !href.isEmpty()) {
                    manifestLookup.put(manifestId, href);
                }
            }
        }

        // 2. Get the <spine> element explicitly to isolate its children
        Element spineElement = (Element) doc.getElementsByTagName("spine").item(0);

        if (spineElement != null) {
            // ONLY scan <itemref> tags that live inside the spine block
            NodeList itemRefs = spineElement.getElementsByTagName("itemref");
            int playOrderCounter = 1;

            for (int i = 0; i < itemRefs.getLength(); i++) {
                Element itemRef = (Element) itemRefs.item(i);

                // CRITICAL: We grab 'idref' here, completely ignoring the itemref's own 'id' attribute
                String targetIdRef = itemRef.getAttribute("idref");

                String linear = itemRef.getAttribute("linear");
                if ("no".equalsIgnoreCase(linear)) {
                    continue;
                }

                // 3. Match the spine's idref directly to the manifest item's real id
                if (manifestLookup.containsKey(targetIdRef)) {
                    String relativeSrc = manifestLookup.get(targetIdRef);
                    String fullPath = basePrefix + opfParentFolder + relativeSrc;

                    String technicalTitle = "Track Sequence #" + playOrderCounter;

                    spinePages.add(new EpubPage(i, playOrderCounter, technicalTitle, fullPath));
                    playOrderCounter++;
                }
            }
        }

        System.out.println("Safely generated linear spine map entries: " + spinePages.size());
        return spinePages;
    }
}
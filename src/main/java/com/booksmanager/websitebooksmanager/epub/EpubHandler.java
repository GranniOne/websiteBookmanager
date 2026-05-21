package com.booksmanager.websitebooksmanager.epub;


import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EpubHandler {
    private final CloudflareR2Client client;
    private final String basePrefix;

    private final EpubContainer container = new EpubContainer();
    private final EpubOpf epubOpf = new EpubOpf();
    private final EpubNcx epubNcx = new EpubNcx();

    private Document opfDoc;
    private String opfParent;

    public EpubHandler(CloudflareR2Client client, String bookKey) {
        this.client = client;
        this.basePrefix = "epubs/" + bookKey + "/";
    }

    public void initialize() throws Exception {
        // 1. Parse Container to find OPF
        String opfPath;
        try (var stream = client.getObjectFromR2(basePrefix + "META-INF/container.xml")) {
            opfPath = container.resolveOpfPath(parseXmlSecurely(stream));
        }

        // 2. Set context and parse OPF
        this.opfParent = opfPath.substring(0, opfPath.lastIndexOf("/") + 1);
        try (var opfStream = client.getObjectFromR2(basePrefix + opfPath)) {
            this.opfDoc = parseXmlSecurely(opfStream);
        }
    }

    // Now uses the internal state, making the API cleaner for your View
    public List<EpubPage> getTableOfContents() throws Exception {
        if (opfDoc == null) throw new IllegalStateException("Initialize handler first!");

        String ncxPath = epubOpf.findNcxPath(opfDoc);
        if (ncxPath == null) return Collections.emptyList();

        try (var stream = client.getObjectFromR2(basePrefix + opfParent + ncxPath)) {
            return epubNcx.parseNcxDocument(parseXmlSecurely(stream), basePrefix, opfParent);
        }
    }

    /**
     * Built-in helper to safely parse XML streams while blocking XXE Injection Attacks
     */
    private Document parseXmlSecurely(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();
        return doc;
    }
}
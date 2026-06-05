package com.booksmanager.websitebooksmanager.epub;


import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        String ncxPath;


        ncxPath = epubOpf.findNcxPath(opfDoc);
        if (ncxPath != null) {
            try (var stream = client.getObjectFromR2(basePrefix + opfParent + ncxPath)) {
                return epubNcx.parseNcxDocument(parseXmlSecurely(stream), basePrefix, opfParent);
            }
        }else{
            return EpubSpine.getSpine(opfDoc, basePrefix, opfParent);
        }

    }

    /**
     * Built-in helper to safely parse XML streams while blocking XXE Injection Attacks
     */
    private Document parseXmlSecurely(InputStream inputStream) throws Exception {
        // 1. Read the input stream into a string
        String xmlContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        // 2. Sanitize: Remove the DOCTYPE declaration
        String sanitizedXml = xmlContent.replaceAll("(?i)<!DOCTYPE[\\s\\S]*?>", "");

        // 3. Convert back to an InputStream
        InputStream sanitizedStream = new ByteArrayInputStream(sanitizedXml.getBytes(StandardCharsets.UTF_8));

        // 4. Proceed with secure parsing
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        // Crucial: ensure Namespace awareness is set if the XML uses them (like your NCX)
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(sanitizedStream);
        doc.getDocumentElement().normalize();
        return doc;
    }
}
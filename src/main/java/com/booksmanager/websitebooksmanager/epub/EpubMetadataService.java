package com.booksmanager.websitebooksmanager.epub;

import com.booksmanager.websitebooksmanager.CloudFlare.CloudflareR2Client;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EpubMetadataService {
    private static final Logger log = LoggerFactory.getLogger(EpubMetadataService.class);

    private final CloudflareR2Client cloudflareR2Client;

    public EpubMetadataService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }

    /**
     * Clear, type-safe representation of a sequential reading page.
     */
    public static class EpubPageEntry {
        private final String id;
        private final String fullPath;
        private final String mediaType;

        public EpubPageEntry(String id, String fullPath, String mediaType) {
            this.id = id;
            this.fullPath = fullPath;
            this.mediaType = mediaType;
        }

        public String getId() { return id; }
        public String getFullPath() { return fullPath; }
        public String getMediaType() { return mediaType; }
    }

    /**
     * Finds the absolute bucket path of the Table of Contents file (NCX or HTML Nav)
     * @param bookKey The base folder name of the book (e.g., "book-of-vaadin-vaadin7")
     * @return The full R2 object key (e.g., "epubs/book-of-vaadin-vaadin7/OEBPS/toc.ncx")
     */
    public List<EpubPageEntry> findTableOfContentsPath(String bookKey) throws Exception {
        String basePrefix = "epubs/" + bookKey + "/";

        // --------------------------------------------------------------------
        // STEP 1: Parse container.xml to locate the .opf file
        // --------------------------------------------------------------------
        String containerKey = basePrefix + "META-INF/container.xml";
        String opfRelativePath;

        try (ResponseInputStream<GetObjectResponse> stream = cloudflareR2Client.getObjectFromR2(containerKey)) {
            Document doc = parseXmlSecurely(stream);
            NodeList rootFiles = doc.getElementsByTagName("rootfile");
            if (rootFiles.getLength() == 0) {
                throw new IllegalStateException("Invalid EPUB format: Missing <rootfile> tag in container.xml");
            }
            opfRelativePath = ((Element) rootFiles.item(0)).getAttribute("full-path");
        }

        String opfKey = basePrefix + opfRelativePath;

        String opfParentFolder = "";
        if (opfRelativePath.contains("/")) {
            opfParentFolder = opfRelativePath.substring(0, opfRelativePath.lastIndexOf("/") + 1);
        }

        // --------------------------------------------------------------------
        // STEP 2: Parse the .opf file to extract structural items
        // --------------------------------------------------------------------
        try (ResponseInputStream<GetObjectResponse> stream = cloudflareR2Client.getObjectFromR2(opfKey)) {
            Document doc = parseXmlSecurely(stream);

            return extractSpineFromOpf(doc,basePrefix,opfParentFolder);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process OPF data links from R2 storage", e);
        }

    }

    private List<EpubPageEntry> extractSpineFromOpf(Document doc, String basePrefix, String opfParentFolder) {
        List<EpubPageEntry> orderedSpineData = new ArrayList<>();

        log.info("====================================================================");
        log.info("EPUB SPINE PARSING START | Parent Folder Context: \"{}\"", opfParentFolder);
        log.info("====================================================================");

        // 1. Build comprehensive manifest catalog dictionary [href, media-type]
        Map<String, List<String>> manifestLookup = new HashMap<>();
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            manifestLookup.put(
                    item.getAttribute("id"),
                    List.of(item.getAttribute("href"), item.getAttribute("media-type"))
            );
        }

        log.info("→ Manifest Mapping complete. Indexed {} items from book manifest.", manifestLookup.size());

        NodeList itemrefs = doc.getElementsByTagName("itemref");
        log.info("→ Found {} references inside <spine>. Resolving paths...", itemrefs.getLength());
        log.info("--------------------------------------------------------------------");

        // 2. Map spine itemrefs to their respective manifest locations sequentially
        for (int i = 0; i < itemrefs.getLength(); i++) {
            Element itemref = (Element) itemrefs.item(i);
            String idref = itemref.getAttribute("idref");
            List<String> assetDetails = manifestLookup.get(idref);

            if (assetDetails != null) {
                String fileHref = assetDetails.get(0);
                String mediaType = assetDetails.get(1);

                // Assemble the absolute path context needed for your Cloudflare R2 calls
                String fullResolvedPath = basePrefix + opfParentFolder + fileHref;

                // Instantiate the object cleanly and add it to our sequential tracker
                orderedSpineData.add(new EpubPageEntry(idref, fullResolvedPath, mediaType));

                String logLine = String.format("[Spine Index Layout %03d] ID: \"%s\"  ──>  Resolved Key Path: \"%s\" (%s)",
                        i, idref, fullResolvedPath, mediaType);
                log.info(logLine);
            } else {
                String warnLine = String.format("[Spine Index Layout %03d] ID: \"%s\"  ──>  ⚠️ WARNING: FAILED TO MATCH IN MANIFEST!",
                        i, idref);
                log.warn(warnLine);
            }
        }

        log.info("====================================================================");
        log.info("EPUB SPINE PARSING COMPLETE | Successfully resolved {} sequential pages.", orderedSpineData.size());
        log.info("====================================================================");

        return orderedSpineData;
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
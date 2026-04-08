package com.booksmanager.websitebooksmanager.CloudFlare;

import com.vaadin.flow.component.html.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudStorageService {



    private final CloudflareR2Client cloudflareR2Client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String BUCKET_NAME = "bookmanager";


    CloudStorageService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }


    public Map<String, Object> createMetaDataMap(Path path, String category, String level) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            Map<String, Object> metadata = new HashMap<>();

            // 1. Extract the raw temp name (e.g., Volume8_Issue2_Paper12_2024.pdf591875.tmp)
            String rawTempName = path.getFileName().toString();

            // 2. CLEANING SURGERY:
            // Get everything before the first ".pdf"
            String baseName = rawTempName.split("(?i)\\.pdf")[0];
            String cleanFileName = (baseName + ".pdf").replace("_"," ");


            // 3. DYNAMIC TITLE GENERATION:
            // We ignore internal PDF metadata and use the filename.
            // We replace underscores with spaces to make it a "Title".
            String title = baseName.replace("_", " ");


            metadata.put("filename", cleanFileName);
            metadata.put("category", category);
            metadata.put("topics", new String[]{});
            metadata.put("level", level);
            metadata.put("type", "book");
            metadata.put("format", "pdf");
            // Use PDFBox to get the actual page count
            try {
                metadata.put("pages", document.getNumberOfPages());
                document.getDocumentInformation().getMetadataKeys().forEach(event -> {
                    switch (event) {
                        case "Author":
                            metadata.put(event, document.getDocumentInformation().getAuthor() == null ? event :  document.getDocumentInformation().getAuthor());
                            break;
                        case "CreationDate":
                            metadata.put(event, document.getDocumentInformation().getCreationDate() == null ? event :  document.getDocumentInformation().getCreationDate());
                            break;
                        case "Creator":
                            metadata.put(event, document.getDocumentInformation().getCreator() == null ? event :  document.getDocumentInformation().getCreator());
                            break;
                        case "KeyWords":
                            metadata.put(event, document.getDocumentInformation().getKeywords() ==  null ? event :  document.getDocumentInformation().getKeywords());
                            break;
                        case "ModDate":
                            metadata.put(event, document.getDocumentInformation().getModificationDate() ==  null ? event :  document.getDocumentInformation().getModificationDate());
                            break;
                        case "Producer":
                            metadata.put(event, document.getDocumentInformation().getProducer() ==  null ? event :  document.getDocumentInformation().getProducer());
                            break;
                        case "Subject":
                            metadata.put(event, document.getDocumentInformation().getSubject() ==   null ? event :  document.getDocumentInformation().getSubject());
                            break;
                        case "Title":
                            if(title.equals(event)){
                                metadata.put(event, document.getDocumentInformation().getTitle() ==    null ? event :  document.getDocumentInformation().getTitle());
                            }else {
                                document.getDocumentInformation().setTitle(title);
                                metadata.put(event, document.getDocumentInformation().getTitle() ==    null ? event :  document.getDocumentInformation().getTitle());
                            }
                            break;
                    }});

                PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

                if (documentOutline != null) {
                    metadata.put("outline", getOutlineJson(documentOutline));
                }else{
                    metadata.put("outline", new ArrayList<>()); // Empty if no TOC
                }

            }catch (Exception e){

            }


            //printDeepMetadata(metadata);

            return metadata;
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze PDF at " + path, e);
        }
    }

    public String convertMapToJson(Map<String, Object> map) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    // Your existing thumbnail logic works perfectly!
    public byte[] generateThumbnailFromPath(Path localFilePath) {
        try (PDDocument document = Loader.loadPDF(localFilePath.toFile())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 72);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    private List<Map<String, Object>> getOutlineJson(PDOutlineNode node) throws IOException {
        List<Map<String, Object>> items = new ArrayList<>();
        PDOutlineItem current = node.getFirstChild();

        while (current != null) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", current.getTitle());

            // Check for sub-chapters (children)
            if (current.hasChildren()) {
                item.put("children", getOutlineJson(current));
            }

            items.add(item);
            current = current.getNextSibling();
        }
        return items;
    }
    private void printDeepMetadata(Map<String, Object> metadata) {
        try {
            System.out.println("\n==================================================");
            System.out.println("🔎 FINAL EXTRACTED METADATA");
            System.out.println("==================================================");

            // Use Jackson to print the map as a pretty JSON string
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
            System.out.println(json);

            System.out.println("==================================================\n");
        } catch (Exception e) {
            System.err.println("Error printing JSON: " + e.getMessage());
        }
    }

}
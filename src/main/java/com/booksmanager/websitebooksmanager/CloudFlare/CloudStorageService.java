package com.booksmanager.websitebooksmanager.CloudFlare;

import com.vaadin.flow.component.html.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import tools.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
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
import java.util.function.Supplier;
import java.util.logging.Logger;

@Service
public class CloudStorageService {



    private final CloudflareR2Client cloudflareR2Client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String BUCKET_NAME = "bookmanager";


    CloudStorageService(CloudflareR2Client cloudflareR2Client) {
        this.cloudflareR2Client = cloudflareR2Client;
    }


    public Map<String, Object> createMetaDataMap(File file, String category, String level) {
        try (PDDocument document = Loader.loadPDF(file)) {
            Map<String, Object> metadata = new HashMap<>();

            // 1. Extract the raw temp name (e.g., Volume8_Issue2_Paper12_2024.pdf591875.tmp)
            String rawTempName = file.getName();

            // 2. CLEANING SURGERY:
            // Get everything before the first ".pdf"
            String baseName = rawTempName.split("(?i)\\.pdf")[0];
            String cleanFileName = (baseName + ".pdf").replace("_"," ");


            // 3. DYNAMIC TITLE GENERATION:
            // We ignore internal PDF metadata and use the filename.
            // We replace underscores with spaces to make it a "Title".
            String title = baseName.replace("_", " ");

            System.out.println(cleanFileName);
            metadata.put("folderName", title);
            metadata.put("filename", cleanFileName);
            metadata.put("category", category);
            metadata.put("topics", new String[]{});
            metadata.put("level", level);
            metadata.put("type", "book");
            metadata.put("format", "pdf");
            // Use PDFBox to get the actual page count
            try {
                metadata.put("pages", document.getNumberOfPages());
                PDDocumentInformation info = document.getDocumentInformation();

                List<String> standardKeys = List.of(
                        "Author", "Title", "Subject", "Keywords",
                        "Creator", "Producer", "CreationDate", "ModDate"
                );


                standardKeys.forEach(key -> {
                    Object value = switch (key) {
                        case "Author" -> info.getAuthor();
                        case "CreationDate" -> info.getCreationDate();
                        case "Creator" -> info.getCreator();
                        case "Keywords" -> info.getKeywords();
                        case "ModDate" -> info.getModificationDate();
                        case "Producer" -> info.getProducer();
                        case "Subject" -> info.getSubject();
                        case "Title" -> {
                            // Your custom Title logic
                            if (info.getTitle() == null) {
                                info.setTitle(title); // Sets it in the PDF object
                            }
                            yield info.getTitle();
                        }
                        default -> null;
                    };


                    metadata.put(key, value != null ? value : key);
                });

                PDDocumentOutline documentOutline = document.getDocumentCatalog().getDocumentOutline();

                if (documentOutline != null) {
                    metadata.put("outline", getOutlineJson(documentOutline));
                }else{
                    metadata.put("outline", new ArrayList<>()); // Empty if no TOC
                }

            }catch (Exception e){

            }
            document.getDocument().getXrefTable().forEach((key, value) -> System.out.println(key + ":    \n " + value));

            //printDeepMetadata(metadata);

            return metadata;
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze PDF at " + file, e);
        }
    }

    public String convertMapToJson(Map<String, Object> map) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    public byte[] generateThumbnailFromPath(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            // Render the image
            BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 200);

            // Create a new RGB image (no transparency) with the same dimensions
            BufferedImage rgbImage = new BufferedImage(bim.getWidth(), bim.getHeight(), BufferedImage.TYPE_INT_RGB);

            // Draw the original image onto the white background
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(bim, 0, 0, Color.WHITE, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Save the solid RGB image instead of the original
            ImageIO.write(rgbImage, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace(); // Log the error to see if it's a font or rendering issue
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
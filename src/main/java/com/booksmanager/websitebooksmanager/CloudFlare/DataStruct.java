package com.booksmanager.websitebooksmanager.CloudFlare;

import java.time.Instant;

public class DataStruct {
    private String presignedCover;
    private Instant coverExpiry;

    private String presignedPdf;
    private Instant pdfExpiry;

    private String presignedMetaData;
    private Instant metaExpiry;


    DataStruct() {
    }

    public String getPresignedCover() {
        return presignedCover;
    }

    public void setPresignedCover(String presignedCover) {
        this.presignedCover = presignedCover;
    }

    public String getPresignedPdf() {
        return presignedPdf;
    }

    public void setPresignedPdf(String presignedPdf) {
        this.presignedPdf = presignedPdf;
    }

    public String getPresignedMetaData() {
        return presignedMetaData;
    }

    public void setPresignedMetaData(String presignedMetaData) {
        this.presignedMetaData = presignedMetaData;
    }

    public Instant getCoverExpiry() {
        return coverExpiry;
    }

    public void setCoverExpiry(Instant coverExpiry) {
        this.coverExpiry = coverExpiry;
    }

    public Instant getPdfExpiry() {
        return pdfExpiry;
    }

    public void setPdfExpiry(Instant pdfExpiry) {
        this.pdfExpiry = pdfExpiry;
    }

    public Instant getMetaExpiry() {
        return metaExpiry;
    }

    public void setMetaExpiry(Instant metaExpiry) {
        this.metaExpiry = metaExpiry;
    }
}

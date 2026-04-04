package com.booksmanager.websitebooksmanager.CloudFlare;

public class DataStruct {
    public String PresignedCover;
    public String PresignedPdf;
    public String PresignedMetaData;


    DataStruct() {
    }

    public String getPresignedCover() {
        return PresignedCover;
    }

    public void setPresignedCover(String presignedCover) {
        PresignedCover = presignedCover;
    }

    public String getPresignedPdf() {
        return PresignedPdf;
    }

    public void setPresignedPdf(String presignedPdf) {
        PresignedPdf = presignedPdf;
    }

    public String getPresignedMetaData() {
        return PresignedMetaData;
    }

    public void setPresignedMetaData(String presignedMetaData) {
        PresignedMetaData = presignedMetaData;
    }
}

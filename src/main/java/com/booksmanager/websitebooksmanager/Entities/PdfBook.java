package com.booksmanager.websitebooksmanager.Entities;

import jakarta.persistence.*;

@Entity
public class PdfBook implements BookInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String r2Key;

    private int pageCount;
    private boolean ocrEnabled;

    public PdfBook(String title, String r2Key) {
        this.title = title;
        this.r2Key = r2Key;

    }

    public PdfBook() {

    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getR2Key() {
        return r2Key;
    }

    @Override
    public String toString() {
        return "PdfBook{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", r2Key='" + r2Key + '\'' +
                ", pageCount=" + pageCount +
                ", ocrEnabled=" + ocrEnabled +
                '}';
    }
}

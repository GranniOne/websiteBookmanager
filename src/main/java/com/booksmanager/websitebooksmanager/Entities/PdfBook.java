package com.booksmanager.websitebooksmanager.Entities;

import jakarta.persistence.*;

@Entity
public class PdfBook implements BookInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String r2Key;
    private String cover;
    private String metadata;
    private BookType bookType;

    private int pageCount;
    private boolean ocrEnabled;

    public PdfBook(String title, String r2Key, String cover, String metadata, BookType bookType) {
        this.title = title;
        this.r2Key = r2Key;
        this.cover = cover;
        this.metadata = metadata;
        this.bookType = bookType;

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
    public BookType getBookType() {
        return bookType;
    }
    @Override
    public String StripFileName() {
        return title.substring(0, title.lastIndexOf("."));
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

package com.booksmanager.websitebooksmanager.Entities;

import jakarta.persistence.*;

@Entity
public class EpubBook implements BookInterface{



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String r2Key;

    private int chapterCount;
    private String language;

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
        return null;
    }

    @Override
    public String StripFileName() {
        return "";
    }

    @Override
    public String toString() {
        return "EpubBook{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", r2Key='" + r2Key + '\'' +
                ", chapterCount=" + chapterCount +
                ", language='" + language + '\'' +
                '}';
    }
}

package com.booksmanager.websitebooksmanager.Entities;

import jakarta.persistence.*;

@Entity
public class EpubBook implements BookInterface{



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String r2Key;
    private BookType bookType;
    private String Coverhref;
    private int chapterCount;
    private String language;

    public EpubBook(String title, String r2Key, BookType bookType) {
        this.title = title;
        this.r2Key = r2Key;
        this.bookType = bookType;
    }

    public EpubBook() {

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
        return title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setR2Key(String r2Key) {
        this.r2Key = r2Key;
    }

    public void setBookType(BookType bookType) {
        this.bookType = bookType;
    }

    public String getCoverhref() {
        return Coverhref;
    }

    public void setCoverhref(String coverhref) {
        Coverhref = coverhref;
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

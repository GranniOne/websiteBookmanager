package com.booksmanager.websitebooksmanager.Entities;

import jakarta.persistence.*;

@Entity
public class EpubBook implements BookInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String r2Key;

    @Enumerated(EnumType.STRING) // Explicitly safe enum handling for DB storage
    private BookType bookType;

    private String coverHref; // Fixed field name casing to maintain strict JavaBean conventions
    private int chapterCount;
    private String language;

    // New Professional Metadata Inclusions
    private String author;



    private String publisher;
    private String isbn;
    private String publicationDate;
    private String officialTitle;

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

    public String getCoverHref() {
        return coverHref;
    }

    public void setCoverHref(String coverHref) {
        this.coverHref = coverHref;
    }
    public String getOfficialTitle() {
        return officialTitle;
    }

    public void setOfficialTitle(String officialTitle) {
        this.officialTitle = officialTitle;
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

    // New Metadata Getters and Setters
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    @Override
    public String toString() {
        return "EpubBook{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", isbn='" + isbn + '\'' +
                ", publicationDate='" + publicationDate + '\'' +
                ", language='" + language + '\'' +
                ", chapterCount=" + chapterCount +
                ", r2Key='" + r2Key + '\'' +
                '}';
    }
}
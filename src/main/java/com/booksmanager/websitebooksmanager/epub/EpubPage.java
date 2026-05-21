package com.booksmanager.websitebooksmanager.epub;

public class EpubPage {

    private final int id;
    private final int playOrder;
    private final String title;
    private final String src;



    public EpubPage(int id, int playOrder, String title, String src) {
        this.id = id;
        this.playOrder = playOrder;
        this.title = title;
        this.src = src;

    }
    public int getId() {
        return id;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getSrc() {
        return src;
    }



    @Override
    public String toString() {
        return "EpubPageOrder{" +
                "id=" + id +
                ", playOrder=" + playOrder +
                ", title='" + title + '\'' +
                ", src='" + src + '\'' +
                '}';
    }
}
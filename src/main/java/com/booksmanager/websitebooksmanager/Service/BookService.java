package com.booksmanager.websitebooksmanager.Service;

import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {

    private final PdfService pdfService;
    private final EpubService epubService;

    public BookService(PdfService pdfService, EpubService epubService) {
        this.pdfService = pdfService;
        this.epubService = epubService;
    }

    public List<BookInterface> getAllBooks() {
        List<BookInterface> books = new ArrayList<>();

        books.addAll(pdfService.getAllPdfs());
        books.addAll(epubService.getAllEpubBooks());

        return books;
    }
}
package com.booksmanager.websitebooksmanager.Service;

import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BookService {

    private final PdfService pdfService;
    private final EpubService epubService;

    public BookService(PdfService pdfService, EpubService epubService) {
        this.pdfService = pdfService;
        this.epubService = epubService;
    }

    /**
     * @deprecated this is an old method
     * @return
     */
    public List<BookInterface> getAllBooks() {
        List<BookInterface> books = new ArrayList<>();

        books.addAll(pdfService.getAllPdfs());
        books.addAll(epubService.getAllEpubBooks());

        return books;
    }
    public List<BookInterface> getBooksPaged(int offset, int limit) {
        List<BookInterface> allBooks = new ArrayList<>();
        allBooks.addAll(pdfService.getAllPdfs());
        allBooks.addAll(epubService.getAllEpubBooks());

        if (offset >= allBooks.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(offset + limit, allBooks.size());
        return allBooks.subList(offset, toIndex);
    }

    public int getTotalBookCount() {
        return pdfService.getAllPdfs().size() + epubService.getAllEpubBooks().size();
    }
}
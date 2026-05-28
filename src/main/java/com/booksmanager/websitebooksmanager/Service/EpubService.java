package com.booksmanager.websitebooksmanager.Service;

import com.booksmanager.websitebooksmanager.Entities.BookInterface;
import com.booksmanager.websitebooksmanager.Entities.EpubBook;
import com.booksmanager.websitebooksmanager.Entities.EpubBookRepository;
import com.booksmanager.websitebooksmanager.Entities.PdfBookRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EpubService {


    private final EpubBookRepository epubBookRepository;

    public EpubService(EpubBookRepository epubBookRepository) {
        this.epubBookRepository = epubBookRepository;
    }

    public EpubBook saveEpub(EpubBook book) {
        return epubBookRepository.save(book);
    }

    public List<EpubBook> getAllEpubBooks() {
        return epubBookRepository.findAll();
    }

}

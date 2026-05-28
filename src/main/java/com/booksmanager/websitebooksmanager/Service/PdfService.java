package com.booksmanager.websitebooksmanager.Service;

import com.booksmanager.websitebooksmanager.Entities.PdfBook;
import com.booksmanager.websitebooksmanager.Entities.PdfBookRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PdfService {


    private final PdfBookRepository pdfRepo;

    public PdfService(PdfBookRepository pdfRepo) {
        this.pdfRepo = pdfRepo;
    }

    public PdfBook savePdf(PdfBook book) {
        return pdfRepo.save(book);
    }

    public List<PdfBook> getAllPdfs() {
        return pdfRepo.findAll();
    }

}
package com.booksmanager.websitebooksmanager.Entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpubBookRepository extends JpaRepository<EpubBook, Long> {
}
package com.bookvault.service;

import com.bookvault.dto.BookDTO;
import com.bookvault.dto.CommentDTO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface BookService {
    List<BookDTO> getAllBooks() throws ExecutionException, InterruptedException;
    List<BookDTO> getPublishedBooks() throws ExecutionException, InterruptedException;
    Optional<BookDTO> getBookById(String id) throws ExecutionException, InterruptedException;
    BookDTO createOrUpdateBook(BookDTO bookDTO) throws ExecutionException, InterruptedException;
    void deleteBook(String id) throws ExecutionException, InterruptedException;
    CommentDTO addComment(String bookId, CommentDTO commentDTO) throws ExecutionException, InterruptedException;
    BookDTO updateComment(String bookId, String commentId, String newContent, String userId, boolean isAdmin) throws ExecutionException, InterruptedException;
    BookDTO deleteComment(String bookId, String commentId, String userId, boolean isAdmin) throws ExecutionException, InterruptedException;
    void incrementReadersCount(String bookId) throws ExecutionException, InterruptedException;
    void incrementFavoritesCount(String bookId, boolean increment) throws ExecutionException, InterruptedException;
}

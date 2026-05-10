package com.bookvault.service.impl;

import com.bookvault.dto.BookDTO;
import com.bookvault.dto.CommentDTO;
import com.bookvault.entity.Book;
import com.bookvault.entity.Comment;
import com.bookvault.mapper.BookMapper;
import com.bookvault.mapper.CommentMapper;
import com.bookvault.repository.BookRepository;
import com.bookvault.service.BookService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private com.bookvault.service.FileService fileService;


    @Override
    public List<BookDTO> getAllBooks() throws ExecutionException, InterruptedException {
        return BookMapper.toDTOList(bookRepository.findAll());
    }

    @Override
    public List<BookDTO> getPublishedBooks() throws ExecutionException, InterruptedException {
        return BookMapper.toDTOList(bookRepository.findByStatus("PUBLISHED"));
    }

    @Override
    public Optional<BookDTO> getBookById(String id) throws ExecutionException, InterruptedException {
        return bookRepository.findById(id).map(BookMapper::toDTO);
    }

    @Override
    public BookDTO createOrUpdateBook(BookDTO bookDTO) throws ExecutionException, InterruptedException {
        Book book = BookMapper.toEntity(bookDTO);
        if (book.getCreatedAt() == null) {
            book.setCreatedAt(LocalDateTime.now().toString());
        }
        Book savedBook = bookRepository.save(book);
        return BookMapper.toDTO(savedBook);
    }

    @Override
    public void deleteBook(String id) throws ExecutionException, InterruptedException {
        bookRepository.findById(id).ifPresent(book -> {
            if (book.getPdfUrl() != null) {
                fileService.deleteFile(book.getPdfUrl());
            }
        });
        bookRepository.deleteById(id);
    }

    @Override
    public CommentDTO addComment(String bookId, CommentDTO commentDTO) throws ExecutionException, InterruptedException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        Comment comment = CommentMapper.toEntity(commentDTO);
        if (comment.getId() == null) {
            comment.setId(UUID.randomUUID().toString());
        }
        comment.setCreatedAt(LocalDateTime.now().toString());
        
        if (book.getComments() == null) {
            book.setComments(new ArrayList<>());
        }
        
        book.getComments().add(comment);
        bookRepository.save(book);
        return CommentMapper.toDTO(comment);
    }

    @Override
    public BookDTO updateComment(String bookId, String commentId, String newContent, String userId, boolean isAdmin) throws ExecutionException, InterruptedException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        Comment commentToUpdate = book.getComments().stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Security check: Only author or admin can update
        if (!isAdmin && !commentToUpdate.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to edit this comment");
        }
        
        commentToUpdate.setContent(newContent);
        commentToUpdate.setUpdatedAt(LocalDateTime.now().toString());
        
        Book updatedBook = bookRepository.save(book);
        return BookMapper.toDTO(updatedBook);
    }

    @Override
    public BookDTO deleteComment(String bookId, String commentId, String userId, boolean isAdmin) throws ExecutionException, InterruptedException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        
        Comment commentToDelete = book.getComments().stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Security check: Only author or admin can delete
        if (!isAdmin && !commentToDelete.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }
        
        book.getComments().remove(commentToDelete);
        Book updatedBook = bookRepository.save(book);
        return BookMapper.toDTO(updatedBook);
    }
    @Override
    public void incrementReadersCount(String bookId) throws ExecutionException, InterruptedException {
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setReadersCount(book.getReadersCount() + 1);
            try {
                bookRepository.save(book);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void incrementFavoritesCount(String bookId, boolean increment) throws ExecutionException, InterruptedException {
        bookRepository.findById(bookId).ifPresent(book -> {
            int current = book.getFavoritesCount();
            book.setFavoritesCount(increment ? current + 1 : Math.max(0, current - 1));
            try {
                bookRepository.save(book);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

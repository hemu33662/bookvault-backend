package com.bookvault.repository;

import com.bookvault.entity.Book;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class BookRepository {

    private static final String COLLECTION_NAME = "books";

    public List<Book> findAll() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = dbFirestore.collection(COLLECTION_NAME).get();
        List<Book> books = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Book book = document.toObject(Book.class);
            if (book != null) {
                book.setId(document.getId());
                books.add(book);
            }
        }
        return books;
    }

    public Optional<Book> findById(String id) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            Book book = document.toObject(Book.class);
            if (book != null) {
                book.setId(document.getId());
                return Optional.of(book);
            }
        }
        return Optional.empty();
    }

    public Book save(Book book) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (book.getId() == null) {
            ApiFuture<DocumentReference> addedDocRef = dbFirestore.collection(COLLECTION_NAME).add(book);
            book.setId(addedDocRef.get().getId());
        } else {
            dbFirestore.collection(COLLECTION_NAME).document(book.getId()).set(book);
        }
        return book;
    }

    public void deleteById(String id) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).document(id).delete();
    }

    public List<Book> findByStatus(String status) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("status", status);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Book> books = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Book book = document.toObject(Book.class);
            if (book != null) {
                book.setId(document.getId());
                books.add(book);
            }
        }
        return books;
    }
}

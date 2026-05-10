package com.bookvault.repository;

import com.bookvault.entity.BookAccess;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class BookAccessRepository {

    private static final String COLLECTION_NAME = "book_access";

    public List<BookAccess> findByUserId(String userId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<BookAccess> accessList = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            BookAccess access = document.toObject(BookAccess.class);
            if (access != null) {
                access.setId(document.getId());
                accessList.add(access);
            }
        }
        return accessList;
    }

    public Optional<BookAccess> findByUserIdAndBookId(String userId, String bookId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookId", bookId);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            BookAccess access = document.toObject(BookAccess.class);
            if (access != null) {
                access.setId(document.getId());
                return Optional.of(access);
            }
        }
        return Optional.empty();
    }

    public BookAccess save(BookAccess access) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (access.getId() == null) {
            ApiFuture<DocumentReference> addedDocRef = dbFirestore.collection(COLLECTION_NAME).add(access);
            access.setId(addedDocRef.get().getId());
        } else {
            dbFirestore.collection(COLLECTION_NAME).document(access.getId()).set(access);
        }
        return access;
    }
}

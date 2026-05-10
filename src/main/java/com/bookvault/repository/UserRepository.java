package com.bookvault.repository;

import com.bookvault.entity.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {

    private static final String COLLECTION_NAME = "users";

    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByFirebaseUid(String firebaseUid) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("firebaseUid", firebaseUid);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public User save(User user) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (user.getId() == null) {
            // Create new
            ApiFuture<DocumentReference> addedDocRef = dbFirestore.collection(COLLECTION_NAME).add(user);
            user.setId(addedDocRef.get().getId());
        } else {
            // Update existing
            dbFirestore.collection(COLLECTION_NAME).document(user.getId()).set(user);
        }
        return user;
    }
    public List<User> findAll() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = dbFirestore.collection(COLLECTION_NAME).get();
        List<User> users = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
                users.add(user);
            }
        }
        return users;
    }
}

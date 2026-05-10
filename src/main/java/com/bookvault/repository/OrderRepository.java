package com.bookvault.repository;

import com.bookvault.entity.Order;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class OrderRepository {

    private static final String COLLECTION_NAME = "orders";

    public Optional<Order> findById(String id) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference documentReference = dbFirestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            Order order = document.toObject(Order.class);
            if (order != null) {
                order.setId(document.getId());
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    public List<Order> findByUserId(String userId) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Order> orders = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Order order = document.toObject(Order.class);
            if (order != null) {
                order.setId(document.getId());
                orders.add(order);
            }
        }
        return orders;
    }

    public Order save(Order order) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (order.getId() == null) {
            ApiFuture<DocumentReference> addedDocRef = dbFirestore.collection(COLLECTION_NAME).add(order);
            order.setId(addedDocRef.get().getId());
        } else {
            dbFirestore.collection(COLLECTION_NAME).document(order.getId()).set(order);
        }
        return order;
    }

    public List<Order> findAll() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = dbFirestore.collection(COLLECTION_NAME).get();
        List<Order> orders = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Order order = document.toObject(Order.class);
            if (order != null) {
                order.setId(document.getId());
                orders.add(order);
            }
        }
        return orders;
    }
}

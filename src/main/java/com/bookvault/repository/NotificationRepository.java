package com.bookvault.repository;

import com.bookvault.entity.Notification;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class NotificationRepository {

    private static final String COLLECTION_NAME = "notifications";

    public Notification save(Notification notification) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (notification.getId() == null) {
            ApiFuture<DocumentReference> addedDocRef = dbFirestore.collection(COLLECTION_NAME).add(notification);
            notification.setId(addedDocRef.get().getId());
        } else {
            dbFirestore.collection(COLLECTION_NAME).document(notification.getId()).set(notification);
        }
        return notification;
    }

    public List<Notification> findActive() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("active", true);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Notification> notifications = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Notification notification = document.toObject(Notification.class);
            if (notification != null) {
                notification.setId(document.getId());
                notifications.add(notification);
            }
        }
        return notifications;
    }

    public List<Notification> findAll() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> querySnapshot = dbFirestore.collection(COLLECTION_NAME).get();
        List<Notification> notifications = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Notification notification = document.toObject(Notification.class);
            if (notification != null) {
                notification.setId(document.getId());
                notifications.add(notification);
            }
        }
        return notifications;
    }

    public void deleteById(String id) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).document(id).delete();
    }
}

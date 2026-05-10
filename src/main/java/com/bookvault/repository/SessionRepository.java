package com.bookvault.repository;

import com.bookvault.entity.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class SessionRepository {
    private static final String COLLECTION_NAME = "sessions";

    public List<UserSession> findAllActive() throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME).whereEqualTo("revoked", false);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<UserSession> sessions = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            UserSession session = document.toObject(UserSession.class);
            if (session != null) {
                session.setId(document.getId());
                sessions.add(session);
            }
        }
        return sessions;
    }

    public void save(UserSession session) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        if (session.getId() == null) {
            dbFirestore.collection(COLLECTION_NAME).add(session);
        } else {
            dbFirestore.collection(COLLECTION_NAME).document(session.getId()).set(session);
        }
    }

    public void revokeSession(String sessionId) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).document(sessionId).update("revoked", true);
    }
}

package com.bookvault.repository;

import com.bookvault.entity.Activity;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class ActivityRepository {
    private static final String COLLECTION_NAME = "activities";

    public List<Activity> findRecent(int limit) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Activity> activities = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Activity activity = document.toObject(Activity.class);
            if (activity != null) {
                activity.setId(document.getId());
                activities.add(activity);
            }
        }
        return activities;
    }

    public List<Activity> findByUser(String userId, int limit) throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<Activity> activities = new ArrayList<>();
        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            Activity activity = document.toObject(Activity.class);
            if (activity != null) {
                activity.setId(document.getId());
                activities.add(activity);
            }
        }
        return activities;
    }

    public void save(Activity activity) {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        dbFirestore.collection(COLLECTION_NAME).add(activity);
    }
}

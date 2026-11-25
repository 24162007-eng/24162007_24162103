package com.example.a24162007_tuan1;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class FirestoreCart {
    private FirestoreCart() {}

    static Task<CollectionReference> items() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) return Tasks.forResult(itemsFor(user.getUid()));
        return auth.signInAnonymously().onSuccessTask(result -> {
            FirebaseUser signedInUser = result.getUser();
            if (signedInUser == null) {
                return Tasks.forException(new IllegalStateException("Chưa có phiên Firebase"));
            }
            return Tasks.forResult(itemsFor(signedInUser.getUid()));
        });
    }

    // Giữ tương thích những màn hình cũ. Món cũ sẽ yêu cầu chọn tùy chọn ở giỏ.
    static Task<Void> add(long productId) {
        return items().onSuccessTask(collection ->
                collection.document(Long.toString(productId)).set(
                        Collections.singletonMap("quantity", FieldValue.increment(1L)),
                        SetOptions.merge()));
    }

    static Task<Void> add(long productId, String color, String size) {
        String key = ProductOptions.key(productId, color, size);
        Map<String, Object> data = metadata(productId, color, size);
        data.put("quantity", FieldValue.increment(1L));
        return items().onSuccessTask(collection -> collection.document(key)
                .set(data, SetOptions.merge()));
    }

    static Task<Void> changeOptions(String oldId, CheckoutItem item,
                                    String color, String size) {
        String newId = ProductOptions.key(item.product.getId(), color, size);
        if (newId.equals(oldId)) return Tasks.forResult(null);
        return items().onSuccessTask(collection -> {
            DocumentReference oldRef = collection.document(oldId);
            DocumentReference newRef = collection.document(newId);
            return FirebaseFirestore.getInstance().<Void>runTransaction(transaction -> {
                DocumentSnapshot oldDoc = transaction.get(oldRef);
                DocumentSnapshot newDoc = transaction.get(newRef);
                Long oldQuantity = oldDoc.getLong("quantity");
                Long targetQuantity = newDoc.getLong("quantity");
                if (!oldDoc.exists() || oldQuantity == null || oldQuantity <= 0) {
                    throw new IllegalStateException("Giỏ hàng đã thay đổi. Bạn mở lại nhé.");
                }
                if (newDoc.exists() && (targetQuantity == null || targetQuantity <= 0)) {
                    throw new IllegalStateException("Số lượng không hợp lệ");
                }
                Map<String, Object> data = metadata(item.product.getId(), color, size);
                data.put("quantity", Math.addExact(oldQuantity,
                        targetQuantity == null ? 0L : targetQuantity));
                transaction.set(newRef, data, SetOptions.merge());
                transaction.delete(oldRef);
                return null;
            });
        });
    }

    private static Map<String, Object> metadata(long id, String color, String size) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", Long.toString(id));
        data.put("color", color);
        data.put("size", size);
        return data;
    }

    private static CollectionReference itemsFor(String uid) {
        return FirebaseFirestore.getInstance().collection("carts")
                .document(uid).collection("items");
    }
}

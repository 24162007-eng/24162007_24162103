package com.example.a24162007_tuan1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private CheckoutItem[] items;
    private TextInputEditText recipient;
    private TextInputEditText phone;
    private TextInputEditText address;
    private MaterialButton submit;

    private String orderId;
    private boolean submitting;
    private boolean completed;

    private final NumberFormat money =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        orderId = savedInstanceState == null
                ? UUID.randomUUID().toString()
                : savedInstanceState.getString("orderId");

        if (orderId == null) {
            orderId = UUID.randomUUID().toString();
        }

        completed = savedInstanceState != null
                && savedInstanceState.getBoolean("completed");

        Object extra = getIntent().getSerializableExtra(
                CheckoutItem.EXTRA_ITEMS
        );

        if (extra instanceof CheckoutItem[]) {
            items = (CheckoutItem[]) extra;
        } else {
            Object productExtra =
                    getIntent().getSerializableExtra("name");

            if (productExtra instanceof Product) {
                items = new CheckoutItem[]{
                        new CheckoutItem(
                                (Product) productExtra, 1, null, "black",
                                ProductOptions.sizes(((Product) productExtra).getId())[0]
                        )
                };
            }
        }

        if (!validItems()) {
            Toast.makeText(
                    this, "Danh sách sản phẩm không hợp lệ",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        recipient = findViewById(R.id.edtOrderName);
        phone = findViewById(R.id.edtOrderPhone);
        address = findViewById(R.id.edtOrderAddress);
        submit = findViewById(R.id.btnPlaceOrder);

        showItems();

        findViewById(R.id.btnPaymentBack).setOnClickListener(v -> {
            if (!submitting) finish();
        });

        submit.setOnClickListener(v -> placeOrder());

        if (completed) {
            showSuccess();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("orderId", orderId);
        outState.putBoolean("completed", completed);
        super.onSaveInstanceState(outState);
    }

    private boolean validItems() {
        if (items == null || items.length == 0 || items.length > 20) {
            return false;
        }

        Set<String> ids = new HashSet<>();

        try {
            long total = 0;

            for (CheckoutItem item : items) {
                if (item == null || item.product == null
                        || item.product.getId() <= 0
                        || item.product.getPrice() < 0
                        || item.quantity <= 0
                        || !ProductOptions.valid(item.product.getId(), item.color, item.size)
                        || !ids.add(ProductOptions.key(item.product.getId(), item.color, item.size))) {
                    return false;
                }

                if (item.cartItemId != null
                        && !item.cartItemId.equals(
                        ProductOptions.key(item.product.getId(), item.color, item.size)
                )) {
                    return false;
                }

                total = Math.addExact(total, item.subtotal());
            }

            return true;
        } catch (ArithmeticException error) {
            return false;
        }
    }

    private void showItems() {
        LinearLayout container = findViewById(R.id.paymentItems);
        long total = 0;

        for (CheckoutItem item : items) {
            View row = getLayoutInflater().inflate(
                    R.layout.item_cart_product, container, false
            );

            row.findViewById(R.id.checkCartItem)
                    .setVisibility(View.GONE);

            row.findViewById(R.id.cartQuantityControls)
                    .setVisibility(View.GONE);

            ImageView image = row.findViewById(R.id.imgProduct);
            TextView name = row.findViewById(R.id.tvProductName);
            TextView quantity = row.findViewById(R.id.tvCartQuantity);
            TextView subtotal = row.findViewById(R.id.tvProductPrice);

            image.setImageResource(item.product.getImage());
            image.setContentDescription(item.product.getName());
            name.setText(item.product.getName());

            quantity.setText(
                    ProductOptions.label(item.color, item.size) + "\n"
                            + item.quantity + " × "
                            + money.format(item.product.getPrice())
            );

            subtotal.setText(money.format(item.subtotal()));
            container.addView(row);

            total = Math.addExact(total, item.subtotal());
        }

        TextView totalView = findViewById(R.id.tvPaymentTotal);
        totalView.setText(money.format(total));
    }

    private void placeOrder() {
        if (submitting || completed) return;

        String nameValue = textOf(recipient);
        String phoneValue = textOf(phone)
                .replaceAll("[\\s().-]", "");
        String addressValue = textOf(address);

        if (nameValue.isEmpty()) {
            recipient.setError("Nhập họ tên người nhận");
            recipient.requestFocus();
            return;
        }

        if (!phoneValue.matches("\\+?[0-9]{8,15}")) {
            phone.setError("Số điện thoại chưa hợp lệ");
            phone.requestFocus();
            return;
        }

        if (addressValue.length() < 10) {
            address.setError("Nhập địa chỉ nhận hàng đầy đủ");
            address.requestFocus();
            return;
        }

        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .getString("username", "");

        if (username == null || username.trim().isEmpty()) {
            Toast.makeText(
                    this, "Bạn cần đăng nhập lại",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        submitting = true;
        submit.setEnabled(false);
        submit.setText("Đang gửi yêu cầu…");

        // Tạo hoặc lấy phiên Firebase đang dùng cho giỏ hàng.
        FirestoreCart.items()
                .onSuccessTask(cartCollection -> {
                    FirebaseUser user =
                            FirebaseAuth.getInstance().getCurrentUser();

                    if (user == null) {
                        throw new IllegalStateException(
                                "Không xác định được người dùng"
                        );
                    }

                    String uid = user.getUid();
                    FirebaseFirestore db =
                            FirebaseFirestore.getInstance();

                    DocumentReference order = db.collection("users")
                            .document(uid)
                            .collection("orders")
                            .document(orderId);

                    return db.<Void>runTransaction(transaction -> {
                        // Cùng một mã yêu cầu thì không tạo đơn lần hai.
                        DocumentSnapshot existing =
                                transaction.get(order);

                        if (existing.exists()) {
                            return null;
                        }

                        List<Map<String, Object>> orderItems =
                                new ArrayList<>();

                        Map<DocumentReference, Long> remaining =
                                new HashMap<>();

                        long subtotal = 0;

                        // Đọc toàn bộ dữ liệu trước khi ghi.
                        for (CheckoutItem item : items) {
                            String productId =
                                    Long.toString(item.product.getId());

                            DocumentSnapshot product =
                                    transaction.get(
                                            db.collection("products")
                                                    .document(productId)
                                    );

                            Long currentPrice = product.getLong("price");
                            String currentName =
                                    product.getString("name");

                            if (!product.exists()
                                    || currentName == null
                                    || currentPrice == null
                                    || currentPrice < 0) {
                                throw new IllegalStateException(
                                        "Một sản phẩm không còn khả dụng. "
                                                + "Bạn quay lại giỏ hàng nhé."
                                );
                            }

                            if (currentPrice
                                    != (long) item.product.getPrice()) {
                                throw new IllegalStateException(
                                        "Giá sản phẩm đã thay đổi. "
                                                + "Bạn quay lại để kiểm tra giá mới."
                                );
                            }

                            long lineTotal = Math.multiplyExact(
                                    currentPrice, item.quantity
                            );

                            subtotal = Math.addExact(
                                    subtotal, lineTotal
                            );

                            Map<String, Object> line = new HashMap<>();
                            line.put("productId", productId);
                            line.put("name", currentName);
                            line.put("color", item.color);
                            line.put("size", item.size);
                            line.put("unitPrice", currentPrice);
                            line.put("quantity", item.quantity);
                            line.put("subtotal", lineTotal);
                            orderItems.add(line);

                            if (item.cartItemId != null) {
                                DocumentReference cartRef =
                                        cartCollection.document(
                                                item.cartItemId
                                        );

                                DocumentSnapshot cartItem =
                                        transaction.get(cartRef);

                                Long currentQuantity =
                                        cartItem.getLong("quantity");

                                if (!cartItem.exists()
                                        || currentQuantity == null
                                        || currentQuantity < item.quantity
                                        || !productId.equals(cartItem.getString("productId"))
                                        || !item.color.equals(cartItem.getString("color"))
                                        || !item.size.equals(cartItem.getString("size"))) {
                                    throw new IllegalStateException(
                                            "Giỏ hàng đã thay đổi. "
                                                    + "Bạn quay lại kiểm tra nhé."
                                    );
                                }

                                remaining.put(
                                        cartRef,
                                        currentQuantity - item.quantity
                                );
                            }
                        }

                        Map<String, Object> data = new HashMap<>();
                        data.put("uid", uid);
                        data.put("recipient", nameValue);
                        data.put("phone", phoneValue);
                        data.put("address", addressValue);
                        data.put("items", orderItems);
                        data.put("subtotal", subtotal);
                        data.put("paymentMethod", "COD");
                        data.put("status", "pending_confirmation");
                        data.put("createdAt", FieldValue.serverTimestamp());

                        transaction.set(order, data);

                        // Chỉ trừ các món nằm trong yêu cầu đặt hàng.
                        for (Map.Entry<DocumentReference, Long> entry
                                : remaining.entrySet()) {
                            if (entry.getValue() == 0) {
                                transaction.delete(entry.getKey());
                            } else {
                                transaction.update(
                                        entry.getKey(),
                                        "quantity",
                                        entry.getValue()
                                );
                            }
                        }

                        return null;
                    });
                })
                .addOnCompleteListener(this, task -> {
                    submitting = false;

                    if (task.isSuccessful()) {
                        completed = true;
                        showSuccess();
                    } else {
                        submit.setEnabled(true);
                        submit.setText("Gửi yêu cầu đặt hàng COD");

                        Exception error = task.getException();

                        Log.e("Checkout", "Không gửi được đơn", error);

                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Chưa gửi được yêu cầu")
                                .setMessage(
                                        error == null
                                                ? "Kiểm tra mạng rồi thử lại."
                                                : error.getMessage()
                                )
                                .setPositiveButton("Đóng", null)
                                .show();
                    }
                });
    }

    private void showSuccess() {
        submit.setEnabled(false);
        submit.setText("Đã gửi yêu cầu");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đã nhận yêu cầu đặt hàng")
                .setMessage(
                        "Mã yêu cầu:\n" + orderId
                                + "\n\nTrạng thái: Chờ shop xác nhận."
                                + "\nPhương thức: COD."
                )
                .setCancelable(false)
                .setPositiveButton("Về giỏ hàng", (dialog, which) -> {
                    Intent intent = new Intent(
                            this, CartActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }
}
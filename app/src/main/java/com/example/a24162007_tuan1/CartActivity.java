package com.example.a24162007_tuan1;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CartActivity extends AppCompatActivity {

    private final Map<String, CheckoutItem> entries =
            new LinkedHashMap<>();

    private final Set<String> selected = new HashSet<>();

    private final NumberFormat money =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private LinearLayout container;
    private TextView emptyLabel;
    private TextView totalLabel;
    private TextView selectedLabel;
    private View totalSection;
    private MaterialCheckBox checkAll;
    private MaterialButton checkout;

    private boolean selectionInitialized;
    private boolean syncing;
    private boolean busy;
    private boolean loading = true;
    private int version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        container = findViewById(R.id.cartItemsContainer);
        emptyLabel = findViewById(R.id.tvCartEmpty);
        totalLabel = findViewById(R.id.tvCartTotal);
        selectedLabel = findViewById(R.id.tvCartSelected);
        totalSection = findViewById(R.id.cartTotalSection);
        checkAll = findViewById(R.id.checkCartAll);
        checkout = findViewById(R.id.btnCartCheckout);

        if (savedInstanceState != null) {
            ArrayList<String> saved =
                    savedInstanceState.getStringArrayList("selected");

            if (saved != null) {
                selected.addAll(saved);
            }

            selectionInitialized =
                    savedInstanceState.getBoolean("selectionInitialized");
        }

        checkAll.setOnCheckedChangeListener((button, checked) -> {
            if (syncing || loading || busy) {
                return;
            }

            selected.clear();

            if (checked) {
                selected.addAll(entries.keySet());
            }

            renderRows();
        });

        checkout.setOnClickListener(v -> {
            if (busy || loading || selected.isEmpty()) {
                return;
            }

            ArrayList<CheckoutItem> chosen = new ArrayList<>();

            for (Map.Entry<String, CheckoutItem> entry
                    : entries.entrySet()) {
                if (selected.contains(entry.getKey())) {
                    CheckoutItem item = entry.getValue();
                    if (!ProductOptions.valid(item.product.getId(), item.color, item.size)) {
                        Toast.makeText(this, "Bạn chọn màu và size cho các món đã chọn nhé",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    chosen.add(item);
                }
            }

            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(
                    CheckoutItem.EXTRA_ITEMS,
                    chosen.toArray(new CheckoutItem[0])
            );

            startActivity(intent);
        });

        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        busy = false;
        loadCart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(
                "selected", new ArrayList<>(selected)
        );
        outState.putBoolean(
                "selectionInitialized", selectionInitialized
        );
        super.onSaveInstanceState(outState);
    }

    private void setupNavigation() {
        int active = ContextCompat.getColor(
                this, R.color.tanh_primary
        );

        findViewById(R.id.tabCart).setSelected(true);

        ImageView icon = findViewById(R.id.imgTabCart);
        TextView label = findViewById(R.id.tvTabCart);

        icon.setColorFilter(active);
        label.setTextColor(active);
        label.setTypeface(null, Typeface.BOLD);

        findViewById(R.id.tabHome).setOnClickListener(v ->
                openScreen(HomeActivity.class)
        );

        findViewById(R.id.tabProfile).setOnClickListener(v ->
                openScreen(ProfileActivity.class)
        );
    }

    private void openScreen(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(intent);
        finish();
    }

    private void loadCart() {
        final int request = ++version;
        loading = true;

        checkAll.setEnabled(false);
        checkout.setEnabled(false);
        totalSection.setVisibility(View.GONE);
        container.removeAllViews();

        emptyLabel.setVisibility(View.VISIBLE);
        emptyLabel.setText("Đang tải giỏ hàng…");

        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .getString("username", "");

        if (username == null || username.trim().isEmpty()) {
            loading = false;
            emptyLabel.setText("Bạn chưa đăng nhập");
            return;
        }

        FirestoreCart.items()
                .onSuccessTask(items -> items.get())
                .addOnSuccessListener(this, cart -> {
                    if (request != version) return;

                    if (cart.isEmpty()) {
                        finishLoading(new LinkedHashMap<>());
                        return;
                    }
                    FirebaseFirestore.getInstance().collection("products").get()
                            .addOnSuccessListener(this, products -> {
                                if (request != version) return;
                                Map<String, DocumentSnapshot> catalog = new HashMap<>();
                                for (DocumentSnapshot doc : products) catalog.put(doc.getId(), doc);
                                Map<String, CheckoutItem> loaded = new LinkedHashMap<>();
                                for (DocumentSnapshot cartDoc : cart) {
                                    Long quantity = cartDoc.getLong("quantity");
                                    String productId = cartDoc.getString("productId");
                                    if (productId == null && cartDoc.getId().matches("[0-9]+")) {
                                        productId = cartDoc.getId();
                                    }
                                    DocumentSnapshot doc = catalog.get(productId);
                                    if (doc == null || quantity == null || quantity <= 0) continue;
                                    Long id = doc.getLong("id");
                                    Long price = doc.getLong("price");
                                    String name = doc.getString("name");
                                    String imageName = doc.getString("image_name");
                                    if (id == null || id <= 0 || price == null || price < 0
                                            || price > Integer.MAX_VALUE || name == null
                                            || imageName == null) continue;
                                    String color = cartDoc.getString("color");
                                    String size = cartDoc.getString("size");
                                    color = color == null ? "" : color;
                                    size = size == null ? "" : size;
                                    boolean legacy = cartDoc.getId().equals(Long.toString(id))
                                            && color.isEmpty() && size.isEmpty();
                                    if (!legacy && (!ProductOptions.valid(id, color, size)
                                            || !cartDoc.getId().equals(ProductOptions.key(id, color, size)))) {
                                        continue;
                                    }
                                    int image = getResources().getIdentifier(imageName,
                                            "drawable", getPackageName());
                                    String description = doc.getString("description");
                                    Product product = new Product(id, name,
                                            description == null ? "" : description, image, price.intValue());
                                    loaded.put(cartDoc.getId(), new CheckoutItem(product, quantity,
                                            cartDoc.getId(), color, size));
                                }

                                finishLoading(loaded);
                            })
                            .addOnFailureListener(this, error -> {
                                if (request == version) loadError(error);
                            });
                })
                .addOnFailureListener(this, error -> {
                    if (request == version) loadError(error);
                });
    }

    private void finishLoading(Map<String, CheckoutItem> loaded) {
        entries.clear();
        entries.putAll(loaded);

        selected.retainAll(entries.keySet());

        if (!selectionInitialized) {
            selected.addAll(entries.keySet());
            selectionInitialized = true;
        }

        loading = false;
        renderRows();
    }

    private void renderRows() {
        container.removeAllViews();

        for (Map.Entry<String, CheckoutItem> entry
                : entries.entrySet()) {
            String itemId = entry.getKey();
            CheckoutItem item = entry.getValue();

            View row = getLayoutInflater().inflate(
                    R.layout.item_cart_product, container, false
            );

            ImageView image = row.findViewById(R.id.imgProduct);
            TextView title = row.findViewById(R.id.tvProductName);
            TextView unit = row.findViewById(R.id.tvCartQuantity);
            TextView amount = row.findViewById(R.id.tvProductPrice);
            TextView quantity =
                    row.findViewById(R.id.tvCartQuantityValue);

            MaterialCheckBox check =
                    row.findViewById(R.id.checkCartItem);

            MaterialButton minus =
                    row.findViewById(R.id.btnCartMinus);
            MaterialButton plus =
                    row.findViewById(R.id.btnCartPlus);
            MaterialButton remove =
                    row.findViewById(R.id.btnCartRemove);

            image.setImageResource(item.product.getImage());
            image.setContentDescription(item.product.getName());
            title.setText(item.product.getName());

            unit.setText(
                    "Đơn giá: " + money.format(item.product.getPrice())
            );

            amount.setText(money.format(item.subtotal()));
            quantity.setText(Long.toString(item.quantity));

            check.setContentDescription(
                    "Chọn " + item.product.getName()
            );
            check.setChecked(selected.contains(itemId));
            check.setEnabled(!busy);

            check.setOnCheckedChangeListener((button, checked) -> {
                if (busy) return;

                if (checked) selected.add(itemId);
                else selected.remove(itemId);

                updateSummary();
            });

            minus.setEnabled(!busy && item.quantity > 1);
            plus.setEnabled(!busy);
            remove.setEnabled(!busy);

            minus.setOnClickListener(v -> mutate(itemId, -1, false));
            plus.setOnClickListener(v -> mutate(itemId, 1, false));

            remove.setOnClickListener(v -> {
                if (busy) return;

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Xóa sản phẩm?")
                        .setMessage(item.product.getName())
                        .setNegativeButton("Giữ lại", null)
                        .setPositiveButton("Xóa", (dialog, which) ->
                                mutate(itemId, 0, true)
                        )
                        .show();
            });

            MaterialButton editOptions = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            editOptions.setText(ProductOptions.label(item.color, item.size) + "  ▾");
            editOptions.setAllCaps(false);
            editOptions.setTextSize(12);
            editOptions.setEnabled(!busy);
            ((LinearLayout) row).addView(editOptions, 1,
                    new LinearLayout.LayoutParams(-1, -2));
            editOptions.setOnClickListener(v -> {
                if (busy || loading) return;
                ProductOptions.Panel panel = new ProductOptions.Panel(this,
                        item.product.getId(), item.color, item.size);
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Chọn màu và size")
                        .setView(panel)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Áp dụng", (dialog, which) -> {
                            if (busy || loading) return;
                            busy = true;
                            renderRows();
                            boolean wasSelected = selected.contains(itemId);
                            String color = panel.color();
                            String size = panel.size();
                            FirestoreCart.changeOptions(itemId, item, color, size)
                                    .addOnCompleteListener(this, task -> {
                                        busy = false;
                                        if (task.isSuccessful()) {
                                            selected.remove(itemId);
                                            if (wasSelected) selected.add(ProductOptions.key(
                                                    item.product.getId(), color, size));
                                        } else {
                                            Log.e("Cart", "Không đổi được tùy chọn", task.getException());
                                            Toast.makeText(this, "Chưa đổi được. Bạn thử lại nhé",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        loadCart();
                                    });
                        }).show();
            });
            container.addView(row);
        }

        boolean empty = entries.isEmpty();

        emptyLabel.setText("Giỏ hàng đang trống");
        emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
        totalSection.setVisibility(empty ? View.GONE : View.VISIBLE);

        updateSummary();
    }

    private void updateSummary() {
        long total = 0;
        long quantity = 0;

        for (String id : selected) {
            CheckoutItem item = entries.get(id);

            if (item != null) {
                total = Math.addExact(total, item.subtotal());
                quantity = Math.addExact(quantity, item.quantity);
            }
        }

        totalLabel.setText(money.format(total));
        selectedLabel.setText(
                "Đã chọn " + selected.size()
                        + " loại · " + quantity + " sản phẩm"
        );

        checkout.setText("Thanh toán (" + quantity + ")");
        checkout.setEnabled(!busy && !loading && !selected.isEmpty());

        syncing = true;
        checkAll.setChecked(
                !entries.isEmpty()
                        && selected.size() == entries.size()
        );
        checkAll.setEnabled(!busy && !loading && !entries.isEmpty());
        syncing = false;
    }

    private void mutate(String id, int change, boolean delete) {
        if (busy || loading) return;

        busy = true;
        renderRows();

        FirestoreCart.items()
                .onSuccessTask(items -> {
                    DocumentReference ref = items.document(id);

                    return FirebaseFirestore.getInstance()
                            .<Void>runTransaction(transaction -> {
                                DocumentSnapshot doc =
                                        transaction.get(ref);

                                if (!doc.exists()) return null;

                                if (delete) {
                                    transaction.delete(ref);
                                } else {
                                    Long current = doc.getLong("quantity");

                                    if (current == null || current <= 0) {
                                        throw new IllegalStateException(
                                                "Số lượng không hợp lệ"
                                        );
                                    }

                                    long next =
                                            Math.addExact(current, change);

                                    if (next >= 1) {
                                        transaction.update(
                                                ref, "quantity", next
                                        );
                                    }
                                }

                                return null;
                            });
                })
                .addOnCompleteListener(this, task -> {
                    busy = false;

                    if (!task.isSuccessful()) {
                        Log.e(
                                "Cart",
                                "Không cập nhật được giỏ",
                                task.getException()
                        );

                        Toast.makeText(
                                this,
                                "Chưa cập nhật được. Bạn thử lại nhé",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    loadCart();
                });
    }

    private void loadError(Exception error) {
        loading = false;
        Log.e("Cart", "Không tải được giỏ hàng", error);
        emptyLabel.setText("Không tải được giỏ hàng. Bạn mở lại nhé.");
    }
}
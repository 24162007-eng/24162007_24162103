package com.example.a24162007_tuan1;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private final Map<String, List<Product>> groups =
            new LinkedHashMap<>();

    private LinearLayout sections;
    private TextView status;
    private TextView productCount;
    private TextInputEditText search;
    private ChipGroup chips;
    private NestedScrollView scroll;

    private boolean loading = true;
    private boolean loadFailed;

    private final NumberFormat priceFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        sections = findViewById(R.id.homeSections);
        status = findViewById(R.id.tvHomeStatus);
        productCount = findViewById(R.id.tvProductCount);
        search = findViewById(R.id.edtSearchProducts);
        chips = findViewById(R.id.categoryChips);
        scroll = findViewById(R.id.homeScroll);

        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .getString("username", "");

        if (username == null || username.trim().isEmpty()) {
            username = "bạn";
        }

        TextView welcome = findViewById(R.id.tvWelcome);
        welcome.setText("Chào " + username + " ♡");

        setupSearch();
        setupNavigation();
        setupFooter();

        chips.setOnCheckedStateChangeListener((group, checkedIds) ->
                renderSections()
        );

        findViewById(R.id.btnExplore).setOnClickListener(v -> {
            search.setText("");
            chips.check(R.id.chipAll);

            scroll.post(() ->
                    scroll.smoothScrollTo(
                            0,
                            ((View) search.getParent().getParent()).getTop()
                    )
            );
        });

        loadProducts();
    }

    private void setupSearch() {
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text, int start, int before, int count
            ) {
                renderSections();
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });

        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager keyboard =
                        (InputMethodManager) getSystemService(
                                INPUT_METHOD_SERVICE
                        );

                if (keyboard != null) {
                    keyboard.hideSoftInputFromWindow(
                            search.getWindowToken(), 0
                    );
                }

                search.clearFocus();
                renderSections();
                return true;
            }

            return false;
        });
    }

    private void setupNavigation() {
        findViewById(R.id.tabHome).setSelected(true);

        int activeColor = ContextCompat.getColor(
                this, R.color.tanh_primary
        );

        ImageView icon = findViewById(R.id.imgTabHome);
        TextView label = findViewById(R.id.tvTabHome);

        icon.setColorFilter(activeColor);
        label.setTextColor(activeColor);
        label.setTypeface(null, Typeface.BOLD);

        findViewById(R.id.tabHome).setOnClickListener(v ->
                scroll.smoothScrollTo(0, 0)
        );

        findViewById(R.id.tabCart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class))
        );

        findViewById(R.id.tabProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void setupFooter() {
        findViewById(R.id.btnFooterAbout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Về tanh.")
                        .setMessage(
                                "tanh. là ứng dụng mua sắm minh họa "
                                        + "cho bài tập Lập trình di động.\n\n"
                                        + "Ứng dụng sử dụng giao diện Material 3 "
                                        + "và Cloud Firestore để lưu dữ liệu."
                        )
                        .setPositiveButton("Đóng", null)
                        .show()
        );

        findViewById(R.id.btnFooterGuide).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Hướng dẫn mua sắm")
                        .setMessage(
                                "1. Tìm sản phẩm bằng tên hoặc danh mục.\n\n"
                                        + "2. Chạm vào sản phẩm để xem chi tiết.\n\n"
                                        + "3. Chọn Thêm vào giỏ để lưu vào giỏ hàng, "
                                        + "hoặc Mua ngay để mở màn hình thanh toán.\n\n"
                                        + "Phần đặt hàng hiện là minh họa cho bài tập."
                        )
                        .setPositiveButton("Đã hiểu", null)
                        .show()
        );

        findViewById(R.id.btnFooterCart).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class))
        );

        findViewById(R.id.btnFooterProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }

    private void loadProducts() {
        loading = true;
        loadFailed = false;
        renderSections();

        FirebaseFirestore.getInstance()
                .collection("products")
                .orderBy("id")
                .get()
                .addOnSuccessListener(this, result -> {
                    groups.clear();
                    groups.put("Trang phục", new ArrayList<>());
                    groups.put("Giày & túi", new ArrayList<>());
                    groups.put("Phụ kiện", new ArrayList<>());
                    groups.put("Sản phẩm khác", new ArrayList<>());

                    for (QueryDocumentSnapshot document : result) {
                        Long id = document.getLong("id");
                        Long price = document.getLong("price");

                        String name = document.getString("name");
                        String description =
                                document.getString("description");
                        String imageName =
                                document.getString("image_name");

                        if (id == null || id <= 0
                                || price == null || price < 0
                                || price > Integer.MAX_VALUE
                                || name == null || name.trim().isEmpty()
                                || imageName == null) {
                            Log.w(
                                    "FirestoreProducts",
                                    "Bỏ dữ liệu thiếu: " + document.getId()
                            );
                            continue;
                        }

                        int image = getResources().getIdentifier(
                                imageName, "drawable", getPackageName()
                        );

                        if (image == 0) {
                            continue;
                        }

                        Product product = new Product(
                                id,
                                name,
                                description == null ? "" : description,
                                image,
                                price.intValue()
                        );

                        List<Product> category =
                                groups.get(categoryFor(imageName));

                        if (category != null) {
                            category.add(product);
                        }
                    }

                    loading = false;
                    renderSections();
                })
                .addOnFailureListener(this, error -> {
                    loading = false;
                    loadFailed = true;
                    renderSections();

                    Log.e(
                            "FirestoreProducts",
                            "Tải sản phẩm thất bại",
                            error
                    );
                });
    }

    private void renderSections() {
        sections.removeAllViews();

        if (loading || loadFailed) {
            productCount.setText("");
            status.setVisibility(View.VISIBLE);

            status.setText(
                    loading
                            ? "Đang tải sản phẩm…"
                            : "Không tải được sản phẩm. "
                            + "Kiểm tra mạng rồi mở lại Trang chủ."
            );
            return;
        }

        String query = normalize(
                search.getText() == null
                        ? ""
                        : search.getText().toString()
        );

        String selectedCategory = selectedCategory();
        int total = 0;

        for (Map.Entry<String, List<Product>> entry
                : groups.entrySet()) {

            if (!selectedCategory.isEmpty()
                    && !selectedCategory.equals(entry.getKey())) {
                continue;
            }

            List<Product> matches = new ArrayList<>();

            for (Product product : entry.getValue()) {
                if (matchesSearch(product.getName(), query)) {
                    matches.add(product);
                }
            }

            if (!matches.isEmpty()) {
                addSection(entry.getKey(), matches);
                total += matches.size();
            }
        }

        productCount.setText(
                query.isEmpty()
                        ? total + " sản phẩm"
                        : total + " sản phẩm phù hợp"
        );

        status.setVisibility(total == 0 ? View.VISIBLE : View.GONE);

        status.setText(
                query.isEmpty()
                        ? "Chưa có sản phẩm trong danh mục này."
                        : "Không tìm thấy sản phẩm.\n"
                        + "Thử tên khác hoặc chọn danh mục Tất cả nhé."
        );
    }

    private boolean matchesSearch(String name, String query) {
        String normalizedName = normalize(name);

        if (query.isEmpty()) {
            return true;
        }

        // Mọi từ khóa đều phải xuất hiện trong tên sản phẩm.
        for (String word : query.split("\\s+")) {
            if (!normalizedName.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String text) {
        return Normalizer.normalize(
                        text.toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .trim();
    }

    private String selectedCategory() {
        int checked = chips.getCheckedChipId();

        if (checked == R.id.chipClothing) return "Trang phục";
        if (checked == R.id.chipBags) return "Giày & túi";
        if (checked == R.id.chipAccessories) return "Phụ kiện";

        return "";
    }

    private String categoryFor(String imageName) {
        switch (imageName) {
            case "ao_thun":
            case "vay_dai":
                return "Trang phục";

            case "giay_cao_got":
            case "tui_xach":
                return "Giày & túi";

            case "dong_ho":
            case "kinh_mat":
                return "Phụ kiện";

            default:
                return "Sản phẩm khác";
        }
    }

    private void addSection(String name, List<Product> products) {
        TextView heading = new TextView(this);
        heading.setText(name);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(
                ContextCompat.getColor(this, R.color.tanh_ink)
        );

        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        headingParams.setMargins(dp(4), dp(22), dp(4), dp(14));
        sections.addView(heading, headingParams);

        for (int index = 0; index < products.size(); index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            rowParams.bottomMargin = dp(16);
            sections.addView(row, rowParams);

            for (int column = 0; column < 2; column++) {
                int position = index + column;

                View cell = position < products.size()
                        ? productCard(products.get(position), row)
                        : new android.widget.Space(this);

                LinearLayout.LayoutParams cellParams =
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        );

                if (column == 0) {
                    cellParams.setMarginEnd(dp(6));
                } else {
                    cellParams.setMarginStart(dp(6));
                }

                row.addView(cell, cellParams);
            }
        }
    }

    private View productCard(Product product, LinearLayout parent) {
        View card = getLayoutInflater().inflate(
                R.layout.item_product_grid, parent, false
        );

        ImageView image = card.findViewById(R.id.imgProduct);
        TextView title = card.findViewById(R.id.tvProductName);
        TextView price = card.findViewById(R.id.tvProductPrice);

        image.setImageResource(product.getImage());
        image.setContentDescription(product.getName());
        title.setText(product.getName());
        price.setText(priceFormat.format(product.getPrice()));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("name", product);
            startActivity(intent);
        });

        return card;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
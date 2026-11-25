package com.example.a24162007_tuan1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private NumberFormat priceFormat;
    private ProductOptions.Panel options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        Object extra = getIntent().getSerializableExtra("name");

        if (!(extra instanceof Product)) {
            Toast.makeText(
                    this,
                    "Không tìm thấy sản phẩm",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        Product product = (Product) extra;

        priceFormat = NumberFormat.getCurrencyInstance(
                new Locale("vi", "VN")
        );

        showProduct(product);
        options = new ProductOptions.Panel(this, product.getId(),
                savedInstanceState == null ? "black" : savedInstanceState.getString("optionColor", "black"),
                savedInstanceState == null ? ProductOptions.sizes(product.getId())[0]
                        : savedInstanceState.getString("optionSize", ""));
        TextView priceView = findViewById(R.id.tvDetailPrice);
        LinearLayout information = (LinearLayout) priceView.getParent();
        information.addView(options, information.indexOfChild(priceView) + 1);
        setupActions(product);
        loadRecommendations(product.getId());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (options != null) {
            outState.putString("optionColor", options.color());
            outState.putString("optionSize", options.size());
        }
        super.onSaveInstanceState(outState);
    }

    private void showProduct(Product product) {
        ImageView image = findViewById(R.id.imgDetail);
        TextView name = findViewById(R.id.tvDetailName);
        TextView code = findViewById(R.id.tvDetailCode);
        TextView price = findViewById(R.id.tvDetailPrice);
        TextView description = findViewById(R.id.tvDetailDesc);
        TextView category = findViewById(R.id.tvDetailCategory);
        TextView style = findViewById(R.id.tvDetailStyle);
        TextView styling = findViewById(R.id.tvDetailStyling);
        TextView care = findViewById(R.id.tvDetailCare);

        image.setImageResource(product.getImage());
        image.setContentDescription(product.getName());

        name.setText(product.getName());
        code.setText("Mã sản phẩm: " + product.getId());
        price.setText(priceFormat.format(product.getPrice()));

        String originalDescription = product.getDescription();

        if (originalDescription == null
                || originalDescription.trim().isEmpty()) {
            description.setText("Thông tin sản phẩm đang được cập nhật.");
        } else {
            description.setText(originalDescription.trim());
        }

        category.setText(categoryFor(product.getId()));
        style.setText(styleFor(product.getId()));
        styling.setText(stylingFor(product.getId()));
        care.setText(careFor(product.getId()));
    }

    private void setupActions(Product product) {
        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            String username = getSharedPreferences(
                    "MyPrefs", MODE_PRIVATE
            ).getString("username", "");

            if (username == null || username.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        "Bạn chưa đăng nhập",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (product.getId() <= 0) {
                Toast.makeText(
                        this,
                        "Hãy mở lại sản phẩm từ Trang chủ",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            v.setEnabled(false);

            FirestoreCart.add(product.getId(), options.color(), options.size())
                    .addOnCompleteListener(this, task -> {
                        v.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    this,
                                    "Đã thêm vào giỏ hàng",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.e(
                                    "FirestoreCart",
                                    "Không thêm được sản phẩm",
                                    task.getException()
                            );

                            Toast.makeText(
                                    this,
                                    "Không thêm được vào giỏ",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        findViewById(R.id.btnOpenPayment).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DetailActivity.this,
                    PaymentActivity.class
            );

            intent.putExtra(CheckoutItem.EXTRA_ITEMS, new CheckoutItem[]{
                    new CheckoutItem(product, 1, null, options.color(), options.size())
            });
            startActivity(intent);
        });

        findViewById(R.id.btnExploreRecommendations)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            DetailActivity.this,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    startActivity(intent);
                    finish();
                });
    }

    private void loadRecommendations(long currentProductId) {
        View section = findViewById(R.id.recommendationSection);
        LinearLayout container =
                findViewById(R.id.recommendationItems);

        section.setVisibility(View.GONE);
        container.removeAllViews();

        FirebaseFirestore.getInstance()
                .collection("products")
                .orderBy("id")
                .limit(5)
                .get()
                .addOnSuccessListener(this, snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    int displayed = 0;

                    for (QueryDocumentSnapshot document : snapshot) {
                        Long id = document.getLong("id");
                        Long price = document.getLong("price");

                        String name = document.getString("name");
                        String description =
                                document.getString("description");
                        String imageName =
                                document.getString("image_name");

                        if (id == null
                                || id <= 0
                                || id == currentProductId
                                || price == null
                                || price < 0
                                || price > Integer.MAX_VALUE
                                || name == null
                                || name.trim().isEmpty()
                                || imageName == null) {
                            continue;
                        }

                        int imageId = getResources().getIdentifier(
                                imageName,
                                "drawable",
                                getPackageName()
                        );

                        if (imageId == 0) {
                            continue;
                        }

                        Product suggestedProduct = new Product(
                                id,
                                name,
                                description == null ? "" : description,
                                imageId,
                                price.intValue()
                        );

                        container.addView(
                                createRecommendationCard(suggestedProduct)
                        );

                        displayed++;

                        if (displayed == 4) {
                            break;
                        }
                    }

                    if (displayed > 0) {
                        section.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(this, error ->
                        Log.e(
                                "DetailRecommendations",
                                "Không tải được sản phẩm gợi ý",
                                error
                        )
                );
    }

    private View createRecommendationCard(Product product) {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        dp(184),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMarginEnd(dp(12));
        card.setLayoutParams(cardParams);

        card.setRadius(dp(18));
        card.setCardElevation(0);
        card.setCardBackgroundColor(color(R.color.tanh_surface));
        card.setStrokeColor(color(R.color.tanh_line));
        card.setStrokeWidth(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        card.addView(
                content,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                )
        );

        MaterialCardView imageFrame = new MaterialCardView(this);
        imageFrame.setRadius(dp(12));
        imageFrame.setCardElevation(0);
        imageFrame.setStrokeWidth(0);
        imageFrame.setCardBackgroundColor(color(R.color.tanh_image));

        content.addView(
                imageFrame,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(190)
                )
        );

        ImageView image = new ImageView(this);
        image.setImageResource(product.getImage());
        image.setContentDescription(product.getName());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(8), dp(8), dp(8), dp(8));

        imageFrame.addView(
                image,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        TextView title = new TextView(this);
        title.setText(product.getName());
        title.setTextColor(color(R.color.tanh_ink));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
        );
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(48)
                );

        titleParams.topMargin = dp(12);
        content.addView(title, titleParams);

        TextView price = new TextView(this);
        price.setText(priceFormat.format(product.getPrice()));
        price.setTextColor(color(R.color.tanh_primary));
        price.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        price.setTypeface(
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
        );

        LinearLayout.LayoutParams priceParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        priceParams.topMargin = dp(6);
        content.addView(price, priceParams);

        MaterialButton button = new MaterialButton(this);
        button.setText("Xem sản phẩm");
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTextColor(color(R.color.tanh_surface));
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setCornerRadius(dp(12));

        button.setBackgroundTintList(
                ColorStateList.valueOf(color(R.color.tanh_primary))
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.topMargin = dp(14);
        content.addView(button, buttonParams);

        View.OnClickListener openProduct = v -> {
            Intent intent = new Intent(
                    DetailActivity.this,
                    DetailActivity.class
            );

            intent.putExtra("name", product);
            startActivity(intent);

            // Thay trang hiện tại, tránh chồng nhiều trang chi tiết.
            finish();
        };

        card.setOnClickListener(openProduct);
        button.setOnClickListener(openProduct);

        return card;
    }

    private String categoryFor(long id) {
        if (id == 1) return "Giày";
        if (id == 2) return "Túi xách";
        if (id == 3 || id == 4) return "Trang phục";
        if (id == 5 || id == 6) return "Phụ kiện";

        return "Sản phẩm";
    }

    private String styleFor(long id) {
        if (id == 1) return "Thanh lịch, nhẹ nhàng";
        if (id == 2) return "Tối giản, hằng ngày";
        if (id == 3) return "Năng động, thoải mái";
        if (id == 4) return "Nữ tính, thanh lịch";
        if (id == 5) return "Gọn gàng, tinh tế";
        if (id == 6) return "Hiện đại, cá tính";

        return "Theo cá tính riêng";
    }

    private String stylingFor(long id) {
        if (id == 1) {
            return "• Kết hợp cùng váy dài cho những buổi gặp gỡ "
                    + "hoặc dạo phố nhẹ nhàng.\n\n"
                    + "• Phối với quần suông và áo sơ mi khi muốn "
                    + "một tổng thể gọn gàng, thanh lịch.\n\n"
                    + "• Chọn túi và phụ kiện có màu sắc hài hòa "
                    + "để đôi giày trở thành điểm nhấn vừa đủ.";
        }

        if (id == 2) {
            return "• Phối cùng áo thun và quần jeans cho những ngày "
                    + "muốn ăn mặc đơn giản.\n\n"
                    + "• Kết hợp với váy hoặc áo sơ mi khi cần "
                    + "một tổng thể chỉn chu hơn.\n\n"
                    + "• Thử chọn một màu chủ đạo trên trang phục "
                    + "rồi dùng túi xách để cân bằng bộ đồ.";
        }

        if (id == 3) {
            return "• Kết hợp cùng quần jeans và giày thể thao "
                    + "cho phong cách năng động hằng ngày.\n\n"
                    + "• Phối với chân váy hoặc quần suông "
                    + "để thay đổi cảm giác của bộ trang phục.\n\n"
                    + "• Có thể khoác thêm sơ mi hoặc áo khoác nhẹ, "
                    + "giữ phụ kiện đơn giản để tổng thể thoáng mắt.";
        }

        if (id == 4) {
            return "• Kết hợp với giày cao gót khi muốn tạo "
                    + "một bộ trang phục thanh lịch.\n\n"
                    + "• Thử phối cùng giày bệt và túi nhỏ "
                    + "cho những buổi dạo phố nhẹ nhàng.\n\n"
                    + "• Ưu tiên phụ kiện vừa phải và màu sắc "
                    + "đồng điệu để giữ sự cân đối.";
        }

        if (id == 5) {
            return "• Phối cùng áo sơ mi hoặc áo thun trơn "
                    + "để tạo điểm nhấn ở cổ tay.\n\n"
                    + "• Chọn thêm một món trang sức nhỏ "
                    + "nếu muốn bộ trang phục có nhiều chi tiết hơn.\n\n"
                    + "• Giữ các phụ kiện có màu sắc hài hòa "
                    + "để tổng thể gọn gàng.";
        }

        if (id == 6) {
            return "• Kết hợp cùng áo thun, quần jeans và túi xách "
                    + "cho phong cách dạo phố.\n\n"
                    + "• Thử phối với trang phục ít họa tiết "
                    + "để chiếc kính trở thành điểm nhấn.\n\n"
                    + "• Chọn thêm một phụ kiện đồng điệu "
                    + "về màu sắc để bộ đồ có sự liên kết.";
        }

        return "• Bắt đầu với trang phục có màu sắc đơn giản.\n\n"
                + "• Chọn một điểm nhấn chính cho bộ đồ.\n\n"
                + "• Điều chỉnh cách phối theo sở thích "
                + "và hoàn cảnh sử dụng của bạn.";
    }

    private String careFor(long id) {
        if (id == 1) {
            return "• Tham khảo hướng dẫn vệ sinh phù hợp "
                    + "với chất liệu thực tế của đôi giày.\n\n"
                    + "• Tránh chà xát mạnh hoặc dùng hóa chất "
                    + "khi chưa kiểm tra độ phù hợp.\n\n"
                    + "• Cất ở nơi khô thoáng, hạn chế đè ép "
                    + "làm biến dạng giày.";
        }

        if (id == 2) {
            return "• Kiểm tra hướng dẫn chăm sóc của túi "
                    + "trước khi lựa chọn cách làm sạch.\n\n"
                    + "• Sắp xếp đồ dùng vừa phải, tránh nhồi "
                    + "quá nhiều làm thay đổi phom túi.\n\n"
                    + "• Khi không sử dụng, cất túi ở nơi "
                    + "khô thoáng và tránh đè ép.";
        }

        if (id == 3 || id == 4) {
            return "• Ưu tiên làm theo hướng dẫn "
                    + "trên nhãn giặt của sản phẩm.\n\n"
                    + "• Phân loại quần áo theo màu trước khi giặt "
                    + "và kiểm tra độ phù hợp của chất tẩy rửa.\n\n"
                    + "• Chọn cách phơi, là và cất giữ "
                    + "phù hợp với chất liệu ghi trên nhãn.";
        }

        if (id == 5) {
            return "• Tham khảo hướng dẫn của nhà sản xuất "
                    + "trước khi vệ sinh đồng hồ.\n\n"
                    + "• Tránh va đập và không tự mở vỏ đồng hồ.\n\n"
                    + "• Kiểm tra thông số chống nước thực tế "
                    + "trước khi để sản phẩm tiếp xúc với nước.";
        }

        if (id == 6) {
            return "• Dùng khăn chuyên dụng sạch và tham khảo "
                    + "hướng dẫn vệ sinh tròng kính.\n\n"
                    + "• Cất trong hộp khi không sử dụng, "
                    + "tránh đặt mặt kính trực tiếp xuống bàn.\n\n"
                    + "• Không để kính chịu lực đè "
                    + "hoặc nhiệt độ cao.";
        }

        return "• Tham khảo hướng dẫn chăm sóc đi kèm sản phẩm.\n\n"
                + "• Chọn cách vệ sinh phù hợp với chất liệu thực tế.\n\n"
                + "• Bảo quản phù hợp và tránh đè ép.";
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private int color(int resourceId) {
        return ContextCompat.getColor(this, resourceId);
    }
}
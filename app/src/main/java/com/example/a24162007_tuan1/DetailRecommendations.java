package com.example.a24162007_tuan1;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public final class DetailRecommendations {

    private DetailRecommendations() {
        // Không cần tạo đối tượng cho lớp hỗ trợ này.
    }

    public static void load(
            AppCompatActivity activity,
            long currentProductId
    ) {
        View section =
                activity.findViewById(R.id.recommendationSection);

        LinearLayout container =
                activity.findViewById(R.id.recommendationItems);

        if (section == null || container == null) {
            return;
        }

        section.setVisibility(View.GONE);
        container.removeAllViews();

        activity.findViewById(R.id.btnExploreRecommendations)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            activity,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    activity.startActivity(intent);
                    activity.finish();
                });

        NumberFormat priceFormat =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );

        FirebaseFirestore.getInstance()
                .collection("products")
                .orderBy("id")
                .limit(5)
                .get()
                .addOnSuccessListener(activity, snapshot -> {
                    if (activity.isFinishing()
                            || activity.isDestroyed()) {
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

                        // Bỏ sản phẩm hiện tại hoặc dữ liệu chưa hợp lệ.
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

                        int imageId = activity.getResources()
                                .getIdentifier(
                                        imageName,
                                        "drawable",
                                        activity.getPackageName()
                                );

                        if (imageId == 0) {
                            continue;
                        }

                        Product product = new Product(
                                id,
                                name,
                                description == null ? "" : description,
                                imageId,
                                price.intValue()
                        );

                        View card = activity.getLayoutInflater()
                                .inflate(
                                        R.layout.item_detail_recommendation,
                                        container,
                                        false
                                );

                        ImageView image = card.findViewById(
                                R.id.imgRecommendation
                        );

                        TextView title = card.findViewById(
                                R.id.tvRecommendationName
                        );

                        TextView amount = card.findViewById(
                                R.id.tvRecommendationPrice
                        );

                        image.setImageResource(product.getImage());
                        image.setContentDescription(product.getName());

                        title.setText(product.getName());

                        amount.setText(
                                priceFormat.format(product.getPrice())
                        );

                        View.OnClickListener openProduct = v -> {
                            Intent intent = new Intent(
                                    activity,
                                    DetailActivity.class
                            );

                            // Truyền một đối tượng Product qua Intent.
                            intent.putExtra("name", product);

                            activity.startActivity(intent);

                            // Thay trang chi tiết hiện tại để tránh
                            // tích lũy nhiều trang khi xem gợi ý liên tục.
                            activity.finish();
                        };

                        card.setOnClickListener(openProduct);

                        card.findViewById(R.id.btnViewRecommendation)
                                .setOnClickListener(openProduct);

                        container.addView(card);
                        displayed++;

                        if (displayed == 4) {
                            break;
                        }
                    }

                    if (displayed > 0) {
                        section.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(activity, error -> {
                    // Gợi ý tải lỗi không ảnh hưởng sản phẩm chính.
                    Log.e(
                            "DetailRecommendations",
                            "Không tải được sản phẩm gợi ý",
                            error
                    );
                });
    }
}
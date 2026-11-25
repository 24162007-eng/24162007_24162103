package com.example.a24162007_tuan1;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        // 1. Mảng Product
        List<Product> products = new ArrayList<>();
        products.add(new Product("Giày cao gót", "Cao 7cm, đế êm, màu đỏ", R.drawable.giay_cao_got));
        products.add(new Product("Túi xách", "Da PU, nhiều ngăn tiện lợi", R.drawable.tui_xach));
        products.add(new Product("Áo thun", "Cotton 100%, thoáng mát", R.drawable.ao_thun));
        products.add(new Product("Váy dài", "Chất voan nhẹ, phong cách nữ tính", R.drawable.vay_dai));
        products.add(new Product("Đồng hồ", "Dây da, chống nước", R.drawable.dong_ho));
        products.add(new Product("Kính mát", "Chống tia UV, gọng nhẹ", R.drawable.kinh_mat));

        // 2. Adapter
        ProductAdapter adapter = new ProductAdapter(this, products);

        // 3. Gắn adapter vào ListView
        ListView lv = findViewById(R.id.lvProduct);
        lv.setAdapter(adapter);

        // Bấm vào 1 ô thì hiện tên
        lv.setOnItemClickListener((parent, view, position, id) ->
                Toast.makeText(this, products.get(position).getName(), Toast.LENGTH_SHORT).show());
    }
}
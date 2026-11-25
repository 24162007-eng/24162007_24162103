package com.example.a24162007_tuan1;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class GridActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
        String[] items = new String[24];
        for (int i = 0; i < items.length; i++) items[i] = "Item " + (i + 1);
        GridView g = findViewById(R.id.gridView);
        g.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        g.setOnItemClickListener((p, v, pos, id) ->
                Toast.makeText(this, items[pos], Toast.LENGTH_SHORT).show());
    }
}
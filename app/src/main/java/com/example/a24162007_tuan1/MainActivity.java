package com.example.a24162007_tuan1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        open(R.id.btnLinear, LinearActivity.class);
        open(R.id.btnRelative, RelativeActivity.class);
        open(R.id.btnTable, TableActivity.class);
        open(R.id.btnFrame, FrameActivity.class);
        open(R.id.btnAbsolute, AbsoluteActivity.class);
        open(R.id.btnList, ListViewActivity.class);
        open(R.id.btnGrid, GridActivity.class);
        open(R.id.btnControls, ControlsActivity.class);
        open(R.id.btnWeb, WebActivity.class);
        open(R.id.btnDialog, DialogActivity.class);
        open(R.id.btnProduct, ProductActivity.class);
        open(R.id.btnLogin, LoginActivity.class);
    }

    private void open(int id, Class<?> target) {
        Button b = findViewById(id);
        b.setOnClickListener(v -> startActivity(new Intent(this, target)));
    }
}
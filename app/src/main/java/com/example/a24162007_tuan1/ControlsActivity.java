package com.example.a24162007_tuan1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ControlsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);
        TextView tv = findViewById(R.id.tvResult);
        EditText edt = findViewById(R.id.edtName);
        Button btn = findViewById(R.id.btnShow);
        ImageButton img = findViewById(R.id.imgBtn);
        btn.setOnClickListener(v -> tv.setText("Xin chào " + edt.getText().toString()));
        img.setOnClickListener(v ->
                Toast.makeText(this, "Bạn bấm ImageButton", Toast.LENGTH_SHORT).show());
    }
}
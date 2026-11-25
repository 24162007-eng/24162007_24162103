package com.example.a24162007_tuan1;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class DialogActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        findViewById(R.id.btnToast).setOnClickListener(v ->
                Toast.makeText(this, "Đây là Toast", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnAlert).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có chắc không?")
                        .setPositiveButton("Có", (d, w) ->
                                Toast.makeText(this, "Bạn chọn Có", Toast.LENGTH_SHORT).show())
                        .setNegativeButton("Không", null)
                        .show());

        findViewById(R.id.btnPrompt).setOnClickListener(v -> {
            EditText input = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Nhập tên của bạn")
                    .setView(input)
                    .setPositiveButton("OK", (d, w) ->
                            Toast.makeText(this, "Xin chào " + input.getText(), Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        findViewById(R.id.btnMenu).setOnClickListener(this::showMenu);
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_about) {
                Toast.makeText(this, "Bài tập UI - Tuần 1", Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == R.id.menu_exit) {
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }
}
package com.example.a24162007_tuan1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class LoginActivity extends AppCompatActivity {

    public static String loggedInUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        EditText edtUser = findViewById(R.id.edtUser);
        EditText edtPass = findViewById(R.id.edtPass);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = edtUser.getText().toString().trim();
            String password = edtPass.getText().toString();

            if (username.isEmpty()) {
                edtUser.setError("Bạn chưa nhập tên đăng nhập");
                edtUser.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                edtPass.setError("Bạn chưa nhập mật khẩu");
                edtPass.requestFocus();
                return;
            }

            getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("username", username)
                    .apply();

            Intent intent = new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            );
            startActivity(intent);
            finish();
        });
    }
}
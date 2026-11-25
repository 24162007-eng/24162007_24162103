package com.example.a24162007_tuan1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    private ImageView avatar;
    private TextInputEditText edtFullName;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPhone;

    private TextInputLayout inputFullName;
    private TextInputLayout inputEmail;
    private TextInputLayout inputPhone;

    private MaterialButton btnSave;
    private MaterialButton btnAvatar;
    private TextView status;

    private String profileUid;
    private boolean restoredForm;

    private final ActivityResultLauncher<String[]> avatarPicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null && profileUid != null) {
                            saveAvatar(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanh);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        restoredForm = savedInstanceState != null;

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        avatar = findViewById(R.id.imgProfileAvatar);

        edtFullName = findViewById(R.id.edtProfileFullName);
        edtEmail = findViewById(R.id.edtProfileEmail);
        edtPhone = findViewById(R.id.edtProfilePhone);

        inputFullName = findViewById(R.id.inputProfileFullName);
        inputEmail = findViewById(R.id.inputProfileEmail);
        inputPhone = findViewById(R.id.inputProfilePhone);

        btnSave = findViewById(R.id.btnSaveProfile);
        btnAvatar = findViewById(R.id.btnChangeAvatar);
        status = findViewById(R.id.tvProfileStatus);

        showDefaultAvatar();
        setupNavigation();

        String username = prefs.getString("username", "");

        if (username == null || username.trim().isEmpty()) {
            openLogin();
            return;
        }

        TextView heading = findViewById(R.id.tvProfileUsername);
        TextView accountName = findViewById(R.id.tvAccountUsername);

        heading.setText(username);
        accountName.setText(username);

        btnAvatar.setEnabled(false);

        btnAvatar.setOnClickListener(v ->
                avatarPicker.launch(new String[]{"image/*"})
        );

        btnSave.setOnClickListener(v -> saveProfile());

        findViewById(R.id.btnProfileLogout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Đăng xuất?")
                        .setMessage(
                                "Bạn có muốn quay về màn hình đăng nhập không?"
                        )
                        .setNegativeButton("Ở lại", null)
                        .setPositiveButton("Đăng xuất", (dialog, which) -> {
                            prefs.edit().remove("username").apply();
                            openLogin();
                        })
                        .show()
        );

        connectProfile();
    }

    private void connectProfile() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            loadProfile(user.getUid());
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(this, result -> {
                    FirebaseUser signedInUser = result.getUser();

                    if (signedInUser == null) {
                        showConnectionError();
                        return;
                    }

                    loadProfile(signedInUser.getUid());
                })
                .addOnFailureListener(this, error -> {
                    Log.e("Profile", "Không tạo được phiên Firebase", error);
                    showConnectionError();
                });
    }

    private void loadProfile(String uid) {
        profileUid = uid;
        btnAvatar.setEnabled(true);
        restoreAvatar();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this, document -> {
                    // Tránh ghi đè nội dung Android vừa khôi phục
                    // khi màn hình được tạo lại.
                    if (!restoredForm) {
                        edtFullName.setText(
                                document.getString("fullName")
                        );
                        edtEmail.setText(
                                document.getString("email")
                        );
                        edtPhone.setText(
                                document.getString("phone")
                        );
                    }

                    btnSave.setEnabled(true);

                    status.setText(
                            document.exists()
                                    ? "Thông tin hồ sơ đã được tải."
                                    : "Bạn có thể bổ sung thông tin bên trên."
                    );
                })
                .addOnFailureListener(this, error -> {
                    Log.e("Profile", "Không tải được hồ sơ", error);

                    // Không cho lưu khi chưa đọc được hồ sơ cũ,
                    // tránh vô tình ghi đè bằng các ô đang trống.
                    btnSave.setEnabled(false);

                    status.setText(
                            "Không tải được hồ sơ. Kiểm tra mạng và "
                                    + "Firestore Rules rồi mở lại màn hình."
                    );
                });
    }

    private void saveProfile() {
        if (profileUid == null) {
            return;
        }

        inputFullName.setError(null);
        inputEmail.setError(null);
        inputPhone.setError(null);

        String fullName = textOf(edtFullName);
        String email = textOf(edtEmail);
        String phone = textOf(edtPhone);

        if (fullName.isEmpty()) {
            inputFullName.setError("Bạn chưa nhập họ và tên");
            edtFullName.requestFocus();
            return;
        }

        if (!email.isEmpty()
                && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Địa chỉ email chưa hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        String normalizedPhone = phone.replaceAll("[\\s().-]", "");

        if (!normalizedPhone.isEmpty()
                && !normalizedPhone.matches("\\+?[0-9]{8,15}")) {
            inputPhone.setError("Số điện thoại chưa hợp lệ");
            edtPhone.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("phone", normalizedPhone);

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu…");
        status.setText("Đang lưu thông tin lên Firestore…");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(profileUid)
                .set(data, SetOptions.merge())
                .addOnCompleteListener(this, task -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thông tin");

                    if (task.isSuccessful()) {
                        status.setText("Đã lưu thông tin lên Firestore.");

                        Toast.makeText(
                                this,
                                "Đã lưu hồ sơ",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Log.e(
                                "Profile",
                                "Không lưu được hồ sơ",
                                task.getException()
                        );

                        status.setText(
                                "Chưa lưu được. Kiểm tra kết nối và thử lại."
                        );
                    }
                });
    }

    private void saveAvatar(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            avatar.setImageTintList(null);
            avatar.setPadding(0, 0, 0, 0);
            avatar.setImageURI(uri);

            if (avatar.getDrawable() == null) {
                throw new IllegalArgumentException("Không đọc được ảnh");
            }

            prefs.edit()
                    .putString("avatar_" + profileUid, uri.toString())
                    .apply();

            Toast.makeText(
                    this,
                    "Đã lưu ảnh đại diện trên thiết bị",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception error) {
            Log.e("Profile", "Không mở được ảnh đại diện", error);
            restoreAvatar();

            Toast.makeText(
                    this,
                    "Không mở được ảnh. Bạn chọn ảnh khác nhé",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void restoreAvatar() {
        showDefaultAvatar();

        String savedUri = prefs.getString(
                "avatar_" + profileUid, ""
        );

        if (savedUri == null || savedUri.isEmpty()) {
            return;
        }

        try {
            avatar.setImageTintList(null);
            avatar.setPadding(0, 0, 0, 0);
            avatar.setImageURI(Uri.parse(savedUri));

            if (avatar.getDrawable() == null) {
                showDefaultAvatar();
            }
        } catch (Exception error) {
            showDefaultAvatar();
        }
    }

    private void showDefaultAvatar() {
        int padding = Math.round(
                30 * getResources().getDisplayMetrics().density
        );

        avatar.setPadding(padding, padding, padding, padding);
        avatar.setImageResource(R.drawable.ic_tab_profile);
        avatar.setImageTintList(
                ContextCompat.getColorStateList(
                        this, R.color.tanh_primary
                )
        );
    }

    private void setupNavigation() {
        int activeColor = ContextCompat.getColor(
                this, R.color.tanh_primary
        );

        findViewById(R.id.tabProfile).setSelected(true);

        ImageView icon = findViewById(R.id.imgTabProfile);
        TextView label = findViewById(R.id.tvTabProfile);

        icon.setColorFilter(activeColor);
        label.setTextColor(activeColor);
        label.setTypeface(null, Typeface.BOLD);

        findViewById(R.id.tabHome).setOnClickListener(v ->
                openScreen(HomeActivity.class)
        );

        findViewById(R.id.tabCart).setOnClickListener(v ->
                openScreen(CartActivity.class)
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

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showConnectionError() {
        btnSave.setEnabled(false);
        btnAvatar.setEnabled(false);

        status.setText(
                "Không kết nối được Firebase. Bạn mở lại màn hình nhé."
        );
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }
}
package com.example.a24162007_tuan1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShopDatabase extends SQLiteOpenHelper {

    private final Context context;

    public ShopDatabase(Context context) {
        super(context.getApplicationContext(), "tanh_shop.db", null, 2);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE products (" +
                        "id INTEGER PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "description TEXT NOT NULL, " +
                        "image_name TEXT NOT NULL, " +
                        "price INTEGER NOT NULL CHECK (price >= 0))"
        );

        createCartTable(db);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        context.getAssets().open("products.sql"),
                        StandardCharsets.UTF_8
                )
        )) {
            StringBuilder sql = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sql.append(line).append('\n');
            }

            db.execSQL(sql.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được products.sql", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createCartTable(db);
        }
    }

    private void createCartTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS cart_items (" +
                        "username TEXT NOT NULL, " +
                        "product_id INTEGER NOT NULL, " +
                        "quantity INTEGER NOT NULL, " +
                        "PRIMARY KEY (username, product_id))"
        );
    }

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, name, description, image_name, price " +
                        "FROM products ORDER BY id",
                null
        )) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                String description = cursor.getString(2);
                String imageName = cursor.getString(3);
                int price = cursor.getInt(4);

                int image = context.getResources().getIdentifier(
                        imageName, "drawable", context.getPackageName()
                );

                products.add(new Product(id, name, description, image, price));
            }
        }

        return products;
    }

    public void addToCart(String username, long productId) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            db.execSQL(
                    "INSERT OR IGNORE INTO cart_items " +
                            "(username, product_id, quantity) VALUES (?, ?, 0)",
                    new Object[]{username, productId}
            );

            db.execSQL(
                    "UPDATE cart_items SET quantity = quantity + 1 " +
                            "WHERE username = ? AND product_id = ?",
                    new Object[]{username, productId}
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getCartItems(String username) {
        return getReadableDatabase().rawQuery(
                "SELECT p.name, p.price, c.quantity, p.image_name " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE c.username = ? ORDER BY p.id",
                new String[]{username}
        );
    }
}
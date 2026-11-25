package com.example.a24162007_tuan1;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Arrays;

public final class ProductOptions {

    private ProductOptions() {
    }

    public static String[] sizes(long id) {
        if (id == 1) {
            return new String[]{"36", "37"};
        }

        if (id == 3 || id == 4) {
            return new String[]{"S", "M"};
        }

        return new String[]{""};
    }

    public static boolean valid(
            long id,
            String color,
            String size
    ) {
        return ("black".equals(color) || "white".equals(color))
                && Arrays.asList(sizes(id)).contains(size);
    }

    public static String key(
            long id,
            String color,
            String size
    ) {
        if (!valid(id, color, size)) {
            throw new IllegalArgumentException(
                    "Màu hoặc size không hợp lệ"
            );
        }

        return id + "_" + color + "_"
                + (size.isEmpty() ? "none" : size);
    }

    public static String label(String color, String size) {
        if (!"black".equals(color) && !"white".equals(color)) {
            return "Chưa chọn màu / size";
        }

        String result = "Màu: "
                + ("black".equals(color) ? "Đen" : "Trắng");

        return size == null || size.isEmpty()
                ? result
                : result + " · Size: " + size;
    }

    public static final class Panel extends LinearLayout {

        private String selectedColor;
        private String selectedSize;

        private final MaterialButton[] colorButtons =
                new MaterialButton[2];

        private MaterialButton[] sizeButtons =
                new MaterialButton[0];

        private final String[] sizeValues;

        public Panel(
                Context context,
                long productId,
                String color,
                String size
        ) {
            super(context);

            setOrientation(VERTICAL);
            setPadding(dp(24), dp(12), dp(24), dp(16));

            selectedColor = "white".equals(color)
                    ? "white"
                    : "black";

            sizeValues = ProductOptions.sizes(productId);

            selectedSize = Arrays.asList(sizeValues).contains(size)
                    ? size
                    : sizeValues[0];

            // Chọn màu
            addTitle("Màu sắc", 0);

            LinearLayout colorRow = newRow();

            String[] labels = {"Đen", "Trắng"};
            String[] values = {"black", "white"};

            for (int index = 0; index < values.length; index++) {
                final String value = values[index];

                MaterialButton button = newButton(labels[index]);
                colorButtons[index] = button;

                addOption(colorRow, button, index);

                button.setOnClickListener(v -> {
                    selectedColor = value;
                    updateSelection();
                });
            }

            // Chỉ hiện size với giày và quần áo
            if (sizeValues.length > 1) {
                addTitle("Kích thước", 22);

                LinearLayout sizeRow = newRow();
                sizeButtons = new MaterialButton[sizeValues.length];

                for (int index = 0;
                     index < sizeValues.length;
                     index++) {

                    final String value = sizeValues[index];

                    MaterialButton button = newButton(value);
                    button.setContentDescription("Size " + value);

                    sizeButtons[index] = button;
                    addOption(sizeRow, button, index);

                    button.setOnClickListener(v -> {
                        selectedSize = value;
                        updateSelection();
                    });
                }
            }

            updateSelection();
        }

        private void addTitle(String text, int marginTop) {
            TextView title = new TextView(getContext());

            title.setText(text);
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(colorResource(R.color.tanh_ink));

            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
            );

            params.topMargin = dp(marginTop);
            params.bottomMargin = dp(10);

            addView(title, params);
        }

        private LinearLayout newRow() {
            LinearLayout row = new LinearLayout(getContext());

            row.setOrientation(HORIZONTAL);
            row.setBaselineAligned(false);

            addView(
                    row,
                    new LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT
                    )
            );

            return row;
        }

        private MaterialButton newButton(String label) {
            MaterialButton button = new MaterialButton(
                    getContext(),
                    null,
                    com.google.android.material.R.attr
                            .materialButtonOutlinedStyle
            );

            button.setId(View.generateViewId());
            button.setText(label);
            button.setAllCaps(false);
            button.setTextSize(15);
            button.setCheckable(true);

            button.setCornerRadius(dp(12));
            button.setStrokeWidth(dp(1));

            button.setInsetTop(0);
            button.setInsetBottom(0);

            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setMinHeight(dp(48));

            button.setPadding(dp(8), 0, dp(8), 0);

            button.setBackgroundTintList(
                    states(
                            R.color.tanh_soft,
                            R.color.tanh_surface
                    )
            );

            button.setStrokeColor(
                    states(
                            R.color.tanh_primary,
                            R.color.tanh_line
                    )
            );

            button.setTextColor(
                    states(
                            R.color.tanh_primary_dark,
                            R.color.tanh_muted
                    )
            );

            return button;
        }

        private void addOption(
                LinearLayout row,
                MaterialButton button,
                int index
        ) {
            LayoutParams params = new LayoutParams(
                    0,
                    dp(48),
                    1
            );

            if (index > 0) {
                params.setMarginStart(dp(12));
            }

            row.addView(button, params);
        }

        private void updateSelection() {
            colorButtons[0].setChecked(
                    "black".equals(selectedColor)
            );

            colorButtons[1].setChecked(
                    "white".equals(selectedColor)
            );

            for (int index = 0;
                 index < sizeButtons.length;
                 index++) {

                sizeButtons[index].setChecked(
                        sizeValues[index].equals(selectedSize)
                );
            }
        }

        private ColorStateList states(
                int checked,
                int normal
        ) {
            return new ColorStateList(
                    new int[][]{
                            {android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{
                            colorResource(checked),
                            colorResource(normal)
                    }
            );
        }

        private int colorResource(int resource) {
            return ContextCompat.getColor(
                    getContext(),
                    resource
            );
        }

        private int dp(int value) {
            return Math.round(
                    value * getResources()
                            .getDisplayMetrics().density
            );
        }

        public String color() {
            return selectedColor;
        }

        public String size() {
            return selectedSize;
        }
    }
}
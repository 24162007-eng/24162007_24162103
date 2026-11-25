package com.example.a24162007_tuan1;

import java.io.Serializable;

public final class CheckoutItem implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final String EXTRA_ITEMS = "checkout_items";
    public final Product product;
    public final long quantity;
    public final String cartItemId;
    public final String color;
    public final String size;

    public CheckoutItem(Product product, long quantity, String cartItemId) {
        this(product, quantity, cartItemId, "", "");
    }

    public CheckoutItem(Product product, long quantity, String cartItemId,
                        String color, String size) {
        this.product = product;
        this.quantity = quantity;
        this.cartItemId = cartItemId;
        this.color = color == null ? "" : color;
        this.size = size == null ? "" : size;
    }

    public long subtotal() {
        return Math.multiplyExact((long) product.getPrice(), quantity);
    }
}

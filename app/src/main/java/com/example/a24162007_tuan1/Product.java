package com.example.a24162007_tuan1;

import java.io.Serializable;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String name;
    private final String description;
    private final int image;
    private final int price;

    public Product(String name, String description, int image) {
        this(name, description, image, 0);
    }

    public Product(String name, String description, int image, int price) {
        this(0, name, description, image, price);
    }

    public Product(long id, String name, String description, int image, int price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = image;
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getImage() {
        return image;
    }

    public int getPrice() {
        return price;
    }
}
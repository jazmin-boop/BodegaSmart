package com.example.bodega.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String barcode;
    private String name;
    private double salePrice;
    private double costPrice;
    private int stock;
    private int minStock;
    private String expirationDate;
    private String imageUrl;
    private String category;

    @Ignore
    public ProductEntity() {
    }

    public ProductEntity(String barcode, String name, double salePrice, double costPrice, int stock, int minStock, String expirationDate, String imageUrl, String category) {
        this.barcode = barcode;
        this.name = name;
        this.salePrice = salePrice;
        this.costPrice = costPrice;
        this.stock = stock;
        this.minStock = minStock;
        this.expirationDate = expirationDate;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getSalePrice() { return salePrice; }
    public void setSalePrice(double salePrice) { this.salePrice = salePrice; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }

    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}

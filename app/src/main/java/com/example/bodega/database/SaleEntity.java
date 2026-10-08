package com.example.bodega.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales")
public class SaleEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private double totalAmount;
    private double totalCost;
    private String paymentMethod; // "EFECTIVO", "YAPE/PLIN", "FIADO"
    private long customerId; // 0 if no customer
    private String productSummary;

    public SaleEntity(long timestamp, double totalAmount, double totalCost, String paymentMethod, long customerId, String productSummary) {
        this.timestamp = timestamp;
        this.totalAmount = totalAmount;
        this.totalCost = totalCost;
        this.paymentMethod = paymentMethod;
        this.customerId = customerId;
        this.productSummary = productSummary;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getProductSummary() { return productSummary; }
    public void setProductSummary(String productSummary) { this.productSummary = productSummary; }
}
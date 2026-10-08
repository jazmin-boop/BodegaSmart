package com.example.bodega.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SaleDao {
    @Insert
    long insertSale(SaleEntity sale);

    @Update
    void updateSale(SaleEntity sale);

    @Delete
    void deleteSale(SaleEntity sale);

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    List<SaleEntity> getAllSales();

    @Query("SELECT * FROM sales WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    List<SaleEntity> getSalesBetween(long startTimestamp, long endTimestamp);

    @Query("SELECT SUM(totalAmount) FROM sales WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    double getTotalSalesBetween(long startTimestamp, long endTimestamp);

    @Query("SELECT SUM(totalAmount - totalCost) FROM sales WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    double getTotalProfitBetween(long startTimestamp, long endTimestamp);
}
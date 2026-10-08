package com.example.bodega.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    long insert(ProductEntity product);

    @Update
    void update(ProductEntity product);

    @Delete
    void delete(ProductEntity product);

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<ProductEntity> getAllProducts();

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcode(String barcode);

    @Query("SELECT * FROM products WHERE stock <= minStock")
    List<ProductEntity> getLowStockProducts();

    @Query("SELECT * FROM products WHERE id = :id")
    ProductEntity getProductById(long id);
}

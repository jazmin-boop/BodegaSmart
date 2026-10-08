package com.example.bodega.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomerDao {
    @Insert
    long insert(CustomerEntity customer);

    @Update
    void update(CustomerEntity customer);

    @Delete
    void delete(CustomerEntity customer);

    @Query("SELECT * FROM customers ORDER BY name ASC")
    List<CustomerEntity> getAllCustomers();

    @Query("SELECT * FROM customers WHERE id = :id")
    CustomerEntity getCustomerById(long id);

    @Query("SELECT * FROM customers WHERE totalDebt > 0 ORDER BY totalDebt DESC")
    List<CustomerEntity> getCustomersWithDebt();
}

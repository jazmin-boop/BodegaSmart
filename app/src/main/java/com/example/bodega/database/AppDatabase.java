package com.example.bodega.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ProductEntity.class, CustomerEntity.class, SaleEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public abstract ProductDao productDao();
    public abstract CustomerDao customerDao();
    public abstract SaleDao saleDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "bodega_smart_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Allow for simple bodega operation
                    .build();
        }
        return INSTANCE;
    }
}
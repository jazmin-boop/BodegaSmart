package com.example.bodega.database;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseBackupHelper {

    private static final String DATABASE_URL = "https://bodegasmarth-default-rtdb.firebaseio.com";

    public static void syncToFirebase(Context context, boolean showToast) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);

            Map<String, Object> backupData = new HashMap<>();

            // Products
            List<ProductEntity> products = db.productDao().getAllProducts();
            backupData.put("products", products);

            // Customers
            List<CustomerEntity> customers = db.customerDao().getAllCustomers();
            backupData.put("customers", customers);

            // Sales
            List<SaleEntity> sales = db.saleDao().getAllSales();
            backupData.put("sales", sales);

            // Global backup timestamp directly at the root of bodega_backup
            long currentTimestamp = System.currentTimeMillis();
            backupData.put("lastBackupTimestamp", currentTimestamp);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            backupData.put("lastBackupDate", sdf.format(new Date(currentTimestamp)));

            FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance(DATABASE_URL);
            DatabaseReference ref = firebaseDb.getReference("bodega_backup");

            ref.setValue(backupData)
                    .addOnSuccessListener(aVoid -> {
                        if (showToast) {
                            Toast.makeText(context, "☁️ ¡Respaldo sincronizado en Firebase con éxito!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "⚠️ Error en respaldo Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
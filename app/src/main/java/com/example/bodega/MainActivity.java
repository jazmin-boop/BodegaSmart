package com.example.bodega;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.bodega.database.AppDatabase;
import com.example.bodega.database.CustomerEntity;
import com.example.bodega.database.ProductEntity;
import com.example.bodega.database.SaleEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemperature, tvWeatherAlert, tvGreeting;
    private TextView tvStockSubtitle, tvStockBadge;
    private TextView tvExpirySubtitle, tvExpiryBadge;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherAlert = findViewById(R.id.tvWeatherAlert);
        tvGreeting = findViewById(R.id.tvGreeting);

        tvStockSubtitle = findViewById(R.id.tvStockSubtitle);
        tvStockBadge = findViewById(R.id.tvStockBadge);

        tvExpirySubtitle = findViewById(R.id.tvExpirySubtitle);
        tvExpiryBadge = findViewById(R.id.tvExpiryBadge);

        CardView cardQuickSale = findViewById(R.id.cardQuickSale);
        CardView cardFiados = findViewById(R.id.cardFiados);
        CardView cardInventory = findViewById(R.id.cardInventory);
        CardView cardReports = findViewById(R.id.cardReports);
        CardView cardQr = findViewById(R.id.cardQr);
        CardView cardStockBanner = findViewById(R.id.cardStockBanner);
        CardView cardExpiryBanner = findViewById(R.id.cardExpiryBanner);

        cardQuickSale.setOnClickListener(v -> startActivity(new Intent(this, QuickSaleActivity.class)));
        cardFiados.setOnClickListener(v -> startActivity(new Intent(this, FiadosActivity.class)));
        cardInventory.setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
        cardReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        cardQr.setOnClickListener(v -> startActivity(new Intent(this, QrCodeActivity.class)));

        cardStockBanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, InventoryActivity.class);
            intent.putExtra("FILTER_MODE", "STOCK");
            startActivity(intent);
        });

        cardExpiryBanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, InventoryActivity.class);
            intent.putExtra("FILTER_MODE", "EXPIRY");
            startActivity(intent);
        });

        // Exit/Logout button listener
        findViewById(R.id.btnExitMain).setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });

        // Bottom Nav bar listeners
        findViewById(R.id.navHome).setOnClickListener(v -> {});
        findViewById(R.id.navInventory).setOnClickListener(v -> startActivity(new Intent(this, InventoryActivity.class)));
        findViewById(R.id.navQuickSaleCenter).setOnClickListener(v -> startActivity(new Intent(this, QuickSaleActivity.class)));
        findViewById(R.id.navFiados).setOnClickListener(v -> startActivity(new Intent(this, FiadosActivity.class)));
        findViewById(R.id.navReports).setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));

        // Seed initial dummy data if empty
        seedDatabaseIfEmpty();

        // Update greeting based on time of day
        updateGreeting();

        // Fetch Weather from Open-Meteo API
        fetchWeatherData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync stock count, expiring products count, and backup to Firebase
        syncStockData();
        syncExpiryData();
        com.example.bodega.database.FirebaseBackupHelper.syncToFirebase(this, true);
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "¡Buenos días, Bodeguero!";
        } else if (hour >= 12 && hour < 19) {
            greeting = "¡Buenas tardes, Bodeguero!";
        } else {
            greeting = "¡Buenas noches, Bodeguero!";
        }
        tvGreeting.setText(greeting);
    }

    private void syncStockData() {
        int count = 0;
        List<ProductEntity> products = db.productDao().getAllProducts();
        for (ProductEntity p : products) {
            if (p.getStock() <= p.getMinStock()) {
                count++;
            }
        }

        if (tvStockBadge != null) {
            tvStockBadge.setText(String.valueOf(count));
        }

        if (tvStockSubtitle != null) {
            if (count == 0) {
                tvStockSubtitle.setText("No tienes productos con stock bajo.");
            } else if (count == 1) {
                tvStockSubtitle.setText("Tienes 1 producto con stock bajo o crítico.");
            } else {
                tvStockSubtitle.setText(String.format(Locale.US, "Tienes %d productos con stock bajo o crítico.", count));
            }
        }
    }

    private void syncExpiryData() {
        int count = 0;
        List<ProductEntity> products = db.productDao().getAllProducts();
        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L;

        SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (ProductEntity p : products) {
            String dateStr = p.getExpirationDate();
            if (dateStr != null && !dateStr.trim().isEmpty() && !dateStr.equalsIgnoreCase("Sin fecha")) {
                try {
                    Date expDate = sdfFull.parse(dateStr.trim());
                    if (expDate != null) {
                        long diff = expDate.getTime() - now;
                        if (diff <= thirtyDaysMs) {
                            count++;
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse exception
                }
            }
        }

        if (tvExpiryBadge != null) {
            tvExpiryBadge.setText(String.valueOf(count));
        }

        if (tvExpirySubtitle != null) {
            if (count == 0) {
                tvExpirySubtitle.setText("No tienes productos vencidos o próximos a caducar.");
            } else if (count == 1) {
                tvExpirySubtitle.setText("Tienes 1 producto vencido o próximo a caducar.");
            } else {
                tvExpirySubtitle.setText(String.format(Locale.US, "Tienes %d productos vencidos o próximos a caducar.", count));
            }
        }
    }

    private void fetchWeatherData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=-10&longitude=-75.25&daily=sunrise,sunset,daylight_duration,sunshine_duration&hourly=temperature_2m,apparent_temperature,temperature_180m,temperature_120m,temperature_80m&current=is_day&forecast_days=16";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(builder.toString());
                    if (json.has("hourly")) {
                        JSONObject hourly = json.getJSONObject("hourly");
                        JSONArray temps = hourly.getJSONArray("temperature_2m");
                        if (temps.length() > 0) {
                            double currentTemp = temps.getDouble(0);
                            double minTemp = currentTemp - 4;
                            double maxTemp = currentTemp + 6;

                            runOnUiThread(() -> {
                                tvTemperature.setText(String.format(Locale.US, "%.0f°C", currentTemp));
                                tvWeatherAlert.setText(String.format(Locale.US, "Min %.0f°C; max %.0f°C", minTemp, maxTemp));
                            });
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void seedDatabaseIfEmpty() {
        if (db.productDao().getAllProducts().isEmpty()) {
            db.productDao().insert(new ProductEntity("75010001", "Arroz Costeño 1kg", 4.50, 3.80, 20, 5, "2026-11-20", "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400", "Abarrotes"));
            db.productDao().insert(new ProductEntity("75010002", "Aceite Primor 1L", 9.80, 8.20, 12, 3, "2026-12-15", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400", "Abarrotes"));
            db.productDao().insert(new ProductEntity("75010003", "Leche Gloria Azul 400g", 4.20, 3.50, 2, 5, "2026-10-15", "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=400", "Lácteos")); // Low stock + Vence pronto
            db.productDao().insert(new ProductEntity("75010004", "Galletas Soda Field", 1.00, 0.70, 30, 10, "2027-01-10", "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=400", "Snacks"));
            db.productDao().insert(new ProductEntity("75010005", "Manzanas Frescas 1kg", 3.50, 2.50, 15, 5, "2026-10-25", "https://images.unsplash.com/photo-1560806887-1e4cd0b6fac6?w=400", "Frutas"));
            db.productDao().insert(new ProductEntity("75010006", "Naranjas de Mesa 1kg", 2.80, 1.90, 25, 5, "2026-10-30", "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=400", "Frutas"));
            db.productDao().insert(new ProductEntity("75010007", "Uvas Negras 1kg", 6.50, 4.80, 18, 5, "2026-10-18", "https://images.unsplash.com/photo-1596368708356-6e1e1025ee72?w=400", "Frutas"));
            db.productDao().insert(new ProductEntity("75010008", "Yogurt Gloria Fresa 1L", 6.80, 5.20, 3, 5, "2026-10-12", "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=400", "Lácteos")); // Low stock
            db.productDao().insert(new ProductEntity("75010009", "Inca Kola 1.5L", 5.50, 4.20, 40, 8, "2027-03-30", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400", "Bebidas"));
            db.productDao().insert(new ProductEntity("75010010", "Pollo Entero Fresco 1kg", 9.50, 7.80, 8, 4, "2026-10-10", "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=400", "Carnes"));
            db.productDao().insert(new ProductEntity("75010011", "Chocolate Sublime 30g", 2.00, 1.40, 50, 10, "2027-05-10", "https://images.unsplash.com/photo-1511381939415-e44015466834?w=400", "Snacks"));
            db.productDao().insert(new ProductEntity("75010012", "Fideos Don Vittorio 500g", 2.80, 2.10, 1, 5, "2027-08-15", "https://images.unsplash.com/photo-1621996346565-e3def6164286?w=400", "Abarrotes")); // Critical stock
        }

        if (db.customerDao().getAllCustomers().isEmpty()) {
            db.customerDao().insert(new CustomerEntity("Juan Pérez", "987654321", "10203040", 25.50, "Fiado azúcar y leche"));
            db.customerDao().insert(new CustomerEntity("María Rodríguez", "912345678", "20304050", 12.00, "Fiado arroz costeño"));
            db.customerDao().insert(new CustomerEntity("Carlos Gómez", "955443322", "30405060", 45.80, "Fiado aceite y galletas"));
        }

        if (db.saleDao().getAllSales().isEmpty()) {
            long now = System.currentTimeMillis();
            long dayMs = 24 * 60 * 60 * 1000L;

            // Sales TODAY (e.g. S/ 45.50 total)
            db.saleDao().insertSale(new SaleEntity(now - (2 * 3600 * 1000L), 25.50, 18.00, "EFECTIVO", 0, "• 1x Arroz Costeño 1kg\n• 2x Leche Gloria"));
            db.saleDao().insertSale(new SaleEntity(now - (5 * 3600 * 1000L), 20.00, 14.50, "YAPE/PLIN", 0, "• 2x Inca Kola 1.5L\n• 1x Galletas Soda"));

            // Sales PAST DAYS OF THIS WEEK (2, 3, 5, 6 days ago)
            db.saleDao().insertSale(new SaleEntity(now - (2 * dayMs), 85.00, 62.00, "EFECTIVO", 0, "• 3x Aceite Primor 1L\n• 2x Arroz Costeño"));
            db.saleDao().insertSale(new SaleEntity(now - (3 * dayMs), 110.00, 78.00, "FIADO", 1, "• 5x Leche Gloria\n• 2x Fideos Don Vittorio"));
            db.saleDao().insertSale(new SaleEntity(now - (5 * dayMs), 95.50, 68.00, "EFECTIVO", 0, "• 2x Pollo Entero Fresco\n• 1x Manzanas"));
            db.saleDao().insertSale(new SaleEntity(now - (6 * dayMs), 140.00, 98.00, "YAPE/PLIN", 0, "• 5x Inca Kola 1.5L\n• 4x Chocolate Sublime"));
        }
    }
}

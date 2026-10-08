package com.example.bodega;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bodega.database.AppDatabase;
import com.example.bodega.database.CustomerEntity;
import com.example.bodega.database.ProductEntity;
import com.example.bodega.database.SaleEntity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    private TextView tvTodaySales, tvTodayProfit, tvWeeklySales, tvWeeklyProfit;
    private TextView tvTodayTransactions, tvInventoryValue, tvPendingDebtCount, tvPendingDebtTotal;
    private TextView tvTodayMarginText, tvWeeklyMarginText, btnExitReports;
    private ProgressBar pbTodayProfit, pbWeeklyProfit;
    private LinearLayout llCategoryContainer;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = AppDatabase.getInstance(this);

        btnExitReports = findViewById(R.id.btnExitReports);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvTodayProfit = findViewById(R.id.tvTodayProfit);
        tvWeeklySales = findViewById(R.id.tvWeeklySales);
        tvWeeklyProfit = findViewById(R.id.tvWeeklyProfit);

        pbTodayProfit = findViewById(R.id.pbTodayProfit);
        pbWeeklyProfit = findViewById(R.id.pbWeeklyProfit);
        tvTodayMarginText = findViewById(R.id.tvTodayMarginText);
        tvWeeklyMarginText = findViewById(R.id.tvWeeklyMarginText);

        llCategoryContainer = findViewById(R.id.llCategoryContainer);

        tvTodayTransactions = findViewById(R.id.tvTodayTransactions);
        tvInventoryValue = findViewById(R.id.tvInventoryValue);
        tvPendingDebtCount = findViewById(R.id.tvPendingDebtCount);
        tvPendingDebtTotal = findViewById(R.id.tvPendingDebtTotal);

        btnExitReports.setOnClickListener(v -> finish());

        loadReports();
    }

    private void loadReports() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        cal.add(Calendar.DAY_OF_YEAR, -7);
        long startOfWeek = cal.getTimeInMillis();

        // Daily Sales / Profit
        double todaySales = db.saleDao().getTotalSalesBetween(startOfDay, now);
        double todayProfit = db.saleDao().getTotalProfitBetween(startOfDay, now);

        double todayMarginPct = (todaySales > 0) ? (todayProfit / todaySales) * 100.0 : 0.0;
        tvTodaySales.setText(String.format(Locale.US, "S/ %.2f", todaySales));
        tvTodayProfit.setText(String.format(Locale.US, "S/ %.2f", todayProfit));
        pbTodayProfit.setProgress((int) Math.min(100, todayMarginPct));
        tvTodayMarginText.setText(String.format(Locale.US, "Margen de utilidad estimada hoy: %.1f%%", todayMarginPct));

        // Weekly Sales / Profit
        double weeklySales = db.saleDao().getTotalSalesBetween(startOfWeek, now);
        double weeklyProfit = db.saleDao().getTotalProfitBetween(startOfWeek, now);

        double weeklyMarginPct = (weeklySales > 0) ? (weeklyProfit / weeklySales) * 100.0 : 0.0;
        tvWeeklySales.setText(String.format(Locale.US, "S/ %.2f", weeklySales));
        tvWeeklyProfit.setText(String.format(Locale.US, "S/ %.2f", weeklyProfit));
        pbWeeklyProfit.setProgress((int) Math.min(100, weeklyMarginPct));
        tvWeeklyMarginText.setText(String.format(Locale.US, "Margen de utilidad semanal: %.1f%%", weeklyMarginPct));

        // Today's Transactions count
        int todayTxCount = 0;
        List<SaleEntity> todaySalesList = db.saleDao().getSalesBetween(startOfDay, now);
        if (todaySalesList != null) {
            todayTxCount = todaySalesList.size();
        }
        tvTodayTransactions.setText(String.valueOf(todayTxCount));

        // Estimated Stock Inventory Value & Category Breakdown
        double totalStockVal = 0.0;
        List<ProductEntity> products = db.productDao().getAllProducts();
        Map<String, Double> categoryValMap = new HashMap<>();
        Map<String, Integer> categoryCountMap = new HashMap<>();

        for (ProductEntity p : products) {
            double productVal = p.getSalePrice() * p.getStock();
            totalStockVal += productVal;

            String cat = (p.getCategory() == null || p.getCategory().isEmpty()) ? "Otros" : p.getCategory();
            categoryValMap.put(cat, categoryValMap.getOrDefault(cat, 0.0) + productVal);
            categoryCountMap.put(cat, categoryCountMap.getOrDefault(cat, 0) + p.getStock());
        }
        tvInventoryValue.setText(String.format(Locale.US, "S/ %.2f", totalStockVal));

        // Dynamically build category breakdown bars
        llCategoryContainer.removeAllViews();
        if (categoryValMap.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay productos registrados en inventario.");
            tvEmpty.setTextColor(Color.parseColor("#79808F"));
            tvEmpty.setTextSize(12);
            llCategoryContainer.addView(tvEmpty);
        } else {
            for (Map.Entry<String, Double> entry : categoryValMap.entrySet()) {
                String catName = entry.getKey();
                double catVal = entry.getValue();
                int catCount = categoryCountMap.getOrDefault(catName, 0);
                int pct = (totalStockVal > 0) ? (int) Math.round((catVal / totalStockVal) * 100) : 0;

                LinearLayout catRow = new LinearLayout(this);
                catRow.setOrientation(LinearLayout.VERTICAL);
                catRow.setPadding(0, 8, 0, 12);

                LinearLayout headerRow = new LinearLayout(this);
                headerRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView tvCatName = new TextView(this);
                tvCatName.setText(catName + " (" + catCount + " unds)");
                tvCatName.setTextColor(Color.parseColor("#081630"));
                tvCatName.setTextSize(12);
                tvCatName.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvCatName.setLayoutParams(lpName);

                TextView tvCatVal = new TextView(this);
                tvCatVal.setText(String.format(Locale.US, "S/ %.2f (%d%%)", catVal, pct));
                tvCatVal.setTextColor(Color.parseColor("#3B82F6"));
                tvCatVal.setTextSize(12);
                tvCatVal.setTypeface(null, android.graphics.Typeface.BOLD);

                headerRow.addView(tvCatName);
                headerRow.addView(tvCatVal);

                ProgressBar pbCat = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                LinearLayout.LayoutParams lpPb = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16);
                lpPb.setMargins(0, 8, 0, 0);
                pbCat.setLayoutParams(lpPb);
                pbCat.setProgress(pct);
                pbCat.setMax(100);

                catRow.addView(headerRow);
                catRow.addView(pbCat);

                llCategoryContainer.addView(catRow);
            }
        }

        // Pending Debt total & customer count
        double totalDebtSum = 0.0;
        int debtCustomerCount = 0;
        List<CustomerEntity> debtCustomers = db.customerDao().getCustomersWithDebt();
        if (debtCustomers != null) {
            debtCustomerCount = debtCustomers.size();
            for (CustomerEntity c : debtCustomers) {
                totalDebtSum += c.getTotalDebt();
            }
        }
        tvPendingDebtCount.setText(debtCustomerCount + (debtCustomerCount == 1 ? " cliente deudor" : " clientes deudores"));
        tvPendingDebtTotal.setText(String.format(Locale.US, "S/ %.2f", totalDebtSum));
    }
}
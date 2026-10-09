package com.example.bodega;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bodega.database.AppDatabase;
import com.example.bodega.database.FirebaseBackupHelper;
import com.example.bodega.database.ProductEntity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryActivity extends AppCompatActivity {

    private static final int MODE_DEFAULT = 0;
    private static final int MODE_STOCK = 1;
    private static final int MODE_CADUCIDAD = 2;

    private int currentMode = MODE_DEFAULT;

    private RecyclerView rvInventory;
    private Button btnAddProduct, btnFilterCategory, btnFilterStock, btnFilterExpiry, btnProductsHeader;
    private Button btnSubFilterStock, btnSubFilterCalendar;
    private EditText etSearchInventory;
    private AppDatabase db;
    private InventoryAdapter adapter;
    private List<ProductEntity> currentProductList = new ArrayList<>();
    private EditText currentDialogBarcodeField;

    private final ActivityResultLauncher<ScanOptions> dialogBarcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null && currentDialogBarcodeField != null) {
                    currentDialogBarcodeField.setText(result.getContents());
                    Toast.makeText(this, "Código escaneado: " + result.getContents(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    private String selectedImageUriString = "";
    private ImageView imgProductPreview;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUriString = uri.toString();
                    if (imgProductPreview != null) {
                        Glide.with(this).load(uri).into(imgProductPreview);
                    }
                    Toast.makeText(this, "Foto seleccionada de galería 📸", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        db = AppDatabase.getInstance(this);

        rvInventory = findViewById(R.id.rvInventory);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnFilterCategory = findViewById(R.id.btnFilterCategory);
        btnFilterStock = findViewById(R.id.btnFilterStock);
        btnFilterExpiry = findViewById(R.id.btnFilterExpiry);
        btnProductsHeader = findViewById(R.id.btnProductsHeader);
        btnSubFilterStock = findViewById(R.id.btnSubFilterStock);
        btnSubFilterCalendar = findViewById(R.id.btnSubFilterCalendar);
        etSearchInventory = findViewById(R.id.etSearchInventory);

        // Exit button listener
        findViewById(R.id.btnExitInventory).setOnClickListener(v -> finish());

        // 2-column Grid Layout Manager
        rvInventory.setLayoutManager(new GridLayoutManager(this, 2));

        btnAddProduct.setOnClickListener(v -> showAddProductDialog(null));

        // Productos Header Button (Resets to Default Catalog Mode, Shows + Producto)
        btnProductsHeader.setOnClickListener(v -> {
            currentMode = MODE_DEFAULT;
            btnAddProduct.setVisibility(View.VISIBLE);
            btnSubFilterStock.setVisibility(View.GONE);
            btnSubFilterCalendar.setVisibility(View.GONE);
            btnFilterCategory.setText("Categoría");
            etSearchInventory.setText("");
            loadInventory();
            Toast.makeText(this, "Catálogo completo de productos", Toast.LENGTH_SHORT).show();
        });

        // Category Filter Button
        btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());

        // Stock Filter Button: Switches to Stock Mode (Hides + Producto, Shows Nivel Stock button)
        btnFilterStock.setOnClickListener(v -> {
            currentMode = MODE_STOCK;
            btnAddProduct.setVisibility(View.GONE);
            btnSubFilterStock.setVisibility(View.VISIBLE);
            btnSubFilterCalendar.setVisibility(View.GONE);
            Collections.sort(currentProductList, (p1, p2) -> Integer.compare(p1.getStock(), p2.getStock()));
            if (adapter != null) adapter.notifyDataSetChanged();
            Toast.makeText(this, "Modo Stock (Menor stock primero)", Toast.LENGTH_SHORT).show();
        });

        // Sub-filter Stock level (Alto, Medio, Bajo)
        btnSubFilterStock.setOnClickListener(v -> showStockLevelFilterDialog());

        // Caducidad Filter Button: Switches to Caducidad Mode (Hides + Producto, Shows Calendario button)
        btnFilterExpiry.setOnClickListener(v -> {
            currentMode = MODE_CADUCIDAD;
            btnAddProduct.setVisibility(View.GONE);
            btnSubFilterStock.setVisibility(View.GONE);
            btnSubFilterCalendar.setVisibility(View.VISIBLE);
            Collections.sort(currentProductList, (p1, p2) -> p1.getExpirationDate().compareTo(p2.getExpirationDate()));
            if (adapter != null) adapter.notifyDataSetChanged();
            Toast.makeText(this, "Modo Caducidad (Productos próximos a vencer)", Toast.LENGTH_SHORT).show();
        });

        // Sub-filter Calendar date picker
        btnSubFilterCalendar.setOnClickListener(v -> openExpirationCalendarFilter());

        // Search text watcher
        etSearchInventory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadInventory();

        String filterMode = getIntent().getStringExtra("FILTER_MODE");
        if ("STOCK".equalsIgnoreCase(filterMode)) {
            btnFilterStock.performClick();
        } else if ("EXPIRY".equalsIgnoreCase(filterMode)) {
            btnFilterExpiry.performClick();
        }
    }

    private void showCategoryFilterDialog() {
        String[] categories = {"Todas las Categorías", "Abarrotes", "Frutas", "Lácteos", "Snacks", "Carnes", "Bebidas"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_select_category, null);

        RecyclerView rvCategoryList = view.findViewById(R.id.rvCategoryList);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        CategoryAdapter categoryAdapter = new CategoryAdapter(categories, selectedCategory -> {
            dialog.dismiss();
            if (selectedCategory.equalsIgnoreCase("Todas las Categorías")) {
                loadInventory();
                btnFilterCategory.setText("Categoría");
            } else {
                filterByCategory(selectedCategory);
                btnFilterCategory.setText(selectedCategory);
            }
        });
        rvCategoryList.setAdapter(categoryAdapter);

        dialog.show();
    }

    private void showStockLevelFilterDialog() {
        String[] stockLevels = {"Todos los Niveles", "Crítico", "Advertencia", "Suficiente"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_select_stock_level, null);

        RecyclerView rvStockLevelList = view.findViewById(R.id.rvStockLevelList);
        rvStockLevelList.setLayoutManager(new LinearLayoutManager(this));

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        CategoryAdapter stockAdapter = new CategoryAdapter(stockLevels, selectedLevel -> {
            dialog.dismiss();
            currentMode = MODE_STOCK;
            btnAddProduct.setVisibility(View.GONE);
            if (selectedLevel.equals("Crítico")) {
                filterByStockLevel(1);
            } else if (selectedLevel.equals("Advertencia")) {
                filterByStockLevel(2);
            } else if (selectedLevel.equals("Suficiente")) {
                filterByStockLevel(3);
            } else {
                loadInventory();
            }
        });
        rvStockLevelList.setAdapter(stockAdapter);

        dialog.show();
    }

    private void filterByStockLevel(int level) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity p : db.productDao().getAllProducts()) {
            if (level == 1 && p.getStock() <= p.getMinStock()) { // Crítico
                filtered.add(p);
            } else if (level == 2 && p.getStock() > p.getMinStock() && p.getStock() <= p.getMinStock() * 2) { // Advertencia
                filtered.add(p);
            } else if (level == 3 && p.getStock() > p.getMinStock() * 2) { // Suficiente
                filtered.add(p);
            }
        }
        currentProductList.clear();
        currentProductList.addAll(filtered);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void openExpirationCalendarFilter() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            List<ProductEntity> filtered = new ArrayList<>();
            for (ProductEntity p : db.productDao().getAllProducts()) {
                if (p.getExpirationDate() != null && !p.getExpirationDate().isEmpty() && p.getExpirationDate().compareTo(selectedDate) <= 0) {
                    filtered.add(p);
                }
            }
            Collections.sort(filtered, (p1, p2) -> p1.getExpirationDate().compareTo(p2.getExpirationDate()));
            currentProductList.clear();
            currentProductList.addAll(filtered);
            if (adapter != null) adapter.notifyDataSetChanged();
            
            // Hide + Producto button in Caducidad mode
            btnAddProduct.setVisibility(View.GONE);
            Toast.makeText(this, "Productos que vencen hasta " + selectedDate, Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadInventory() {
        currentProductList = db.productDao().getAllProducts();
        adapter = new InventoryAdapter(currentProductList);
        rvInventory.setAdapter(adapter);

        if (currentMode == MODE_DEFAULT) {
            btnAddProduct.setVisibility(View.VISIBLE);
            btnSubFilterStock.setVisibility(View.GONE);
            btnSubFilterCalendar.setVisibility(View.GONE);
        } else if (currentMode == MODE_STOCK) {
            btnAddProduct.setVisibility(View.GONE);
            btnSubFilterStock.setVisibility(View.VISIBLE);
            btnSubFilterCalendar.setVisibility(View.GONE);
        } else if (currentMode == MODE_CADUCIDAD) {
            btnAddProduct.setVisibility(View.GONE);
            btnSubFilterStock.setVisibility(View.GONE);
            btnSubFilterCalendar.setVisibility(View.VISIBLE);
        }
    }

    private void filterByCategory(String category) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity p : db.productDao().getAllProducts()) {
            if (p.getCategory().equalsIgnoreCase(category)) {
                filtered.add(p);
            }
        }
        currentProductList.clear();
        currentProductList.addAll(filtered);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void filterBySearchQuery(String query) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity p : db.productDao().getAllProducts()) {
            if (p.getName().toLowerCase().contains(query.toLowerCase()) ||
                p.getBarcode().contains(query)) {
                filtered.add(p);
            }
        }
        currentProductList.clear();
        currentProductList.addAll(filtered);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showAddProductDialog(ProductEntity existingProduct) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_product, null);

        TextView tvTitle = view.findViewById(R.id.tvAddDialogTitle);
        EditText etBarcode = view.findViewById(R.id.etBarcode);
        ImageButton btnScanDialogBarcode = view.findViewById(R.id.btnScanDialogBarcode);
        EditText etName = view.findViewById(R.id.etName);
        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etSalePrice = view.findViewById(R.id.etSalePrice);
        EditText etCostPrice = view.findViewById(R.id.etCostPrice);
        EditText etStock = view.findViewById(R.id.etStock);
        EditText etMinStock = view.findViewById(R.id.etMinStock);
        EditText etExpirationDate = view.findViewById(R.id.etExpirationDate);
        EditText etImageUrl = view.findViewById(R.id.etImageUrl);
        Button btnCancel = view.findViewById(R.id.btnCancelProduct);
        Button btnSave = view.findViewById(R.id.btnSaveProduct);

        currentDialogBarcodeField = etBarcode;

        // Dynamic Title
        if (existingProduct == null) {
            tvTitle.setText("Nuevo Producto");
            btnSave.setText("Guardar");
        } else {
            tvTitle.setText("Editar Producto");
            btnSave.setText("Actualizar");
        }

        // Barcode Scanner Camera (ML Kit / CameraX / ZXing in vertical portrait mode)
        btnScanDialogBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("← Salir | Escanea el código de barras");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureActivityPortrait.class);
            dialogBarcodeLauncher.launch(options);
        });

        // Date Picker Calendar Selector for Expiration Date
        etExpirationDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this, (dpView, year, month, dayOfMonth) -> {
                String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etExpirationDate.setText(date);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        selectedImageUriString = "";
        imgProductPreview = view.findViewById(R.id.imgProductPreview);
        Button btnPickGallery = view.findViewById(R.id.btnPickImageGallery);

        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // Prefill if editing
        if (existingProduct != null) {
            etBarcode.setText(existingProduct.getBarcode());
            etName.setText(existingProduct.getName());
            etCategory.setText(existingProduct.getCategory());
            etSalePrice.setText(String.valueOf(existingProduct.getSalePrice()));
            etCostPrice.setText(String.valueOf(existingProduct.getCostPrice()));
            etStock.setText(String.valueOf(existingProduct.getStock()));
            etMinStock.setText(String.valueOf(existingProduct.getMinStock()));
            etExpirationDate.setText(existingProduct.getExpirationDate());
            if (existingProduct.getImageUrl() != null && !existingProduct.getImageUrl().isEmpty()) {
                selectedImageUriString = existingProduct.getImageUrl();
                Glide.with(this).load(existingProduct.getImageUrl()).into(imgProductPreview);
            }
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String barcode = etBarcode.getText().toString();
            String name = etName.getText().toString();
            String category = etCategory.getText().toString().isEmpty() ? "Abarrotes" : etCategory.getText().toString();
            double salePrice = etSalePrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(etSalePrice.getText().toString());
            double costPrice = etCostPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(etCostPrice.getText().toString());
            int stock = etStock.getText().toString().isEmpty() ? 0 : Integer.parseInt(etStock.getText().toString());
            int minStock = etMinStock.getText().toString().isEmpty() ? 0 : Integer.parseInt(etMinStock.getText().toString());
            String expDate = etExpirationDate.getText().toString();
            String imgUrl = !selectedImageUriString.isEmpty()
                    ? selectedImageUriString
                    : "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400";

            if (!name.isEmpty()) {
                if (existingProduct == null) {
                    ProductEntity newProduct = new ProductEntity(barcode, name, salePrice, costPrice, stock, minStock, expDate, imgUrl, category);
                    db.productDao().insert(newProduct);
                    Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show();
                } else {
                    existingProduct.setBarcode(barcode);
                    existingProduct.setName(name);
                    existingProduct.setCategory(category);
                    existingProduct.setSalePrice(salePrice);
                    existingProduct.setCostPrice(costPrice);
                    existingProduct.setStock(stock);
                    existingProduct.setMinStock(minStock);
                    existingProduct.setExpirationDate(expDate);
                    existingProduct.setImageUrl(imgUrl);
                    db.productDao().update(existingProduct);
                    Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show();
                }
                FirebaseBackupHelper.syncToFirebase(this, false);
                loadInventory();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showProductDetailModal(ProductEntity p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_detail, null);

        ImageView imgDetail = view.findViewById(R.id.imgDetailProduct);
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvSalePrice = view.findViewById(R.id.tvDetailSalePrice);
        TextView tvCostPrice = view.findViewById(R.id.tvDetailCostPrice);
        TextView tvStock = view.findViewById(R.id.tvDetailStock);
        TextView tvMinStock = view.findViewById(R.id.tvDetailMinStock);
        TextView tvBarcode = view.findViewById(R.id.tvDetailBarcode);
        TextView tvExpirationDate = view.findViewById(R.id.tvDetailExpirationDate);
        TextView btnExitModal = view.findViewById(R.id.btnExitDetailModal);
        Button btnEdit = view.findViewById(R.id.btnEditProduct);
        Button btnDelete = view.findViewById(R.id.btnDeleteProduct);

        tvTitle.setText(p.getName());
        tvCategory.setText("Categoría: " + p.getCategory());
        tvSalePrice.setText(String.format(Locale.US, "S/ %.2f", p.getSalePrice()));
        tvCostPrice.setText(String.format(Locale.US, "S/ %.2f", p.getCostPrice()));
        tvStock.setText(p.getStock() + " unidades");
        tvMinStock.setText(p.getMinStock() + " unidades");
        tvBarcode.setText(p.getBarcode() == null || p.getBarcode().isEmpty() ? "Sin código" : p.getBarcode());
        tvExpirationDate.setText(p.getExpirationDate() == null || p.getExpirationDate().isEmpty() ? "Sin fecha" : p.getExpirationDate());

        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            Glide.with(this).load(p.getImageUrl()).into(imgDetail);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnExitModal.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showConfirmDeleteDialog(p);
        });

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showAddProductDialog(p);
        });

        dialog.show();
    }

    private void showConfirmDeleteDialog(ProductEntity p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        TextView tvMsg = view.findViewById(R.id.tvDeleteMessage);
        Button btnCancel = view.findViewById(R.id.btnCancelDelete);
        Button btnConfirm = view.findViewById(R.id.btnConfirmDelete);

        tvMsg.setText("¿Estás seguro de que deseas eliminar '" + p.getName() + "' de la bodega? Esta acción no se puede deshacer.");

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            db.productDao().delete(p);
            FirebaseBackupHelper.syncToFirebase(this, false);
            loadInventory();
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
        private List<ProductEntity> list;

        public InventoryAdapter(List<ProductEntity> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductEntity p = list.get(position);
            holder.tvTitle.setText(p.getName());
            holder.tvCategory.setText(p.getCategory());
            holder.tvPrice.setText(String.format(Locale.US, "S/ %.2f", p.getSalePrice()));

            if (currentMode == MODE_DEFAULT) {
                // Default Mode -> Hide stock/expiry badge!
                holder.tvStockStatus.setVisibility(View.GONE);
            } else if (currentMode == MODE_STOCK) {
                // Stock Mode -> Show Stock badge with Traffic Light colors!
                holder.tvStockStatus.setVisibility(View.VISIBLE);
                holder.tvStockStatus.setText("Stock: " + p.getStock());

                int badgeColor;
                if (p.getStock() <= p.getMinStock()) {
                    badgeColor = Color.parseColor("#E53935"); // Red critical
                } else if (p.getStock() <= p.getMinStock() * 2) {
                    badgeColor = Color.parseColor("#FB8C00"); // Yellow/Orange warning
                } else {
                    badgeColor = Color.parseColor("#4CAF50"); // Green safe
                }
                holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

            } else if (currentMode == MODE_CADUCIDAD) {
                // Caducidad Mode -> Show Expiration Date status badge with Traffic Light!
                holder.tvStockStatus.setVisibility(View.VISIBLE);
                String expStr = p.getExpirationDate();
                if (expStr == null || expStr.isEmpty()) {
                    holder.tvStockStatus.setText("Sin fecha");
                    holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#79808F")));
                } else {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Date expDate = sdf.parse(expStr);
                        Date today = new Date();

                        long diffMs = expDate.getTime() - today.getTime();
                        long diffDays = diffMs / (24 * 60 * 60 * 1000);

                        if (diffDays < 0) {
                            holder.tvStockStatus.setText("Vencido (" + expStr + ")");
                            holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935"))); // Red
                        } else if (diffDays <= 15) {
                            holder.tvStockStatus.setText("Próximo a Vencer (" + expStr + ")");
                            holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FB8C00"))); // Yellow/Orange
                        } else {
                            holder.tvStockStatus.setText("Vigente: " + expStr);
                            holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green
                        }
                    } catch (Exception e) {
                        holder.tvStockStatus.setText("Vence: " + expStr);
                        holder.tvStockStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FB8C00")));
                    }
                }
            }

            if (p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(p.getImageUrl().trim())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.mipmap.ic_launcher);
            }

            // Click product card -> open detail modal
            holder.itemView.setOnClickListener(v -> showProductDetailModal(p));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory, tvPrice, tvStockStatus;
            ImageView imgProduct;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
                imgProduct = itemView.findViewById(R.id.imgProduct);
            }
        }
    }

    // Custom Category Adapter for clean white dialog
    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private String[] categories;
        private OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onCategoryClick(String category);
        }

        public CategoryAdapter(String[] categories, OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(16);
            tv.setTextColor(Color.parseColor("#081630"));
            tv.setPadding(32, 24, 32, 24);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String cat = categories[position];
            holder.tv.setText(cat);
            holder.tv.setOnClickListener(v -> listener.onCategoryClick(cat));
        }

        @Override
        public int getItemCount() {
            return categories.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }
}

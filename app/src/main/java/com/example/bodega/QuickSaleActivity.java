package com.example.bodega;

import android.content.Intent;
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
import com.example.bodega.database.ProductEntity;
import com.example.bodega.database.SaleEntity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuickSaleActivity extends AppCompatActivity {

    private View llCatalogView, llCartView, cardOpenCartBar, cardCheckoutPaymentBar, rlOpenCartBar;
    private EditText etSearchProduct;
    private TextView tvTotalAmountCatalog, tvTotalAmountCheckout, tvCartBadge;
    private Button btnScanBarcode, btnFilterCategory, btnGoToCartView, btnBackToCatalog;
    private Button btnPayCash, btnPayQr, btnPayFiado;
    private RecyclerView rvCatalogGrid, rvCartItems;
    private AppDatabase db;
    private double totalAmount = 0.0;
    private double totalCost = 0.0;
    
    private List<ProductEntity> availableProducts = new ArrayList<>();
    private Map<Long, CartItem> cartItemMap = new HashMap<>();
    private CatalogAdapter catalogAdapter;
    private CartPanelAdapter cartPanelAdapter;

    private static class CartItem {
        ProductEntity product;
        int quantity;

        CartItem(ProductEntity product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String barcode = result.getContents();
                    addProductByBarcode(barcode);
                }
            }
    );

    private final ActivityResultLauncher<Intent> fiadoActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    long customerId = 0;
                    if (result.getData() != null) {
                        customerId = result.getData().getLongExtra("SELECTED_CUSTOMER_ID", 0);
                    }
                    // Sale completed via FiadosActivity customer selection
                    processSale("FIADO", customerId, false);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_sale);

        db = AppDatabase.getInstance(this);

        llCatalogView = findViewById(R.id.llCatalogView);
        llCartView = findViewById(R.id.llCartView);
        cardOpenCartBar = findViewById(R.id.cardOpenCartBar);
        cardCheckoutPaymentBar = findViewById(R.id.cardCheckoutPaymentBar);
        rlOpenCartBar = findViewById(R.id.rlOpenCartBar);

        etSearchProduct = findViewById(R.id.etSearchProduct);
        tvTotalAmountCatalog = findViewById(R.id.tvTotalAmountCatalog);
        tvTotalAmountCheckout = findViewById(R.id.tvTotalAmountCheckout);
        tvCartBadge = findViewById(R.id.tvCartBadge);

        btnScanBarcode = findViewById(R.id.btnScanBarcode);
        btnFilterCategory = findViewById(R.id.btnFilterCategory);
        btnGoToCartView = findViewById(R.id.btnGoToCartView);
        btnBackToCatalog = findViewById(R.id.btnBackToCatalog);

        btnPayCash = findViewById(R.id.btnPayCash);
        btnPayQr = findViewById(R.id.btnPayQr);
        btnPayFiado = findViewById(R.id.btnPayFiado);

        rvCatalogGrid = findViewById(R.id.rvCatalogGrid);
        rvCartItems = findViewById(R.id.rvCartItems);

        // Exit button listener
        findViewById(R.id.btnExitQuickSale).setOnClickListener(v -> finish());

        // 2-column Grid Layout Manager for Catalog
        rvCatalogGrid.setLayoutManager(new GridLayoutManager(this, 2));

        // Linear Layout Manager for Cart View
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));

        btnScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("← Salir | Escanea el código de barras");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureActivityPortrait.class);
            barcodeLauncher.launch(options);
        });

        btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());

        // View Cart Listeners
        View.OnClickListener openCartListener = v -> {
            if (getCartItemCount() == 0) {
                Toast.makeText(this, "El carrito está vacío. Toca los productos para agregarlos.", Toast.LENGTH_SHORT).show();
            } else {
                switchToCartView();
            }
        };

        btnGoToCartView.setOnClickListener(openCartListener);
        rlOpenCartBar.setOnClickListener(openCartListener);

        btnBackToCatalog.setOnClickListener(v -> switchToCatalogView());

        etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPayCash.setOnClickListener(v -> processSale("EFECTIVO", 0, true));

        btnPayQr.setOnClickListener(v -> {
            processSale("YAPE/PLIN", 0, true);
            startActivity(new Intent(this, QrCodeActivity.class));
        });

        btnPayFiado.setOnClickListener(v -> {
            if (cartItemMap.isEmpty()) {
                Toast.makeText(this, "Añade al menos 1 producto para vender", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FiadosActivity.class);
            intent.putExtra("FIADO_SALE_AMOUNT", totalAmount);
            fiadoActivityLauncher.launch(intent);
        });

        loadProducts();
    }

    private void switchToCartView() {
        llCatalogView.setVisibility(View.GONE);
        llCartView.setVisibility(View.VISIBLE);
        cardOpenCartBar.setVisibility(View.GONE);
        cardCheckoutPaymentBar.setVisibility(View.VISIBLE);

        List<CartItem> itemList = new ArrayList<>(cartItemMap.values());
        cartPanelAdapter = new CartPanelAdapter(itemList, () -> {
            updateCartTotals();
            if (cartItemMap.isEmpty()) {
                switchToCatalogView();
            }
        });
        rvCartItems.setAdapter(cartPanelAdapter);
    }

    private void switchToCatalogView() {
        llCatalogView.setVisibility(View.VISIBLE);
        llCartView.setVisibility(View.GONE);
        cardOpenCartBar.setVisibility(View.VISIBLE);
        cardCheckoutPaymentBar.setVisibility(View.GONE);
        if (catalogAdapter != null) catalogAdapter.notifyDataSetChanged();
    }

    private void loadProducts() {
        availableProducts = db.productDao().getAllProducts();
        catalogAdapter = new CatalogAdapter(availableProducts);
        rvCatalogGrid.setAdapter(catalogAdapter);
        updateCartTotals();
    }

    private void filterProducts(String query) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity p : db.productDao().getAllProducts()) {
            if (p.getName().toLowerCase().contains(query.toLowerCase()) ||
                p.getBarcode().contains(query)) {
                filtered.add(p);
            }
        }
        availableProducts.clear();
        availableProducts.addAll(filtered);
        if (catalogAdapter != null) catalogAdapter.notifyDataSetChanged();
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
                loadProducts();
                btnFilterCategory.setText("Categoría");
            } else {
                List<ProductEntity> filtered = new ArrayList<>();
                for (ProductEntity p : db.productDao().getAllProducts()) {
                    if (p.getCategory().equalsIgnoreCase(selectedCategory)) {
                        filtered.add(p);
                    }
                }
                availableProducts.clear();
                availableProducts.addAll(filtered);
                if (catalogAdapter != null) catalogAdapter.notifyDataSetChanged();
                btnFilterCategory.setText(selectedCategory);
            }
        });
        rvCategoryList.setAdapter(categoryAdapter);

        dialog.show();
    }

    private void addProductToCart(ProductEntity p) {
        if (cartItemMap.containsKey(p.getId())) {
            cartItemMap.get(p.getId()).quantity++;
        } else {
            cartItemMap.put(p.getId(), new CartItem(p, 1));
        }
        updateCartTotals();
        Toast.makeText(this, "🛒 " + p.getName() + " añadido al carrito", Toast.LENGTH_SHORT).show();
    }

    private void addProductByBarcode(String barcode) {
        ProductEntity product = db.productDao().getProductByBarcode(barcode);
        if (product != null) {
            addProductToCart(product);
            etSearchProduct.setText("");
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCartItemCount() {
        int count = 0;
        for (CartItem item : cartItemMap.values()) {
            count += item.quantity;
        }
        return count;
    }

    private void updateCartTotals() {
        totalAmount = 0.0;
        totalCost = 0.0;
        int totalItems = 0;

        for (CartItem item : cartItemMap.values()) {
            totalAmount += item.product.getSalePrice() * item.quantity;
            totalCost += item.product.getCostPrice() * item.quantity;
            totalItems += item.quantity;
        }

        String formattedTotal = String.format(Locale.US, "S/ %.2f", totalAmount);
        tvTotalAmountCatalog.setText(formattedTotal);
        tvTotalAmountCheckout.setText(formattedTotal);
        tvCartBadge.setText(String.valueOf(totalItems));
    }

    private void processSale(String paymentMethod, long customerId, boolean finishActivity) {
        if (cartItemMap.isEmpty()) {
            Toast.makeText(this, "Añade al menos 1 producto para vender", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder itemsBuilder = new StringBuilder();
        for (CartItem item : cartItemMap.values()) {
            if (itemsBuilder.length() > 0) itemsBuilder.append("\n");
            itemsBuilder.append("• ").append(item.quantity).append("x ").append(item.product.getName());
        }
        String productSummary = itemsBuilder.toString();

        SaleEntity sale = new SaleEntity(
                System.currentTimeMillis(),
                totalAmount,
                totalCost,
                paymentMethod,
                customerId,
                productSummary
        );

        db.saleDao().insertSale(sale);

        // Update Room DB stock for each product
        for (CartItem item : cartItemMap.values()) {
            ProductEntity p = item.product;
            p.setStock(p.getStock() - item.quantity);
            db.productDao().update(p);
        }

        cartItemMap.clear();
        updateCartTotals();

        Toast.makeText(this, "¡Venta completada con éxito! ⚡", Toast.LENGTH_LONG).show();

        if (finishActivity) {
            finish();
        } else {
            switchToCatalogView();
            loadProducts();
        }
    }

    // Catalog Adapter (2-column Grid)
    private class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.ViewHolder> {
        private List<ProductEntity> list;

        public CatalogAdapter(List<ProductEntity> list) {
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
            holder.tvStockStatus.setVisibility(View.GONE);

            if (p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(p.getImageUrl().trim())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imgProduct);
            } else {
                holder.imgProduct.setImageResource(R.mipmap.ic_launcher);
            }

            // Click product card -> Add to Cart
            holder.itemView.setOnClickListener(v -> addProductToCart(p));
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

    // Cart Panel Adapter
    private class CartPanelAdapter extends RecyclerView.Adapter<CartPanelAdapter.ViewHolder> {
        private List<CartItem> itemList;
        private Runnable onCartChanged;

        public CartPanelAdapter(List<CartItem> itemList, Runnable onCartChanged) {
            this.itemList = itemList;
            this.onCartChanged = onCartChanged;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_product, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CartItem item = itemList.get(position);
            holder.tvTitle.setText(item.product.getName());
            holder.tvUnitPrice.setText(String.format(Locale.US, "S/ %.2f c/u", item.product.getSalePrice()));
            holder.tvItemTotal.setText(String.format(Locale.US, "S/ %.2f", item.product.getSalePrice() * item.quantity));
            holder.tvQuantity.setText(String.valueOf(item.quantity));

            if (item.product.getImageUrl() != null && !item.product.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.product.getImageUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .into(holder.imgProduct);
            }

            // + Button
            holder.btnAdd.setOnClickListener(v -> {
                item.quantity++;
                holder.tvQuantity.setText(String.valueOf(item.quantity));
                holder.tvItemTotal.setText(String.format(Locale.US, "S/ %.2f", item.product.getSalePrice() * item.quantity));
                onCartChanged.run();
            });

            // - Button
            holder.btnRemove.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    holder.tvQuantity.setText(String.valueOf(item.quantity));
                    holder.tvItemTotal.setText(String.format(Locale.US, "S/ %.2f", item.product.getSalePrice() * item.quantity));
                } else {
                    cartItemMap.remove(item.product.getId());
                    itemList.remove(position);
                    notifyDataSetChanged();
                }
                onCartChanged.run();
            });
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvUnitPrice, tvItemTotal, tvQuantity;
            ImageView imgProduct;
            ImageButton btnAdd, btnRemove;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvCartTitle);
                tvUnitPrice = itemView.findViewById(R.id.tvCartUnitPrice);
                tvItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
                tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
                imgProduct = itemView.findViewById(R.id.imgCartProduct);
                btnAdd = itemView.findViewById(R.id.btnAddItem);
                btnRemove = itemView.findViewById(R.id.btnRemoveItem);
            }
        }
    }

    // Custom Category Adapter
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

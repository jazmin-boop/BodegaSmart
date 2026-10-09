package com.example.bodega;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bodega.database.AppDatabase;
import com.example.bodega.database.CustomerEntity;
import com.example.bodega.database.FirebaseBackupHelper;
import com.example.bodega.database.SaleEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FiadosActivity extends AppCompatActivity {

    private RecyclerView rvCustomers, rvCustomerPurchaseHistory;
    private Button btnAddCustomer, btnEditCustomerDetail, btnDeleteCustomerDetail;
    private EditText etSearchCustomer;
    private TextView tvCustomerDetailName, tvCustomerDetailPhone, tvCustomerDetailDni, tvCustomerDetailDebt, btnBackToList, btnExitFiados;
    private LinearLayout llCustomerListPanel, llCustomerDetailPanel;

    private AppDatabase db;
    private CustomerAdapter adapter;
    private List<CustomerEntity> currentCustomerList = new ArrayList<>();
    private double fiadoSaleAmount = 0.0;
    private CustomerEntity activeCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fiados);

        db = AppDatabase.getInstance(this);

        fiadoSaleAmount = getIntent().getDoubleExtra("FIADO_SALE_AMOUNT", 0.0);

        // Panels
        llCustomerListPanel = findViewById(R.id.llCustomerListPanel);
        llCustomerDetailPanel = findViewById(R.id.llCustomerDetailPanel);

        // List Panel Views
        rvCustomers = findViewById(R.id.rvCustomers);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        btnExitFiados = findViewById(R.id.btnExitFiados);

        // Detail Panel Views
        btnBackToList = findViewById(R.id.btnBackToList);
        tvCustomerDetailName = findViewById(R.id.tvCustomerDetailName);
        tvCustomerDetailPhone = findViewById(R.id.tvCustomerDetailPhone);
        tvCustomerDetailDni = findViewById(R.id.tvCustomerDetailDni);
        tvCustomerDetailDebt = findViewById(R.id.tvCustomerDetailDebt);
        btnEditCustomerDetail = findViewById(R.id.btnEditCustomerDetail);
        btnDeleteCustomerDetail = findViewById(R.id.btnDeleteCustomerDetail);
        rvCustomerPurchaseHistory = findViewById(R.id.rvCustomerPurchaseHistory);

        // Exit / Back listeners
        btnExitFiados.setOnClickListener(v -> {
            if (llCustomerDetailPanel.getVisibility() == View.VISIBLE) {
                showCustomerListPanel();
            } else {
                finish();
            }
        });

        btnBackToList.setOnClickListener(v -> showCustomerListPanel());

        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerPurchaseHistory.setLayoutManager(new LinearLayoutManager(this));

        btnAddCustomer.setOnClickListener(v -> showAddCustomerDialog(null));

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (llCustomerDetailPanel != null && llCustomerDetailPanel.getVisibility() == View.VISIBLE) {
                    showCustomerListPanel();
                } else {
                    finish();
                }
            }
        });

        loadCustomers();

        if (fiadoSaleAmount > 0) {
            Toast.makeText(this, "Toca 'Seleccionar' en un cliente para agregar el fiado de S/ " + String.format(Locale.US, "%.2f", fiadoSaleAmount), Toast.LENGTH_LONG).show();
        }
    }

    private void showCustomerListPanel() {
        llCustomerListPanel.setVisibility(View.VISIBLE);
        llCustomerDetailPanel.setVisibility(View.GONE);
    }

    private void showCustomerDetailPanel(CustomerEntity c) {
        activeCustomer = c;
        llCustomerListPanel.setVisibility(View.GONE);
        llCustomerDetailPanel.setVisibility(View.VISIBLE);

        tvCustomerDetailName.setText(c.getName());
        tvCustomerDetailPhone.setText("Teléfono / WhatsApp: " + (c.getPhone().isEmpty() ? "Sin número" : c.getPhone()));
        tvCustomerDetailDni.setText("DNI: " + (c.getDni() == null || c.getDni().isEmpty() ? "Sin DNI" : c.getDni()));
        tvCustomerDetailDebt.setText(String.format(Locale.US, "S/ %.2f", c.getTotalDebt()));

        // Get sales history for this customer
        List<SaleEntity> allSales = db.saleDao().getAllSales();
        List<SaleEntity> customerSales = new ArrayList<>();
        double salesTotal = 0.0;
        for (SaleEntity s : allSales) {
            if (s.getCustomerId() == c.getId() && s.getTotalAmount() > 0) {
                customerSales.add(s);
                salesTotal += s.getTotalAmount();
            }
        }

        List<FiadoHistoryItem> historyList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.US);
        for (SaleEntity s : customerSales) {
            String dateStr = sdf.format(new Date(s.getTimestamp()));
            String summary = (s.getProductSummary() != null && !s.getProductSummary().isEmpty())
                    ? s.getProductSummary().replace(", ", "\n• ")
                    : "• Venta Fiada general";
            if (!summary.startsWith("• ")) {
                summary = "• " + summary;
            }
            historyList.add(new FiadoHistoryItem(
                    s.getId(),
                    dateStr,
                    String.format(Locale.US, "S/ %.2f", s.getTotalAmount()),
                    s.getTotalAmount(),
                    summary,
                    "Deuda Pendiente"
            ));
        }

        double remainingInitialDebt = c.getTotalDebt() - salesTotal;
        if (remainingInitialDebt > 0.01) {
            String dateStr = sdf.format(new Date());
            String summary = (c.getNotes() != null && !c.getNotes().isEmpty())
                    ? "• " + c.getNotes().replace(", ", "\n• ")
                    : "• Deuda / Productos iniciales";
            historyList.add(new FiadoHistoryItem(
                    0,
                    dateStr,
                    String.format(Locale.US, "S/ %.2f", remainingInitialDebt),
                    remainingInitialDebt,
                    summary,
                    "Deuda Pendiente"
            ));
        }

        PurchaseHistoryAdapter historyAdapter = new PurchaseHistoryAdapter(historyList);
        rvCustomerPurchaseHistory.setAdapter(historyAdapter);

        btnEditCustomerDetail.setOnClickListener(v -> showAddCustomerDialog(c));
        btnDeleteCustomerDetail.setOnClickListener(v -> showConfirmDeleteCustomerDialog(c));
    }

    private void loadCustomers() {
        currentCustomerList = db.customerDao().getAllCustomers();
        adapter = new CustomerAdapter(currentCustomerList);
        rvCustomers.setAdapter(adapter);
    }

    private void filterCustomers(String query) {
        List<CustomerEntity> filtered = new ArrayList<>();
        for (CustomerEntity c : db.customerDao().getAllCustomers()) {
            if (c.getName().toLowerCase().contains(query.toLowerCase()) ||
                c.getPhone().contains(query) ||
                (c.getDni() != null && c.getDni().contains(query))) {
                filtered.add(c);
            }
        }
        currentCustomerList.clear();
        currentCustomerList.addAll(filtered);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showAddCustomerDialog(CustomerEntity existingCustomer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_customer, null);

        TextView tvTitle = view.findViewById(R.id.tvAddCustomerTitle);
        EditText etName = view.findViewById(R.id.etName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etDni = view.findViewById(R.id.etDni);
        EditText etDebt = view.findViewById(R.id.etInitialDebt);
        Button btnCancel = view.findViewById(R.id.btnCancelCustomer);
        Button btnSave = view.findViewById(R.id.btnSaveCustomer);

        if (existingCustomer == null) {
            tvTitle.setText("Nuevo Cliente de Fiado");
            btnSave.setText("Guardar");
        } else {
            tvTitle.setText("Editar Cliente");
            btnSave.setText("Actualizar");
            etName.setText(existingCustomer.getName());
            etPhone.setText(existingCustomer.getPhone());
            etDni.setText(existingCustomer.getDni());
            etDebt.setText(String.valueOf(existingCustomer.getTotalDebt()));
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String phone = etPhone.getText().toString();
            String dni = etDni.getText().toString();
            double debt = etDebt.getText().toString().isEmpty() ? 0.0 : Double.parseDouble(etDebt.getText().toString());

            if (!name.isEmpty()) {
                if (existingCustomer == null) {
                    CustomerEntity customer = new CustomerEntity(name, phone, dni, debt, "");
                    long newId = db.customerDao().insert(customer);
                    if (debt > 0) {
                        SaleEntity initialSale = new SaleEntity(System.currentTimeMillis(), debt, 0, "FIADO", newId, "Deuda / Productos iniciales");
                        db.saleDao().insertSale(initialSale);
                    }
                    Toast.makeText(this, "Cliente guardado", Toast.LENGTH_SHORT).show();
                } else {
                    existingCustomer.setName(name);
                    existingCustomer.setPhone(phone);
                    existingCustomer.setDni(dni);
                    existingCustomer.setTotalDebt(debt);
                    db.customerDao().update(existingCustomer);
                    Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show();
                }
                FirebaseBackupHelper.syncToFirebase(this, false);
                loadCustomers();
                if (activeCustomer != null && activeCustomer.getId() == (existingCustomer != null ? existingCustomer.getId() : 0)) {
                    CustomerEntity updated = db.customerDao().getCustomerById(activeCustomer.getId());
                    if (updated != null) showCustomerDetailPanel(updated);
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showPaymentDialog(CustomerEntity customer, FiadoHistoryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Abonar / Pagar Deuda");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#081630"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Monto pendiente actual: " + item.amount);
        tvInfo.setTextSize(14);
        tvInfo.setTextColor(Color.parseColor("#79808F"));
        tvInfo.setPadding(0, 12, 0, 24);
        layout.addView(tvInfo);

        EditText etPayAmount = new EditText(this);
        etPayAmount.setHint("Monto a pagar (S/.)");
        etPayAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPayAmount.setText(String.format(Locale.US, "%.2f", item.amountValue));
        etPayAmount.setBackgroundResource(R.drawable.bg_search_bar);
        etPayAmount.setPadding(32, 24, 32, 24);
        layout.addView(etPayAmount);

        builder.setView(layout);
        builder.setPositiveButton("Confirmar Pago", (dialog, which) -> {
            String valStr = etPayAmount.getText().toString();
            if (valStr.isEmpty()) return;
            double payVal = Double.parseDouble(valStr);
            if (payVal <= 0) return;

            // 1. Deduct payVal from customer's total debt
            double newTotalDebt = Math.max(0.0, customer.getTotalDebt() - payVal);
            customer.setTotalDebt(newTotalDebt);
            db.customerDao().update(customer);

            // 2. If linked to a SaleEntity, update or remove sale
            if (item.saleId > 0) {
                for (SaleEntity s : db.saleDao().getAllSales()) {
                    if (s.getId() == item.saleId) {
                        double remainingSaleAmount = Math.max(0.0, s.getTotalAmount() - payVal);
                        s.setTotalAmount(remainingSaleAmount);
                        if (remainingSaleAmount <= 0.01) {
                            db.saleDao().deleteSale(s);
                        } else {
                            db.saleDao().updateSale(s);
                        }
                        break;
                    }
                }
            }

            Toast.makeText(this, "🎉 Pago de S/ " + String.format(Locale.US, "%.2f", payVal) + " registrado con éxito", Toast.LENGTH_LONG).show();

            FirebaseBackupHelper.syncToFirebase(this, false);

            // Refresh customer list and detail panel
            loadCustomers();
            CustomerEntity updatedCustomer = db.customerDao().getCustomerById(customer.getId());
            if (updatedCustomer != null) {
                showCustomerDetailPanel(updatedCustomer);
            } else {
                showCustomerListPanel();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }
        dialog.show();
    }

    private void showConfirmDeleteCustomerDialog(CustomerEntity c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        TextView tvMsg = view.findViewById(R.id.tvDeleteMessage);
        Button btnCancel = view.findViewById(R.id.btnCancelDelete);
        Button btnConfirm = view.findViewById(R.id.btnConfirmDelete);

        tvMsg.setText("¿Estás seguro de que deseas eliminar al cliente '" + c.getName() + "'? Se borrará su historial de deudas.");

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            db.customerDao().delete(c);
            FirebaseBackupHelper.syncToFirebase(this, false);
            loadCustomers();
            showCustomerListPanel();
            Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {
        private List<CustomerEntity> list;

        public CustomerAdapter(List<CustomerEntity> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CustomerEntity c = list.get(position);
            holder.tvName.setText(c.getName());
            holder.tvPhoneDni.setText("Tel: " + (c.getPhone().isEmpty() ? "-" : c.getPhone()) + " | DNI: " + (c.getDni() == null || c.getDni().isEmpty() ? "-" : c.getDni()));
            holder.tvDebt.setText(String.format(Locale.US, "Deuda Total: S/ %.2f", c.getTotalDebt()));

            boolean overdue = isCustomerOverdue10Days(c);
            if (overdue && c.getTotalDebt() > 0) {
                holder.tvOverdueBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvOverdueBadge.setVisibility(View.GONE);
            }

            if (fiadoSaleAmount > 0) {
                // Selection mode for a Fiado sale: hide eye & whatsapp, show "Seleccionar" button
                holder.llModuleActions.setVisibility(View.GONE);
                holder.btnSelect.setVisibility(View.VISIBLE);

                View.OnClickListener selectListener = v -> {
                    c.setTotalDebt(c.getTotalDebt() + fiadoSaleAmount);
                    db.customerDao().update(c);
                    FirebaseBackupHelper.syncToFirebase(FiadosActivity.this, false);
                    Toast.makeText(FiadosActivity.this, "📝 Fiado de S/ " + String.format(Locale.US, "%.2f", fiadoSaleAmount) + " agregado a " + c.getName(), Toast.LENGTH_LONG).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("SELECTED_CUSTOMER_ID", c.getId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                };

                holder.btnSelect.setOnClickListener(selectListener);
                holder.itemView.setOnClickListener(selectListener);
            } else {
                // Module mode: show eye & whatsapp, hide "Seleccionar" button
                holder.llModuleActions.setVisibility(View.VISIBLE);
                holder.btnSelect.setVisibility(View.GONE);

                holder.btnViewDetail.setOnClickListener(v -> showCustomerDetailPanel(c));
                holder.btnWhatsapp.setOnClickListener(v -> sendWhatsappReminder(c));
                holder.itemView.setOnClickListener(v -> showCustomerDetailPanel(c));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhoneDni, tvDebt, tvOverdueBadge;
            LinearLayout llModuleActions;
            ImageButton btnViewDetail, btnWhatsapp;
            Button btnSelect;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCustomerName);
                tvPhoneDni = itemView.findViewById(R.id.tvCustomerPhoneDni);
                tvDebt = itemView.findViewById(R.id.tvDebtAmount);
                tvOverdueBadge = itemView.findViewById(R.id.tvOverdueBadge);
                llModuleActions = itemView.findViewById(R.id.llModuleActions);
                btnViewDetail = itemView.findViewById(R.id.btnViewCustomerDetail);
                btnWhatsapp = itemView.findViewById(R.id.btnWhatsappCustomer);
                btnSelect = itemView.findViewById(R.id.btnSelectCustomerFiado);
            }
        }
    }

    private boolean isCustomerOverdue10Days(CustomerEntity c) {
        if (c == null || c.getTotalDebt() <= 0) return false;
        long tenDaysMs = 10L * 24 * 60 * 60 * 1000L;
        long now = System.currentTimeMillis();

        long oldestTimestamp = now;
        boolean foundSale = false;
        List<SaleEntity> sales = db.saleDao().getAllSales();
        for (SaleEntity s : sales) {
            if (s.getCustomerId() == c.getId() && s.getTotalAmount() > 0) {
                foundSale = true;
                if (s.getTimestamp() < oldestTimestamp) {
                    oldestTimestamp = s.getTimestamp();
                }
            }
        }

        if (foundSale) {
            return (now - oldestTimestamp) > tenDaysMs;
        } else {
            return true;
        }
    }

    private void sendWhatsappReminder(CustomerEntity c) {
        if (c.getPhone() == null || c.getPhone().trim().isEmpty()) {
            Toast.makeText(this, "El cliente '" + c.getName() + "' no tiene un número registrado", Toast.LENGTH_SHORT).show();
            return;
        }

        String phoneDigits = c.getPhone().replaceAll("[^0-9]", "");
        if (phoneDigits.length() == 9) {
            phoneDigits = "51" + phoneDigits; // Prepend Peru country code
        }

        if (phoneDigits.isEmpty()) {
            Toast.makeText(this, "El número de teléfono del cliente no es válido", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean overdue = isCustomerOverdue10Days(c);
        String message;
        if (overdue) {
            message = String.format(
                    Locale.US,
                    "Estimado %s, le saludamos de BodegaSmart. Le recordamos que cuenta con una deuda pendiente de S/ %.2f con MÁS DE 10 DÍAS SIN ABONAR. Por favor, acércate a cancelar a la brevedad. ¡Muchas gracias!",
                    c.getName(),
                    c.getTotalDebt()
            );
        } else {
            message = String.format(
                    Locale.US,
                    "Hola %s, te saludamos de la bodega. Tienes una deuda pendiente de un total de S/ %.2f. Por favor, acércate a cancelar. ¡Muchas gracias!",
                    c.getName(),
                    c.getTotalDebt()
            );
        }

        try {
            String url = "https://api.whatsapp.com/send?phone=" + phoneDigits + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    public static class FiadoHistoryItem {
        public long saleId;
        public String date;
        public String amount;
        public double amountValue;
        public String productSummary;
        public String status;

        public FiadoHistoryItem(long saleId, String date, String amount, double amountValue, String productSummary, String status) {
            this.saleId = saleId;
            this.date = date;
            this.amount = amount;
            this.amountValue = amountValue;
            this.productSummary = productSummary;
            this.status = status;
        }
    }

    // Purchase History Adapter
    private class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.ViewHolder> {
        private List<FiadoHistoryItem> historyList;

        public PurchaseHistoryAdapter(List<FiadoHistoryItem> historyList) {
            this.historyList = historyList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fiado_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FiadoHistoryItem item = historyList.get(position);
            holder.tvDate.setText(item.date);
            holder.tvAmount.setText(item.amount);
            holder.tvProducts.setText(item.productSummary);
            holder.tvStatus.setText(item.status);

            holder.btnPay.setOnClickListener(v -> showPaymentDialog(activeCustomer, item));
        }

        @Override
        public int getItemCount() {
            return historyList != null ? historyList.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvAmount, tvProducts, tvStatus;
            Button btnPay;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
                tvProducts = itemView.findViewById(R.id.tvHistoryProducts);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                btnPay = itemView.findViewById(R.id.btnPayDebtItem);
            }
        }
    }
}
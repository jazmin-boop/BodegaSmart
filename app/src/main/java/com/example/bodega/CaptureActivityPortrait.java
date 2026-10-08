package com.example.bodega;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CaptureActivityPortrait extends CaptureActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_barcode_scanner);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        if (barcodeScannerView != null) {
            View btnExit = barcodeScannerView.findViewById(R.id.btnExitScanner);
            if (btnExit != null) {
                btnExit.setOnClickListener(v -> finish());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (barcodeScannerView != null && barcodeScannerView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
    }
}
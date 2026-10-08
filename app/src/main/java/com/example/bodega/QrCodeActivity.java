package com.example.bodega;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QrCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        ImageView imgQrCode = findViewById(R.id.imgQrCode);

        // Exit button listener
        findViewById(R.id.btnExitQr).setOnClickListener(v -> finish());

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap("00020101021226480009pe.yape0124YAPE BODEGA SMART LIMA02109876543215204541153036045802PE5912BODEGA SMART6004LIMA63041234", BarcodeFormat.QR_CODE, 400, 400);
            imgQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

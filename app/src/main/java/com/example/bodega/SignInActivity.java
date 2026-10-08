package com.example.bodega;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.equalsIgnoreCase("mi@bodega.com") && password.equals("1234")) {
                Toast.makeText(this, "¡Bienvenido a BodegaSmart!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Credenciales incorrectas. Usa mi@bodega.com y 1234", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnQuickAccess).setOnClickListener(v -> {
            Toast.makeText(this, "⚡ Acceso Rápido: ¡Bienvenido a BodegaSmart!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            finish();
        });
    }
}

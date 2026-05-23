package com.example.ridenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.ridenow.network.ApiClient;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialCardView cardPassenger, cardDriver;
    private CheckBox checkboxTerms;
    private ProgressBar progressBar;
    private Button btnRegister;
    private TextView tvLogin, tvStepLabel;
    private View step1, step2, step3, step4;

    private String selectedRole = "client";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
        updateStepIndicator(2);
        selectRole("client");
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        cardPassenger = findViewById(R.id.cardPassenger);
        cardDriver = findViewById(R.id.cardDriver);

        checkboxTerms = findViewById(R.id.checkboxTerms);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        tvStepLabel = findViewById(R.id.tvStepLabel);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
    }

    private void setupListeners() {
        cardPassenger.setOnClickListener(v -> selectRole("client"));
        cardDriver.setOnClickListener(v -> selectRole("driver"));

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateStepIndicator(int step) {
        if (tvStepLabel != null) {
            tvStepLabel.setText("Étape " + step + " sur 4 — Informations personnelles");
        }

        int inactiveColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int activeColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);

        if (step1 != null) step1.setBackgroundColor(step >= 1 ? activeColor : inactiveColor);
        if (step2 != null) step2.setBackgroundColor(step >= 2 ? activeColor : inactiveColor);
        if (step3 != null) step3.setBackgroundColor(step >= 3 ? activeColor : inactiveColor);
        if (step4 != null) step4.setBackgroundColor(step >= 4 ? activeColor : inactiveColor);
    }

    private void selectRole(String role) {
        selectedRole = role;
        int activeColor = Color.parseColor("#00E676");
        int activeBgColor = Color.parseColor("#F0FFF8");
        int inactiveColor = Color.parseColor("#E0E0E0");
        int inactiveBgColor = Color.WHITE;

        if ("client".equals(role)) {
            cardPassenger.setStrokeColor(activeColor);
            cardPassenger.setStrokeWidth(3);
            cardPassenger.setCardBackgroundColor(activeBgColor);
            cardDriver.setStrokeColor(inactiveColor);
            cardDriver.setStrokeWidth(1);
            cardDriver.setCardBackgroundColor(inactiveBgColor);
        } else {
            cardDriver.setStrokeColor(activeColor);
            cardDriver.setStrokeWidth(3);
            cardDriver.setCardBackgroundColor(activeBgColor);
            cardPassenger.setStrokeColor(inactiveColor);
            cardPassenger.setStrokeWidth(1);
            cardPassenger.setCardBackgroundColor(inactiveBgColor);
        }
    }

    private boolean validateInputs() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (firstName.isEmpty()) {
            etFirstName.setError("Prénom requis");
            etFirstName.requestFocus();
            return false;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Nom requis");
            etLastName.requestFocus();
            return false;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email requis");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email invalide");
            etEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Mot de passe requis");
            etPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Mot de passe trop court (min 6 caractères)");
            etPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Les mots de passe ne correspondent pas");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "Veuillez accepter les conditions d'utilisation", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void register() {
        if (!validateInputs()) {
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String fullName = etFirstName.getText().toString().trim() + " " + etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        btnRegister.setEnabled(false);
        btnRegister.setText("Inscription...");
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.register(email, password, selectedRole, fullName, phone, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject json = new JSONObject(response);
                        String userRole = json.getString("role");

                        Toast.makeText(RegisterActivity.this, "✅ Inscription réussie ! Veuillez vous connecter", Toast.LENGTH_LONG).show();

                        // ✅ CORRECTION : TOUJOURS rediriger vers LoginActivity
                        // Ne JAMAIS rediriger directement vers DriverHomeActivity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("just_registered", true);
                        startActivity(intent);
                        finish();

                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "❌ " + error, Toast.LENGTH_SHORT).show();
                    resetButton();
                });
            }
        });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("S'INSCRIRE");
    }
}
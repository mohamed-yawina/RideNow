package com.example.ridenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ridenow.network.ApiClient;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private TextInputLayout tilEmail, tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
        checkIfLoggedIn();

        // Message de bienvenue si vient de l'inscription
        if (getIntent().getBooleanExtra("just_registered", false)) {
            Toast.makeText(this, "✅ Compte créé ! Connectez-vous", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void checkIfLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("RideNow", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String role = prefs.getString("role", null);
        String token = prefs.getString("token", null);

        if (userId != null && token != null) {
            ApiClient.setAuthToken(token);
            redirectBasedOnRole(role);
        }
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

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
        return true;
    }

    private void login() {
        if (!validateInputs()) {
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnLogin.setEnabled(false);
        btnLogin.setText("Connexion...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiClient.login(email, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject json = new JSONObject(response);
                        String userId = json.getString("user_id");
                        String role = json.getString("role");
                        String userEmail = json.getString("email");
                        String fullName = json.optString("full_name", "");
                        String token = json.optString("token", "");

                        SharedPreferences prefs = getSharedPreferences("RideNow", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_id", userId);
                        editor.putString("email", userEmail);
                        editor.putString("full_name", fullName);
                        editor.putString("role", role);
                        editor.putString("token", token);
                        editor.apply();

                        ApiClient.setAuthToken(token);

                        Toast.makeText(LoginActivity.this, "Bienvenue " + userEmail + " !", Toast.LENGTH_SHORT).show();
                        redirectBasedOnRole(role);

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButton();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "❌ " + error, Toast.LENGTH_SHORT).show();
                    resetButton();
                });
            }
        });
    }

    private void redirectBasedOnRole(String role) {
        Intent intent;
        if ("driver".equals(role)) {
            intent = new Intent(LoginActivity.this, DriverHomeActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, ClientHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("SE CONNECTER");
    }
}
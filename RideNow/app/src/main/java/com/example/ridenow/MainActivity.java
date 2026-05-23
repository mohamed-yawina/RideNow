package com.example.ridenow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ridenow.network.ApiClient;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier si une session existe
        SharedPreferences prefs = getSharedPreferences("RideNow", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String role = prefs.getString("role", null);
        String token = prefs.getString("token", null);

        if (userId != null && token != null) {
            // Restaurer le token pour l'ApiClient
            ApiClient.setAuthToken(token);
            
            // Rediriger vers l'accueil correspondant
            Intent intent;
            if ("driver".equals(role)) {
                intent = new Intent(this, DriverHomeActivity.class);
            } else {
                intent = new Intent(this, ClientHomeActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            // Sinon, aller à l'écran de connexion
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
package com.example.ridenow;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ridenow.network.ApiClient;
import org.json.JSONObject;

public class NegotiationActivity extends AppCompatActivity {

    private TextView tvClientName, tvPickup, tvDropoff, tvClientOfferPrice, tvNegStatus;
    private EditText etCounterOffer;
    private Button btnAccept, btnRefuse, btnSendCounter;

    private String rideId;
    private double clientOffer;
    private Handler mainHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_negotiation);

        rideId = getIntent().getStringExtra("ride_id");
        String clientName = getIntent().getStringExtra("client_name");
        String destination = getIntent().getStringExtra("destination");
        clientOffer = getIntent().getDoubleExtra("offer", 0);

        initViews();
        populateUI(clientName, destination);
        setupListeners();
    }

    private void initViews() {
        tvClientName = findViewById(R.id.tvClientName);
        tvPickup = findViewById(R.id.tvPickup);
        tvDropoff = findViewById(R.id.tvDropoff);
        tvClientOfferPrice = findViewById(R.id.tvClientOfferPrice);
        tvNegStatus = findViewById(R.id.tvNegStatus);
        etCounterOffer = findViewById(R.id.etCounterOffer);
        btnAccept = findViewById(R.id.btnAccept);
        btnRefuse = findViewById(R.id.btnRefuse);
        btnSendCounter = findViewById(R.id.btnSendCounter);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void populateUI(String clientName, String destination) {
        tvClientName.setText(clientName);
        tvPickup.setText("Départ: Position actuelle");
        tvDropoff.setText("Destination: " + destination);
        tvClientOfferPrice.setText((int) clientOffer + " MAD");
    }

    private void setupListeners() {
        btnAccept.setOnClickListener(v -> acceptOffer());
        btnRefuse.setOnClickListener(v -> refuseRequest());
        btnSendCounter.setOnClickListener(v -> sendCounterOffer());
    }

    private void sendCounterOffer() {
        String val = etCounterOffer.getText().toString().trim();
        if (val.isEmpty()) {
            etCounterOffer.setError("Entrez un montant");
            return;
        }

        double counterPrice = Double.parseDouble(val);
        tvNegStatus.setText("Contre-offre envoyée: " + (int) counterPrice + " MAD");

        // Ici faire l'appel API pour envoyer la contre-offre
        Toast.makeText(this, "Contre-offre envoyée", Toast.LENGTH_SHORT).show();
    }

    private void acceptOffer() {
        ApiClient.acceptRide(rideId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    Toast.makeText(NegotiationActivity.this, "✅ Course acceptée !", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(NegotiationActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void refuseRequest() {
        ApiClient.cancelRide(rideId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    Toast.makeText(NegotiationActivity.this, "Course refusée", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(NegotiationActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
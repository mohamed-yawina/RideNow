package com.example.ridenow;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.ridenow.network.ApiClient;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.Locale;

public class ActiveRideTrackingActivity extends AppCompatActivity {

    private static final String TAG = "RideTracking";
    private TextView tvClientName, tvClientPhone, tvPickup, tvDestination, tvStatus, tvPrice, tvStatusBadge;
    private LinearLayout layoutPhone;
    private MaterialButton btnNavigate, btnClientPickedUp, btnStartRide, btnCompleteRide, btnCancelRide;
    private ProgressBar progressBar;
    
    private String rideId;
    private String currentStatus = "";
    private double pickupLat = 0, pickupLng = 0;
    private double destLat = 0, destLng = 0;
    private String pickupAddress = "";
    private String destinationAddress = "";
    private String clientPhone = "";
    private double price = 0;
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler pollingHandler = new Handler();
    private Runnable pollingRunnable;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_ride_tracking);

        rideId = getIntent().getStringExtra("ride_id");
        Log.d(TAG, "Initialisation avec rideId: " + rideId);

        initViews();
        setupListeners();
        loadRideDetails();
        startStatusPolling();
    }

    private void initViews() {
        tvClientName = findViewById(R.id.tvClientName);
        tvClientPhone = findViewById(R.id.tvClientPhone);
        layoutPhone = findViewById(R.id.layoutPhone);
        tvPickup = findViewById(R.id.tvPickup);
        tvDestination = findViewById(R.id.tvDestination);
        tvStatus = findViewById(R.id.tvStatus);
        tvPrice = findViewById(R.id.tvPrice);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        
        btnNavigate = findViewById(R.id.btnNavigate);
        btnClientPickedUp = findViewById(R.id.btnClientPickedUp);
        btnStartRide = findViewById(R.id.btnStartRide);
        btnCompleteRide = findViewById(R.id.btnCompleteRide);
        btnCancelRide = findViewById(R.id.btnCancelRide);
        progressBar = findViewById(R.id.progressBar);
        
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnNavigate.setOnClickListener(v -> openNavigation());
        btnClientPickedUp.setOnClickListener(v -> handleClientPickedUp());
        btnStartRide.setOnClickListener(v -> handleStartRide());
        btnCompleteRide.setOnClickListener(v -> handleCompleteRide());
        btnCancelRide.setOnClickListener(v -> showCancelConfirmation());
        if (layoutPhone != null) {
            layoutPhone.setOnClickListener(v -> callClient());
        }
    }

    private void startStatusPolling() {
        pollingRunnable = () -> {
            if (!isProcessing) {
                loadRideDetails();
            }
            pollingHandler.postDelayed(pollingRunnable, 5000);
        };
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void loadRideDetails() {
        ApiClient.getCurrentRide(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONObject ride = json.optJSONObject("ride");
                        if (ride == null && json.has("id")) ride = json;

                        if (ride != null) {
                            String newStatus = ride.optString("status", "").toLowerCase().trim();
                            rideId = ride.optString("id", rideId);
                            price = ride.optDouble("offer", 0);
                            
                            pickupAddress = ride.optString("origin_address", ride.optString("pickup_address", "Position actuelle"));
                            destinationAddress = ride.optString("destination", ride.optString("destination_address", "Destination"));

                            extractCoordinates(ride);
                            
                            if (ride.has("client") && !ride.isNull("client")) {
                                JSONObject client = ride.getJSONObject("client");
                                tvClientName.setText(client.optString("full_name", "Client"));
                                clientPhone = client.optString("tel", "");
                                if (tvClientPhone != null) tvClientPhone.setText(clientPhone);
                                if (layoutPhone != null) layoutPhone.setVisibility(clientPhone.isEmpty() ? View.GONE : View.VISIBLE);
                            }

                            tvPickup.setText(pickupAddress);
                            tvDestination.setText(destinationAddress);
                            tvPrice.setText(String.format(Locale.getDefault(), "%.0f MAD", price));
                            
                            currentStatus = newStatus;
                            updateUIForStatus(currentStatus);
                        } else {
                            // Pas de course active, on retourne à l'accueil
                            Log.d(TAG, "Aucune course active trouvée, fermeture.");
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur parsing détails course", e);
                    }
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    if (error.contains("Aucune course")) {
                        finish();
                    }
                });
            }
        });
    }

    private void extractCoordinates(JSONObject ride) {
        pickupLat = ride.optDouble("origin_lat", ride.optDouble("pickup_lat", ride.optDouble("lat", 0)));
        pickupLng = ride.optDouble("origin_lng", ride.optDouble("pickup_lng", ride.optDouble("lng", 0)));
        destLat = ride.optDouble("destination_lat", ride.optDouble("dest_lat", 0));
        destLng = ride.optDouble("destination_lng", ride.optDouble("dest_lng", 0));
    }

    private void updateUIForStatus(String status) {
        // Cacher tous les boutons par défaut pour éviter les clics erronés
        btnNavigate.setVisibility(View.GONE);
        btnClientPickedUp.setVisibility(View.GONE);
        btnStartRide.setVisibility(View.GONE);
        btnCompleteRide.setVisibility(View.GONE);
        btnCancelRide.setVisibility(View.GONE);

        switch (status) {
            case "matching":
                // ⏳ Statut B : En attente du client
                tvStatusBadge.setText("En attente");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.orange_500));
                tvStatus.setText("⏳ Attente de confirmation du client...");
                btnCancelRide.setVisibility(View.VISIBLE);
                break;

            case "acceptée":
            case "accepted":
                // 📍 Statut C : Acceptée - Aller vers le client
                tvStatusBadge.setText("Acceptée");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.green_primary));
                tvStatus.setText("📍 Allez chercher le client");
                btnNavigate.setVisibility(View.VISIBLE);
                btnNavigate.setText("🗺️ NAVIGUER VERS LE CLIENT");
                btnClientPickedUp.setVisibility(View.VISIBLE);
                btnCancelRide.setVisibility(View.VISIBLE);
                break;

            case "picked_up":
            case "client_pris":
                // 🚗 Statut D : Picked_up - Prêt à démarrer
                tvStatusBadge.setText("Client à bord");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.blue_500));
                tvStatus.setText("📍 Client pris en charge. Prêt ?");
                btnNavigate.setVisibility(View.VISIBLE);
                btnNavigate.setText("🗺️ NAVIGUER VERS LA DESTINATION");
                btnStartRide.setVisibility(View.VISIBLE);
                btnCancelRide.setVisibility(View.GONE); // Plus d'annulation après pickup
                break;

            case "en cours":
            case "started":
            case "in_progress":
                // 🏁 Statut E : En cours - Vers la destination
                tvStatusBadge.setText("En cours");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.orange_500));
                tvStatus.setText("🚗 Course en cours...");
                btnNavigate.setVisibility(View.VISIBLE);
                btnNavigate.setText("🗺️ NAVIGUER VERS LA DESTINATION");
                btnCompleteRide.setVisibility(View.VISIBLE);
                break;

            case "terminée":
            case "completed":
                // ✅ Statut F : Terminée
                tvStatusBadge.setText("Terminée");
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.green_primary));
                tvStatus.setText("🏁 Course terminée");
                break;
        }
    }

    private void openNavigation() {
        double lat = 0, lng = 0;
        if (currentStatus.equals("acceptée") || currentStatus.equals("accepted")) {
            lat = pickupLat;
            lng = pickupLng;
        } else {
            lat = destLat;
            lng = destLng;
        }

        if (lat != 0 && lng != 0) {
            openGoogleMaps(lat, lng);
        } else {
            Toast.makeText(this, "Coordonnées non disponibles", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGoogleMaps(double lat, double lng) {
        try {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/maps?q=" + lat + "," + lng)));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur navigation", Toast.LENGTH_SHORT).show();
        }
    }

    private void callClient() {
        if (clientPhone != null && !clientPhone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + clientPhone));
            startActivity(intent);
        }
    }

    private void handleClientPickedUp() {
        if (!currentStatus.equals("acceptée") && !currentStatus.equals("accepted")) {
            Toast.makeText(this, "Action impossible : attendez que le client accepte", Toast.LENGTH_LONG).show();
            return;
        }
        executeAction(() -> ApiClient.clientPickedUp(rideId, new ApiClient.ApiCallback() {
            @Override public void onSuccess(String r) { actionDone("✅ Client à bord !"); }
            @Override public void onError(String e) { actionError(e); }
        }));
    }

    private void handleStartRide() {
        executeAction(() -> ApiClient.startRide(rideId, new ApiClient.ApiCallback() {
            @Override public void onSuccess(String r) { actionDone("🚗 Course démarrée !"); }
            @Override public void onError(String e) { actionError(e); }
        }));
    }

    private void handleCompleteRide() {
        executeAction(() -> ApiClient.completeRide(rideId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String r) {
                mainHandler.post(() -> {
                    Toast.makeText(ActiveRideTrackingActivity.this, "🏁 Course terminée !", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
            @Override public void onError(String e) { actionError(e); }
        }));
    }

    private void executeAction(Runnable action) {
        if (isProcessing) return;
        isProcessing = true;
        showLoading(true);
        action.run();
    }

    private void actionDone(String msg) {
        mainHandler.post(() -> {
            isProcessing = false;
            showLoading(false);
            Toast.makeText(ActiveRideTrackingActivity.this, msg, Toast.LENGTH_SHORT).show();
            loadRideDetails();
        });
    }

    private void actionError(String err) {
        mainHandler.post(() -> {
            isProcessing = false;
            showLoading(false);
            Toast.makeText(ActiveRideTrackingActivity.this, "❌ Erreur: " + err, Toast.LENGTH_LONG).show();
        });
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Annuler")
                .setMessage("Annuler cette course ?")
                .setPositiveButton("Oui", (d, w) -> cancelRide())
                .setNegativeButton("Non", null)
                .show();
    }

    private void cancelRide() {
        executeAction(() -> ApiClient.cancelRide(rideId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String r) {
                mainHandler.post(() -> {
                    Toast.makeText(ActiveRideTrackingActivity.this, "Course annulée", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override public void onError(String e) { actionError(e); }
        }));
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollingHandler.removeCallbacks(pollingRunnable);
    }
}
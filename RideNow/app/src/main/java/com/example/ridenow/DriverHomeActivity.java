package com.example.ridenow;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ridenow.network.ApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DriverHomeActivity extends AppCompatActivity {

    private static final String TAG = "DriverHome";
    private SwitchMaterial switchOnline;
    private TextView tvStatusPill, tvDriverName, tvVehicleInfo, tvRating, tvAvatarInitials;
    private LinearLayout layoutOffline;
    private RecyclerView rvRideRequests;
    private MaterialButton btnGoOnline;
    private CardView cardAvatar;
    private TextView tvTodayTrips, tvTodayEarnings, tvAcceptRate;
    private BottomNavigationView bottomNav;
    private View scrollContent;
    private View fragmentContainer;

    private SharedPreferences prefs;
    private boolean isOnline = false;
    private String driverId;
    private RideRequestAdapter adapter;
    private Handler pollingHandler = new Handler();
    private Runnable pollingRunnable;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Handler activeRideHandler = new Handler();
    private Runnable activeRideRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        prefs = getSharedPreferences("RideNow", MODE_PRIVATE);
        driverId = prefs.getString("user_id", "");

        initViews();
        setupListeners();
        setupBottomNavigation();
        loadDriverProfile();
        loadDriverStatus();
        loadDriverStats();
        startPolling();
        checkForNewActiveRide();
    }

    private void initViews() {
        switchOnline = findViewById(R.id.switchOnline);
        tvStatusPill = findViewById(R.id.tvStatusPill);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        tvRating = findViewById(R.id.tvRating);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        layoutOffline = findViewById(R.id.layoutOffline);
        rvRideRequests = findViewById(R.id.rvRideRequests);
        btnGoOnline = findViewById(R.id.btnGoOnline);
        cardAvatar = findViewById(R.id.cardAvatar);
        tvTodayTrips = findViewById(R.id.tvTodayTrips);
        tvTodayEarnings = findViewById(R.id.tvTodayEarnings);
        tvAcceptRate = findViewById(R.id.tvAcceptRate);
        bottomNav = findViewById(R.id.bottomNav);
        scrollContent = findViewById(R.id.scrollContent);
        fragmentContainer = findViewById(R.id.fragmentContainer);

        rvRideRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RideRequestAdapter(new ArrayList<>(), new RideRequestAdapter.OnRideActionListener() {
            @Override
            public void onNegotiate(RideRequest request) {
                onNegotiateClicked(request);
            }

            @Override
            public void onAccept(RideRequest request) {
                onAcceptClicked(request);
            }

            @Override
            public void onRefuse(RideRequest request) {
                onRefuseClicked(request);
            }
        });
        rvRideRequests.setAdapter(adapter);
        fragmentContainer.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnGoOnline.setOnClickListener(v -> setOnlineStatus(true));
        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> setOnlineStatus(isChecked));
        cardAvatar.setOnClickListener(v -> showProfileMenu());
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showHomeFragment();
                return true;
            } else if (itemId == R.id.nav_history) {
                showHistoryFragment();
                return true;
            } else if (itemId == R.id.nav_profile) {
                showProfileFragment();
                return true;
            }
            return false;
        });
    }

    private void showHomeFragment() {
        scrollContent.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        fetchRideRequests();
        loadDriverStats();
    }

    private void showHistoryFragment() {
        scrollContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new HistoryFragment())
                .commit();
    }

    private void showProfileFragment() {
        scrollContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new ProfileFragment())
                .commit();
    }

    private void loadDriverProfile() {
        String fullName = prefs.getString("full_name", "Chauffeur");
        String firstName = fullName.split(" ")[0];
        tvDriverName.setText(firstName);
        tvAvatarInitials.setText(fullName.substring(0, 1).toUpperCase());

        String vehicleType = prefs.getString("vehicle_type", "voiture");
        String vehicleIcon = getVehicleIcon(vehicleType);
        tvVehicleInfo.setText(vehicleIcon + " " + vehicleType);
    }

    private String getVehicleIcon(String vehicleType) {
        if (vehicleType == null) return "🚗";
        switch (vehicleType) {
            case "taxi": return "🚖";
            case "moto": return "🏍️";
            default: return "🚗";
        }
    }

    private void loadDriverStatus() {
        ApiClient.getDriverStatus(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    boolean available = json.optBoolean("available", false);
                    double rating = json.optDouble("rating", 4.5);
                    String ratingText = String.format("★ %.1f", rating);

                    prefs.edit().putFloat("driver_rating", (float) rating).apply();

                    mainHandler.post(() -> {
                        tvRating.setText(ratingText);
                        setOnlineStatus(available);
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> setOnlineStatus(false));
                }
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> setOnlineStatus(false));
            }
        });
    }

    private void setOnlineStatus(boolean online) {
        isOnline = online;
        switchOnline.setChecked(online);

        if (online) {
            tvStatusPill.setText("En ligne");
            tvStatusPill.setTextColor(ContextCompat.getColor(this, R.color.green_primary));
            tvStatusPill.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_online_pill));
            layoutOffline.setVisibility(View.GONE);
            rvRideRequests.setVisibility(View.VISIBLE);
            ApiClient.updateDriverStatus(true, new ApiClient.ApiCallback() {
                @Override public void onSuccess(String r) {}
                @Override public void onError(String e) {}
            });
            fetchRideRequests();
        } else {
            tvStatusPill.setText("Hors ligne");
            tvStatusPill.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            tvStatusPill.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_online_pill));
            layoutOffline.setVisibility(View.VISIBLE);
            rvRideRequests.setVisibility(View.GONE);
            ApiClient.updateDriverStatus(false, new ApiClient.ApiCallback() {
                @Override public void onSuccess(String r) {}
                @Override public void onError(String e) {}
            });
        }
    }

    private void loadDriverStats() {
        ApiClient.getDriverStats(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        int todayTrips = json.optInt("today_trips", 0);
                        double todayEarnings = json.optDouble("today_earnings", 0);
                        int acceptRate = json.optInt("accept_rate", 0);
                        tvTodayTrips.setText(String.valueOf(todayTrips));
                        tvTodayEarnings.setText(String.valueOf((int)todayEarnings));
                        tvAcceptRate.setText(acceptRate + "%");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    tvTodayTrips.setText("0");
                    tvTodayEarnings.setText("0");
                    tvAcceptRate.setText("0%");
                });
            }
        });
    }

    private void startPolling() {
        pollingRunnable = () -> {
            if (isOnline) {
                fetchRideRequests();
                loadDriverStats();
                refreshRating();
            }
            pollingHandler.postDelayed(pollingRunnable, 8000);
        };
        pollingHandler.post(pollingRunnable);
    }

    private void refreshRating() {
        ApiClient.getDriverStatus(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    double rating = json.optDouble("rating", 4.5);
                    String ratingText = String.format("★ %.1f", rating);
                    prefs.edit().putFloat("driver_rating", (float) rating).apply();
                    mainHandler.post(() -> tvRating.setText(ratingText));
                } catch (Exception e) {}
            }
            @Override
            public void onError(String error) {}
        });
    }

    private void fetchRideRequests() {
        ApiClient.getAvailableRides(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray rides = json.getJSONArray("rides");
                        List<RideRequest> requests = new ArrayList<>();

                        for (int i = 0; i < rides.length(); i++) {
                            JSONObject ride = rides.getJSONObject(i);
                            RideRequest request = new RideRequest();
                            request.setId(ride.getString("id"));
                            request.setDestination(ride.getString("destination"));
                            request.setVehicleType(ride.optString("vehicle_type", "voiture"));
                            request.setOffer(ride.optInt("offer", 30));
                            request.setDistance(ride.optDouble("distance", 5.0));
                            request.setClientRating(ride.optDouble("client_rating", 4.5));
                            request.setClientName(ride.optString("client_name", "Client"));
                            requests.add(request);
                        }
                        adapter.updateList(requests);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {});
            }
        });
    }

    private void checkForNewActiveRide() {
        activeRideRunnable = () -> {
            if (isOnline) {
                ApiClient.getActiveRides(new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            JSONArray rides = json.getJSONArray("rides");

                            if (rides.length() > 0) {
                                JSONObject ride = rides.getJSONObject(0);
                                String status = ride.getString("status").toLowerCase();
                                String id = ride.getString("id");

                                if (status.equals("acceptée") || status.equals("accepted") || 
                                    status.equals("picked_up") || status.equals("en cours")) {
                                    
                                    mainHandler.post(() -> {
                                        startActivity(new Intent(DriverHomeActivity.this, ActiveRideTrackingActivity.class)
                                                .putExtra("ride_id", id));
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onError(String error) {}
                });
            }
            activeRideHandler.postDelayed(activeRideRunnable, 8000);
        };
        activeRideHandler.post(activeRideRunnable);
    }

    private void onNegotiateClicked(RideRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Négocier le prix");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Prix proposé (MAD)");
        input.setText(String.valueOf((int)request.getOffer()));
        builder.setView(input);

        builder.setPositiveButton("Envoyer", (dialog, which) -> {
            String priceStr = input.getText().toString();
            if (!priceStr.isEmpty()) {
                double counterOffer = Double.parseDouble(priceStr);
                sendNegotiation(request.getId(), counterOffer);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void sendNegotiation(String rideId, double counterOffer) {
        ApiClient.sendNegotiation(rideId, counterOffer, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    Toast.makeText(DriverHomeActivity.this, "✅ Contre-offre envoyée", Toast.LENGTH_LONG).show();
                    showLocalNotification("Offre envoyée", "Votre offre de " + (int)counterOffer + " MAD a été envoyée");
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(DriverHomeActivity.this, "❌ Erreur: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onAcceptClicked(RideRequest request) {
        // Direct acceptance as requested to bypass matching issues
        showLocalNotification("Acceptation", "Acceptation de la course...");
        acceptRideDirectly(request.getId());
    }

    private void acceptRideDirectly(String rideId) {
        ApiClient.acceptRide(rideId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    Toast.makeText(DriverHomeActivity.this, "✅ Course acceptée !", Toast.LENGTH_LONG).show();
                    showLocalNotification("Course acceptée", "La course a été acceptée avec succès");
                    fetchRideRequests();
                    startRideTracking(rideId);
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(DriverHomeActivity.this, "❌ Erreur: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onRefuseClicked(RideRequest request) {
        ApiClient.declineRide(request.getId(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    showLocalNotification("Course refusée", "Vous avez refusé la course vers " + request.getDestination());
                    Toast.makeText(DriverHomeActivity.this, "Course refusée", Toast.LENGTH_SHORT).show();
                    fetchRideRequests();
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(DriverHomeActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLocalNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "ride_now_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "RideNow Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void startRideTracking(String rideId) {
        Intent intent = new Intent(this, ActiveRideTrackingActivity.class);
        intent.putExtra("ride_id", rideId);
        startActivity(intent);
    }

    private void showProfileMenu() {
        String[] options = {"👤 Mon profil", "🚗 Changer véhicule", "🚪 Déconnexion"};

        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showProfileDialog();
                            break;
                        case 1:
                            showVehicleTypeDialog();
                            break;
                        case 2:
                            logout();
                            break;
                    }
                })
                .show();
    }

    private void showProfileDialog() {
        String fullName = prefs.getString("full_name", "Chauffeur");
        String email = prefs.getString("email", "");
        String vehicleType = prefs.getString("vehicle_type", "voiture");
        float rating = prefs.getFloat("driver_rating", 0);

        String message = "👤 " + fullName + "\n" +
                "📧 " + email + "\n" +
                "🚗 " + getVehicleIcon(vehicleType) + " " + vehicleType + "\n" +
                "⭐ " + String.format("%.1f", rating);

        new AlertDialog.Builder(this)
                .setTitle("Mon profil")
                .setMessage(message)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showVehicleTypeDialog() {
        String[] vehicleTypes = {"🚗 Voiture", "🚖 Taxi", "🏍️ Moto"};
        String currentType = prefs.getString("vehicle_type", "voiture");
        int currentIndex = currentType.equals("taxi") ? 1 : (currentType.equals("moto") ? 2 : 0);

        new AlertDialog.Builder(this)
                .setTitle("Changer de véhicule")
                .setSingleChoiceItems(vehicleTypes, currentIndex, (dialog, which) -> {
                    String newType = which == 1 ? "taxi" : (which == 2 ? "moto" : "voiture");
                    updateVehicleType(newType);
                    dialog.dismiss();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateVehicleType(String newType) {
        ApiClient.updateDriverVehicle(newType, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                prefs.edit().putString("vehicle_type", newType).apply();
                mainHandler.post(() -> {
                    String vehicleIcon = getVehicleIcon(newType);
                    tvVehicleInfo.setText(vehicleIcon + " " + newType);
                    Toast.makeText(DriverHomeActivity.this, "Véhicule changé: " + newType, Toast.LENGTH_SHORT).show();
                    fetchRideRequests();
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> Toast.makeText(DriverHomeActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void logout() {
        ApiClient.logout(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    prefs.edit().clear().apply();
                    ApiClient.clearAuthToken();
                    Intent intent = new Intent(DriverHomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    prefs.edit().clear().apply();
                    ApiClient.clearAuthToken();
                    Intent intent = new Intent(DriverHomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        if (activeRideRunnable != null) {
            activeRideHandler.removeCallbacks(activeRideRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOnline) {
            fetchRideRequests();
            loadDriverStats();
        }
    }
}
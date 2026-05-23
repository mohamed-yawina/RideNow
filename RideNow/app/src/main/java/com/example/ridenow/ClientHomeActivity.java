package com.example.ridenow;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ridenow.network.ApiClient;
import com.example.ridenow.utils.PlaceSuggestionAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import java.util.ArrayList;
import java.util.List;

public class ClientHomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL_MS = 30000;
    private static final long SEARCH_POLLING_INTERVAL = 5000;

    // UI Components
    private MapView mapView;
    private AutoCompleteTextView etDestination;
    private EditText etPrice;
    private TextView tvWelcome, tvPriceHint;
    private MaterialButton btnRequestRide;
    private CardView btnMyLocation;
    private ImageView btnLogout;
    private BottomNavigationView bottomNavigation;
    private FrameLayout fragmentContainer;
    private CardView cardBottom;

    // Searching UI
    private CardView cardSearching;
    private ProgressBar progressBarSearch;
    private TextView tvSearchStatus;
    private Button btnCancelSearch;

    // Driver Offer UI
    private CardView cardDriverOffer;
    private TextView tvOfferDriverName, tvOfferDriverVehicle, tvOfferDriverRating;
    private TextView tvOfferPickup, tvOfferDestination, tvOfferPrice, tvOfferTimer;
    private Button btnAcceptOffer, btnRefuseOffer;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int timeLeft = 30;
    private String currentOfferRideId = null;

    // Active Ride UI
    private CardView cardActiveRide;
    private TextView tvActiveDriverName, tvActiveDriverVehicle, tvActiveDriverRating;
    private TextView tvActivePickup, tvActiveDestination, tvActivePrice, tvRideStatus;
    private Button btnCompleteActiveRide;

    // Data
    private SharedPreferences prefs;
    private GeoPoint currentLocation;
    private Marker currentLocationMarker;
    private final List<Marker> driverMarkers = new ArrayList<>();
    private PlaceSuggestionAdapter suggestionAdapter;
    private String selectedPlaceName = "";
    private double selectedDestLat = 0, selectedDestLng = 0;
    private String selectedVehicleType = "voiture";
    private String currentRideId = null;
    private String currentSearchRideId = null;
    private boolean isSelectingFromList = false;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private final Handler locationHandler = new Handler();
    private Runnable locationRunnable;
    private final Handler rideStatusHandler = new Handler();
    private Runnable rideStatusRunnable;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        setContentView(R.layout.activity_client_home);

        prefs = getSharedPreferences("RideNow", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        initViews();
        setupBottomNavigation();
        setupLocationListener();
        checkTokenAndLoadProfile();
        setupListeners();
        setupMap();
        setupAutocomplete();
        checkLocationPermission();
        checkCurrentRide();
        startWatchingForRideStatus();
        checkNotifications();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        etDestination = findViewById(R.id.etDestination);
        etPrice = findViewById(R.id.etPrice);
        tvPriceHint = findViewById(R.id.tvPriceHint);
        btnRequestRide = findViewById(R.id.btnRequestRide);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnLogout = findViewById(R.id.btnLogout);
        tvWelcome = findViewById(R.id.tvWelcome);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        cardBottom = findViewById(R.id.cardBottom);

        // Searching UI
        cardSearching = findViewById(R.id.cardSearching);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        btnCancelSearch = findViewById(R.id.btnCancelSearch);

        // Driver Offer UI
        cardDriverOffer = findViewById(R.id.cardDriverOffer);
        tvOfferDriverName = findViewById(R.id.tvOfferDriverName);
        tvOfferDriverVehicle = findViewById(R.id.tvOfferDriverVehicle);
        tvOfferDriverRating = findViewById(R.id.tvOfferDriverRating);
        tvOfferPickup = findViewById(R.id.tvOfferPickup);
        tvOfferDestination = findViewById(R.id.tvOfferDestination);
        tvOfferPrice = findViewById(R.id.tvOfferPrice);
        tvOfferTimer = findViewById(R.id.tvOfferTimer);
        btnAcceptOffer = findViewById(R.id.btnAcceptOffer);
        btnRefuseOffer = findViewById(R.id.btnRefuseOffer);

        // Active Ride UI
        cardActiveRide = findViewById(R.id.cardActiveRide);
        tvActiveDriverName = findViewById(R.id.tvActiveDriverName);
        tvActiveDriverVehicle = findViewById(R.id.tvActiveDriverVehicle);
        tvActiveDriverRating = findViewById(R.id.tvActiveDriverRating);
        tvActivePickup = findViewById(R.id.tvActivePickup);
        tvActiveDestination = findViewById(R.id.tvActiveDestination);
        tvActivePrice = findViewById(R.id.tvActivePrice);
        tvRideStatus = findViewById(R.id.tvRideStatus);
        btnCompleteActiveRide = findViewById(R.id.btnCompleteActiveRide);

        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(14.0);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
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
        cardBottom.setVisibility(View.VISIBLE);
        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.GONE);
        mapView.setVisibility(View.VISIBLE);
        btnMyLocation.setVisibility(View.VISIBLE);
    }

    private void showHistoryFragment() {
        cardBottom.setVisibility(View.GONE);
        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        mapView.setVisibility(View.GONE);
        btnMyLocation.setVisibility(View.GONE);

        Fragment fragment = new ClientHistoryFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void showProfileFragment() {
        cardBottom.setVisibility(View.GONE);
        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        mapView.setVisibility(View.GONE);
        btnMyLocation.setVisibility(View.GONE);

        Fragment fragment = new ClientProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void setupListeners() {
        btnRequestRide.setOnClickListener(v -> requestRide());
        btnLogout.setOnClickListener(v -> logout());
        btnMyLocation.setOnClickListener(v -> centerOnMyLocation());
        btnCancelSearch.setOnClickListener(v -> cancelSearch());
        btnAcceptOffer.setOnClickListener(v -> acceptOffer());
        btnRefuseOffer.setOnClickListener(v -> refuseOffer());
        btnCompleteActiveRide.setOnClickListener(v -> completeActiveRide());
    }

    private void checkTokenAndLoadProfile() {
        String token = prefs.getString("token", null);
        String fullName = prefs.getString("full_name", "");
        String email = prefs.getString("email", "");

        if (token == null) { redirectToLogin(); return; }

        ApiClient.setAuthToken(token);

        if (tvWelcome != null) {
            String displayName = (!fullName.isEmpty()) ? fullName.split(" ")[0] : email.split("@")[0];
            tvWelcome.setText("Bonjour, " + displayName + " 👋");
        }
    }

    private void setupMap() {
        if (mapView != null) {
            mapView.getController().setCenter(new GeoPoint(33.5731, -7.5898));
        }
    }

    private void setupAutocomplete() {
        suggestionAdapter = new PlaceSuggestionAdapter(this, new ArrayList<>());
        if (etDestination != null) {
            etDestination.setAdapter(suggestionAdapter);
            etDestination.setThreshold(1);

            etDestination.setOnItemClickListener((parent, view, position, id) -> {
                isSelectingFromList = true;
                PlaceSuggestionAdapter.PlaceItem item = (PlaceSuggestionAdapter.PlaceItem) parent.getItemAtPosition(position);
                if (item != null) {
                    selectedPlaceName = item.name.trim();
                    selectedDestLat = item.latitude;
                    selectedDestLng = item.longitude;

                    etDestination.setText(item.name, false);
                    addDestinationMarker();
                    updateSuggestedPrice();
                    btnRequestRide.setEnabled(true);

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etDestination.getWindowToken(), 0);

                    Toast.makeText(this, "✅ Destination validée", Toast.LENGTH_SHORT).show();
                }
                new Handler(Looper.getMainLooper()).postDelayed(() -> isSelectingFromList = false, 500);
            });

            etDestination.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isSelectingFromList || etDestination.isPerformingCompletion()) return;

                    String query = s.toString().trim();
                    if (!selectedPlaceName.isEmpty() && query.equalsIgnoreCase(selectedPlaceName)) {
                        return;
                    }

                    selectedDestLat = 0;
                    selectedDestLng = 0;
                    selectedPlaceName = "";
                    btnRequestRide.setEnabled(false);

                    if (query.length() >= 2) {
                        fetchSuggestions(query);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void fetchSuggestions(String query) {
        ApiClient.searchPlaces(query, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray suggestions = json.getJSONArray("suggestions");
                        List<PlaceSuggestionAdapter.PlaceItem> items = new ArrayList<>();
                        for (int i = 0; i < suggestions.length(); i++) {
                            JSONObject place = suggestions.getJSONObject(i);
                            items.add(new PlaceSuggestionAdapter.PlaceItem(
                                    place.getString("name"),
                                    place.getString("address"),
                                    place.getDouble("latitude"),
                                    place.getDouble("longitude"),
                                    place.getString("description")));
                        }
                        suggestionAdapter.setData(items);
                        if (!items.isEmpty() && etDestination.hasFocus()) {
                            etDestination.showDropDown();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
            @Override public void onError(String error) {}
        });
    }

    private void addDestinationMarker() {
        if (mapView == null || selectedDestLat == 0) return;
        for (Overlay overlay : new ArrayList<>(mapView.getOverlays())) {
            if (overlay instanceof Marker && "Destination".equals(((Marker) overlay).getTitle())) {
                mapView.getOverlays().remove(overlay);
                break;
            }
        }

        Marker destMarker = new Marker(mapView);
        destMarker.setPosition(new GeoPoint(selectedDestLat, selectedDestLng));
        destMarker.setTitle("Destination");
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(destMarker);
        mapView.invalidate();
        mapView.getController().animateTo(destMarker.getPosition());
    }

    private void updateSuggestedPrice() {
        if (selectedDestLat != 0 && selectedDestLng != 0 && currentLocation != null) {
            double distance = calculateDistance(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    selectedDestLat, selectedDestLng
            );
            int suggestedPrice = (int) (distance * 6) + 10;
            tvPriceHint.setText("💰 Prix suggéré: ~" + suggestedPrice + " MAD");
            etPrice.setHint(String.valueOf(suggestedPrice));
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void centerOnMyLocation() {
        if (currentLocation != null) {
            mapView.getController().animateTo(currentLocation);
            mapView.getController().setZoom(16.0);
            Toast.makeText(this, "Centrage sur votre position", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Position non disponible", Toast.LENGTH_LONG).show();
            getCurrentLocation();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void setupLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                addCurrentLocationMarker();
                if (mapView != null && mapView.getZoomLevel() < 15) {
                    mapView.getController().animateTo(currentLocation);
                }
                findNearbyDrivers();
                updateSuggestedPrice();
            }
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            if (currentLocation == null) {
                currentLocation = new GeoPoint(33.5731, -7.5898);
                addCurrentLocationMarker();
            }
            return;
        }

        Location lastKnownLocation = null;
        if (networkEnabled) lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (gpsEnabled && (lastKnownLocation == null)) lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {
            currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            addCurrentLocationMarker();
            findNearbyDrivers();
        }

        if (locationListener != null) {
            try {
                if (networkEnabled) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
                if (gpsEnabled) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            } catch (SecurityException e) { e.printStackTrace(); }
        }
    }

    private void addCurrentLocationMarker() {
        if (mapView == null || currentLocation == null) return;
        if (currentLocationMarker == null) {
            currentLocationMarker = new Marker(mapView);
            currentLocationMarker.setTitle("Vous êtes ici");
            currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(currentLocationMarker);
        }
        currentLocationMarker.setPosition(currentLocation);
        mapView.invalidate();
    }

    private void findNearbyDrivers() {
        if (currentLocation == null) return;
        ApiClient.getNearbyDrivers(currentLocation.getLatitude(), currentLocation.getLongitude(), "voiture", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        for (Marker m : driverMarkers) mapView.getOverlays().remove(m);
                        driverMarkers.clear();
                        JSONObject json = new JSONObject(response);
                        JSONArray drivers = json.getJSONArray("drivers");
                        for (int i = 0; i < drivers.length(); i++) {
                            JSONObject d = drivers.getJSONObject(i);
                            Marker m = new Marker(mapView);
                            m.setPosition(new GeoPoint(d.getDouble("latitude"), d.getDouble("longitude")));
                            m.setTitle(d.optString("full_name", "Chauffeur"));
                            mapView.getOverlays().add(m);
                            driverMarkers.add(m);
                        }
                        mapView.invalidate();
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
            @Override public void onError(String error) {}
        });
    }

    private void requestRide() {
        if (currentLocation == null) {
            Toast.makeText(this, "🔄 Récupération de votre position...", Toast.LENGTH_LONG).show();
            getCurrentLocation();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (currentLocation != null) requestRide();
                else Toast.makeText(this, "⚠️ Position non trouvée", Toast.LENGTH_LONG).show();
            }, 2000);
            return;
        }

        if (selectedDestLat == 0 || selectedDestLng == 0) {
            Toast.makeText(this, "❌ Veuillez sélectionner une destination", Toast.LENGTH_SHORT).show();
            etDestination.requestFocus();
            return;
        }

        String priceStr = etPrice.getText().toString().trim();
        int offer = priceStr.isEmpty() ? Integer.parseInt(etPrice.getHint().toString()) : Integer.parseInt(priceStr);

        if (offer < 10) {
            Toast.makeText(this, "Le prix minimum est de 10 MAD", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRequestRide.setEnabled(false);
        btnRequestRide.setText("Recherche...");

        ApiClient.requestRide(currentLocation.getLatitude(), currentLocation.getLongitude(), "Ma position",
                selectedPlaceName, selectedDestLat, selectedDestLng, selectedVehicleType, offer,
                new ApiClient.ApiCallback() {
                    @Override public void onSuccess(String r) {
                        runOnUiThread(() -> {
                            try {
                                JSONObject json = new JSONObject(r);
                                currentSearchRideId = json.getString("ride_id");
                                startSearching();
                                startSearchPolling();
                                btnRequestRide.setEnabled(true);
                                btnRequestRide.setText("DEMANDER UN TRAJET");
                                Toast.makeText(ClientHomeActivity.this, "🔍 Recherche de chauffeur en cours...", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            btnRequestRide.setEnabled(true);
                            btnRequestRide.setText("DEMANDER UN TRAJET");
                            Toast.makeText(ClientHomeActivity.this, "❌ Erreur: " + e, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void startSearching() {
        cardBottom.setVisibility(View.GONE);
        cardSearching.setVisibility(View.VISIBLE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.GONE);
        tvSearchStatus.setText("Recherche d'un chauffeur...");
    }

    private void startSearchPolling() {
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

        searchRunnable = () -> {
            if (currentSearchRideId != null) {
                ApiClient.getRideStatus(currentSearchRideId, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            JSONObject ride = json.getJSONObject("ride");
                            String status = ride.getString("status");

                            if (status.equals("matching")) {
                                // Un chauffeur a fait une offre
                                stopSearchPolling();
                                showDriverOffer(ride);
                            } else if (status.equals("acceptée")) {
                                stopSearchPolling();
                                showActiveRide(ride, "en_route");
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    @Override public void onError(String error) {}
                });
                searchHandler.postDelayed(searchRunnable, SEARCH_POLLING_INTERVAL);
            }
        };
        searchHandler.post(searchRunnable);
    }

    private void stopSearchPolling() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        currentSearchRideId = null;
    }

    private void showDriverOffer(JSONObject ride) throws Exception {
        String driverName = ride.getJSONObject("driver").optString("full_name", "Chauffeur");
        String driverVehicle = ride.getJSONObject("driver").optString("vehicle_type", "voiture");
        double driverRating = ride.getJSONObject("driver").optDouble("rating", 4.5);
        String destination = ride.getString("destination");
        String pickup = ride.optString("origin_address", "Position actuelle");
        int offer = ride.optInt("offer", 0);
        currentOfferRideId = ride.getString("id");

        tvOfferDriverName.setText(driverName);
        tvOfferDriverVehicle.setText(getVehicleIcon(driverVehicle) + " " + driverVehicle);
        tvOfferDriverRating.setText(String.format("★ %.1f", driverRating));
        tvOfferPickup.setText(pickup);
        tvOfferDestination.setText(destination);
        tvOfferPrice.setText(offer + " MAD");

        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.VISIBLE);

        startOfferTimer();
    }

    private void startOfferTimer() {
        timeLeft = 30;
        tvOfferTimer.setText(timeLeft + "s");

        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeLeft--;
                if (timeLeft <= 0) {
                    timerHandler.removeCallbacks(this);
                    refuseOffer();
                } else {
                    tvOfferTimer.setText(timeLeft + "s");
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void acceptOffer() {
        if (currentOfferRideId == null) return;

        // Afficher un indicateur de chargement
        btnAcceptOffer.setEnabled(false);
        btnAcceptOffer.setText("Acceptation...");

        ApiClient.respondToMatching(currentOfferRideId, true, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String status = json.optString("status", "acceptée");

                        Toast.makeText(ClientHomeActivity.this, "✅ Course acceptée !", Toast.LENGTH_LONG).show();

                        // Arrêter le timer
                        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

                        // Récupérer l'ID de la course
                        currentRideId = currentOfferRideId;
                        currentOfferRideId = null;

                        // Cacher l'offre et forcer le rechargement
                        cardDriverOffer.setVisibility(View.GONE);

                        // Attendre un peu que le backend mette à jour
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Recharger la course en cours
                            checkCurrentRide();
                            // Démarrer le polling des statuts
                            startRideStatusPolling();
                        }, 1000);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ClientHomeActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnAcceptOffer.setEnabled(true);
                        btnAcceptOffer.setText("ACCEPTER");
                    }
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ClientHomeActivity.this, "❌ Erreur: " + error, Toast.LENGTH_SHORT).show();
                    btnAcceptOffer.setEnabled(true);
                    btnAcceptOffer.setText("ACCEPTER");
                });
            }
        });
    }

    private void refuseOffer() {
        if (currentOfferRideId == null) return;

        ApiClient.respondToMatching(currentOfferRideId, false, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(ClientHomeActivity.this, "Offre refusée", Toast.LENGTH_SHORT).show();
                    if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
                    cardDriverOffer.setVisibility(View.GONE);
                    currentOfferRideId = null;
                });
            }
            @Override public void onError(String error) {}
        });
    }

    private void cancelSearch() {
        if (currentSearchRideId != null) {
            ApiClient.cancelRide(currentSearchRideId, new ApiClient.ApiCallback() {
                @Override public void onSuccess(String r) {
                    runOnUiThread(() -> {
                        stopSearchPolling();
                        cardSearching.setVisibility(View.GONE);
                        cardBottom.setVisibility(View.VISIBLE);
                        Toast.makeText(ClientHomeActivity.this, "Recherche annulée", Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onError(String e) {}
            });
        }
    }

    private void checkCurrentRide() {
        ApiClient.getCurrentRide(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        android.util.Log.d("ClientHome", "checkCurrentRide: " + response);

                        if (json.has("ride") && !json.isNull("ride")) {
                            JSONObject ride = json.getJSONObject("ride");
                            String status = ride.getString("status");
                            currentRideId = ride.getString("id");

                            android.util.Log.d("ClientHome", "Statut détecté: " + status);

                            switch (status) {
                                case "acceptée":
                                case "accepted":
                                    if (cardActiveRide.getVisibility() != View.VISIBLE) {
                                        showActiveRide(ride, "en_route");
                                        Toast.makeText(ClientHomeActivity.this,
                                                "✅ Un chauffeur a accepté votre course !",
                                                Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                case "picked_up":
                                    if (cardActiveRide.getVisibility() != View.VISIBLE) {
                                        showActiveRide(ride, "client_pris");
                                    } else {
                                        // Mettre à jour l'affichage si déjà visible
                                        updateActiveRideStatus("client_pris");
                                    }
                                    break;
                                case "en cours":
                                    updateActiveRideStatus("en_cours");
                                    break;
                                case "terminée":
                                    showRideCompleted();
                                    break;
                                default:
                                    // Aucune course active, montrer le formulaire
                                    if (cardActiveRide.getVisibility() == View.VISIBLE) {
                                        hideAllCards();
                                    }
                                    break;
                            }
                        } else {
                            // Aucune course, afficher le formulaire
                            if (cardActiveRide.getVisibility() == View.VISIBLE) {
                                hideAllCards();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ClientHome", "Erreur: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
            @Override
            public void onError(String error) {
                android.util.Log.e("ClientHome", "API Error: " + error);
            }
        });
    }
    private void showActiveRide(JSONObject ride, String phase) throws Exception {
        JSONObject driver = ride.optJSONObject("driver");
        String driverName = "Chauffeur";
        String driverVehicle = "voiture";
        double driverRating = 4.5;

        if (driver != null) {
            driverName = driver.optString("full_name", "Chauffeur");
            driverVehicle = driver.optString("vehicle_type", "voiture");
            driverRating = driver.optDouble("rating", 4.5);
        }

        String destination = ride.getString("destination");
        String pickup = ride.optString("origin_address", "Position actuelle");
        int offer = ride.optInt("offer", 0);
        long createdAt = ride.optLong("created_at", System.currentTimeMillis());

        tvActiveDriverName.setText(driverName);
        tvActiveDriverVehicle.setText(getVehicleIcon(driverVehicle) + " " + driverVehicle);
        tvActiveDriverRating.setText(String.format("★ %.1f", driverRating));
        tvActivePickup.setText(pickup);
        tvActiveDestination.setText(destination);
        tvActivePrice.setText(offer + " MAD");

        // Mettre à jour le statut affiché
        String statusText;
        switch (phase) {
            case "en_route":
                statusText = "🚗 Chauffeur en route";
                break;
            case "client_pris":
                statusText = "👤 Client pris en charge";
                break;
            case "en_cours":
                statusText = "🚗 Course en cours";
                break;
            default:
                statusText = "✅ Course acceptée";
        }
        tvRideStatus.setText(statusText);

        // Afficher la bonne interface
        cardBottom.setVisibility(View.GONE);
        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.VISIBLE);

        // Le bouton terminer est toujours visible
        btnCompleteActiveRide.setVisibility(View.VISIBLE);
    }

    private void hideAllCards() {
        cardBottom.setVisibility(View.VISIBLE);
        cardSearching.setVisibility(View.GONE);
        cardDriverOffer.setVisibility(View.GONE);
        cardActiveRide.setVisibility(View.GONE);
    }

    private void startRideStatusPolling() {
        if (rideStatusRunnable != null) rideStatusHandler.removeCallbacks(rideStatusRunnable);

        rideStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentRideId != null) {
                    android.util.Log.d("ClientHome", "Polling: Vérification statut course " + currentRideId);
                    ApiClient.getRideStatus(currentRideId, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                JSONObject json = new JSONObject(response);
                                JSONObject ride = json.getJSONObject("ride");
                                String newStatus = ride.getString("status");

                                android.util.Log.d("ClientHome", "Nouveau statut reçu: " + newStatus);

                                runOnUiThread(() -> {
                                    try {
                                        switch (newStatus) {
                                            case "acceptée":
                                            case "accepted":
                                                showActiveRide(ride, "en_route");
                                                break;
                                            case "picked_up":
                                                showActiveRide(ride, "client_pris");
                                                break;
                                            case "en cours":
                                                showActiveRide(ride, "en_cours");
                                                break;
                                            case "terminée":
                                                showRideCompleted();
                                                break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(String error) {
                            android.util.Log.e("ClientHome", "Erreur polling: " + error);
                        }
                    });
                    rideStatusHandler.postDelayed(this, 3000); // Vérifier toutes les 3 secondes
                }
            }
        };
        rideStatusHandler.post(rideStatusRunnable);
    }

    private void updateActiveRideStatus(String phase) {
        runOnUiThread(() -> {
            String statusText;
            switch (phase) {
                case "client_pris":
                    statusText = "👤 Client pris en charge";
                    break;
                case "en_cours":
                    statusText = "🚗 Course en cours";
                    break;
                default:
                    statusText = "🚗 En route";
            }
            tvRideStatus.setText(statusText);
        });
    }



    private void completeActiveRide() {
        if (currentRideId == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Terminer la course")
                .setMessage("Confirmez-vous la fin de cette course ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // ✅ Désactiver le bouton pendant l'appel
                    btnCompleteActiveRide.setEnabled(false);
                    btnCompleteActiveRide.setText("Finalisation...");

                    ApiClient.completeRide(currentRideId, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                Toast.makeText(ClientHomeActivity.this, "🏁 Course terminée !", Toast.LENGTH_LONG).show();
                                btnCompleteActiveRide.setEnabled(true);
                                btnCompleteActiveRide.setText("🏁 TERMINER LA COURSE");
                                currentRideId = null;

                                // Arrêter le polling
                                if (rideStatusRunnable != null) rideStatusHandler.removeCallbacks(rideStatusRunnable);

                                // Retourner à l'accueil
                                hideAllCards();

                                // Rafraîchir la position
                                getCurrentLocation();
                            });
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(ClientHomeActivity.this, "❌ Erreur: " + error, Toast.LENGTH_SHORT).show();
                                btnCompleteActiveRide.setEnabled(true);
                                btnCompleteActiveRide.setText("🏁 TERMINER LA COURSE");
                            });
                        }
                    });
                })
                .setNegativeButton("Non", null)
                .show();
    }

    // Ajoute cette variable
    private boolean isWatchingForRide = false;
    private Handler watchHandler = new Handler(Looper.getMainLooper());
    private Runnable watchRunnable;

    private void startWatchingForRideStatus() {
        if (isWatchingForRide) return;
        isWatchingForRide = true;

        watchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isWatchingForRide) return;

                ApiClient.getCurrentRide(new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.has("ride") && !json.isNull("ride")) {
                                JSONObject ride = json.getJSONObject("ride");
                                String status = ride.getString("status");
                                String rideId = ride.getString("id");

                                android.util.Log.d("ClientHome", "WatchRide - Statut: " + status);

                                runOnUiThread(() -> {
                                    try {
                                        switch (status) {
                                            case "acceptée":
                                            case "accepted":
                                                if (cardActiveRide.getVisibility() != View.VISIBLE) {
                                                    currentRideId = rideId;
                                                    showActiveRide(ride, "en_route");
                                                    Toast.makeText(ClientHomeActivity.this,
                                                            "✅ Course acceptée par le chauffeur !",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                                break;
                                            case "picked_up":
                                                if (cardActiveRide.getVisibility() == View.VISIBLE) {
                                                    updateActiveRideStatus("client_pris");
                                                } else {
                                                    currentRideId = rideId;
                                                    showActiveRide(ride, "client_pris");
                                                }
                                                break;
                                            case "en cours":
                                                if (cardActiveRide.getVisibility() == View.VISIBLE) {
                                                    updateActiveRideStatus("en_cours");
                                                } else {
                                                    currentRideId = rideId;
                                                    showActiveRide(ride, "en_cours");
                                                }
                                                break;
                                            case "terminée":
                                                showRideCompleted();
                                                break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (isWatchingForRide) {
                            watchHandler.postDelayed(watchRunnable, 3000);
                        }
                    }
                    @Override
                    public void onError(String error) {
                        if (isWatchingForRide) {
                            watchHandler.postDelayed(watchRunnable, 3000);
                        }
                    }
                });
            }
        };

        watchHandler.post(watchRunnable);
    }

    private void stopWatchingForRideStatus() {
        isWatchingForRide = false;
        if (watchRunnable != null) {
            watchHandler.removeCallbacks(watchRunnable);
        }
    }

    private String getVehicleIcon(String vehicleType) {
        switch (vehicleType) {
            case "taxi": return "🚖";
            case "moto": return "🏍️";
            default: return "🚗";
        }
    }

    private void logout() {
        ApiClient.logout(new ApiClient.ApiCallback() {
            @Override public void onSuccess(String r) { runOnUiThread(() -> clearAndRedirect()); }
            @Override public void onError(String e) { runOnUiThread(() -> clearAndRedirect()); }
        });
    }

    private void clearAndRedirect() {
        prefs.edit().clear().apply();
        ApiClient.clearAuthToken();
        redirectToLogin();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        startLocationUpdates();

        // ✅ Re-vérifier la course quand l'app revient au premier plan
        if (currentRideId == null) {
            checkCurrentRide();
        }
    }
    private void checkNotifications() {
        ApiClient.getNotifications(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray notifications = json.getJSONArray("notifications");

                    for (int i = 0; i < notifications.length(); i++) {
                        JSONObject notif = notifications.getJSONObject(i);
                        String type = notif.getString("type");
                        String rideId = notif.getString("ride_id");
                        boolean isRead = notif.optBoolean("is_read", false);

                        if (type.equals("ride_accepted") && !isRead) {
                            // Marquer comme lue
                            String notifId = notif.getString("id");
                            ApiClient.markNotificationRead(notifId, new ApiClient.ApiCallback() {
                                @Override public void onSuccess(String r) {}
                                @Override public void onError(String e) {}
                            });

                            // Afficher la notification
                            runOnUiThread(() -> {
                                Toast.makeText(ClientHomeActivity.this,
                                        notif.optString("message", "Course acceptée !"),
                                        Toast.LENGTH_LONG).show();
                                checkCurrentRide();
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

    private void startLocationUpdates() {
        locationRunnable = () -> { getCurrentLocation(); locationHandler.postDelayed(locationRunnable, LOCATION_UPDATE_INTERVAL_MS); };
        locationHandler.post(locationRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_LONG).show();
                currentLocation = new GeoPoint(33.5731, -7.5898);
                addCurrentLocationMarker();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWatchingForRideStatus();
        if (rideStatusRunnable != null) rideStatusHandler.removeCallbacks(rideStatusRunnable);
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private void showRideCompleted() {
        android.util.Log.d("ClientHome", "Course terminée !");
        Toast.makeText(this, "🏁 Course terminée ! Merci !", Toast.LENGTH_LONG).show();

        // Réinitialiser les variables
        currentRideId = null;
        currentOfferRideId = null;
        currentSearchRideId = null;

        // Arrêter le polling
        if (rideStatusRunnable != null) rideStatusHandler.removeCallbacks(rideStatusRunnable);
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        // Afficher l'écran d'accueil
        hideAllCards();

        // Réinitialiser le formulaire
        etDestination.setText("");
        etPrice.setText("");
        selectedDestLat = 0;
        selectedDestLng = 0;
        selectedPlaceName = "";
        btnRequestRide.setEnabled(false);
        btnRequestRide.setText("DEMANDER UN TRAJET");

        // Rafraîchir la position
        getCurrentLocation();
    }
}
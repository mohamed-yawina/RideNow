package com.example.ridenow.network;

import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;

public class ApiClient {
    public static final String BASE_URL = "http://127.0.0.1:8000";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static String authToken = null;

    public static void setAuthToken(String token) { authToken = token; }
    public static String getAuthToken() { return authToken; }
    public static void clearAuthToken() { authToken = null; }

    private static void addAuthHeader(Request.Builder builder) {
        if (authToken != null) { builder.addHeader("Authorization", "Bearer " + authToken); }
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void register(String email, String password, String role, String fullName, String phone, ApiCallback callback) {
        Map<String, String> jsonParams = new HashMap<>();
        jsonParams.put("email", email);
        jsonParams.put("password", password);
        jsonParams.put("role", role);
        if (fullName != null && !fullName.isEmpty()) jsonParams.put("full_name", fullName);
        if (phone != null && !phone.isEmpty()) jsonParams.put("tel", phone);
        String json = gson.toJson(jsonParams);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(BASE_URL + "/register").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError("Erreur réseau : " + e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseStr = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseStr, JsonObject.class);
                        if (jsonResponse.has("token")) setAuthToken(jsonResponse.get("token").getAsString());
                    } catch (Exception e) {}
                    callback.onSuccess(responseStr);
                } else { callback.onError("Erreur : " + response.code()); }
            }
        });
    }

    public static void login(String email, String password, ApiCallback callback) {
        Map<String, String> jsonParams = new HashMap<>();
        jsonParams.put("email", email);
        jsonParams.put("password", password);
        String json = gson.toJson(jsonParams);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(BASE_URL + "/login").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError("Erreur réseau : " + e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseStr = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseStr, JsonObject.class);
                        if (jsonResponse.has("token")) setAuthToken(jsonResponse.get("token").getAsString());
                    } catch (Exception e) {}
                    callback.onSuccess(responseStr);
                } else { callback.onError("Email ou mot de passe incorrect"); }
            }
        });
    }

    public static void getProfile(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/profile").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError("Erreur réseau : " + e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Session expirée");
            }
        });
    }

    public static void logout(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/logout").post(RequestBody.create(new byte[0], null));
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { clearAuthToken(); callback.onSuccess("{}"); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { clearAuthToken(); callback.onSuccess("{}"); }
        });
    }

    public static void getNearbyDrivers(double lat, double lng, String vehicleType, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/drivers/nearby?lat=" + lat + "&lng=" + lng + "&vehicle_type=" + vehicleType).get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void searchPlaces(String query, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/places/search?q=" + query).get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getNearbyPlaces(double lat, double lng, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/places/nearby?lat=" + lat + "&lng=" + lng).get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void requestRide(double originLat, double originLng, String originAddress, String destination, double destLat, double destLng, String vehicleType, int offer, ApiCallback callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("origin_lat", originLat); params.put("origin_lng", originLng); params.put("origin_address", originAddress);
        params.put("destination", destination); params.put("destination_lat", destLat); params.put("destination_lng", destLng);
        params.put("vehicle_type", vehicleType); params.put("offer", offer);
        String json = gson.toJson(params);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/request").post(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError("Erreur réseau: " + e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur serveur: " + response.code());
            }
        });
    }

    public static void getCurrentRide(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/current").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Aucune course");
            }
        });
    }

    public static void cancelRide(String rideId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/cancel/" + rideId).delete();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getDriverStatus(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/status").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void updateDriverStatus(boolean available, ApiCallback callback) {
        JSONObject jsonParams = new JSONObject();
        try { jsonParams.put("available", available); } catch (Exception e) {}
        RequestBody body = RequestBody.create(jsonParams.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/status").post(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getAvailableRides(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/available-rides").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void acceptRide(String rideId, ApiCallback callback) { matchRide(rideId, callback); }

    public static void updateDriverLocation(double lat, double lng, boolean available, ApiCallback callback) {
        JSONObject jsonParams = new JSONObject();
        try { jsonParams.put("lat", lat); jsonParams.put("lng", lng); jsonParams.put("available", available); } catch (Exception e) {}
        RequestBody body = RequestBody.create(jsonParams.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/location").post(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void sendNegotiation(String rideId, double counterOffer, ApiCallback callback) {
        JSONObject jsonParams = new JSONObject();
        try { jsonParams.put("counter_offer", counterOffer); } catch (Exception e) {}
        RequestBody body = RequestBody.create(jsonParams.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/negotiate/" + rideId).post(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }
    public static void declineRide(String rideId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/decline/" + rideId).post(RequestBody.create(new byte[0], null));
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void matchRide(String rideId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/match/" + rideId).post(RequestBody.create(new byte[0], null));
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getMatchingStatus(String rideId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/matching-status/" + rideId).get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void updateRideStatus(String rideId, String action, ApiCallback callback) {
        String url = BASE_URL + "/ride/" + action + "/" + rideId;
        Request.Builder builder = new Request.Builder().url(url).put(RequestBody.create(new byte[0], null));
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void driverArrived(String rideId, ApiCallback callback) { updateRideStatus(rideId, "pickup", callback); }
    public static void startRide(String rideId, ApiCallback callback) { updateRideStatus(rideId, "start", callback); }
    public static void completeRide(String rideId, ApiCallback callback) { updateRideStatus(rideId, "complete", callback); }

    public static void markNotificationRead(String notificationId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/notifications/read/" + notificationId).put(RequestBody.create(new byte[0], null));
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void updateDriverVehicle(String vehicleType, ApiCallback callback) {
        JSONObject jsonParams = new JSONObject();
        try { jsonParams.put("vehicle_type", vehicleType); } catch (Exception e) {}
        RequestBody body = RequestBody.create(jsonParams.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/update-vehicle").put(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess("{}");
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getDriverStats(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/stats").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getRideHistory(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/rides/history").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getActiveRides(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/active-rides").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getDriverRating(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/driver/status").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void respondToMatching(String rideId, boolean accept, ApiCallback callback) {
        JSONObject jsonParams = new JSONObject();
        try { jsonParams.put("accept", accept); } catch (Exception e) {}
        RequestBody body = RequestBody.create(jsonParams.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/match/response/" + rideId).post(body);
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void clientPickedUp(String rideId, ApiCallback callback) { updateRideStatus(rideId, "pickup", callback); }

    public static void getNotifications(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/notifications").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getClientStats(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/client/stats").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getClientRideHistory(ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/client/ride-history").get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }

    public static void getRideStatus(String rideId, ApiCallback callback) {
        Request.Builder builder = new Request.Builder().url(BASE_URL + "/ride/matching-status/" + rideId).get();
        addAuthHeader(builder);
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e.getMessage()); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body().string());
                else callback.onError("Erreur: " + response.code());
            }
        });
    }
}

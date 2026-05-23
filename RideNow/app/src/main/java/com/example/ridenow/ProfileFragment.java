package com.example.ridenow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ridenow.network.ApiClient;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, tvPhone, tvVehicleType, tvRating, tvTotalTrips;
    private Button btnEditProfile;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialisation des vues
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvVehicleType = view.findViewById(R.id.tvVehicleType);
        tvRating = view.findViewById(R.id.tvRating);
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        if (getActivity() != null) {
            prefs = getActivity().getSharedPreferences("RideNow", Context.MODE_PRIVATE);
        }

        loadProfile();

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadProfile() {
        if (prefs == null) return;

        String fullName = prefs.getString("full_name", "Chauffeur");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "Non renseigné");
        String vehicleType = prefs.getString("vehicle_type", "voiture");
        float rating = prefs.getFloat("driver_rating", 0);

        // Vérifier que les vues ne sont pas null avant de setText
        if (tvFullName != null) tvFullName.setText(fullName);
        if (tvEmail != null) tvEmail.setText(email);
        if (tvPhone != null) tvPhone.setText(phone);

        if (tvVehicleType != null) {
            String vehicleIcon = getVehicleIcon(vehicleType);
            tvVehicleType.setText(vehicleIcon + " " + vehicleType);
        }

        if (tvRating != null) tvRating.setText("★ " + rating);

        // Récupérer les stats du chauffeur
        ApiClient.getDriverStats(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        int totalTrips = json.optInt("total_trips", 0);
                        if (tvTotalTrips != null) tvTotalTrips.setText(String.valueOf(totalTrips));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (tvTotalTrips != null) tvTotalTrips.setText("0");
                    }
                });
            }
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (tvTotalTrips != null) tvTotalTrips.setText("0");
                    });
                }
            }
        });
    }

    private String getVehicleIcon(String vehicleType) {
        if (vehicleType == null) return "🚗";
        switch (vehicleType) {
            case "taxi": return "🚖";
            case "moto": return "🏍️";
            default: return "🚗";
        }
    }
}
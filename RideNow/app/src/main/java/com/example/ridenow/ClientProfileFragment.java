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

public class ClientProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, tvPhone, tvTotalTrips, tvTotalSpent, tvMemberSince;
    private Button btnEditProfile;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client_profile, container, false);

        if (getActivity() != null) {
            prefs = getActivity().getSharedPreferences("RideNow", requireContext().MODE_PRIVATE);
        }

        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips);
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        loadProfile();

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadProfile() {
        if (prefs == null) return;

        String fullName = prefs.getString("full_name", "Client");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "Non renseigné");

        tvFullName.setText(fullName);
        tvEmail.setText(email);
        tvPhone.setText(phone);
        tvMemberSince.setText("Membre depuis 2024");

        ApiClient.getClientStats(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        int totalTrips = json.optInt("total_trips", 0);
                        double totalSpent = json.optDouble("total_spent", 0);
                        tvTotalTrips.setText(String.valueOf(totalTrips));
                        tvTotalSpent.setText((int) totalSpent + " MAD");
                    } catch (Exception e) {
                        e.printStackTrace();
                        tvTotalTrips.setText("0");
                        tvTotalSpent.setText("0 MAD");
                    }
                });
            }
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvTotalTrips.setText("0");
                        tvTotalSpent.setText("0 MAD");
                    });
                }
            }
        });
    }
}
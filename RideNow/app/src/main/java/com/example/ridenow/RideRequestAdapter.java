package com.example.ridenow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {

    private List<RideRequest> requests;
    private OnRideActionListener listener;

    public interface OnRideActionListener {
        void onNegotiate(RideRequest request);
        void onAccept(RideRequest request);
        void onRefuse(RideRequest request);
    }

    public RideRequestAdapter(List<RideRequest> requests, OnRideActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        holder.bind(request, listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateList(List<RideRequest> newList) {
        this.requests = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientInitials, tvClientName, tvClientRating, tvDistance, tvDestination, tvVehicleType, tvOffer;
        Button btnRefuse, btnNegotiate, btnAccept;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientInitials = itemView.findViewById(R.id.tvClientInitials);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientRating = itemView.findViewById(R.id.tvClientRating);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
            tvOffer = itemView.findViewById(R.id.tvOffer);
            btnRefuse = itemView.findViewById(R.id.btnRefuse);
            btnNegotiate = itemView.findViewById(R.id.btnNegotiate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }

        public void bind(RideRequest request, OnRideActionListener listener) {
            tvClientName.setText(request.getClientName());
            tvDestination.setText(request.getDestination());

            String initials = getInitials(request.getClientName());
            tvClientInitials.setText(initials);

            tvClientRating.setText("★ " + request.getClientRating());
            tvDistance.setText(String.format("%.1f km", request.getDistance()));
            tvOffer.setText("Offre: " + (int) request.getOffer() + " MAD");

            String vehicleIcon = getVehicleIcon(request.getVehicleType());
            tvVehicleType.setText(vehicleIcon + " " + request.getVehicleType());

            btnRefuse.setOnClickListener(v -> listener.onRefuse(request));
            btnNegotiate.setOnClickListener(v -> listener.onNegotiate(request));
            btnAccept.setOnClickListener(v -> listener.onAccept(request));
        }

        private String getInitials(String name) {
            if (name == null || name.isEmpty()) return "U";
            String[] parts = name.split(" ");
            if (parts.length >= 2) {
                return parts[0].substring(0, 1) + parts[1].substring(0, 1);
            }
            return name.substring(0, 1).toUpperCase();
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
}
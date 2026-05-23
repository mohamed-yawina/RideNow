package com.example.ridenow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ridenow.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private HistoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(adapter);

        loadHistory();

        return view;
    }

    private void loadHistory() {
        ApiClient.getRideHistory(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray rides = json.getJSONArray("rides");
                        List<HistoryItem> items = new ArrayList<>();

                        for (int i = 0; i < rides.length(); i++) {
                            JSONObject ride = rides.getJSONObject(i);
                            HistoryItem item = new HistoryItem();
                            item.destination = ride.getString("destination");
                            item.offer = ride.optInt("offer", 0);
                            item.status = ride.getString("status");
                            String date = ride.getString("created_at");
                            if (date.length() >= 10) {
                                item.date = date.substring(0, 10);
                            } else {
                                item.date = date;
                            }
                            items.add(item);
                        }

                        adapter.updateList(items);
                        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
                }
            }
        });
    }

    static class HistoryItem {
        String destination, status, date;
        int offer;
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items;

        HistoryAdapter(List<HistoryItem> items) { this.items = items; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            holder.tvDestination.setText(item.destination);
            holder.tvPrice.setText(item.offer + " MAD");
            holder.tvDate.setText(item.date);
            String statusText = item.status.equals("terminée") ? "✅ Terminée" : "❌ " + item.status;
            holder.tvStatus.setText(statusText);
        }

        @Override public int getItemCount() { return items.size(); }

        void updateList(List<HistoryItem> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDestination, tvPrice, tvDate, tvStatus;
            ViewHolder(View itemView) {
                super(itemView);
                tvDestination = itemView.findViewById(R.id.tvDestination);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}
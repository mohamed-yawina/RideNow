package com.example.ridenow.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.ridenow.R;
import java.util.ArrayList;
import java.util.List;

public class PlaceSuggestionAdapter extends ArrayAdapter<PlaceSuggestionAdapter.PlaceItem> {
    
    private List<PlaceItem> mItems;
    private final List<PlaceItem> mOriginalItems;

    public static class PlaceItem {
        public String name;
        public String address;
        public double latitude;
        public double longitude;
        public String description;

        public PlaceItem(String name, String address, double lat, double lng, String desc) {
            this.name = name;
            this.address = address;
            this.latitude = lat;
            this.longitude = lng;
            this.description = desc;
        }

        @NonNull
        @Override
        public String toString() {
            return name != null ? name : "";
        }
    }

    public PlaceSuggestionAdapter(Context context, List<PlaceItem> items) {
        super(context, R.layout.item_place_suggestion, items);
        this.mItems = items;
        this.mOriginalItems = new ArrayList<>(items);
    }

    public void setData(List<PlaceItem> newItems) {
        this.mItems = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public PlaceItem getItem(int position) {
        return mItems.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place_suggestion, parent, false);
        }
        
        TextView tvName = convertView.findViewById(R.id.tvPlaceName);
        TextView tvAddress = convertView.findViewById(R.id.tvPlaceAddress);
        
        PlaceItem item = getItem(position);
        if (item != null) {
            tvName.setText(item.name);
            tvAddress.setText(item.address);
        }
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = mItems;
                results.count = mItems.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}

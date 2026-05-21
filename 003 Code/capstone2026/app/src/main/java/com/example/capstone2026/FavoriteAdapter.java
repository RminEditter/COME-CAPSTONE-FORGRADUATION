package com.example.capstone2026;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private List<FavoriteCafe> favoriteList;

    public FavoriteAdapter(List<FavoriteCafe> favoriteList) {
        this.favoriteList = favoriteList;
    }

    @NonNull
    @Override
    public FavoriteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteAdapter.ViewHolder holder, int position) {
        FavoriteCafe cafe = favoriteList.get(position);

        holder.tvName.setText(cafe.name);
        holder.tvAddress.setText(cafe.address);
        holder.tvTags.setText(cafe.tags);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CafeDetailActivity.class);
            intent.putExtra("cafe_id", cafe.id);
            intent.putExtra("cafe_name", cafe.name);
            intent.putExtra("cafe_address", cafe.address);
            intent.putExtra("cafe_tags", cafe.tags);
            intent.putExtra("cafe_reason", cafe.reason);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvTags;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvFavName);
            tvAddress = v.findViewById(R.id.tvFavAddress);
            tvTags = v.findViewById(R.id.tvFavTags);
        }
    }
}
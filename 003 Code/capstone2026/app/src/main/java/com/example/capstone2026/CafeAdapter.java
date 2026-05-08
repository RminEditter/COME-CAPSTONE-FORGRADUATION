package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    private List<Recommender.Recommendation> resultList;
    private Map<String, CafeRatingStats> ratingStatsMap = new HashMap<>();

    public CafeAdapter(List<Recommender.Recommendation> resultList) {
        this.resultList = resultList;
    }

    public void updateList(List<Recommender.Recommendation> newList) {
        this.resultList = newList;
        notifyDataSetChanged();
    }

    public void setRatingStatsMap(Map<String, CafeRatingStats> ratingStatsMap) {
        this.ratingStatsMap = ratingStatsMap;
        notifyDataSetChanged();
    }

    public void setRatingsStatsMap(Map<String, CafeRatingStats> ratingStatsMap) {
        this.ratingStatsMap = ratingStatsMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CafeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cafe, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CafeAdapter.ViewHolder holder, int position) {
        Recommender.Recommendation result = resultList.get(position);

        holder.tvName.setText(result.cafe.name);
        holder.tvReason.setText(result.reason);
        holder.tvMatch.setText("추천 점수: " + result.score + "점");
        holder.tvDistance.setText("약 500m");

        CafeRatingStats stats = ratingStatsMap.get(result.cafe.name);

        if (stats != null) {
            holder.tvMyRating.setText("내 별점: ★ " + stats.avgRating);
            holder.tvVisitRecord.setText("방문 기록 " + stats.visitCount + "회");
        } else {
            holder.tvMyRating.setText("내 별점 없음");
            holder.tvVisitRecord.setText("방문 기록 없음");
        }

        holder.itemView.setOnClickListener(v -> {
            String cafeName = result.cafe.name;

            String uriString = "nmap://search?query=" + cafeName + "&appname=com.example.capstone2026";
            android.net.Uri uri = android.net.Uri.parse(uriString);
            android.content.Intent intent =
                    new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);

            try {
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                String webUrl = "https://www.google.com/maps/search/" + cafeName;
                android.content.Intent webIntent =
                        new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(webUrl)
                        );
                v.getContext().startActivity(webIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resultList != null ? resultList.size() : 0;
    }

    public List<Recommender.Recommendation> getResultList() {
        return resultList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvReason;
        TextView tvDistance;
        TextView tvMatch;
        TextView tvMyRating;
        TextView tvVisitRecord;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
            tvMyRating = v.findViewById(R.id.tv_my_rating);
            tvVisitRecord = v.findViewById(R.id.tv_visit_record);
        }
    }
}
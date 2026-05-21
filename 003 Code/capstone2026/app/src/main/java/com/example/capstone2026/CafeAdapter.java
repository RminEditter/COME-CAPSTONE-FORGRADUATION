package com.example.capstone2026;

import android.content.Intent;
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

        StringBuilder tagText = new StringBuilder();
        if (result.cafe.tags != null) {
            for (Tag tag : result.cafe.tags) {
                tagText.append("#").append(tag.getKoreanLabel()).append(" ");
            }
        }

        holder.tvTags.setText(tagText.toString());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CafeDetailActivity.class);

            intent.putExtra("cafe_id", result.cafe.id);
            intent.putExtra("cafe_name", result.cafe.name);
            intent.putExtra("cafe_address", result.cafe.address);
            intent.putExtra("cafe_reason", result.reason);
            intent.putExtra("cafe_tags", tagText.toString());

            v.getContext().startActivity(intent);
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
        TextView tvTags;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
            tvMyRating = v.findViewById(R.id.tv_my_rating);
            tvVisitRecord = v.findViewById(R.id.tv_visit_record);
            tvTags = v.findViewById(R.id.tv_tags);
        }
    }
}
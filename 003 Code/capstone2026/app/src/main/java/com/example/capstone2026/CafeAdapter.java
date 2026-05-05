package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    private List<Recommender.Recommendation> resultList;

    // 카페별 평균 별점/방문 횟수 저장용
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

        // 카페별 내 평균 별점 표시
        CafeRatingStats stats = ratingStatsMap.get(result.cafe.name);

        if (stats != null) {
            holder.tvMyRating.setText(
                    "⭐ 내 평균 별점: "
                            + String.format("%.1f", stats.avgRating)
                            + " / 방문 "
                            + stats.visitCount
                            + "회"
            );
        } else {
            holder.tvMyRating.setText("⭐ 내 방문 기록 없음");
        }

        // 카페 카드 클릭 → 방문 기록/별점 화면
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    v.getContext(),
                    VisitRecordActivity.class
            );

            intent.putExtra("cafeName", result.cafe.name);
            v.getContext().startActivity(intent);
        });

        // 지도 버튼 클릭 → 네이버 지도 실행
        holder.btnMap.setOnClickListener(v -> {
            String cafeName = result.cafe.name;

            String uriString = "nmap://search?query="
                    + android.net.Uri.encode(cafeName)
                    + "&appname=com.example.capstone2026";

            android.net.Uri uri = android.net.Uri.parse(uriString);

            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    uri
            );

            try {
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                String webUrl = "https://www.google.com/maps/search/"
                        + android.net.Uri.encode(cafeName);

                android.content.Intent webIntent = new android.content.Intent(
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
        TextView tvName, tvReason, tvDistance, tvMatch, tvMyRating;
        Button btnMap;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
            tvMyRating = v.findViewById(R.id.tv_my_rating);
            btnMap = v.findViewById(R.id.btnMap);
        }
    }
}
package com.example.capstone2026;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        if (result.distanceMeters >= 1000) {
            holder.tvDistance.setText(
                    String.format("약 %.1fkm", result.distanceMeters / 1000.0)
            );
        } else {
            holder.tvDistance.setText("약 " + (int) result.distanceMeters + "m");
        }

        CafeRatingStats stats = ratingStatsMap.get(result.cafe.name);

        if (stats != null) {
            // 💡 [2번 기준 텍스트 매칭] 평균값 기준이므로 '내 평점'으로 유지하되 가시성을 높였습니다.
            holder.tvMyRating.setText("평점: ★ " + stats.avgRating);
            holder.tvVisitRecord.setText("방문 기록 " + stats.visitCount + "회");
        } else {
            holder.tvMyRating.setText("내 평점 없음");
            holder.tvVisitRecord.setText("방문 기록 없음");
        }

        StringBuilder tagText = new StringBuilder();

        if (result.cafe.tags != null) {
            for (Tag tag : result.cafe.tags) {
                tagText.append("#")
                        .append(tag.getKoreanLabel())
                        .append(" ");
            }
        }

        holder.tvTags.setText(tagText.toString());

        // 지도 버튼
        holder.btnMap.setOnClickListener(v -> {

            if (result.cafe == null) {
                Toast.makeText(
                        v.getContext(),
                        "카페 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String query;

            if (result.cafe.address != null &&
                    !result.cafe.address.isEmpty()) {
                query = result.cafe.address;
            } else {
                query = result.cafe.name;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setData(
                    Uri.parse(
                            "https://map.naver.com/v5/search/"
                                    + Uri.encode(query)
                    )
            );

            v.getContext().startActivity(intent);
        });

        // 💡 카드 클릭 시 사용자가 선택한 카페 이름을 '최근 본 카페'로 실시간 영구 저장!
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();

            if (result.cafe != null && result.cafe.name != null) {
                context.getSharedPreferences("CafeFitRecent", Context.MODE_PRIVATE)
                        .edit()
                        .putString("recentCafe", result.cafe.name)
                        .apply();
            }

            Intent intent = new Intent(context, CafeDetailActivity.class);

            intent.putExtra("cafe_id", result.cafe.id);
            intent.putExtra("cafe_name", result.cafe.name);
            intent.putExtra("cafe_address", result.cafe.address);
            intent.putExtra("cafe_reason", result.reason);
            intent.putExtra("cafe_tags", tagText.toString());

            context.startActivity(intent);
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

        Button btnMap;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
            tvMyRating = v.findViewById(R.id.tv_my_rating);
            tvVisitRecord = v.findViewById(R.id.tv_visit_record);
            tvTags = v.findViewById(R.id.tv_tags);

            btnMap = v.findViewById(R.id.btnMap);
        }
    }
}
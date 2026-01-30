package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {
    // 이제 일반 Cafe가 아니라 RecommendResult를 리스트로 받습니다.
    private List<Recommender.RecommendResult> resultList;

    public CafeAdapter(List<Recommender.RecommendResult> resultList) { this.resultList = resultList; }

    public void updateList(List<Recommender.RecommendResult> newList) {
        this.resultList = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cafe, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommender.RecommendResult result = resultList.get(position);

        holder.tvName.setText(result.cafeName); // 카페 이름
        holder.tvReason.setText(result.reason); // "산미 · 가까움 포인트가 있어요"

        // 거리 표시 (m -> km 변환)
        if (result.distanceMeters < 1000) {
            holder.tvDistance.setText((int)result.distanceMeters + "m");
        } else {
            holder.tvDistance.setText(String.format("%.1fkm", result.distanceMeters / 1000.0));
        }

        // 취향 일치도 표시
        holder.tvMatch.setText(result.matchPercent + "% 일치");
    }

    @Override public int getItemCount() { return resultList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvReason, tvDistance, tvMatch;
        public ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason); // XML에 추가 필요
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match); // XML에 추가 필요
        }
    }
}
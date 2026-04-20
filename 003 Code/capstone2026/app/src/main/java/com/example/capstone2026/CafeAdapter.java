package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    // [수정] RecommendResult -> Recommender.Recommendation으로 변경
    private List<Recommender.Recommendation> resultList;

    public CafeAdapter(List<Recommender.Recommendation> resultList) {
        this.resultList = resultList;
    }

    public void updateList(List<Recommender.Recommendation> newList) {
        this.resultList = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // R.layout.item_cafe 파일이 실제 레이아웃 이름과 맞는지 확인하세요!
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cafe, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recommender.Recommendation result = resultList.get(position);

        // [수정] CafeModel 객체 안의 필드에 접근합니다.
        holder.tvName.setText(result.cafe.name);
        holder.tvReason.setText(result.reason); // "산미 · 가까움 포인트가 있어요"

        // 매칭 점수를 %로 표시 (예: 3개 중 2개 일치 시 66%)
        // 현재는 score만 있으므로 일단 score를 기반으로 표시하거나 텍스트를 다듬습니다.
        holder.tvMatch.setText("추천 점수: " + result.score + "점");

        // 거리 표시 (임시 500m 예시 데이터 사용 중이므로)
        holder.tvDistance.setText("약 500m");
    }

    @Override
    public int getItemCount() {
        return resultList != null ? resultList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvReason, tvDistance, tvMatch;
        public ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
        }
    }
    public List<Recommender.Recommendation> getResultList() {
        return resultList;
    }
}
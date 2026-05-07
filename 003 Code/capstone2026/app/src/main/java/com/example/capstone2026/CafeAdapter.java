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

        // 1. 기존 데이터 표시 코드
        holder.tvName.setText(result.cafe.name);
        holder.tvReason.setText(result.reason);
        holder.tvMatch.setText("추천 점수: " + result.score + "점");
        holder.tvDistance.setText("약 500m");

        // 2. [추가] 클릭 시 지도 연결 코드
        holder.itemView.setOnClickListener(v -> {
            String cafeName = result.cafe.name; // 카페 이름

            // 네이버 지도에서 검색어로 바로 연결하는 주소 (URL Scheme)
            // %s 부분에 카페 이름을 넣어서 검색 페이지를 띄웁니다.
            String uriString = "nmap://search?query=" + cafeName + "&appname=com.example.capstone2026";
            android.net.Uri uri = android.net.Uri.parse(uriString);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);

            try {
                // 네이버 지도 앱 실행
                v.getContext().startActivity(intent);
            } catch (Exception e) {
                // 네이버 지도 앱이 없으면 구글 지도를 웹 브라우저로 실행 (백업용)
                String webUrl = "https://www.google.com/maps/search/" + cafeName;
                android.content.Intent webIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl));
                v.getContext().startActivity(webIntent);
            }
        });
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
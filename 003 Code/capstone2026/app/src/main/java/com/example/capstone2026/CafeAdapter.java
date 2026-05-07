package com.example.capstone2026;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    private List<Recommender.Recommendation> resultList;

    public CafeAdapter(List<Recommender.Recommendation> resultList) {
        this.resultList = resultList;
    }

    public void updateList(List<Recommender.Recommendation> newList) {
        this.resultList = newList;
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
        Context context = holder.itemView.getContext();

        String cafeId = result.cafe.id;
        String cafeName = result.cafe.name;

        holder.tvName.setText(cafeName);
        holder.tvAddress.setText(result.cafe.address);
        holder.tvReason.setText(result.reason);
        holder.tvMatch.setText("추천 점수: " + result.score + "점");
        holder.tvDistance.setText("약 500m");

        SharedPreferences prefs = context.getSharedPreferences("CafeFitVisitStats", Context.MODE_PRIVATE);

        float rating = prefs.getFloat("rating_" + cafeId, 0f);
        int visitCount = prefs.getInt("visit_count_" + cafeId, 0);
        String lastMemo = prefs.getString("memo_" + cafeId, "");
        String lastDate = prefs.getString("date_" + cafeId, "");

        if (rating > 0) {
            holder.tvRating.setText("내 별점: ★ " + rating);
        } else {
            holder.tvRating.setText("내 별점: 아직 없음");
        }

        if (visitCount > 0) {
            String recordText = "방문 기록 " + visitCount + "회";
            if (!lastDate.isEmpty()) {
                recordText += " · 최근 방문: " + lastDate;
            }
            if (!lastMemo.isEmpty()) {
                recordText += "\n메모: " + lastMemo;
            }
            holder.tvVisitRecord.setText(recordText);
        } else {
            holder.tvVisitRecord.setText("방문 기록 없음");
        }

        holder.itemView.setOnClickListener(v -> {
            String uriString = "nmap://search?query=" + cafeName + "&appname=com.example.capstone2026";
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                String webUrl = "https://www.google.com/maps/search/" + cafeName;
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                context.startActivity(webIntent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            Toast.makeText(context, "방문 기록 삭제 기능은 백 코드와 연결 필요", Toast.LENGTH_SHORT).show();
            return true;
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
        TextView tvName, tvAddress, tvReason, tvDistance, tvMatch, tvRating, tvVisitRecord;

        public ViewHolder(@NonNull View v) {
            super(v);

            tvName = v.findViewById(R.id.tv_name);
            tvAddress = v.findViewById(R.id.tv_address);
            tvReason = v.findViewById(R.id.tv_reason);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvMatch = v.findViewById(R.id.tv_match);
            tvRating = v.findViewById(R.id.tv_rating);
            tvVisitRecord = v.findViewById(R.id.tv_visit_record);
        }
    }
}
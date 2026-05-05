package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitHistoryAdapter extends RecyclerView.Adapter<VisitHistoryAdapter.ViewHolder> {

    private List<VisitRecord> recordList;

    public VisitHistoryAdapter(List<VisitRecord> recordList) {
        this.recordList = recordList;
    }

    @NonNull
    @Override
    public VisitHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_visit_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VisitHistoryAdapter.ViewHolder holder, int position) {
        VisitRecord record = recordList.get(position);

        holder.tvCafeName.setText(record.getCafeName());
        holder.tvRating.setText("별점: " + record.getRating());
        holder.tvMemo.setText("메모: " + record.getMemo());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
        String date = sdf.format(new Date(record.getVisitedAt()));
        holder.tvDate.setText("방문일: " + date);

        holder.itemView.setOnLongClickListener(v -> {

            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("삭제")
                    .setMessage("이 방문 기록을 삭제할까요?")
                    .setPositiveButton("삭제", (dialog, which) -> {

                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(v.getContext());
                            db.visitRecordDao().delete(record);

                            ((android.app.Activity) v.getContext()).runOnUiThread(() -> {
                                recordList.remove(position);
                                notifyDataSetChanged();
                            });

                        }).start();

                    })
                    .setNegativeButton("취소", null)
                    .show();

            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    v.getContext(),
                    VisitRecordActivity.class
            );

            intent.putExtra("mode", "edit");
            intent.putExtra("id", record.getId());
            intent.putExtra("cafeName", record.getCafeName());
            intent.putExtra("rating", record.getRating());
            intent.putExtra("memo", record.getMemo());
            intent.putExtra("visitedAt", record.getVisitedAt());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recordList != null ? recordList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCafeName, tvRating, tvMemo, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCafeName = itemView.findViewById(R.id.tvCafeName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvMemo = itemView.findViewById(R.id.tvMemo);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
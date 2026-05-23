package com.example.capstone2026;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(record.getVisitedAt())));

        // [보안 핵심] 현재 로그인한 유저의 UID 가져오기
        String currentUid = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 현재 유저가 이 글의 작성자인지 확인하는 보이지 않는 자물쇠
        final boolean isOwner = currentUid.equals(record.getUserUid());

        // 1. 길게 누를 때 (삭제 로직)
        holder.itemView.setOnLongClickListener(v -> {
            // 작성자가 아니라면 삭제 차단!
            if (!isOwner) {
                Toast.makeText(v.getContext(), "본인이 작성한 기록만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return true; // 이벤트 소비
            }

            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("방문 기록 삭제")
                    .setMessage("이 방문 기록을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("visit_records")
                                .document(record.getFbId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(v.getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    recordList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, recordList.size());
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(v.getContext(), "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("취소", null)
                    .show();

            return true;
        });

        // 2. 짧게 누를 때 (수정 화면 이동 로직)
        holder.itemView.setOnClickListener(v -> {
            // 작성자가 아니라면 수정화면 진입 차단!
            if (!isOwner) {
                Toast.makeText(v.getContext(), "본인이 작성한 기록만 수정할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.Intent intent = new android.content.Intent(
                    v.getContext(),
                    VisitRecordActivity.class
            );

            intent.putExtra("mode", "edit");
            intent.putExtra("id", record.getFbId());
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
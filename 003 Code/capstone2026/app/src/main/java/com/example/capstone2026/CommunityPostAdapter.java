package com.example.capstone2026;

import android.content.Intent;
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

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.ViewHolder> {

    private final List<CommunityPost> postList;

    public CommunityPostAdapter(List<CommunityPost> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public CommunityPostAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CommunityPostAdapter.ViewHolder holder,
            int position
    ) {

        CommunityPost post = postList.get(position);

        String nickname = post.getNickname();

        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "사용자";
        }

        holder.tvPostWriter.setText(nickname);
        holder.tvPostContent.setText(post.getContent());

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                );

        holder.tvPostDate.setText(
                sdf.format(
                        new Date(post.getCreatedAt())
                )
        );

        String currentUid = "";

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid =
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();
        }

        final boolean isOwner =
                currentUid.equals(post.getUserUid());

        if (isOwner) {
            holder.tvPostWriter.setText(
                    nickname + " · 나"
            );
        }

        holder.itemView.setOnClickListener(v -> {

            if (post.getId() == null) {
                return;
            }

            Intent intent =
                    new Intent(
                            v.getContext(),
                            CommunityPostDetailActivity.class
                    );

            intent.putExtra(
                    "postId",
                    post.getId()
            );

            intent.putExtra(
                    "postUserUid",
                    post.getUserUid()
            );

            intent.putExtra(
                    "postNickname",
                    post.getNickname()
            );

            intent.putExtra(
                    "postContent",
                    post.getContent()
            );

            intent.putExtra(
                    "postCreatedAt",
                    post.getCreatedAt()
            );

            v.getContext().startActivity(
                    intent
            );
        });

        holder.itemView.setOnLongClickListener(v -> {

            if (!isOwner) {
                Toast.makeText(
                        v.getContext(),
                        "본인이 작성한 글만 삭제할 수 있습니다.",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            new android.app.AlertDialog.Builder(
                    v.getContext()
            )
                    .setTitle("게시글 삭제")
                    .setMessage("이 글을 삭제하시겠습니까?")
                    .setPositiveButton(
                            "삭제",
                            (dialog, which) -> {

                                int currentPosition =
                                        holder.getAdapterPosition();

                                if (currentPosition ==
                                        RecyclerView.NO_POSITION) {
                                    return;
                                }

                                if (post.getId() == null) {
                                    return;
                                }

                                FirebaseFirestore.getInstance()
                                        .collection("community_posts")
                                        .document(post.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {

                                            Toast.makeText(
                                                    v.getContext(),
                                                    "삭제되었습니다.",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            postList.remove(
                                                    currentPosition
                                            );

                                            notifyItemRemoved(
                                                    currentPosition
                                            );

                                            notifyItemRangeChanged(
                                                    currentPosition,
                                                    postList.size()
                                            );
                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    v.getContext(),
                                                    "삭제 실패: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        });
                            }
                    )
                    .setNegativeButton(
                            "취소",
                            null
                    )
                    .show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return postList != null
                ? postList.size()
                : 0;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvPostWriter;
        TextView tvPostContent;
        TextView tvPostDate;

        public ViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            tvPostWriter =
                    itemView.findViewById(
                            R.id.tvPostWriter
                    );

            tvPostContent =
                    itemView.findViewById(
                            R.id.tvPostContent
                    );

            tvPostDate =
                    itemView.findViewById(
                            R.id.tvPostDate
                    );
        }
    }
}
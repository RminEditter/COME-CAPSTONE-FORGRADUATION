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

public class CommunityCommentAdapter extends RecyclerView.Adapter<CommunityCommentAdapter.ViewHolder> {

    private final List<CommunityComment> commentList;
    private final String postId;

    public CommunityCommentAdapter(
            List<CommunityComment> commentList,
            String postId
    ) {
        this.commentList = commentList;
        this.postId = postId;
    }

    @NonNull
    @Override
    public CommunityCommentAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_community_comment,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CommunityCommentAdapter.ViewHolder holder,
            int position
    ) {

        CommunityComment comment =
                commentList.get(position);

        String nickname =
                comment.getNickname();

        if (nickname == null ||
                nickname.trim().isEmpty()) {

            nickname = "사용자";
        }

        String currentUid = "";

        if (FirebaseAuth.getInstance()
                .getCurrentUser() != null) {

            currentUid =
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();
        }

        final boolean isOwner =
                currentUid.equals(
                        comment.getUserUid()
                );

        if (isOwner) {
            holder.tvCommentWriter.setText(
                    nickname + " · 나"
            );
        } else {
            holder.tvCommentWriter.setText(
                    nickname
            );
        }

        holder.tvCommentContent.setText(
                comment.getContent()
        );

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                );

        holder.tvCommentDate.setText(
                sdf.format(
                        new Date(
                                comment.getCreatedAt()
                        )
                )
        );

        holder.itemView.setOnLongClickListener(v -> {

            if (!isOwner) {

                Toast.makeText(
                        v.getContext(),
                        "본인이 작성한 댓글만 삭제할 수 있습니다.",
                        Toast.LENGTH_SHORT
                ).show();

                return true;
            }

            if (comment.getId() == null ||
                    postId == null) {

                return true;
            }

            new android.app.AlertDialog.Builder(
                    v.getContext()
            )
                    .setTitle("댓글 삭제")
                    .setMessage(
                            "이 댓글을 삭제하시겠습니까?"
                    )
                    .setPositiveButton(
                            "삭제",
                            (dialog, which) -> {

                                int currentPosition =
                                        holder.getAdapterPosition();

                                if (currentPosition ==
                                        RecyclerView.NO_POSITION) {

                                    return;
                                }

                                FirebaseFirestore
                                        .getInstance()
                                        .collection(
                                                "community_posts"
                                        )
                                        .document(
                                                postId
                                        )
                                        .collection(
                                                "comments"
                                        )
                                        .document(
                                                comment.getId()
                                        )
                                        .delete()
                                        .addOnSuccessListener(
                                                aVoid -> {

                                                    Toast.makeText(
                                                            v.getContext(),
                                                            "댓글이 삭제되었습니다.",
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    commentList.remove(
                                                            currentPosition
                                                    );

                                                    notifyItemRemoved(
                                                            currentPosition
                                                    );

                                                    notifyItemRangeChanged(
                                                            currentPosition,
                                                            commentList.size()
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    Toast.makeText(
                                                            v.getContext(),
                                                            "댓글 삭제 실패: "
                                                                    + e.getMessage(),
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        );
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

        return commentList != null
                ? commentList.size()
                : 0;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvCommentWriter;
        TextView tvCommentContent;
        TextView tvCommentDate;

        public ViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            tvCommentWriter =
                    itemView.findViewById(
                            R.id.tvCommentWriter
                    );

            tvCommentContent =
                    itemView.findViewById(
                            R.id.tvCommentContent
                    );

            tvCommentDate =
                    itemView.findViewById(
                            R.id.tvCommentDate
                    );
        }
    }
}
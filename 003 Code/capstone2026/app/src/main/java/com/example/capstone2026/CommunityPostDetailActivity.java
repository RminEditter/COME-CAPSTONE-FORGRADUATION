package com.example.capstone2026;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityPostDetailActivity extends AppCompatActivity {

    private TextView tvDetailPostWriter;
    private TextView tvDetailPostContent;
    private TextView tvDetailPostDate;
    private TextView tvCommentCount;

    private EditText editComment;
    private Button btnWriteComment;

    private RecyclerView rvComments;

    private FirebaseFirestore db;

    private String postId;

    private final List<CommunityComment> commentList =
            new ArrayList<>();

    private CommunityCommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(
                R.layout.activity_community_post_detail
        );

        db = FirebaseFirestore.getInstance();

        setupBackButton();

        tvDetailPostWriter =
                findViewById(
                        R.id.tvDetailPostWriter
                );

        tvDetailPostContent =
                findViewById(
                        R.id.tvDetailPostContent
                );

        tvDetailPostDate =
                findViewById(
                        R.id.tvDetailPostDate
                );

        tvCommentCount =
                findViewById(
                        R.id.tvCommentCount
                );

        editComment =
                findViewById(
                        R.id.editComment
                );

        btnWriteComment =
                findViewById(
                        R.id.btnWriteComment
                );

        rvComments =
                findViewById(
                        R.id.rvComments
                );

        postId =
                getIntent().getStringExtra(
                        "postId"
                );

        String postUserUid =
                getIntent().getStringExtra(
                        "postUserUid"
                );

        String postNickname =
                getIntent().getStringExtra(
                        "postNickname"
                );

        String postContent =
                getIntent().getStringExtra(
                        "postContent"
                );

        long postCreatedAt =
                getIntent().getLongExtra(
                        "postCreatedAt",
                        0L
                );

        String currentUid = "";

        if (FirebaseAuth.getInstance()
                .getCurrentUser() != null) {

            currentUid =
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();
        }

        if (postNickname == null ||
                postNickname.trim().isEmpty()) {

            postNickname = "사용자";
        }

        if (currentUid.equals(postUserUid)) {

            tvDetailPostWriter.setText(
                    postNickname + " · 나"
            );

        } else {

            tvDetailPostWriter.setText(
                    postNickname
            );
        }

        tvDetailPostContent.setText(
                postContent
        );

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                );

        tvDetailPostDate.setText(
                sdf.format(
                        new Date(postCreatedAt)
                )
        );

        rvComments.setLayoutManager(
                new LinearLayoutManager(this)
        );

        commentAdapter =
                new CommunityCommentAdapter(
                        commentList,
                        postId
                );

        rvComments.setAdapter(
                commentAdapter
        );

        btnWriteComment.setOnClickListener(
                v -> writeComment()
        );

        loadComments();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadComments();
    }

    private void setupBackButton() {

        AppCompatButton btnBack =
                findViewById(
                        R.id.btnBack
                );

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }
    }

    private void loadComments() {

        if (postId == null ||
                postId.isEmpty()) {

            return;
        }

        db.collection(
                        "community_posts"
                )
                .document(
                        postId
                )
                .collection(
                        "comments"
                )
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        commentList.clear();

                        for (QueryDocumentSnapshot document :
                                task.getResult()) {

                            String userUid =
                                    document.getString(
                                            "userUid"
                                    );

                            String nickname =
                                    document.getString(
                                            "nickname"
                                    );

                            String content =
                                    document.getString(
                                            "content"
                                    );

                            Long createdAtLong =
                                    document.getLong(
                                            "createdAt"
                                    );

                            long createdAt =
                                    createdAtLong != null
                                            ? createdAtLong
                                            : 0L;

                            CommunityComment comment =
                                    new CommunityComment(
                                            userUid,
                                            nickname,
                                            content,
                                            createdAt
                                    );

                            comment.setId(
                                    document.getId()
                            );

                            commentList.add(
                                    comment
                            );
                        }

                        Collections.sort(
                                commentList,
                                (a, b) ->
                                        Long.compare(
                                                a.getCreatedAt(),
                                                b.getCreatedAt()
                                        )
                        );

                        tvCommentCount.setText(
                                "댓글 "
                                        + commentList.size()
                                        + "개"
                        );

                        commentAdapter
                                .notifyDataSetChanged();

                    } else {

                        Toast.makeText(
                                this,
                                "댓글을 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void writeComment() {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "로그인이 필요합니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (postId == null ||
                postId.isEmpty()) {

            Toast.makeText(
                    this,
                    "게시글 정보가 없습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String content =
                editComment
                        .getText()
                        .toString()
                        .trim();

        if (content.isEmpty()) {

            Toast.makeText(
                    this,
                    "댓글 내용을 입력해주세요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid =
                currentUser.getUid();

        btnWriteComment.setEnabled(
                false
        );

        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {

                    String nickname =
                            "사용자";

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        DocumentSnapshot document =
                                task.getResult();

                        String savedNickname =
                                document.getString(
                                        "nickname"
                                );

                        if (savedNickname != null &&
                                !savedNickname
                                        .trim()
                                        .isEmpty()) {

                            nickname =
                                    savedNickname;
                        }
                    }

                    saveComment(
                            uid,
                            nickname,
                            content
                    );
                });
    }

    private void saveComment(
            String uid,
            String nickname,
            String content
    ) {

        Map<String, Object> commentData =
                new HashMap<>();

        commentData.put(
                "userUid",
                uid
        );

        commentData.put(
                "nickname",
                nickname
        );

        commentData.put(
                "content",
                content
        );

        commentData.put(
                "createdAt",
                System.currentTimeMillis()
        );

        db.collection(
                        "community_posts"
                )
                .document(
                        postId
                )
                .collection(
                        "comments"
                )
                .add(
                        commentData
                )
                .addOnSuccessListener(
                        documentReference -> {

                            btnWriteComment.setEnabled(
                                    true
                            );

                            editComment.setText("");

                            Toast.makeText(
                                    this,
                                    "댓글이 등록되었습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadComments();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            btnWriteComment.setEnabled(
                                    true
                            );

                            Toast.makeText(
                                    this,
                                    "댓글 등록 실패: "
                                            + e.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }
}
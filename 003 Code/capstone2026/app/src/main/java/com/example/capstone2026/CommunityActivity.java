package com.example.capstone2026;

import android.os.Bundle;
import android.view.View;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rvCommunity;
    private TextView txtCommunityCount;

    private Button btnReviewTab;
    private Button btnTalkTab;

    private View layoutReviewCommunity;
    private View layoutTalkCommunity;

    private EditText editCommunityPost;
    private Button btnWriteCommunityPost;

    private FirebaseFirestore db;

    private List<VisitRecord> communityRecords =
            new ArrayList<>();

    private VisitHistoryAdapter adapter;

    private List<CommunityPost> communityPosts =
            new ArrayList<>();

    private CommunityPostAdapter postAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        db = FirebaseFirestore.getInstance();

        setupBackButton();

        btnReviewTab =
                findViewById(R.id.btnReviewTab);

        btnTalkTab =
                findViewById(R.id.btnTalkTab);

        layoutReviewCommunity =
                findViewById(
                        R.id.layoutReviewCommunity
                );

        layoutTalkCommunity =
                findViewById(
                        R.id.layoutTalkCommunity
                );

        rvCommunity =
                findViewById(
                        R.id.rvCommunity
                );

        txtCommunityCount =
                findViewById(
                        R.id.txtCommunityCount
                );

        editCommunityPost =
                findViewById(
                        R.id.editCommunityPost
                );

        btnWriteCommunityPost =
                findViewById(
                        R.id.btnWriteCommunityPost
                );

        rvCommunity.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new VisitHistoryAdapter(
                        communityRecords
                );

        postAdapter =
                new CommunityPostAdapter(
                        communityPosts
                );

        rvCommunity.setAdapter(adapter);

        btnReviewTab.setOnClickListener(v ->
                showReviewTab()
        );

        btnTalkTab.setOnClickListener(v ->
                showTalkTab()
        );

        btnWriteCommunityPost.setOnClickListener(v ->
                writeCommunityPost()
        );

        showReviewTab();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadCommunityReviews();
        loadCommunityPosts();
    }

    private void setupBackButton() {
        AppCompatButton btnBack =
                findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(
                    v -> finish()
            );
        }
    }

    private void showReviewTab() {

        layoutReviewCommunity.setVisibility(
                View.VISIBLE
        );

        layoutTalkCommunity.setVisibility(
                View.GONE
        );

        rvCommunity.setAdapter(adapter);

        txtCommunityCount.setVisibility(
                View.VISIBLE
        );

        txtCommunityCount.setText(
                "전체 후기 "
                        + communityRecords.size()
                        + "개"
        );
    }

    private void showTalkTab() {

        layoutReviewCommunity.setVisibility(
                View.GONE
        );

        layoutTalkCommunity.setVisibility(
                View.VISIBLE
        );

        rvCommunity.setAdapter(
                postAdapter
        );

        txtCommunityCount.setVisibility(
                View.VISIBLE
        );

        txtCommunityCount.setText(
                "전체 게시글 "
                        + communityPosts.size()
                        + "개"
        );
    }

    private void loadCommunityReviews() {

        db.collection("visit_records")
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        communityRecords.clear();

                        for (QueryDocumentSnapshot document :
                                task.getResult()) {

                            String cafeName =
                                    document.getString(
                                            "cafeName"
                                    );

                            Double ratingDouble =
                                    document.getDouble(
                                            "rating"
                                    );

                            float rating =
                                    ratingDouble != null
                                            ? ratingDouble.floatValue()
                                            : 0.0f;

                            String memo =
                                    document.getString(
                                            "memo"
                                    );

                            Long visitedAtLong =
                                    document.getLong(
                                            "visitedAt"
                                    );

                            long visitedAt =
                                    visitedAtLong != null
                                            ? visitedAtLong
                                            : 0L;

                            String userUid =
                                    document.getString(
                                            "userUid"
                                    );

                            VisitRecord record =
                                    new VisitRecord(
                                            cafeName,
                                            rating,
                                            memo,
                                            visitedAt
                                    );

                            record.setId(
                                    document.getId()
                            );

                            record.setUserUid(
                                    userUid
                            );

                            communityRecords.add(
                                    record
                            );
                        }

                        // 최신 후기가 위로 오도록 정렬
                        Collections.sort(
                                communityRecords,
                                (a, b) ->
                                        Long.compare(
                                                b.getVisitedAt(),
                                                a.getVisitedAt()
                                        )
                        );

                        adapter.notifyDataSetChanged();

                        if (layoutReviewCommunity.getVisibility()
                                == View.VISIBLE) {

                            txtCommunityCount.setText(
                                    "전체 후기 "
                                            + communityRecords.size()
                                            + "개"
                            );
                        }

                    } else {

                        Toast.makeText(
                                this,
                                "커뮤니티 후기를 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void loadCommunityPosts() {

        db.collection("community_posts")
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() &&
                            task.getResult() != null) {

                        communityPosts.clear();

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

                            CommunityPost post =
                                    new CommunityPost(
                                            userUid,
                                            nickname,
                                            content,
                                            createdAt
                                    );

                            post.setId(
                                    document.getId()
                            );

                            communityPosts.add(
                                    post
                            );
                        }

                        Collections.sort(
                                communityPosts,
                                (a, b) ->
                                        Long.compare(
                                                b.getCreatedAt(),
                                                a.getCreatedAt()
                                        )
                        );

                        postAdapter.notifyDataSetChanged();

                        if (layoutTalkCommunity.getVisibility()
                                == View.VISIBLE) {

                            txtCommunityCount.setText(
                                    "전체 게시글 "
                                            + communityPosts.size()
                                            + "개"
                            );
                        }

                    } else {

                        Toast.makeText(
                                this,
                                "게시글을 불러오지 못했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void writeCommunityPost() {

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

        String content =
                editCommunityPost
                        .getText()
                        .toString()
                        .trim();

        if (content.isEmpty()) {

            Toast.makeText(
                    this,
                    "내용을 입력해주세요.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid =
                currentUser.getUid();

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
                                !savedNickname.trim().isEmpty()) {

                            nickname =
                                    savedNickname;
                        }
                    }

                    saveCommunityPost(
                            uid,
                            nickname,
                            content
                    );
                });
    }

    private void saveCommunityPost(
            String uid,
            String nickname,
            String content
    ) {

        Map<String, Object> post =
                new HashMap<>();

        post.put(
                "userUid",
                uid
        );

        post.put(
                "nickname",
                nickname
        );

        post.put(
                "content",
                content
        );

        post.put(
                "createdAt",
                System.currentTimeMillis()
        );

        btnWriteCommunityPost.setEnabled(
                false
        );

        db.collection("community_posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {

                    btnWriteCommunityPost.setEnabled(
                            true
                    );

                    editCommunityPost.setText("");

                    Toast.makeText(
                            this,
                            "게시글이 등록되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadCommunityPosts();
                })
                .addOnFailureListener(e -> {

                    btnWriteCommunityPost.setEnabled(
                            true
                    );

                    Toast.makeText(
                            this,
                            "게시글 등록 실패: "
                                    + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}
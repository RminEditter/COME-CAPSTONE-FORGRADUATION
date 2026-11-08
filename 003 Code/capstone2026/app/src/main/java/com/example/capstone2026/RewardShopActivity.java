package com.example.capstone2026;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class RewardShopActivity extends AppCompatActivity {

    private final NumberFormat formatter =
            NumberFormat.getNumberInstance(Locale.KOREA);

    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private final FirebaseAuth auth =
            FirebaseAuth.getInstance();

    private final PointManager pointManager =
            new PointManager();

    private TextView balanceView;
    private TextView statusView;
    private LinearLayout requestsContainer;

    private long currentBalance = -1;
    private boolean submitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
        loadRequests();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248, 249, 247));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root);

        root.addView(text(
                "CafeFit 리워드샵",
                24, Color.rgb(29, 45, 36), true
        ));

        addWithTopMargin(root, text(
                "모은 포인트로 기프티콘 교환을 신청하세요.",
                14, Color.DKGRAY, false
        ), 8);

        LinearLayout wallet = new LinearLayout(this);
        wallet.setOrientation(LinearLayout.VERTICAL);
        wallet.setPadding(dp(18), dp(18), dp(18), dp(18));
        wallet.setBackground(rounded(Color.WHITE));
        addWithTopMargin(root, wallet, 20);

        wallet.addView(text(
                "내 보유 포인트",
                14, Color.DKGRAY, false
        ));

        balanceView = text(
                "조회 중...",
                25, Color.rgb(35, 104, 70), true
        );
        addWithTopMargin(wallet, balanceView, 8);

        statusView = text("", 12, Color.DKGRAY, false);
        addWithTopMargin(wallet, statusView, 6);

        addWithTopMargin(root, text(
                "포인트 적립 방법",
                18, Color.rgb(29, 45, 36), true
        ), 20);

        addWithTopMargin(root, text(
                "카페 상세 → 방문 기록 남기기에서 어울리는 특징을 " +
                        "선택하고 저장하세요.\n" +
                        "관리자 확인 후 100P · 계정당 같은 카페 최초 1회\n" +
                        "수정·삭제·재등록으로 중복 적립되지 않습니다.",
                14, Color.DKGRAY, false
        ), 8);

        Button history = new Button(this);
        history.setText("포인트 적립 내역 보기");
        history.setAllCaps(false);
        history.setOnClickListener(v ->
                startActivity(new Intent(
                        this, PointHistoryActivity.class
                ))
        );
        addWithTopMargin(root, history, 10);

        addWithTopMargin(root, text(
                "교환 상품",
                18, Color.rgb(29, 45, 36), true
        ), 26);

        addReward(root, "coffee",
                "커피 교환권", 3000);

        addReward(root, "dessert",
                "디저트 교환권", 5000);

        addReward(root, "cafe",
                "카페 이용권", 10000);

        addWithTopMargin(root, text(
                "나의 교환 신청 내역",
                18, Color.rgb(29, 45, 36), true
        ), 26);

        requestsContainer = new LinearLayout(this);
        requestsContainer.setOrientation(LinearLayout.VERTICAL);
        addWithTopMargin(root, requestsContainer, 10);

        Button refresh = new Button(this);
        refresh.setText("신청 내역 새로고침");
        refresh.setAllCaps(false);
        refresh.setOnClickListener(v -> loadRequests());
        addWithTopMargin(root, refresh, 10);

        addWithTopMargin(root, text(
                "교환 신청 후 관리자가 포인트 잔액을 확인하고 " +
                        "차감합니다. 실제 기프티콘은 관리자 확인 후 " +
                        "별도로 발송됩니다.\n" +
                        "현재 상품은 졸업작품 시연용 예시입니다. " +
                        "실제 지급 상품과 전달 방법은 관리자가 안내합니다.",
                13, Color.DKGRAY, false
        ), 20);

        setContentView(scroll);
    }

    private void addReward(
            LinearLayout root,
            String rewardId,
            String name,
            int cost
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(Color.WHITE));
        addWithTopMargin(root, card, 12);

        card.addView(text(name, 17, Color.BLACK, true));

        addWithTopMargin(card, text(
                formatter.format(cost) + " P",
                16, Color.rgb(35, 104, 70), true
        ), 6);

        addWithTopMargin(card, text(
                "관리자 확인 후 수동 지급",
                12, Color.DKGRAY, false
        ), 6);

        Button button = new Button(this);
        button.setText("교환 신청");
        button.setAllCaps(false);

        button.setOnClickListener(v ->
                confirmExchange(rewardId, name, cost)
        );

        addWithTopMargin(card, button, 10);
    }

    private void confirmExchange(
            String rewardId,
            String name,
            int cost
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            toast("로그인이 필요합니다.");
            return;
        }

        if (submitting) {
            toast("신청을 처리하고 있습니다.");
            return;
        }

        if (currentBalance < 0) {
            toast("포인트 조회 후 다시 시도하세요.");
            return;
        }

        if (currentBalance < cost) {
            toast("보유 포인트가 부족합니다.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("교환 신청")
                .setMessage(
                        name + "\n" +
                                formatter.format(cost) + " P\n\n" +
                                "관리자가 잔액을 확인한 후 " +
                                "포인트를 차감하고 지급합니다.\n" +
                                "교환을 신청하시겠습니까?"
                )
                .setNegativeButton("취소", null)
                .setPositiveButton("신청", (dialog, which) ->
                        submitRequest(rewardId, cost)
                )
                .show();
    }

    private void submitRequest(
            String rewardId,
            int cost
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            toast("로그인이 필요합니다.");
            return;
        }

        if (submitting) return;
        submitting = true;

        String uid = user.getUid();
        String requestId = uid + "_" + rewardId + "_"
                + UUID.randomUUID().toString().replace("-", "");

        java.util.Map<String, Object> data =
                new java.util.HashMap<>();

        data.put("uid", uid);
        data.put("rewardId", rewardId);
        data.put("cost", cost);
        data.put("status", "PENDING");
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("reward_requests")
                .document(requestId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    submitting = false;
                    toast("교환 신청이 접수되었습니다.");
                    loadRequests();
                    refreshBalance();
                })
                .addOnFailureListener(e -> {
                    submitting = false;
                    toast("신청 실패: " + e.getMessage());
                    loadRequests();
                });
    }

    private void loadRequests() {
        FirebaseUser user = auth.getCurrentUser();

        if (requestsContainer == null) return;

        requestsContainer.removeAllViews();

        if (user == null) {
            requestsContainer.addView(text(
                    "로그인이 필요합니다.",
                    14, Color.DKGRAY, false
            ));
            return;
        }

        String uid = user.getUid();

        requestsContainer.addView(text(
                "조회 중...",
                14, Color.DKGRAY, false
        ));

        db.collection("reward_requests")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (auth.getCurrentUser() == null ||
                            !uid.equals(auth.getCurrentUser().getUid())) {
                        return;
                    }

                    requestsContainer.removeAllViews();

                    if (snapshot.isEmpty()) {
                        requestsContainer.addView(text(
                                "아직 교환 신청 내역이 없습니다.",
                                14, Color.DKGRAY, false
                        ));
                        return;
                    }

                    java.util.List<DocumentSnapshot> documents =
                            new java.util.ArrayList<>(
                                    snapshot.getDocuments()
                            );

                    documents.sort((a, b) -> {
                        Date da = a.getDate("createdAt");
                        Date dbDate = b.getDate("createdAt");
                        long ta = da == null ? 0 : da.getTime();
                        long tb = dbDate == null ? 0 : dbDate.getTime();
                        return Long.compare(tb, ta);
                    });

                    for (DocumentSnapshot doc : documents) {
                        addRequestCard(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    requestsContainer.removeAllViews();
                    requestsContainer.addView(text(
                            "신청 내역을 불러오지 못했습니다.",
                            14, Color.RED, false
                    ));
                });
    }

    private void addRequestCard(DocumentSnapshot doc) {
        String rewardId = doc.getString("rewardId");
        String status = doc.getString("status");

        Long costValue = doc.getLong("cost");
        long cost = costValue == null ? 0 : costValue;

        String name = rewardName(rewardId);
        String statusText = statusName(status);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(Color.WHITE));

        addWithTopMargin(requestsContainer, card, 10);

        card.addView(text(
                name, 16, Color.BLACK, true
        ));

        addWithTopMargin(card, text(
                formatter.format(cost) + " P",
                14, Color.rgb(35, 104, 70), true
        ), 5);

        addWithTopMargin(card, text(
                "상태: " + statusText,
                14, Color.DKGRAY, false
        ), 5);

        Date createdAt = doc.getDate("createdAt");

        if (createdAt != null) {
            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.KOREA
                    );

            addWithTopMargin(card, text(
                    "신청일: " + format.format(createdAt),
                    12, Color.GRAY, false
            ), 5);
        }
    }

    private String rewardName(String id) {
        if ("coffee".equals(id)) return "커피 교환권";
        if ("dessert".equals(id)) return "디저트 교환권";
        if ("cafe".equals(id)) return "카페 이용권";
        return "기타 교환 상품";
    }

    private String statusName(String status) {
        if ("PENDING".equals(status)) return "관리자 확인 대기";
        if ("APPROVED".equals(status)) return "승인 및 지급 대기";
        if ("COMPLETED".equals(status)) return "지급 완료";
        if ("REJECTED".equals(status)) return "교환 거절";
        return "상태 확인 중";
    }

    private void refreshBalance() {
        if (balanceView == null) return;

        balanceView.setText("조회 중...");
        statusView.setText("");

        pointManager.observeMyPoints(
                this,
                new PointManager.PointCallback() {
                    @Override
                    public void onSuccess(long points) {
                        if (isFinishing() || isDestroyed()) return;

                        currentBalance = points;
                        balanceView.setText(
                                formatter.format(points) + " P"
                        );
                        statusView.setText(
                                "계정에 연결된 잔액 · 처리 시 자동 갱신"
                        );
                    }

                    @Override
                    public void onFailure(String message) {
                        if (isFinishing() || isDestroyed()) return;

                        currentBalance = -1;
                        balanceView.setText("조회 불가");
                        statusView.setText(message);
                    }
                }
        );
    }

    private TextView text(
            String value,
            int sp,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        view.setGravity(Gravity.START);
        return view;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(14));
        return background;
    }

    private void addWithTopMargin(
            LinearLayout parent,
            View child,
            int topDp
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dp(topDp);
        parent.addView(child, params);
    }

    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics().density
        );
    }
}

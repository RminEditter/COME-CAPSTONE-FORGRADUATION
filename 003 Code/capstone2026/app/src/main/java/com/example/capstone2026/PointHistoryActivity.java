package com.example.capstone2026;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PointHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView statusText;
    private TextView balanceText;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        root.setBackgroundColor(Color.WHITE);

        TextView title = makeText("포인트 내역", 24, Color.BLACK);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        balanceText = makeText("보유 포인트 조회 중...", 18, Color.rgb(35, 90, 72));
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(-1, -2);
        balanceParams.topMargin = dp(16);
        root.addView(balanceText, balanceParams);

        statusText = makeText("내역을 불러오는 중입니다.", 14, Color.DKGRAY);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(18);
        root.addView(statusText, statusParams);

        ScrollView scrollView = new ScrollView(this);
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(historyContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(12);
        root.addView(scrollView, scrollParams);
        setContentView(root);
    }

    private void loadHistory() {
        FirebaseUser user = auth.getCurrentUser();
        historyContainer.removeAllViews();
        if (user == null) {
            balanceText.setText("보유 포인트: -");
            statusText.setText("로그인 후 확인할 수 있습니다.");
            return;
        }
        String uid = user.getUid();
        statusText.setText("내역을 불러오는 중입니다.");

        // 서버 함수의 저장 경로: users/{uid}.pointBalance
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Long balance = doc.getLong("pointBalance");
                    balanceText.setText("보유 포인트: " + numberFormat.format(balance == null ? 0 : balance) + " P");
                })
                .addOnFailureListener(e -> balanceText.setText("보유 포인트 조회 실패"));

        // 서버 함수의 적립 원장 경로: users/{uid}/point_history
        db.collection("users").document(uid).collection("point_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    statusText.setText(docs.isEmpty() ? "포인트 내역이 없습니다." : "최근 내역 " + docs.size() + "건");
                    for (DocumentSnapshot doc : docs) addHistoryRow(doc);
                })
                .addOnFailureListener(e -> statusText.setText("포인트 내역을 불러오지 못했습니다: " + e.getMessage()));
    }

    private void addHistoryRow(DocumentSnapshot doc) {
        String type = doc.getString("type");
        String label = "VISIT_TAG_REWARD".equals(type) ? "방문 태그 평가 적립" :
                "GIFT_EXCHANGE".equals(type) ? "기프티콘 교환" : "포인트 변동";
        Long points = doc.getLong("points");
        long amount = points == null ? 0L : points;
        Date created = doc.getDate("createdAt");
        String date = created == null ? "날짜 확인 중" :
                new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(created);
        String cafeId = doc.getString("cafeId");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.addView(makeText(label + "   " + (amount > 0 ? "+" : "") + numberFormat.format(amount) + " P",
                16, amount >= 0 ? Color.rgb(20, 110, 80) : Color.rgb(170, 65, 65)));
        row.addView(makeText(date + (cafeId == null ? "" : " · 카페 ID: " + cafeId), 12, Color.GRAY));
        historyContainer.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(230, 230, 230));
        historyContainer.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private TextView makeText(String value, int sp, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

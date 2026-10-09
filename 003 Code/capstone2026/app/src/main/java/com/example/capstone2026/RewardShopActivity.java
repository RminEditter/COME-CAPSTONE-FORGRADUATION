package com.example.capstone2026;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CafeFit reward-shop preview.
 * The exchange button is deliberately disabled until a trusted server-side
 * redemption endpoint, inventory handling and Firestore security rules exist.
 */
public class RewardShopActivity extends AppCompatActivity {

    private final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
    private TextView balanceView;
    private TextView statusView;
    private final PointManager pointManager = new PointManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248, 249, 247));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root);

        TextView title = text("CafeFit 리워드샵", 24, Color.rgb(29, 45, 36), true);
        root.addView(title);
        TextView description = text("모은 포인트로 리워드를 교환하는 공간입니다.", 14, Color.DKGRAY, false);
        addWithTopMargin(root, description, 8);

        LinearLayout wallet = new LinearLayout(this);
        wallet.setOrientation(LinearLayout.VERTICAL);
        wallet.setPadding(dp(18), dp(18), dp(18), dp(18));
        wallet.setBackground(rounded(Color.WHITE));
        addWithTopMargin(root, wallet, 20);
        wallet.addView(text("내 보유 포인트", 14, Color.DKGRAY, false));
        balanceView = text("조회 중...", 25, Color.rgb(35, 104, 70), true);
        addWithTopMargin(wallet, balanceView, 8);
        statusView = text("", 12, Color.DKGRAY, false);
        addWithTopMargin(wallet, statusView, 6);

        TextView section = text("교환 상품 (화면 예시)", 18, Color.rgb(29, 45, 36), true);
        addWithTopMargin(root, section, 26);
        addReward(root, "커피 교환권", 3000, "예시 상품 · 실제 발급 상품이 아닙니다.");
        addReward(root, "디저트 교환권", 5000, "예시 상품 · 실제 발급 상품이 아닙니다.");
        addReward(root, "카페 이용권", 10000, "예시 상품 · 실제 발급 상품이 아닙니다.");

        TextView notice = text(
                "현재는 리워드샵 화면 미리보기입니다. 실제 교환은 서버의 포인트 차감, " +
                "중복 신청 방지, 재고 관리, 관리자 승인 및 보안 규칙 구현 후 활성화됩니다.",
                13, Color.DKGRAY, false);
        addWithTopMargin(root, notice, 20);
        setContentView(scroll);
    }

    private void addReward(LinearLayout root, String name, int cost, String detail) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(Color.WHITE));
        addWithTopMargin(root, card, 12);
        card.addView(text(name, 17, Color.BLACK, true));
        addWithTopMargin(card, text(formatter.format(cost) + " P", 16,
                Color.rgb(35, 104, 70), true), 6);
        addWithTopMargin(card, text(detail, 12, Color.DKGRAY, false), 6);
        Button button = new Button(this);
        button.setText("교환 준비 중");
        button.setAllCaps(false);
        button.setEnabled(false);
        addWithTopMargin(card, button, 10);
    }

    private void refreshBalance() {
        balanceView.setText("조회 중...");
        statusView.setText("");
        pointManager.getMyPoints(new PointManager.PointCallback() {
            @Override
            public void onSuccess(long points) {
                if (isFinishing() || isDestroyed()) return;
                balanceView.setText(formatter.format(points) + " P");
            }

            @Override
            public void onFailure(String message) {
                if (isFinishing() || isDestroyed()) return;
                balanceView.setText("조회 불가");
                statusView.setText(message);
            }
        });
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.START);
        return view;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(14));
        return background;
    }

    private void addWithTopMargin(LinearLayout parent, android.view.View child, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

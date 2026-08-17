package com.example.capstone2026;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SurveyActivity extends AppCompatActivity {

    private ViewPager2 viewPagerSurvey;
    private TextView txtProgress;
    private ProgressBar progressSurvey;
    private Button btnPrev, btnNext, btnCloseSurvey;

    private ArrayList<Question> questions;
    private ArrayList<String> selectedTags = new ArrayList<>();
    private String priorityTag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        viewPagerSurvey = findViewById(R.id.viewPagerSurvey);
        txtProgress = findViewById(R.id.txtProgress);
        progressSurvey = findViewById(R.id.progressSurvey);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnCloseSurvey = findViewById(R.id.btnCloseSurvey);

        setupQuestions();

        SurveyPagerAdapter adapter = new SurveyPagerAdapter(questions);
        viewPagerSurvey.setAdapter(adapter);
        viewPagerSurvey.setUserInputEnabled(false);

        updateProgress(0);

        btnCloseSurvey.setOnClickListener(v -> {
            Intent intent = new Intent(SurveyActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnPrev.setOnClickListener(v -> {
            int current = viewPagerSurvey.getCurrentItem();
            if (current > 0) {
                viewPagerSurvey.setCurrentItem(current - 1, true);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPagerSurvey.getCurrentItem();

            if (!questions.get(current).isAnswered()) {
                Toast.makeText(this, "항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (current < questions.size() - 1) {
                viewPagerSurvey.setCurrentItem(current + 1, true);
            } else {
                submitSurvey();
            }
        });

        viewPagerSurvey.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateProgress(position);
            }
        });
    }

    private void setupQuestions() {
        questions = new ArrayList<>();

        questions.add(new Question(
                "어떤 분위기의 카페를 찾고 있나요?",
                "최대 2개까지 선택할 수 있어요.",
                true,
                2,
                new Option("조용하고 차분한 분위기", "WORK_FRIENDLY"),
                new Option("카공하기 좋은 곳", "WORK_FRIENDLY"),
                new Option("인테리어가 예쁜 곳", "INTERIOR_PRETTY"),
                new Option("커피가 맛있는 곳", "DRINK_TASTY"),
                new Option("디저트가 맛있는 곳", "DESSERT"),
                new Option("힙한 감성의 카페", "HIP"),
                new Option("대화하기 좋은 곳", "INTERIOR_PRETTY")
        ));

        questions.add(new Question(
                "누구와 방문하시나요?",
                "",
                false,
                new Option("혼자", "SOLO"),
                new Option("친구", "FRIEND"),
                new Option("연인", "COUPLE"),
                new Option("가족", "FAMILY"),
                new Option("직장동료", "COLLEAGUE")
        ));

        questions.add(new Question(
                "어떤 커피를 선호하시나요?",
                "",
                false,
                new Option("고소한 원두", "BEAN_NUTTY"),
                new Option("산미 있는 원두", "BEAN_ACIDIC"),
                new Option("둘 다 괜찮음", "BEAN_NUTTY,BEAN_ACIDIC"),
                new Option("커피는 중요하지 않음", "")
        ));

        questions.add(new Question(
                "음료는 무엇을 더 자주 드시나요?",
                "",
                false,
                new Option("커피", "DRINK_TASTY"),
                new Option("논커피", "DRINK_TASTY"),
                new Option("둘 다", "DRINK_TASTY")
        ));

        questions.add(new Question(
                "디저트를 드실 예정인가요?",
                "",
                false,
                new Option("꼭 먹는다", "DESSERT"),
                new Option("있으면 먹는다", "DESSERT"),
                new Option("관심 없다", "")
        ));

        questions.add(new Question(
                "카페 규모는?",
                "",
                false,
                new Option("아담한 개인카페", "SMALL_CAFE"),
                new Option("적당한 규모", ""),
                new Option("넓은 대형카페", "LARGE_CAFE")
        ));

        // =========================
        // 카공 세부 조건
        // =========================
        questions.add(new Question(
                "카페에서 작업하거나 노트북을 사용하시나요?",
                "원하는 조건을 모두 선택할 수 있어요.",
                true,
                3,
                new Option("콘센트가 많은 곳", "OUTLET_MANY"),
                new Option("와이파이가 빠른 곳", "WIFI_FAST"),
                new Option("노트북 사용이 편한 곳", "LAPTOP_OK")
        ));

        questions.add(new Question(
                "지금 가장 중요한 것은?",
                "추천 점수의 가중치로 사용돼요.",
                false,
                new Option("분위기", "MOOD"),
                new Option("커피 맛", "DRINK_TASTY"),
                new Option("디저트", "DESSERT"),
                new Option("작업하기 편함", "WORK_FRIENDLY"),
                new Option("접근성(거리)", "DISTANCE"),
                new Option("사진 찍기 좋음", "INTERIOR_PRETTY")
        ));

        questions.add(new Question(
                "스페셜티 커피에 관심이 있나요?",
                "",
                false,
                new Option("매우 좋아함", "SPECIALTY_DRIP"),
                new Option("가끔 마심", "SPECIALTY_DRIP"),
                new Option("관심 없음", "")
        ));
    }

    private void updateProgress(int position) {
        txtProgress.setText((position + 1) + " / " + questions.size());
        progressSurvey.setMax(questions.size());
        progressSurvey.setProgress(position + 1);
        btnPrev.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(position == questions.size() - 1 ? "추천 카페 보기" : "다음");
    }

    private void submitSurvey() {
        selectedTags.clear();

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);

            if (i == 7) { // 8번째 질문: 가장 중요한 가중치 태그
                priorityTag = question.getSelectedTag();
            } else {
                selectedTags.addAll(question.getSelectedTags());
            }
        }

        // 💡 1. 로컬 SharedPreferences 저장 (빠른 화면 전환 및 전달용)
        saveSurveyToLocal(selectedTags, priorityTag);

        // 💡 2. 파이어베이스 Firestore DB 원상복구 적재 (계정별 데이터 보존용)
        saveSurveyToFirestore(selectedTags, priorityTag);

        // 💡 3. 추천 액티비티로 이동
        Intent intent = new Intent(SurveyActivity.this, RecommendCafeActivity.class);
        intent.putStringArrayListExtra("user_tags", selectedTags);
        intent.putExtra("priority_tag", priorityTag);
        startActivity(intent);
        finish(); // 설문 완료 후 뒤로가기로 돌아오지 못하게 finish 처리
    }

    private void saveSurvey(ArrayList<String> tags, String priority) {
        Set<String> tagSet = new HashSet<>(tags);

        SharedPreferences prefs = getSharedPreferences("survey", MODE_PRIVATE);
        prefs.edit()
                .putStringSet("user_tags", tagSet)
                .putString("priority_tag", priority)
                .apply();
    }

    static class Option {
        String text;
        String tag;

        Option(String text, String tag) {
            this.text = text;
            this.tag = tag;
        }
    }

    static class Question {
        String title;
        String subtitle;
        boolean multiple;
        int maxSelections;
        ArrayList<Option> options = new ArrayList<>();
        ArrayList<Integer> selectedIndexes = new ArrayList<>();

        Question(String title, String subtitle, boolean multiple, Option... options) {
            this.title = title;
            this.subtitle = subtitle;
            this.multiple = multiple;

            // 기본 다중 선택 개수는 2개
            this.maxSelections = 2;

            for (Option option : options) {
                this.options.add(option);
            }
        }

        Question(String title, String subtitle, boolean multiple, int maxSelections, Option... options) {
            this.title = title;
            this.subtitle = subtitle;
            this.multiple = multiple;

            // 질문별 최대 선택 개수를 지정
            this.maxSelections = maxSelections;

            for (Option option : options) {
                this.options.add(option);
            }
        }

        boolean isAnswered() {
            return !selectedIndexes.isEmpty();
        }

        ArrayList<String> getSelectedTags() {
            ArrayList<String> result = new ArrayList<>();

            for (int index : selectedIndexes) {
                String tag = options.get(index).tag;

                if (tag == null || tag.isEmpty()) {
                    continue;
                }

                String[] splitTags = tag.split(",");
                for (String t : splitTags) {
                    String cleanTag = t.trim();
                    if (!cleanTag.isEmpty() && !result.contains(cleanTag)) {
                        result.add(cleanTag);
                    }
                }
            }

            return result;
        }

        String getSelectedTag() {
            ArrayList<String> tags = getSelectedTags();
            if (tags.isEmpty()) return "";
            return tags.get(0);
        }
    }

    class SurveyPagerAdapter extends RecyclerView.Adapter<SurveyPagerAdapter.SurveyViewHolder> {

        private ArrayList<Question> questionList;

        SurveyPagerAdapter(ArrayList<Question> questionList) {
            this.questionList = questionList;
        }

        @NonNull
        @Override
        public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_survey_page, parent, false);
            return new SurveyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
            Question question = questionList.get(position);

            holder.txtQuestion.setText(question.title);
            holder.txtSubQuestion.setText(question.subtitle);
            holder.txtSubQuestion.setVisibility(question.subtitle.isEmpty() ? View.GONE : View.VISIBLE);

            holder.layoutOptions.removeAllViews();

            if (question.multiple) {
                for (int i = 0; i < question.options.size(); i++) {
                    int index = i;

                    CheckBox checkBox = new CheckBox(SurveyActivity.this);
                    checkBox.setText(question.options.get(i).text);
                    checkBox.setTextSize(18);
                    checkBox.setPadding(12, 12, 12, 12);
                    checkBox.setChecked(question.selectedIndexes.contains(index));

                    checkBox.setOnClickListener(v -> {
                        if (checkBox.isChecked()) {
                            if (question.selectedIndexes.size() >= question.maxSelections) {
                                checkBox.setChecked(false);

                                Toast.makeText(
                                        SurveyActivity.this,
                                        "최대 " + question.maxSelections + "개까지만 선택할 수 있어요.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!question.selectedIndexes.contains(index)) {
                                question.selectedIndexes.add(index);
                            }
                        } else {
                            question.selectedIndexes.remove(Integer.valueOf(index));
                        }
                    });

                    holder.layoutOptions.addView(checkBox);
                }
            } else {
                RadioGroup radioGroup = new RadioGroup(SurveyActivity.this);
                radioGroup.setOrientation(RadioGroup.VERTICAL);

                for (int i = 0; i < question.options.size(); i++) {
                    int index = i;

                    RadioButton radioButton = new RadioButton(SurveyActivity.this);
                    radioButton.setText(question.options.get(i).text);
                    radioButton.setTextSize(18);
                    radioButton.setPadding(12, 12, 12, 12);
                    radioButton.setId(View.generateViewId());

                    if (question.selectedIndexes.contains(index)) {
                        radioButton.setChecked(true);
                    }

                    radioButton.setOnClickListener(v -> {
                        question.selectedIndexes.clear();
                        question.selectedIndexes.add(index);
                    });

                    radioGroup.addView(radioButton);
                }

                holder.layoutOptions.addView(radioGroup);
            }
        }

        @Override
        public int getItemCount() {
            return questionList.size();
        }

        class SurveyViewHolder extends RecyclerView.ViewHolder {
            TextView txtQuestion, txtSubQuestion;
            LinearLayout layoutOptions;

            SurveyViewHolder(@NonNull View itemView) {
                super(itemView);
                txtQuestion = itemView.findViewById(R.id.txtQuestion);
                txtSubQuestion = itemView.findViewById(R.id.txtSubQuestion);
                layoutOptions = itemView.findViewById(R.id.layoutOptions);
            }
        }
    }

    private void saveSurveyToLocal(ArrayList<String> tags, String priority) {
        Set<String> tagSet = new HashSet<>(tags);

        SharedPreferences prefs = getSharedPreferences("survey", MODE_PRIVATE);
        prefs.edit()
                .putStringSet("user_tags", tagSet)
                .putString("priority_tag", priority)
                .apply();
    }

    private void saveSurveyToFirestore(ArrayList<String> tags, String priority) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 로그인된 유저가 존재할 때만 Firestore에 저장
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> surveyData = new HashMap<>();
            surveyData.put("user_tags", tags);
            surveyData.put("priority_tag", priority);

            // users 컬렉션의 내 uid 문서에 태그 업데이트 (SetOptions.merge()로 기존 유저 정보 유지)
            db.collection("users").document(uid)
                    .set(surveyData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("SURVEY_DB", "Firestore 설문 저장 성공"))
                    .addOnFailureListener(e -> Log.e("SURVEY_DB", "Firestore 설문 저장 실패", e));
        } else {
            Log.w("SURVEY_DB", "로그인된 유저가 없어 Firestore에 저장하지 못했습니다.");
        }
    }
}
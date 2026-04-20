package com.example.capstone2026;

public class SurveyActivity extends AppCompatActivity {

    RadioGroup rgBean, rgStyle, rgSize, rgCompanion;
    Switch switchDessert, switchSpecialty;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        rgBean = findViewById(R.id.rgBean);
        rgStyle = findViewById(R.id.rgStyle);
        rgSize = findViewById(R.id.rgSize);
        rgCompanion = findViewById(R.id.rgCompanion);
        switchDessert = findViewById(R.id.switchDessert);
        switchSpecialty = findViewById(R.id.switchSpecialty);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> submitSurvey());
    }

    private void submitSurvey() {

        String bean = getSelectedText(rgBean);
        String style = getSelectedText(rgStyle);
        String size = getSelectedText(rgSize);
        String companion = getSelectedText(rgCompanion);

        boolean dessert = switchDessert.isChecked();
        boolean specialty = switchSpecialty.isChecked();

        List<Tag> userTags = SurveyTagConverter.convertSurveyToTags(
                bean, style, dessert, specialty, size, companion
        );

        Tag[] tagArray = userTags.toArray(new Tag[0]);

        // 여기서 Recommender 호출
    }

    private String getSelectedText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        return rb.getText().toString();
    }
}

public class SurveyActivity extends AppCompatActivity {

    RadioGroup rgBean, rgStyle, rgSize, rgCompanion;
    Switch switchDessert, switchSpecialty;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        rgBean = findViewById(R.id.rgBean);
        rgStyle = findViewById(R.id.rgStyle);
        rgSize = findViewById(R.id.rgSize);
        rgCompanion = findViewById(R.id.rgCompanion);
        switchDessert = findViewById(R.id.switchDessert);
        switchSpecialty = findViewById(R.id.switchSpecialty);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> submitSurvey());
    }

    private void submitSurvey() {

        String bean = getSelectedText(rgBean);
        String style = getSelectedText(rgStyle);
        String size = getSelectedText(rgSize);
        String companion = getSelectedText(rgCompanion);

        boolean dessert = switchDessert.isChecked();
        boolean specialty = switchSpecialty.isChecked();

        List<Tag> userTags = SurveyTagConverter.convertSurveyToTags(
                bean, style, dessert, specialty, size, companion
        );

        Tag[] tagArray = userTags.toArray(new Tag[0]);

        // 여기서 Recommender 호출
    }

    private String getSelectedText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        return rb.getText().toString();
    }
}

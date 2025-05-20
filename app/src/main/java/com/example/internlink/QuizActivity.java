package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private TextView quizTitle, quizInstructions, questionText, timerText;
    private Button nextButton, submitButton;
    private View multipleChoiceLayout, trueFalseLayout, shortAnswerLayout;
    private RadioGroup mcRadioGroup, tfRadioGroup;
    private EditText shortAnswerInput;

    private Quiz quiz;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Get quiz data from intent
        quiz = getIntent().getParcelableExtra("QUIZ_DATA");
        if (quiz == null) {
            finish();
            return;
        }

        questions = quiz.getQuestions();
        if (questions == null || questions.isEmpty()) {
            finish();
            return;
        }

        initViews();
        if (questions.size() == 1) {
            nextButton.setVisibility(View.GONE);
            submitButton.setVisibility(View.VISIBLE);
        }

        setupTimer();
        displayQuestion(currentQuestionIndex);
    }

    private void initViews() {
        quizTitle = findViewById(R.id.quiz_title);
        quizInstructions = findViewById(R.id.quiz_instructions);
        questionText = findViewById(R.id.question_text);
        timerText = findViewById(R.id.timer_text);

        nextButton = findViewById(R.id.next_button);
        submitButton = findViewById(R.id.submit_button);

        multipleChoiceLayout = findViewById(R.id.multiple_choice_layout);
        trueFalseLayout = findViewById(R.id.true_false_layout);
        shortAnswerLayout = findViewById(R.id.short_answer_layout);

        mcRadioGroup = findViewById(R.id.mc_radio_group);
        tfRadioGroup = findViewById(R.id.tf_radio_group);
        shortAnswerInput = findViewById(R.id.short_answer_input);


        quizTitle.setText(quiz.getTitle());
        quizInstructions.setText(quiz.getInstructions());

        nextButton.setOnClickListener(v -> {
            // Validate and score current question
            if (validateAndScoreQuestion(currentQuestionIndex)) {
                currentQuestionIndex++;
                if (currentQuestionIndex < questions.size() - 1) {
                    displayQuestion(currentQuestionIndex);
                } else if (currentQuestionIndex == questions.size() - 1) {
                    displayQuestion(currentQuestionIndex);
                    nextButton.setVisibility(View.GONE);
                    submitButton.setVisibility(View.VISIBLE);
                }
            }
        });

        submitButton.setOnClickListener(v -> {
            // Validate and score last question
            if (validateAndScoreQuestion(currentQuestionIndex)) {
                finishQuiz();
            }
        });
    }

    private void setupTimer() {
        timeLeftInMillis = quiz.getTimeLimit() * 60 * 1000; // convert minutes to milliseconds
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                // Time's up - auto submit
                finishQuiz();
            }
        }.start();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerText.setText(timeLeftFormatted);
    }

    private void displayQuestion(int index) {
        Question question = questions.get(index);
        questionText.setText(question.getText());

        // Hide all question type layouts first
        multipleChoiceLayout.setVisibility(View.GONE);
        trueFalseLayout.setVisibility(View.GONE);
        shortAnswerLayout.setVisibility(View.GONE);

        // Show the appropriate layout based on question type
        switch (question.getType()) {
            case "Multiple Choice":
                setupMultipleChoiceQuestion(question);
                break;
            case "True/False":
                setupTrueFalseQuestion(question);
                break;
            case "Short Answer":
                setupShortAnswerQuestion(question);
                break;
        }
    }

    private void setupMultipleChoiceQuestion(Question question) {
        multipleChoiceLayout.setVisibility(View.VISIBLE);
        mcRadioGroup.removeAllViews(); // clear old options

        List<Option> options = question.getOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(options.get(i).getText());
                rb.setId(i); // ID = index
                mcRadioGroup.addView(rb);
            }
        }
    }


    @SuppressLint("ResourceType")
    private void setupTrueFalseQuestion(Question question) {
        trueFalseLayout.setVisibility(View.VISIBLE);
        tfRadioGroup.removeAllViews();

        RadioButton trueBtn = new RadioButton(this);
        trueBtn.setText("True");
        trueBtn.setId(0);

        RadioButton falseBtn = new RadioButton(this);
        falseBtn.setText("False");
        falseBtn.setId(1);

        tfRadioGroup.addView(trueBtn);
        tfRadioGroup.addView(falseBtn);
    }


    private void setupShortAnswerQuestion(Question question) {
        shortAnswerLayout.setVisibility(View.VISIBLE);
        shortAnswerInput.setText(""); // clear previous input
    }


    private boolean validateAndScoreQuestion(int index) {
        Question question = questions.get(index);
        String type = question.getType();
        List<Option> options = question.getOptions(); // ✅ No conversion needed

        switch (type) {
            case "Multiple Choice":
                int selectedMcId = mcRadioGroup.getCheckedRadioButtonId();
                if (selectedMcId == -1) return false;

                if (options.get(selectedMcId).isCorrect()) score++;
                return true;

            case "True/False":
                int selectedTfId = tfRadioGroup.getCheckedRadioButtonId();
                if (selectedTfId == -1) return false;

                boolean userAnswer = selectedTfId == 0;
                boolean correctAnswer = options.get(0).isCorrect(); // assuming index 0 = "True"
                if (userAnswer == correctAnswer) score++;
                return true;

            case "Short Answer":
                String answer = shortAnswerInput.getText().toString().trim();
                return !answer.isEmpty(); // You can add keyword-matching logic later
        }

        return false;
    }




    private void finishQuiz() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Calculate percentage score
        int percentageScore = (int) (((double) score / questions.size()) * 100);
        boolean passed = percentageScore >= quiz.getPassingScore();

        // Return result to ApplyNowActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("QUIZ_PASSED", passed);
        resultIntent.putExtra("QUIZ_SCORE", percentageScore);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
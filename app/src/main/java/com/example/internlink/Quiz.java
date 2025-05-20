package com.example.internlink;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

public class Quiz implements Parcelable {
    private String title;
    private String instructions;
    private int timeLimit; // in minutes
    private int passingScore; // percentage
    private List<Question> questions;

    // Required empty constructor for Firebase
    public Quiz() {
    }

    // Parameterized constructor for manual creation
    public Quiz(String title, String instructions, int timeLimit, int passingScore, List<Question> questions) {
        this.title = title;
        this.instructions = instructions;
        this.timeLimit = timeLimit;
        this.passingScore = passingScore;
        this.questions = questions;
    }

    protected Quiz(Parcel in) {
        title = in.readString();
        instructions = in.readString();
        timeLimit = in.readInt();
        passingScore = in.readInt();
        questions = in.createTypedArrayList(Question.CREATOR);
    }

    public static final Creator<Quiz> CREATOR = new Creator<Quiz>() {
        @Override
        public Quiz createFromParcel(Parcel in) {
            return new Quiz(in);
        }

        @Override
        public Quiz[] newArray(int size) {
            return new Quiz[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(instructions);
        dest.writeInt(timeLimit);
        dest.writeInt(passingScore);
        dest.writeTypedList(questions);
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getInstructions() {
        return instructions;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    // Setters with validation
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Quiz title cannot be null or empty");
        }
        this.title = title;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setTimeLimit(int timeLimit) {
        if (timeLimit <= 0) {
            throw new IllegalArgumentException("Time limit must be positive");
        }
        this.timeLimit = timeLimit;
    }

    public void setPassingScore(int passingScore) {
        if (passingScore < 0 || passingScore > 100) {
            throw new IllegalArgumentException("Passing score must be between 0 and 100");
        }
        this.passingScore = passingScore;
    }

    public void setQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be null or empty");
        }
        this.questions = questions;
    }

    // Helper methods
    public int getTotalPoints() {
        return questions != null ? questions.size() : 0;
    }

    public boolean isValid() {
        return title != null && !title.isEmpty() &&
                timeLimit > 0 &&
                passingScore >= 0 && passingScore <= 100 &&
                questions != null && !questions.isEmpty();
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "title='" + title + '\'' +
                ", timeLimit=" + timeLimit +
                ", passingScore=" + passingScore +
                ", questionsCount=" + (questions != null ? questions.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quiz quiz = (Quiz) o;
        return timeLimit == quiz.timeLimit &&
                passingScore == quiz.passingScore &&
                Objects.equals(title, quiz.title) &&
                Objects.equals(instructions, quiz.instructions) &&
                Objects.equals(questions, quiz.questions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, instructions, timeLimit, passingScore, questions);
    }
}
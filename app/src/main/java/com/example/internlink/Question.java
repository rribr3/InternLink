package com.example.internlink;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Question implements Parcelable {
    private String text;
    private String type; // "Multiple Choice", "True/False", or "Short Answer"
    private List<Option> options; // Can be null for short answer

    public Question() {
    }

    protected Question(Parcel in) {
        text = in.readString();
        type = in.readString();
        options = in.createTypedArrayList(Option.CREATOR); // handles Option parcelable list
    }

    public static final Creator<Question> CREATOR = new Creator<Question>() {
        @Override
        public Question createFromParcel(Parcel in) {
            return new Question(in);
        }

        @Override
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    // ✅ ADD: Custom method to convert Firebase data
    public static Question fromFirebaseData(Map<String, Object> data) {
        Question question = new Question();

        question.setText((String) data.get("text"));
        question.setType((String) data.get("type"));

        // Convert options
        Object optionsData = data.get("options");
        if (optionsData instanceof List) {
            List<Option> convertedOptions = new ArrayList<>();
            List<?> optionsList = (List<?>) optionsData;

            for (Object optionObj : optionsList) {
                if (optionObj instanceof Map) {
                    Map<String, Object> optionMap = (Map<String, Object>) optionObj;
                    Option option = new Option();

                    option.setText((String) optionMap.get("text"));

                    Object correctObj = optionMap.get("correct");
                    boolean correct = false;
                    if (correctObj instanceof Boolean) {
                        correct = (Boolean) correctObj;
                    } else if (correctObj instanceof String) {
                        correct = Boolean.parseBoolean((String) correctObj);
                    }
                    option.setCorrect(correct);

                    convertedOptions.add(option);
                }
            }

            question.setOptions(convertedOptions);
        }

        return question;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(type);
        dest.writeTypedList(options);
    }
}
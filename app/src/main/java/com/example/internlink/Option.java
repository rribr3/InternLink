package com.example.internlink;

import android.os.Parcel;
import android.os.Parcelable;

public class Option implements Parcelable {
    private String text;
    private boolean correct;

    public Option() {
    }
    protected Option(Parcel in) {
        text = in.readString();
        correct = in.readByte() != 0;
    }
    public static final Creator<Option> CREATOR = new Creator<Option>() {
        @Override
        public Option createFromParcel(Parcel in) {
            return new Option(in);
        }
        @Override
        public Option[] newArray(int size) {
            return new Option[size];
        }
    };

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public int describeContents() { return 0; }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeByte((byte) (correct ? 1 : 0));
    }
}

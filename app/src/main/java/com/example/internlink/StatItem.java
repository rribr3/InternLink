package com.example.internlink;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class StatItem extends LinearLayout {
    private TextView countText;
    private TextView labelText;

    public StatItem(Context context) {
        super(context);
        init(null);
    }

    public StatItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public StatItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        LayoutInflater.from(getContext()).inflate(R.layout.stat_item, this, true);
        setOrientation(VERTICAL);

        countText = findViewById(R.id.count_text);
        labelText = findViewById(R.id.label_text);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StatItem);

            String label = a.getString(R.styleable.StatItem_label);
            int count = a.getInt(R.styleable.StatItem_count, 0);
            int color = a.getColor(R.styleable.StatItem_color, 0);

            setLabel(label);
            setCount(count);
            setColor(color);

            a.recycle();
        }
    }

    public void setLabel(String label) {
        labelText.setText(label);
    }

    public void setCount(int count) {
        countText.setText(String.valueOf(count));
    }

    public void setColor(int color) {
        countText.setTextColor(color);
    }
}
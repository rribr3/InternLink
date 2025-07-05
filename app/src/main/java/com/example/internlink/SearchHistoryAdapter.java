package com.example.internlink;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder> {

    private final List<String> searchHistory;
    private final OnHistoryItemClickListener listener;

    // Make the interface public and static
    public static interface OnHistoryItemClickListener {
        void onHistoryClick(String query);
        void onHistoryDelete(String query);
        void onClearAllHistory();
    }

    public SearchHistoryAdapter(List<String> searchHistory, OnHistoryItemClickListener listener) {
        this.searchHistory = searchHistory;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (position == searchHistory.size()) {
            // Last item is "Clear All History"
            holder.bindClearAllItem();
        } else {
            // Regular history item
            String query = searchHistory.get(position);
            holder.bind(query);
        }
    }

    @Override
    public int getItemCount() {
        return searchHistory.size() + (searchHistory.isEmpty() ? 0 : 1); // +1 for "Clear All" button
    }

    @Override
    public int getItemViewType(int position) {
        return position == searchHistory.size() ? 1 : 0; // 1 for clear all, 0 for regular item
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView queryText;
        private final ImageView historyIcon;
        private final ImageView deleteIcon;

        HistoryViewHolder(View itemView) {
            super(itemView);
            queryText = itemView.findViewById(R.id.tv_search_query);
            historyIcon = itemView.findViewById(R.id.iv_history_icon);
            deleteIcon = itemView.findViewById(R.id.iv_delete_icon);
        }

        void bind(String query) {
            queryText.setText(query);
            queryText.setTextColor(Color.BLACK);
            historyIcon.setImageResource(R.drawable.ic_pending);
            historyIcon.setVisibility(View.VISIBLE);
            deleteIcon.setImageResource(R.drawable.ic_close);
            deleteIcon.setVisibility(View.VISIBLE);

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(query);
                }
            });

            // Set click listener for delete icon
            deleteIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryDelete(query);
                }
            });

            // Add ripple effect
            itemView.setBackgroundResource(R.drawable.rounded_background);
        }

        void bindClearAllItem() {
            queryText.setText("Clear Search History");
            queryText.setTextColor(Color.parseColor("#FF5722")); // Red color
            historyIcon.setImageResource(R.drawable.ic_delete_white);
            historyIcon.setVisibility(View.VISIBLE);
            deleteIcon.setVisibility(View.GONE);

            // Set click listener for clear all
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClearAllHistory();
                }
            });

            // Add ripple effect
            itemView.setBackgroundResource(R.drawable.rounded_background);
        }
    }
}
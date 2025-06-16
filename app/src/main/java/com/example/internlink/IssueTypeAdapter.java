package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class IssueTypeAdapter extends RecyclerView.Adapter<IssueTypeAdapter.ViewHolder> {
    private final List<IssueType> issueTypes;
    private final OnIssueTypeSelectedListener listener;

    public interface OnIssueTypeSelectedListener {
        void onIssueTypeSelected(IssueType issueType);
    }

    public IssueTypeAdapter(OnIssueTypeSelectedListener listener) {
        this.issueTypes = Arrays.asList(IssueType.values());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IssueType issueType = issueTypes.get(position);
        holder.textView.setText(issueType.toString());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIssueTypeSelected(issueType);
            }
        });
    }

    @Override
    public int getItemCount() {
        return issueTypes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}

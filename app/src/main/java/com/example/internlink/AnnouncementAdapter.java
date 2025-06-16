package com.example.internlink;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {
    private List<Announcement> originalList, filteredList;
    private final AnnouncementClickListener listener;

    public AnnouncementAdapter(List<Announcement> list, AnnouncementClickListener listener) {
        this.listener = listener;
        this.originalList = list;
        this.filteredList = new ArrayList<>(list);
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, body, date;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.announcement_title);
            body = view.findViewById(R.id.announcement_body);
            date = view.findViewById(R.id.announcement_date);
        }
    }

    @Override
    public AnnouncementAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.announcement_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(AnnouncementAdapter.ViewHolder holder, int position) {
        Announcement item = filteredList.get(position);
        if ("warning".equals(item.getCategory())) {
            // Set warning-specific styling
            holder.title.setTextColor(Color.RED);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light red background

        } else {
            // Reset to default styling for non-warning announcements
            holder.title.setTextColor(Color.BLACK);
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
        holder.itemView.setAlpha(item.isRead() ? 0.7f : 1.0f);
        holder.title.setText(item.getTitle());
        holder.body.setText(item.getBody());
        holder.date.setText("Posted: " + item.getDate());

        holder.itemView.setBackgroundColor(item.isRead() ? Color.WHITE : Color.parseColor("#E6E6FA"));

        holder.itemView.setOnClickListener(v -> {
            // Pass the announcement ID to the popup method
            listener.showAnnouncementPopup(
                    item.getId(),
                    item.getTitle(),
                    item.getBody(),
                    item.getDate()
            );
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // 🔍 Filter announcements by search query
    public void filterBy(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (Announcement a : originalList) {
                if (a.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        a.getBody().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(a);
                }
            }
        }
        notifyDataSetChanged();
    }

    // 🔍 Filter announcements by chip type: All / Read / Unread
    public void filterChip(String type) {
        filteredList.clear();
        switch (type) {
            case "All":
                filteredList.addAll(originalList);
                break;
            case "Read":
                for (Announcement a : originalList) {
                    if (a.isRead()) filteredList.add(a);
                }
                break;
            case "Unread":
                for (Announcement a : originalList) {
                    if (!a.isRead()) filteredList.add(a);
                }
                break;
        }
        notifyDataSetChanged();
    }
    public interface AnnouncementClickListener {
        void showAnnouncementPopup(String id, String title, String body, String date);
    }

}
package com.example.internlink;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<User> studentList;
    private Context context;

    public StudentAdapter(Context context, List<User> studentList) {
        this.context = context;
        this.studentList = studentList;
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name, email;
        Button startChat;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.student_photo);
            name = itemView.findViewById(R.id.student_name);
            email = itemView.findViewById(R.id.student_email);
            startChat = itemView.findViewById(R.id.btn_start_chat);
        }
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        User student = studentList.get(position);
        holder.name.setText(student.getName());
        holder.email.setText(student.getEmail());

        if (student.getLogoUrl() != null && !student.getLogoUrl().isEmpty()) {
            Glide.with(context).load(student.getLogoUrl()).into(holder.photo);
        }
        holder.startChat.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("CHAT_WITH_ID", student.getUid()); // You'll need to make sure `User` model has getUid()
            intent.putExtra("CHAT_WITH_NAME", student.getName());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }
}

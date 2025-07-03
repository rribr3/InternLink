package com.example.internlink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedProjectsAdapter extends RecyclerView.Adapter<CompletedProjectsAdapter.ViewHolder> {

    private List<Project> projects;
    private OnCertificateClickListener listener;
    private Context context;

    public interface OnCertificateClickListener {
        void onCertificateClick(Project project);
    }

    public CompletedProjectsAdapter(Context context, List<Project> projects, OnCertificateClickListener listener) {
        this.context = context;
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project project = projects.get(position);

        // Set project details
        holder.projectTitle.setText(project.getTitle());
        holder.companyName.setText(project.getCompanyName());

        // Format completion date using deadline
        long completionDate = project.getDeadline(); // Using deadline as completion date
        if (completionDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String completedDate = sdf.format(new Date(completionDate));
            holder.completionDate.setText("Completed on " + completedDate);
        } else {
            holder.completionDate.setText("Completion date not available");
        }

        // Load company logo if available
        if (project.getCompanyLogoUrl() != null && !project.getCompanyLogoUrl().isEmpty()) {
            Glide.with(context)
                    .load(project.getCompanyLogoUrl())
                    .placeholder(R.drawable.ic_company)
                    .error(R.drawable.ic_company)
                    .into(holder.companyLogo);
        }

        // Handle certificate button click
        holder.btnViewCertificate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCertificateClick(project);
            }
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateProjects(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView companyLogo;
        TextView projectTitle;
        TextView companyName;
        TextView completionDate;
        Button btnViewCertificate;

        ViewHolder(View view) {
            super(view);
            companyLogo = view.findViewById(R.id.company_logo);
            projectTitle = view.findViewById(R.id.project_title);
            companyName = view.findViewById(R.id.company_name);
            completionDate = view.findViewById(R.id.completion_date);
            btnViewCertificate = view.findViewById(R.id.btn_view_certificate);
        }
    }
}
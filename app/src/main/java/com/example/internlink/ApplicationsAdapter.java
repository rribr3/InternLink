package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.ApplicationViewHolder> {

    private List<ApplicationWithProject> applications;

    public ApplicationsAdapter(List<ApplicationWithProject> applications) {
        this.applications = applications;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        ApplicationWithProject applicationWithProject = applications.get(position);
        Application application = applicationWithProject.getApplication();
        Project project = applicationWithProject.getProject();

        holder.projectTitle.setText(project.getTitle());
        holder.companyName.setText("Company: " + project.getCompanyId()); // You might want to fetch company name
        holder.status.setText("Status: " + application.getStatus());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.appliedDate.setText("Applied: " + sdf.format(new Date(application.getAppliedDate())));
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {
        TextView projectTitle, companyName, status, appliedDate;

        public ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
            companyName = itemView.findViewById(R.id.company_name);
            status = itemView.findViewById(R.id.application_status);
            appliedDate = itemView.findViewById(R.id.applied_date);
        }
    }
}
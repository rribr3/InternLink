package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {

    private final List<Application> applicationList;
    private final String currentUserId;
    private String applicationId;

    public ApplicationAdapter(List<Application> applicationList, String currentUserId) {
        this.applicationList = applicationList;
        this.currentUserId = currentUserId;
    }
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application app = applicationList.get(position);

        holder.status.setText(app.getStatus());
        holder.date.setText("Applied: " + android.text.format.DateFormat.format("MMM dd, yyyy", app.getAppliedDate()));

        // 🔍 Fetch project title
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(app.getProjectId());
        projectRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.child("title").getValue() != null) {
                holder.title.setText(snapshot.child("title").getValue(String.class));
            } else {
                holder.title.setText("Unknown Project");
            }
        }).addOnFailureListener(e -> holder.title.setText("Error loading project"));

        // 🏢 Fetch company name
        DatabaseReference companyRef = FirebaseDatabase.getInstance().getReference("users").child(app.getCompanyId());
        companyRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.child("name").getValue() != null) {
                holder.company.setText(snapshot.child("name").getValue(String.class));
            } else {
                holder.company.setText("Unknown Company");
            }
        }).addOnFailureListener(e -> holder.company.setText("Error loading company"));
        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, ViewApplications.class);
            intent.putExtra("APPLICATION_ID", app.getApplicationId());
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return applicationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, status, company, date;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.project_title);
            status = itemView.findViewById(R.id.application_status);
            company = itemView.findViewById(R.id.company_name);
            date = itemView.findViewById(R.id.applied_date);
        }
    }

}

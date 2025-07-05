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
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedProjectsAdapter extends RecyclerView.Adapter<CompletedProjectsAdapter.ViewHolder> {

    private List<Project> projects;
    private Context context;
    private RecyclerView recyclerView;
    private static final int MAX_VISIBLE_ITEMS = 3;
    private static final int ITEM_HEIGHT_DP = 120;


    public CompletedProjectsAdapter(Context context, RecyclerView recyclerView, List<Project> projects) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.projects = projects;

        updateRecyclerViewHeight();
    }
    private void updateRecyclerViewHeight() {
        if (recyclerView != null && projects.size() > MAX_VISIBLE_ITEMS) {
            int maxHeightPx = (int) (MAX_VISIBLE_ITEMS * ITEM_HEIGHT_DP *
                    context.getResources().getDisplayMetrics().density);
            ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
            params.height = maxHeightPx;
            recyclerView.setLayoutParams(params);
            recyclerView.setNestedScrollingEnabled(true);
        }
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

        // Load company details including logo
        loadCompanyDetails(project.getCompanyId(), holder);

        Long deadline = project.getDeadline();
        long completionDate = deadline != null ? deadline : 0L; // Use 0L as fallback value
        if (completionDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String completedDate = sdf.format(new Date(completionDate));
            holder.completionDate.setText("Completed on " + completedDate);
        } else {
            holder.completionDate.setText("Completion date not available");
        }


    }

    private void loadCompanyDetails(String companyId, ViewHolder holder) {
        if (companyId == null || companyId.isEmpty()) {
            setDefaultCompanyDisplay(holder);
            return;
        }

        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("users").child(companyId);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Set company name
                    String companyName = snapshot.child("name").getValue(String.class);
                    holder.companyName.setText(companyName != null ? companyName : "Company");

                    // Load company logo
                    String logoUrl = snapshot.child("logoUrl").getValue(String.class);
                    if (logoUrl != null && !logoUrl.isEmpty()) {
                        RequestOptions options = new RequestOptions()
                                .placeholder(R.drawable.ic_company)
                                .error(R.drawable.ic_company)
                                .centerCrop();

                        Glide.with(context)
                                .load(logoUrl)
                                .apply(options)
                                .into(holder.companyLogo);
                    } else {
                        holder.companyLogo.setImageResource(R.drawable.ic_company);
                    }
                } else {
                    setDefaultCompanyDisplay(holder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDefaultCompanyDisplay(holder);
            }
        });
    }

    private void setDefaultCompanyDisplay(ViewHolder holder) {
        holder.companyLogo.setImageResource(R.drawable.ic_company);
        holder.companyName.setText("Company");
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateProjects(List<Project> newProjects) {
        this.projects = newProjects;

        if (recyclerView != null) {
            if (newProjects.size() > MAX_VISIBLE_ITEMS) {
                int maxHeightPx = (int) (MAX_VISIBLE_ITEMS * ITEM_HEIGHT_DP *
                        context.getResources().getDisplayMetrics().density);
                ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = maxHeightPx;
                recyclerView.setLayoutParams(params);
                recyclerView.setNestedScrollingEnabled(true);
            } else {
                ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                recyclerView.setLayoutParams(params);
                recyclerView.setNestedScrollingEnabled(false);
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView companyLogo;
        TextView projectTitle;
        TextView companyName;
        TextView completionDate;

        ViewHolder(View view) {
            super(view);
            companyLogo = view.findViewById(R.id.company_logo);
            projectTitle = view.findViewById(R.id.project_title);
            companyName = view.findViewById(R.id.company_name);
            completionDate = view.findViewById(R.id.completion_date);
        }
    }
}
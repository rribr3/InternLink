package com.example.internlink;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ProjectDetailsAdapter extends RecyclerView.Adapter<ProjectDetailsAdapter.ViewHolder> {

    private List<EmployerProject> projectList;
    private OnItemClickListener itemClickListener;

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(EmployerProject project);
    }

    // Constructor
    public ProjectDetailsAdapter(List<EmployerProject> projects, OnItemClickListener listener) {
        this.projectList = projects;
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each project item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the project at the given position
        EmployerProject project = projectList.get(position);

        // Bind the data to the views
        holder.projectTitle.setText(project.getTitle());
        holder.positionsCount.setText(String.valueOf(project.getPositionsCount()));
        holder.applicantsCount.setText(String.valueOf(project.getApplicantsCount()));

        // Set onClickListener for the "View Details" button
        holder.viewDetailsButton.setOnClickListener(v -> {
            // Call the listener's method when the button is clicked
            itemClickListener.onItemClick(project);
        });
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), holder.menuButton);
            popupMenu.getMenu().add("Delete");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Delete")) {
                    String projectId = project.getProjectId();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("projects").child(projectId);
                    ref.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (position >= 0 && position < projectList.size()) {
                                projectList.remove(position);
                                notifyItemRemoved(position);
                            } else {
                                Log.w("ProjectAdapter", "Invalid position on remove: " + position);
                            }
                            Toast.makeText(holder.itemView.getContext(), "Project deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(holder.itemView.getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return true;
            });
            popupMenu.show();
        });

    }

    private void deleteProjectFromFirebase(String projectId, int position) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

        ref.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                projectList.remove(position);
                notifyItemRemoved(position);
                // Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(context, "Failed to delete project", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public int getItemCount() {
        // Return the size of the project list
        return projectList.size();
    }

    // ViewHolder class that holds the views
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView projectTitle, positionsCount, applicantsCount;
        Button viewDetailsButton;
        ImageView menuButton;

        // Constructor for ViewHolder
        public ViewHolder(View itemView) {
            super(itemView);

            // Initialize the views
            projectTitle = itemView.findViewById(R.id.project_title);
            positionsCount = itemView.findViewById(R.id.positions_count);
            applicantsCount = itemView.findViewById(R.id.applicants_count);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
            menuButton = itemView.findViewById(R.id.menu_button);
        }
    }
}
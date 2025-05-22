package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MyProjectsAdapter extends RecyclerView.Adapter<MyProjectsAdapter.ProjectViewHolder> {

    public interface OnViewDetailsClickListener {
        void onViewDetailsClick(Project project);
    }

    private List<Project> projectList;
    private OnViewDetailsClickListener listener;

    public MyProjectsAdapter(List<Project> projectList, OnViewDetailsClickListener listener) {
        this.projectList = projectList;
        this.listener = listener;
    }

    public void updateList(List<Project> newList) {
        this.projectList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_vertical, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText, positionsText, applicantsText;
        private final Button viewDetailsButton;
        private final ImageView menuButton;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.project_title);
            positionsText = itemView.findViewById(R.id.positions_count);
            applicantsText = itemView.findViewById(R.id.applicants_count);
            viewDetailsButton = itemView.findViewById(R.id.view_details_button);
            menuButton = itemView.findViewById(R.id.menu_button);
        }

        public void bind(Project project) {
            titleText.setText(project.getTitle());
            positionsText.setText(String.valueOf(project.getStudentsRequired()));
            applicantsText.setText(String.valueOf(project.getApplicants()));

            viewDetailsButton.setOnClickListener(v -> listener.onViewDetailsClick(project));

            menuButton.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(itemView.getContext(), menuButton);
                popupMenu.getMenu().add("Delete");

                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Delete")) {
                        deleteProjectFromFirebase(project.getProjectId(), getAdapterPosition());
                    }
                    return true;
                });

                popupMenu.show();
            });
        }

        private void deleteProjectFromFirebase(String projectId, int position) {
            DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
            DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("projects").child(projectId);

            // Step 1: Delete related applications
            applicationsRef.orderByChild("projectId").equalTo(projectId)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            for (com.google.firebase.database.DataSnapshot appSnap : snapshot.getChildren()) {
                                appSnap.getRef().removeValue();
                            }

                            // Step 2: Delete the project itself
                            projectRef.removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    projectList.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(itemView.getContext(), "Project and its applications deleted", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(itemView.getContext(), "Failed to delete project", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                            Toast.makeText(itemView.getContext(), "Failed to delete applications", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }
}

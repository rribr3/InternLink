package com.example.internlink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProjectsWithApplicantsAdapter extends RecyclerView.Adapter<ProjectsWithApplicantsAdapter.ProjectViewHolder> {

    private List<CompanyHomeActivity.ProjectWithApplicants> projects;
    private EnhancedApplicantsAdapter.OnApplicantActionListener actionListener;

    public ProjectsWithApplicantsAdapter(List<CompanyHomeActivity.ProjectWithApplicants> projects,
                                         EnhancedApplicantsAdapter.OnApplicantActionListener actionListener) {
        this.projects = projects;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_with_applicants, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        CompanyHomeActivity.ProjectWithApplicants project = projects.get(position);
        holder.bind(project);
    }

    public void updateData(List<CompanyHomeActivity.ProjectWithApplicants> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged(); // Tells RecyclerView to refresh
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private TextView projectTitle;
        private TextView applicantsCount;
        private TextView projectStatus;
        private RecyclerView applicantsRecycler;
        private ImageView expandCollapseIcon;
        private LinearLayout applicantsContainer;
        private View progressIndicator;
        private ImageView projectIcon;
        private FrameLayout sortButton;
        private FrameLayout filterButton;
        private TextView currentSectionTitle;

        // Section filter TextViews
        private TextView sectionAllApplicants;
        private TextView sectionAccepted;
        private TextView sectionShortlist;

        private boolean isExpanded = false;
        private String currentFilter = "all"; // "all", "accepted", "shortlist"
        private List<Applicant> allApplicants = new ArrayList<>();
        private List<Applicant> filteredApplicants = new ArrayList<>();

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews();
            setupClickListeners();
            setupInitialState();
        }

        private void initializeViews() {
            projectTitle = itemView.findViewById(R.id.project_title);
            applicantsCount = itemView.findViewById(R.id.applicants_count);
            projectStatus = itemView.findViewById(R.id.project_status);
            applicantsRecycler = itemView.findViewById(R.id.applicants_recycler);
            expandCollapseIcon = itemView.findViewById(R.id.expand_collapse_icon);
            applicantsContainer = itemView.findViewById(R.id.applicants_container);
            progressIndicator = itemView.findViewById(R.id.progress_indicator);
            projectIcon = itemView.findViewById(R.id.project_icon);
            sortButton = itemView.findViewById(R.id.sort_applicants);
            filterButton = itemView.findViewById(R.id.filter_applicants);
            currentSectionTitle = itemView.findViewById(R.id.current_section_title);

            // Initialize section filter views
            sectionAllApplicants = itemView.findViewById(R.id.section_all_applicants);
            sectionAccepted = itemView.findViewById(R.id.section_accepted);
            sectionShortlist = itemView.findViewById(R.id.section_shortlist);

            // Setup RecyclerView
            applicantsRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }

        private void setupInitialState() {
            // Initially hide applicants
            applicantsContainer.setVisibility(View.GONE);
            expandCollapseIcon.setRotation(0);
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }
            updateSectionHighlight();
        }

        private void setupClickListeners() {
            // Main expand/collapse click
            View.OnClickListener expandListener = v -> toggleExpansion();

            itemView.setOnClickListener(expandListener);

            // Section filter click listeners
            sectionAllApplicants.setOnClickListener(v -> {
                currentFilter = "all";
                updateSectionHighlight();
                filterApplicants();
                currentSectionTitle.setText("All Team Applications");
            });

            sectionAccepted.setOnClickListener(v -> {
                currentFilter = "accepted";
                updateSectionHighlight();
                filterApplicants();
                currentSectionTitle.setText("Accepted Applications");
            });

            sectionShortlist.setOnClickListener(v -> {
                currentFilter = "shortlist";
                updateSectionHighlight();
                filterApplicants();
                currentSectionTitle.setText("Shortlisted Applications");
            });

            // Sort button
            if (sortButton != null) {
                sortButton.setOnClickListener(v -> {
                    showToastWithoutAnimation("Sort applicants by date, status, name");
                });
            }

            // Filter button
            if (filterButton != null) {
                filterButton.setOnClickListener(v -> {
                    showToastWithoutAnimation("Filter applicants by status");
                });
            }
        }

        private void updateSectionHighlight() {
            // Reset all section styles
            resetSectionStyle(sectionAllApplicants);
            resetSectionStyle(sectionAccepted);
            resetSectionStyle(sectionShortlist);

            // Highlight current section
            switch (currentFilter) {
                case "all":
                    highlightSection(sectionAllApplicants);
                    break;
                case "accepted":
                    highlightSection(sectionAccepted);
                    break;
                case "shortlist":
                    highlightSection(sectionShortlist);
                    break;
            }
        }

        private void resetSectionStyle(TextView section) {
            section.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            section.setBackgroundResource(android.R.color.transparent);
        }

        private void highlightSection(TextView section) {
            section.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.blue_500));
            section.setBackgroundResource(R.drawable.rounded_background);
        }

        private void filterApplicants() {
            filteredApplicants.clear();

            for (Applicant applicant : allApplicants) {
                String status = applicant.getStatus();
                if (status == null) status = "Pending";

                switch (currentFilter) {
                    case "all":
                        filteredApplicants.add(applicant);
                        break;
                    case "accepted":
                        // Check for both "Accepted" and "accepted" to be safe
                        if ("Accepted".equalsIgnoreCase(status)) {
                            filteredApplicants.add(applicant);
                        }
                        break;
                    case "shortlist":
                        // Check for both "Shortlisted" and "shortlist" to be safe
                        if ("Shortlisted".equalsIgnoreCase(status) || "Shortlist".equalsIgnoreCase(status)) {
                            filteredApplicants.add(applicant);
                        }
                        break;
                }
            }

            // Update section counts
            updateSectionCounts();

            // Update adapter with filtered data
            EnhancedApplicantsAdapter adapter = new EnhancedApplicantsAdapter(
                    filteredApplicants,
                    actionListener
            );
            applicantsRecycler.setAdapter(adapter);

            // Notify that data has changed
            adapter.notifyDataSetChanged();
        }

        private void updateSectionCounts() {
            int allCount = allApplicants.size();
            int acceptedCount = 0;
            int shortlistCount = 0;

            for (Applicant applicant : allApplicants) {
                String status = applicant.getStatus();
                if (status == null) status = "Pending";

                if ("Accepted".equalsIgnoreCase(status)) {
                    acceptedCount++;
                } else if ("Shortlisted".equalsIgnoreCase(status) || "Shortlist".equalsIgnoreCase(status)) {
                    shortlistCount++;
                }
            }

            sectionAllApplicants.setText("All (" + allCount + ")");
            sectionAccepted.setText("Accepted (" + acceptedCount + ")");
            sectionShortlist.setText("Shortlist (" + shortlistCount + ")");
        }

        public void bind(CompanyHomeActivity.ProjectWithApplicants project) {
            projectTitle.setText(project.getProjectTitle());

            // Store all applicants
            allApplicants.clear();
            allApplicants.addAll(project.getApplicants());

            applicantsCount.setText(project.getApplicants().size() + " applicants");

            // Update project status without animation
            updateProjectStatusWithoutAnimation(project.getApplicants().size());

            // Setup applicants adapter with current filter
            filterApplicants();
        }

        private void updateProjectStatusWithoutAnimation(int applicantCount) {
            String status;
            int textColor;

            if (applicantCount == 0) {
                status = "New";
                textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_dark);
            } else if (applicantCount < 5) {
                status = "Active";
                textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
            } else {
                status = "Popular";
                textColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark);
            }

            // Update status without animation
            if (projectStatus != null) {
                projectStatus.setText(status);
                projectStatus.setTextColor(textColor);
            }
        }

        private void toggleExpansion() {
            boolean willExpand = !isExpanded;
            isExpanded = willExpand;

            if (willExpand) {
                expandWithoutAnimation();
            } else {
                collapseWithoutAnimation();
            }

            // Simple icon rotation without animation
            expandCollapseIcon.setRotation(isExpanded ? 90f : 0f);
        }

        private void expandWithoutAnimation() {
            // Show progress indicator
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.VISIBLE);
            }

            // Make container visible immediately
            applicantsContainer.setVisibility(View.VISIBLE);
        }

        private void collapseWithoutAnimation() {
            // Hide progress indicator
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.GONE);
            }

            // Hide container immediately
            applicantsContainer.setVisibility(View.GONE);
        }

        private void showToastWithoutAnimation(String message) {
            Toast toast = Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
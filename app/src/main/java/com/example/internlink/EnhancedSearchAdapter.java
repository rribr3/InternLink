package com.example.internlink;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EnhancedSearchAdapter extends RecyclerView.Adapter<EnhancedSearchAdapter.SearchViewHolder> {

    private final List<StudentHomeActivity.SearchResult> searchResults;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public EnhancedSearchAdapter(List<StudentHomeActivity.SearchResult> searchResults) {
        this.searchResults = searchResults;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enhanced_search_result, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        StudentHomeActivity.SearchResult result = searchResults.get(position);
        Context context = holder.itemView.getContext();

        if ("PROJECT".equals(result.type)) {
            bindProjectResult(holder, result, context);
        } else {
            bindCompanyResult(holder, result, context);
        }

        // Set match reasons
        holder.matchReasonsLayout.removeAllViews();
        if (result.matchReasons != null && !result.matchReasons.isEmpty()) {
            for (String reason : result.matchReasons) {
                TextView reasonView = new TextView(context);
                reasonView.setText("• " + reason);
                reasonView.setTextSize(12f);
                reasonView.setTextColor(context.getResources().getColor(R.color.gray_800));
                reasonView.setPadding(0, 2, 0, 2);
                holder.matchReasonsLayout.addView(reasonView);
            }
        }

        // Handle click
        holder.cardView.setOnClickListener(v -> {
            if ("PROJECT".equals(result.type)) {
                // Handle project click - you can navigate to project details or apply
                Intent intent = new Intent(context, ApplyNowActivity.class);
                intent.putExtra("PROJECT_ID", result.projectId);
                context.startActivity(intent);

                // For now, just show a toast
                android.widget.Toast.makeText(context, "Selected project: " + result.project.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(context, CompanyProfileViewActivity.class);
                intent.putExtra("COMPANY_ID", result.company.id);
                context.startActivity(intent);
            }
        });
    }

    private void bindProjectResult(SearchViewHolder holder, StudentHomeActivity.SearchResult result, Context context) {
        Project project = result.project;
        StudentHomeActivity.CompanyInfo company = result.company;

        holder.typeIndicator.setText("PROJECT");
        holder.typeIndicator.setBackgroundResource(R.drawable.dialog_background_modern);

        holder.titleText.setText(project.getTitle());
        holder.subtitleText.setText(company != null ? company.name : "Unknown Company");

        // Build description with key details
        StringBuilder description = new StringBuilder();

        if (project.getCategory() != null) {
            description.append("Category: ").append(project.getCategory()).append("\n");
        }

        if (project.getDuration() != null) {
            description.append("Duration: ").append(project.getDuration()).append("\n");
        }

        if (project.getEducationLevel() != null) {
            description.append("Education: ").append(project.getEducationLevel()).append("\n");
        }

        if (project.getCompensationType() != null) {
            if ("Paid".equals(project.getCompensationType()) && project.getAmount() > 0) {
                description.append("Compensation: $").append(project.getAmount()).append("\n");
            } else {
                description.append("Compensation: ").append(project.getCompensationType()).append("\n");
            }
        }

        project.getLocation();
        description.append("Location: ").append(project.getLocation()).append("\n");

        description.append("Students needed: ").append(project.getStudentsRequired())
                .append(" (").append(project.getApplicants()).append(" applied)");

        holder.descriptionText.setText(description.toString());

        // Skills
        holder.skillsLayout.removeAllViews();
        if (project.getSkills() != null) {
            TextView skillsLabel = new TextView(context);
            skillsLabel.setText("Required Skills:");
            skillsLabel.setTextColor(context.getResources().getColor(R.color.gray_800));
            skillsLabel.setTextSize(12f);
            skillsLabel.setTextSize(Typeface.BOLD);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.setMargins(0, 0, 0, 8);
            skillsLabel.setLayoutParams(labelParams);
            holder.skillsLayout.addView(skillsLabel);

            LinearLayout skillsContainer = new LinearLayout(context);
            skillsContainer.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            skillsContainer.setLayoutParams(containerParams);

            for (String skill : project.getSkills()) {
                TextView skillChip = createSkillChip(context, skill);
                skillsContainer.addView(skillChip);
            }
            holder.skillsLayout.addView(skillsContainer);
        }

        // Dates
        if (project.getDeadline() > 0) {
            holder.deadlineText.setText("Deadline: " + dateFormat.format(new Date(project.getDeadline())));
            holder.deadlineText.setVisibility(View.VISIBLE);
        } else {
            holder.deadlineText.setVisibility(View.GONE);
        }

        if (project.getStartDate() > 0) {
            holder.startDateText.setText("Starts: " + dateFormat.format(new Date(project.getStartDate())));
            holder.startDateText.setVisibility(View.VISIBLE);
        } else {
            holder.startDateText.setVisibility(View.GONE);
        }

        // Company info section
        if (company != null) {
            holder.companyInfoLayout.setVisibility(View.VISIBLE);
            if (company.industry != null) {
                holder.companyIndustryText.setText("Industry: " + company.industry);
                holder.companyIndustryText.setVisibility(View.VISIBLE);
            } else {
                holder.companyIndustryText.setVisibility(View.GONE);
            }

            if (company.location != null) {
                holder.companyLocationText.setText("Location: " + company.location);
                holder.companyLocationText.setVisibility(View.VISIBLE);
            } else {
                holder.companyLocationText.setVisibility(View.GONE);
            }
        } else {
            holder.companyInfoLayout.setVisibility(View.GONE);
        }
    }

    private void bindCompanyResult(SearchViewHolder holder, StudentHomeActivity.SearchResult result, Context context) {
        StudentHomeActivity.CompanyInfo company = result.company;

        holder.typeIndicator.setText("COMPANY");
        holder.typeIndicator.setBackgroundResource(R.drawable.dialog_background_modern);

        holder.titleText.setText(company.name);
        holder.subtitleText.setText(company.industry != null ? company.industry : "Company");

        StringBuilder description = new StringBuilder();
        if (company.description != null && !company.description.isEmpty()) {
            description.append(company.description).append("\n\n");
        }

        if (company.location != null) {
            description.append("Location: ").append(company.location);
        }

        holder.descriptionText.setText(description.toString());

        // Hide project-specific elements
        holder.skillsLayout.removeAllViews();
        holder.deadlineText.setVisibility(View.GONE);
        holder.startDateText.setVisibility(View.GONE);
        holder.companyInfoLayout.setVisibility(View.GONE);
    }

    private TextView createSkillChip(Context context, String skill) {
        TextView chip = new TextView(context);
        chip.setText(skill);
        chip.setTextSize(12f);
        chip.setTextColor(context.getResources().getColor(R.color.white));
        chip.setBackgroundResource(R.drawable.button_rounded);
        chip.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 8);
        chip.setLayoutParams(params);

        return chip;
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView typeIndicator;
        TextView titleText;
        TextView subtitleText;
        TextView descriptionText;
        LinearLayout skillsLayout;
        TextView deadlineText;
        TextView startDateText;
        LinearLayout companyInfoLayout;
        TextView companyIndustryText;
        TextView companyLocationText;
        LinearLayout matchReasonsLayout;

        SearchViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_search_result);
            typeIndicator = itemView.findViewById(R.id.tv_type_indicator);
            titleText = itemView.findViewById(R.id.tv_result_title);
            subtitleText = itemView.findViewById(R.id.tv_result_subtitle);
            descriptionText = itemView.findViewById(R.id.tv_result_description);
            skillsLayout = itemView.findViewById(R.id.layout_skills);
            deadlineText = itemView.findViewById(R.id.tv_deadline);
            startDateText = itemView.findViewById(R.id.tv_start_date);
            companyInfoLayout = itemView.findViewById(R.id.layout_company_info);
            companyIndustryText = itemView.findViewById(R.id.tv_company_industry);
            companyLocationText = itemView.findViewById(R.id.tv_company_location);
            matchReasonsLayout = itemView.findViewById(R.id.layout_match_reasons);
        }
    }
}
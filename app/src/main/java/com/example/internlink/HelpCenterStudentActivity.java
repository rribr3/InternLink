package com.example.internlink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.search.SearchBar;

public class HelpCenterStudentActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SearchBar searchBar;
    private MaterialCardView postProjectCard, manageApplicantsCard, videoTutorialsCard, knowledgeBaseCard;
    private Button supportCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_center_student);

        initializeToolbar();
        initializeViews();
        setupClickListeners();
    }

    private void initializeToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Student Help Center");
            getSupportActionBar().setSubtitle("Get support for using InternLink");
        }

        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        searchBar = findViewById(R.id.searchBar);
        postProjectCard = findViewById(R.id.viewProjectsCard);
        manageApplicantsCard = findViewById(R.id.applyProjectsCard);
        videoTutorialsCard = findViewById(R.id.studentVideoTutorialsCard);
        knowledgeBaseCard = findViewById(R.id.studentKnowledgeBaseCard);
        supportCard = findViewById(R.id.studentContactSupportButton);
    }

    private void setupClickListeners() {
        searchBar.setOnClickListener(v ->
                Toast.makeText(this, "Search our knowledge base for instant answers", Toast.LENGTH_SHORT).show());

        postProjectCard.setOnClickListener(v -> showTutorialPopup(
                R.drawable.ic_post_project,
                "🔍 Explore Projects",
                "Learn how to find internship opportunities and view project details.",
                new String[]{
                        "1. Tap 'Explore Projects' on the home screen",
                        "2. Filter projects by category or location",
                        "3. Read full details and required skills",
                        "4. Save projects for later or apply directly"
                },
                new String[]{"Real-time Updates", "Diverse Categories"},
                "PRO TIP: Use filters to quickly find what matches your profile!",
                "EXPLORE PROJECTS",
                R.color.colorPostProject
        ));

        manageApplicantsCard.setOnClickListener(v -> showTutorialPopup(
                R.drawable.ic_applicant_management,
                "📤 Apply for Projects",
                "Guide for applying to projects using InternLink.",
                new String[]{
                        "• Select a project and click 'Apply'",
                        "• Upload your resume or portfolio",
                        "• Complete any quizzes if required",
                        "• Submit your application and track status"
                },
                new String[]{"One-Tap Apply", "Quiz Support"},
                "PRO TIP: Tailor your resume to match the project's needs.",
                "APPLY NOW",
                R.color.colorManageCandidates
        ));

        videoTutorialsCard.setOnClickListener(v -> showTutorialPopup(
                R.drawable.ic_video_tutorial,
                "🎥 Video Tutorials",
                "Watch short videos to understand how to use the app effectively.",
                new String[]{
                        "▶ Navigating InternLink (2:30)",
                        "▶ Applying for Projects (3:00)",
                        "▶ Using the Student Dashboard (2:45)",
                        "▶ Tips to Improve Your Profile (3:10)"
                },
                new String[]{"Visual Guides", "Quick & Easy"},
                "PRO TIP: Watch on Wi-Fi to save data.",
                "WATCH NOW",
                R.color.colorVideoTutorials
        ));

        knowledgeBaseCard.setOnClickListener(v -> showTutorialPopup(
                R.drawable.ic_knowledge_base,
                "📘 Knowledge Base",
                "Browse frequently asked questions and troubleshooting articles.",
                new String[]{
                        "• Account Setup",
                        "• Applying to Projects",
                        "• Managing Applications",
                        "• Using Filters & Notifications",
                        "• App Settings & Preferences"
                },
                new String[]{"200+ Topics", "Search Enabled"},
                "PRO TIP: Use keywords like 'apply', 'reset', or 'update' for best results.",
                "READ MORE",
                R.color.colorKnowledgeBase
        ));

        supportCard.setOnClickListener(v -> showTutorialPopup(
                R.drawable.ic_help,
                "✉ Contact Support",
                "Need help? Our support team is ready to assist.",
                new String[]{
                        "• Submit a support ticket with your details",
                        "• Email: support@InternLink.com",
                        "• Chat: Use the support bubble in the corner",
                        "• Response Time: Within 24 hours"
                },
                new String[]{"Live Chat Available", "Priority Support for Students"},
                "PRO TIP: Share screenshots to resolve issues faster.",
                "CONTACT NOW",
                R.color.colorSupport
        ));
    }

    private void showTutorialPopup(int iconRes, String title, String description,
                                   String[] steps, String[] stats, String proTip,
                                   String actionText, int colorRes) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ProfessionalDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.popup_tutorial, null);

        ImageView icon = view.findViewById(R.id.popup_icon);
        TextView titleView = view.findViewById(R.id.popup_title);
        TextView descriptionView = view.findViewById(R.id.popup_description);
        LinearLayout stepsLayout = view.findViewById(R.id.popup_steps);
        TextView stat1 = view.findViewById(R.id.popup_stat1);
        TextView stat2 = view.findViewById(R.id.popup_stat2);
        TextView proTipView = view.findViewById(R.id.popup_pro_tip);
        Button actionButton = view.findViewById(R.id.popup_action_button);
        View accentBar = view.findViewById(R.id.accent_bar);

        icon.setImageResource(iconRes);
        titleView.setText(title);
        descriptionView.setText(description);
        accentBar.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        actionButton.setBackgroundColor(ContextCompat.getColor(this, colorRes));

        stepsLayout.removeAllViews();
        for (String step : steps) {
            TextView stepView = (TextView) LayoutInflater.from(this)
                    .inflate(R.layout.item_step, stepsLayout, false);
            stepView.setText(step);
            stepsLayout.addView(stepView);
        }

        stat1.setText(stats[0]);
        stat2.setText(stats[1]);
        proTipView.setText(proTip);
        actionButton.setText(actionText);

        AlertDialog dialog = builder.setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
        }

        actionButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        MenuItem item = menu.findItem(R.id.action_notifications);
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(this, android.R.color.black));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_notifications) {
            startActivity(new Intent(this, StudentAnnounce.class));
            return true;
        } else if (id == R.id.action_help) {
            showTutorialPopup(
                    R.drawable.ic_help,
                    "ℹ Help Menu",
                    "Quick access to all student support resources.",
                    new String[]{
                            "• Explore project listings",
                            "• Apply easily using our streamlined process",
                            "• Learn with videos and FAQs",
                            "• Contact our support team"
                    },
                    new String[]{"Guided Interface", "Student-Focused Help"},
                    "TIP: Use the search bar above to find answers instantly.",
                    "OKAY",
                    R.color.blue_500
            );
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, StudentSettingsActivity.class));
            return true;
        } else if (id == R.id.action_feedback) {
            Toast.makeText(this, "We appreciate your feedback!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            Toast.makeText(this, "InternLink v1.0 – Empowering Students & Employers", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }
}
package com.example.internlink;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.search.SearchBar;

public class CompanyHelpCenterActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SearchBar searchBar;
    private TextView quickHelpTitle, helpCategoriesTitle, supportTitle, supportDescription;
    private Button contactSupportButton;
    private MaterialCardView videoTutorialsCard, knowledgeBaseCard, postProjectCard, manageApplicantsCard, supportCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_center_company);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // SearchBar setup
        searchBar = findViewById(R.id.searchBar);
        searchBar.setOnClickListener(v -> Toast.makeText(this, "Search our knowledge base for instant answers", Toast.LENGTH_SHORT).show());

        // Initialize Help Sections
        quickHelpTitle = findViewById(R.id.quickHelpTitle);
        helpCategoriesTitle = findViewById(R.id.helpCategoriesTitle);
        supportTitle = findViewById(R.id.supportTitle);
        supportDescription = findViewById(R.id.supportDescription);

        // Cards
        postProjectCard = findViewById(R.id.postProjectCard);
        manageApplicantsCard = findViewById(R.id.manageApplicantsCard);
        videoTutorialsCard = findViewById(R.id.videoTutorialsCard);
        knowledgeBaseCard = findViewById(R.id.knowledgeBaseCard);
        supportCard = findViewById(R.id.supportCard);
        contactSupportButton = findViewById(R.id.contactSupportButton);

        // Set up click listeners with professional help dialogs
        postProjectCard.setOnClickListener(v -> showProfessionalPopup(
                "Streamline Your Hiring Process",
                "Posting projects on InternLink is designed to save you time while attracting top talent:\n\n" +
                        "• Create detailed listings in minutes with our intuitive form\n" +
                        "• Add screening questions to automatically filter applicants\n" +
                        "• Showcase your company culture to attract the right candidates\n" +
                        "• Set clear expectations with structured project details\n\n" +
                        "Our data shows companies who complete all project fields receive 40% more qualified applications.",
                "GOT IT"));

        manageApplicantsCard.setOnClickListener(v -> showProfessionalPopup(
                "Effortless Candidate Management",
                "Take control of your hiring pipeline with our powerful applicant management tools:\n\n" +
                        "• Organize applications with customizable status tags\n" +
                        "• Compare candidates side-by-side with unified profiles\n" +
                        "• Communicate directly through our secure messaging system\n" +
                        "• Schedule interviews without leaving the platform\n\n" +
                        "85% of companies report faster hiring decisions using our management tools.",
                "UNDERSTOOD"));

        videoTutorialsCard.setOnClickListener(v -> showProfessionalPopup(
                "Master InternLink Quickly",
                "Our video library helps you maximize the platform's potential:\n\n" +
                        "• Step-by-step walkthroughs of all major features\n" +
                        "• Best practices from successful companies\n" +
                        "• Advanced tips for experienced users\n" +
                        "• Regular updates on new features\n\n" +
                        "New users typically reduce their onboarding time by 60% after watching our tutorials.",
                "WATCH LATER"));

        knowledgeBaseCard.setOnClickListener(v -> showProfessionalPopup(
                "Instant Answers to Your Questions",
                "Our comprehensive knowledge base includes:\n\n" +
                        "• Detailed guides for every feature\n" +
                        "• Troubleshooting for common issues\n" +
                        "• Templates for projects and communications\n" +
                        "• Case studies from top-performing companies\n\n" +
                        "Most companies find answers to their questions in under 2 minutes using our searchable database.",
                "EXPLORE"));

        contactSupportButton.setOnClickListener(v -> showProfessionalPopup(
                "Dedicated Support for Your Success",
                "Our support team is committed to helping you achieve your hiring goals:\n\n" +
                        "• Priority response for premium members\n" +
                        "• Specialized help for complex hiring needs\n" +
                        "• Platform experts available 24/7\n" +
                        "• Regular check-ins for enterprise clients\n\n" +
                        "We maintain a 98% satisfaction rating from companies using our support services.\n\n" +
                        "Email: support@internlink.com\nPhone: (555) 123-4567",
                "CONTACT NOW"));
    }

    private void showProfessionalPopup(String title, String message, String buttonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ProfessionalDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.popup_help_info, null);

        TextView popupTitle = view.findViewById(R.id.popupTitle);
        TextView popupMessage = view.findViewById(R.id.popupMessage);
        Button closeButton = view.findViewById(R.id.closeButton);

        popupTitle.setText(title);
        popupMessage.setText(message);
        closeButton.setText(buttonText);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Customize dialog appearance
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void searchHelpArticles(String query) {
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter what you're looking for", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Searching our resources for: " + query, Toast.LENGTH_SHORT).show();
            // Implement actual search functionality here
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
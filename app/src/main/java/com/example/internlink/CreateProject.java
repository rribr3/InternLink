package com.example.internlink;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CreateProject extends AppCompatActivity {

    // Project Fields
    private TextInputEditText titleEditText, descriptionEditText;
    private TextInputEditText studentsEditText, amountEditText;
    private TextInputEditText startDateEditText, deadlineEditText;
    private AutoCompleteTextView locationAutoComplete;
    private ChipGroup skillsChipGroup;
    private TextView contactPersonText, contactEmailText, contactPhoneText;
    private LinearLayout amountInputLayout;

    // Quiz Fields
    private TextInputEditText quizTitleEditText, quizInstructionsEditText;
    private TextInputEditText timeLimitEditText, passingScoreEditText;
    private LinearLayout quizFieldsContainer, questionsContainer;

    // Dropdown Views
    private AutoCompleteTextView categoryAutoComplete, durationAutoComplete;
    private AutoCompleteTextView educationAutoComplete, stipendAutoComplete;
    private AutoCompleteTextView skillsAutoComplete;

    // Data
    private List<Uri> fileUris = new ArrayList<>();
    private List<Map<String, Object>> questions = new ArrayList<>();

    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference categoriesRef;
    private String companyId;
    private String projectId;

    // Date Picker
    final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("projects");
        categoriesRef = FirebaseDatabase.getInstance().getReference("categories");
        companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();

        // Setup dropdowns
        setupDropdowns();

        // Setup date pickers
        setupDatePickers();

        // Setup skills input
        setupSkillsInput();

        // Load company contact info
        loadContactInfo();

        // Setup quiz toggle
        setupQuizToggle();

        // Setup buttons
        setupButtons();
    }

    private void initializeViews() {
        // Project fields
        titleEditText = findViewById(R.id.title_edit_text);
        descriptionEditText = findViewById(R.id.description_edit_text);
        studentsEditText = findViewById(R.id.students_edit_text);
        amountEditText = findViewById(R.id.amount_edit_text);
        startDateEditText = findViewById(R.id.start_date_edit_text);
        deadlineEditText = findViewById(R.id.deadline_edit_text);
        locationAutoComplete = findViewById(R.id.location_edit_text);
        skillsChipGroup = findViewById(R.id.skills_chip_group);
        amountInputLayout = findViewById(R.id.amount_input_layout);

        // Quiz fields
        quizTitleEditText = findViewById(R.id.quiz_title_edit_text);
        quizInstructionsEditText = findViewById(R.id.quiz_instructions_edit_text);
        timeLimitEditText = findViewById(R.id.time_limit_edit_text);
        passingScoreEditText = findViewById(R.id.passing_score_edit_text);
        quizFieldsContainer = findViewById(R.id.quiz_fields_container);
        questionsContainer = findViewById(R.id.questions_container);

        // Dropdowns
        categoryAutoComplete = findViewById(R.id.category_auto_complete);
        durationAutoComplete = findViewById(R.id.duration_auto_complete);
        educationAutoComplete = findViewById(R.id.education_auto_complete);
        stipendAutoComplete = findViewById(R.id.stipend_auto_complete);
        skillsAutoComplete = findViewById(R.id.skills_auto_complete);
    }

    private void setupDropdowns() {
        // Categories from Firebase
        loadCategoriesFromFirebase();

        // Locations
        String[] locations = getResources().getStringArray(R.array.project_locations);
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, locations);
        locationAutoComplete.setAdapter(locationAdapter);

        // Durations
        String[] durations = getResources().getStringArray(R.array.project_durations);
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, durations);
        durationAutoComplete.setAdapter(durationAdapter);

        // Education levels
        String[] educationLevels = getResources().getStringArray(R.array.education_levels);
        ArrayAdapter<String> educationAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, educationLevels);
        educationAutoComplete.setAdapter(educationAdapter);

        // Stipend options
        String[] stipendOptions = {"Unpaid", "Paid"};
        ArrayAdapter<String> stipendAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, stipendOptions);
        stipendAutoComplete.setAdapter(stipendAdapter);
        stipendAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            amountInputLayout.setVisibility(selection.equals("Paid") ? View.VISIBLE : View.GONE);
        });

        // Skills
        String[] skills = getResources().getStringArray(R.array.tech_skills);
        ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, skills);
        skillsAutoComplete.setAdapter(skillsAdapter);
    }

    private void loadCategoriesFromFirebase() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categories = new ArrayList<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String category = categorySnapshot.getKey();
                    if (category != null) {
                        categories.add(category);
                    }
                }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                        CreateProject.this,
                        R.layout.dropdown_item,
                        categories
                );
                categoryAutoComplete.setAdapter(categoryAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CreateProject.this,
                        "Failed to load categories: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();

                // Fallback to local categories
                String[] localCategories = getResources().getStringArray(R.array.project_categories);
                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                        CreateProject.this,
                        R.layout.dropdown_item,
                        localCategories
                );
                categoryAutoComplete.setAdapter(categoryAdapter);
            }
        });
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener startDateListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateLabel(startDateEditText);
        };

        DatePickerDialog.OnDateSetListener deadlineListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateLabel(deadlineEditText);
        };

        startDateEditText.setOnClickListener(v -> showDatePicker(startDateListener));
        deadlineEditText.setOnClickListener(v -> showDatePicker(deadlineListener));
    }

    private void showDatePicker(DatePickerDialog.OnDateSetListener listener) {
        new DatePickerDialog(
                CreateProject.this,
                listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDateLabel(TextInputEditText editText) {
        String dateFormat = "MMM dd, yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }

    private void setupSkillsInput() {
        // Keep existing predefined skills dropdown
        String[] predefinedSkills = getResources().getStringArray(R.array.tech_skills);
        ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, predefinedSkills);
        skillsAutoComplete.setAdapter(skillsAdapter);

        // Handle both dropdown selection and custom input
        skillsAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSkill = (String) parent.getItemAtPosition(position);
            addSkillChip(selectedSkill);
            skillsAutoComplete.setText("");
        });

        // Add custom skill on Enter/Done key press
        skillsAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String customSkill = skillsAutoComplete.getText().toString().trim();
                if (!customSkill.isEmpty()) {
                    addSkillChip(customSkill);
                    skillsAutoComplete.setText("");
                }
                return true;
            }
            return false;
        });

        // Allow free text input
        skillsAutoComplete.setThreshold(0);
        skillsAutoComplete.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String customSkill = skillsAutoComplete.getText().toString().trim();
                if (!customSkill.isEmpty()) {
                    addSkillChip(customSkill);
                    skillsAutoComplete.setText("");
                }
            }
        });
    }

    private void addSkillChip(String skill) {
        // Validate skill before adding
        if (skill == null || skill.trim().isEmpty()) {
            return;
        }

        // Check for duplicates
        for (int i = 0; i < skillsChipGroup.getChildCount(); i++) {
            Chip existingChip = (Chip) skillsChipGroup.getChildAt(i);
            if (existingChip.getText().toString().equalsIgnoreCase(skill.trim())) {
                Toast.makeText(this, "Skill already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Add the chip
        Chip chip = new Chip(this);
        chip.setText(skill.trim());
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> skillsChipGroup.removeView(chip));
        skillsChipGroup.addView(chip);
    }

    private void loadContactInfo() {
        DatabaseReference companyRef = FirebaseDatabase.getInstance()
                .getReference("companies")
                .child(companyId);

        companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String contactPerson = dataSnapshot.child("contactPerson").getValue(String.class);
                    String contactEmail = dataSnapshot.child("contactEmail").getValue(String.class);
                    String contactPhone = dataSnapshot.child("contactPhone").getValue(String.class);

                    contactPersonText.setText(contactPerson);
                    contactEmailText.setText(contactEmail);
                    contactPhoneText.setText(contactPhone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                contactPersonText.setText("Not available");
                contactEmailText.setText("Not available");
                contactPhoneText.setText("Not available");
            }
        });
    }

    private void setupQuizToggle() {
        SwitchMaterial quizToggle = findViewById(R.id.quiz_toggle);
        quizToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            quizFieldsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void setupButtons() {
        findViewById(R.id.add_question_button).setOnClickListener(v -> showAddQuestionDialog());
        findViewById(R.id.submit_button).setOnClickListener(v -> validateAndSubmit());
    }

    private void showAddQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Question");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_question, null);
        builder.setView(dialogView);

        TextInputEditText questionText = dialogView.findViewById(R.id.question_text);
        Spinner questionTypeSpinner = dialogView.findViewById(R.id.question_type_spinner);
        LinearLayout trueFalseContainer = dialogView.findViewById(R.id.true_false_container);
        RadioGroup trueFalseRadioGroup = dialogView.findViewById(R.id.true_false_radio_group);
        LinearLayout multipleChoiceContainer = dialogView.findViewById(R.id.multiple_choice_container);
        LinearLayout optionsContainer = dialogView.findViewById(R.id.options_container);
        Button addOptionButton = dialogView.findViewById(R.id.add_option_button);

        // Setup question type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.question_types,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        questionTypeSpinner.setAdapter(typeAdapter);

        // Handle question type selection
        questionTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                trueFalseContainer.setVisibility(selectedType.equals("True/False") ? View.VISIBLE : View.GONE);
                multipleChoiceContainer.setVisibility(selectedType.equals("Multiple Choice") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Handle adding options for multiple choice
        addOptionButton.setOnClickListener(v -> {
            View optionView = LayoutInflater.from(this)
                    .inflate(R.layout.item_option, optionsContainer, false);

            TextInputEditText optionText = optionView.findViewById(R.id.option_text);
            CheckBox correctCheckbox = optionView.findViewById(R.id.correct_checkbox);
            ImageView removeOption = optionView.findViewById(R.id.remove_option);

            removeOption.setOnClickListener(removeView -> optionsContainer.removeView(optionView));
            optionsContainer.addView(optionView);
        });

        builder.setPositiveButton("Add Question", (dialog, which) -> {
            String question = questionText.getText().toString().trim();
            String type = questionTypeSpinner.getSelectedItem().toString();

            if (question.isEmpty()) {
                Toast.makeText(this, "Question text is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> questionData = new HashMap<>();
            questionData.put("text", question);
            questionData.put("type", type);

            if (type.equals("Multiple Choice")) {
                List<Map<String, Object>> options = new ArrayList<>();
                boolean hasCorrectAnswer = false;

                for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                    View optionView = optionsContainer.getChildAt(i);
                    TextInputEditText optionText = optionView.findViewById(R.id.option_text);
                    CheckBox correctCheckbox = optionView.findViewById(R.id.correct_checkbox);

                    String option = optionText.getText().toString().trim();
                    if (!option.isEmpty()) {
                        Map<String, Object> optionData = new HashMap<>();
                        optionData.put("text", option);
                        optionData.put("correct", correctCheckbox.isChecked());
                        options.add(optionData);

                        if (correctCheckbox.isChecked()) {
                            hasCorrectAnswer = true;
                        }
                    }
                }

                if (options.size() < 2) {
                    Toast.makeText(this, "Multiple choice needs at least 2 options", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!hasCorrectAnswer) {
                    Toast.makeText(this, "Please mark at least one correct answer", Toast.LENGTH_SHORT).show();
                    return;
                }

                questionData.put("options", options);
            }
            else if (type.equals("True/False")) {
                int selectedId = trueFalseRadioGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(this, "Please select True or False", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isTrue = selectedId == R.id.true_radio;
                List<Map<String, Object>> options = new ArrayList<>();

                Map<String, Object> trueOption = new HashMap<>();
                trueOption.put("text", "True");
                trueOption.put("correct", isTrue);
                options.add(trueOption);

                Map<String, Object> falseOption = new HashMap<>();
                falseOption.put("text", "False");
                falseOption.put("correct", !isTrue);
                options.add(falseOption);

                questionData.put("options", options);
            }

            questions.add(questionData);
            addQuestionToView(questionData);
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addQuestionToView(Map<String, Object> questionData) {
        View questionView = LayoutInflater.from(this).inflate(R.layout.item_question, questionsContainer, false);

        TextView questionText = questionView.findViewById(R.id.question_text);
        TextView questionType = questionView.findViewById(R.id.question_type);
        ImageView removeQuestion = questionView.findViewById(R.id.remove_question);

        questionText.setText(questionData.get("text").toString());
        questionType.setText(questionData.get("type").toString());

        removeQuestion.setOnClickListener(v -> {
            questions.remove(questionData);
            questionsContainer.removeView(questionView);
        });

        questionsContainer.addView(questionView);
    }

    private void validateAndSubmit() {
        // Validate required fields
        if (titleEditText.getText().toString().trim().isEmpty()) {
            showError("Project title is required");
            return;
        }

        if (locationAutoComplete.getText().toString().trim().isEmpty()) {
            showError("Project location is required");
            return;
        }

        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            showError("Project description is required");
            return;
        }

        if (skillsChipGroup.getChildCount() == 0) {
            showError("At least one skill is required");
            return;
        }

        if (startDateEditText.getText().toString().isEmpty()) {
            showError("Start date is required");
            return;
        }

        if (deadlineEditText.getText().toString().isEmpty()) {
            showError("Application deadline is required");
            return;
        }

        try {
            Integer.parseInt(studentsEditText.getText().toString());
        } catch (NumberFormatException e) {
            showError("Please enter a valid number of students");
            return;
        }

        // Check if paid project has amount
        String compensationType = stipendAutoComplete.getText().toString();
        if (compensationType.equals("Paid")) {
            if (amountEditText.getText().toString().trim().isEmpty()) {
                showError("Please enter amount for paid project");
                return;
            }
        }

        // Create project data
        Map<String, Object> project = new HashMap<>();
        project.put("title", titleEditText.getText().toString().trim());
        project.put("description", descriptionEditText.getText().toString().trim());
        project.put("location", locationAutoComplete.getText().toString().trim());
        project.put("skills", getSkillsList());
        project.put("category", categoryAutoComplete.getText().toString());
        project.put("duration", durationAutoComplete.getText().toString());
        project.put("startDate", parseDateToTimestamp(startDateEditText.getText().toString()));
        project.put("deadline", parseDateToTimestamp(deadlineEditText.getText().toString()));
        project.put("studentsRequired", Integer.parseInt(studentsEditText.getText().toString()));
        project.put("educationLevel", educationAutoComplete.getText().toString());
        project.put("compensationType", compensationType);
        project.put("companyName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        project.put("createdAt", ServerValue.TIMESTAMP);
        project.put("status", "pending");
        project.put("applicants", 0);

        if (compensationType.equals("Paid")) {
            project.put("amount", Integer.parseInt(amountEditText.getText().toString()));
        }

        project.put("companyId", companyId);
        project.put("timestamp", System.currentTimeMillis());
        project.put("createdBy", companyId);

        // Add quiz if enabled
        SwitchMaterial quizToggle = findViewById(R.id.quiz_toggle);
        if (quizToggle.isChecked()) {
            if (!validateQuizFields()) return;

            Map<String, Object> quiz = new HashMap<>();
            quiz.put("title", quizTitleEditText.getText().toString());
            quiz.put("instructions", quizInstructionsEditText.getText().toString());
            quiz.put("timeLimit", Integer.parseInt(timeLimitEditText.getText().toString()));
            quiz.put("passingScore", Integer.parseInt(passingScoreEditText.getText().toString()));
            quiz.put("questions", questions);
            project.put("quiz", quiz);
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Publishing project...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Save to Firebase
        projectId = databaseReference.push().getKey(); // Set the class field
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "You must be logged in to publish a project", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(projectId).setValue(project)
                .addOnSuccessListener(aVoid -> {
                    // Send notification to admins after successful project creation
                    sendProjectNotificationToAdmins(project.get("title").toString());
                    progressDialog.dismiss();
                    Toast.makeText(this, "Project published successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to publish project: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("CreateProject", "Error publishing project", e);
                });
    }

    private boolean validateQuizFields() {
        if (quizTitleEditText.getText().toString().trim().isEmpty()) {
            showError("Quiz title is required");
            return false;
        }

        if (quizInstructionsEditText.getText().toString().trim().isEmpty()) {
            showError("Quiz instructions are required");
            return false;
        }

        try {
            Integer.parseInt(timeLimitEditText.getText().toString());
        } catch (NumberFormatException e) {
            showError("Please enter a valid time limit");
            return false;
        }

        try {
            int passingScore = Integer.parseInt(passingScoreEditText.getText().toString());
            if (passingScore < 0 || passingScore > 100) {
                showError("Passing score must be between 0-100");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid passing score");
            return false;
        }

        if (questions.isEmpty()) {
            showError("Please add at least one question");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private List<String> getSkillsList() {
        List<String> skills = new ArrayList<>();
        for (int i = 0; i < skillsChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) skillsChipGroup.getChildAt(i);
            skills.add(chip.getText().toString());
        }
        return skills;
    }

    private long parseDateToTimestamp(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            return sdf.parse(dateString).getTime();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }
    private void sendProjectNotificationToAdmins(String projectTitle) {
        // Get reference to admin announcements node
        DatabaseReference adminAnnouncementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role")
                .child("admin");

        // Create announcement ID
        String announcementId = adminAnnouncementsRef.push().getKey();
        if (announcementId == null) return;

        // Get company name from users node where role = company
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(companyId);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.child("role").getValue(String.class).equals("company")) {
                    String companyName = dataSnapshot.child("name").getValue(String.class);
                    if (companyName == null || companyName.isEmpty()) {
                        companyName = "Company";
                    }

                    // Create announcement data
                    Map<String, Object> announcementMap = new HashMap<>();
                    announcementMap.put("id", announcementId);
                    announcementMap.put("title", "New Project Created");
                    announcementMap.put("message", String.format("A new project \"%s\" has been created by %s and needs approval.",
                            projectTitle, companyName));
                    announcementMap.put("date", "2025-06-15 11:15:41"); // Current UTC time
                    announcementMap.put("timestamp", System.currentTimeMillis());
                    announcementMap.put("isRead", false);
                    announcementMap.put("createdBy", companyId);
                    announcementMap.put("type", "project_created");
                    announcementMap.put("projectId", projectId);
                    announcementMap.put("companyId", companyId);

                    // Save only to admin announcements node
                    adminAnnouncementsRef.child(announcementId).setValue(announcementMap)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("CreateProject", "Admin notification sent successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CreateProject", "Error sending admin notification", e);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CreateProject", "Error fetching company name", error.toException());
                // Create announcement with default company name
                createAndSendAnnouncement(announcementId, projectTitle, "Company", adminAnnouncementsRef);
            }
        });
    }

    private void createAndSendAnnouncement(String announcementId, String projectTitle, String companyName,
                                           DatabaseReference adminAnnouncementsRef) {
        Map<String, Object> announcementMap = new HashMap<>();
        announcementMap.put("id", announcementId);
        announcementMap.put("title", "New Project Created");
        announcementMap.put("message", String.format("A new project \"%s\" has been created by %s and needs approval.",
                projectTitle, companyName));
        announcementMap.put("date", "2025-06-15 11:15:41"); // Current UTC time
        announcementMap.put("timestamp", System.currentTimeMillis());
        announcementMap.put("isRead", false);
        announcementMap.put("createdBy", companyId);
        announcementMap.put("type", "project_created");
        announcementMap.put("projectId", projectId);
        announcementMap.put("companyId", companyId);

        adminAnnouncementsRef.child(announcementId).setValue(announcementMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CreateProject", "Admin notification sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateProject", "Error sending admin notification", e);
                });
    }
}
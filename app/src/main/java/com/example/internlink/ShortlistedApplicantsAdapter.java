package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShortlistedApplicantsAdapter extends RecyclerView.Adapter<ShortlistedApplicantsAdapter.ViewHolder> {

    private List<ShortlistedApplicant> applicants;
    private ScheduleActivity activity;
    private Context context;


    public ShortlistedApplicantsAdapter(List<ShortlistedApplicant> applicants, ScheduleActivity activity) {
        this.applicants = applicants;
        this.activity = activity;
        this.context = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortlisted_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortlistedApplicant applicant = applicants.get(position);
        holder.bind(applicant);
    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    public void updateData(List<ShortlistedApplicant> newApplicants) {
        this.applicants = newApplicants;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvDegree, tvProject;
        private TextView tvDate, tvTime, tvMode, tvLocation, tvNotes;
        private Chip chipStatus;
        private LinearLayout layoutNotes;
        private ImageView ivEditInterview;
        private MaterialButton btnEditInterview, btnChat, btnJoinInterview;
        private MaterialButton btnViewCV, btnMoreOptions;
        private LinearLayout layoutReschedulePrompt;
        private MaterialButton btnScheduleAgain, btnNoSchedule;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
        }

        private void initViews() {
            // Applicant info
            tvName = itemView.findViewById(R.id.tv_name);
            tvDegree = itemView.findViewById(R.id.tv_degree);
            tvProject = itemView.findViewById(R.id.tv_project);
            chipStatus = itemView.findViewById(R.id.chip_status);

            // Interview details
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvMode = itemView.findViewById(R.id.tv_mode);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            layoutNotes = itemView.findViewById(R.id.layout_notes);
            ivEditInterview = itemView.findViewById(R.id.iv_edit_interview);

            // Action buttons
            btnEditInterview = itemView.findViewById(R.id.btn_edit_interview);
            btnChat = itemView.findViewById(R.id.btn_chat);
            btnJoinInterview = itemView.findViewById(R.id.btn_join_interview);
            btnViewCV = itemView.findViewById(R.id.btn_view_cv);
            btnMoreOptions = itemView.findViewById(R.id.btn_more_options);

            layoutReschedulePrompt = itemView.findViewById(R.id.layout_reschedule_prompt);
            btnScheduleAgain = itemView.findViewById(R.id.btn_schedule_again);
            btnNoSchedule = itemView.findViewById(R.id.btn_no_schedule);

        }

        @SuppressLint("SetTextI18n")
        public void bind(ShortlistedApplicant applicant) {
            // Bind applicant info
            tvName.setText(applicant.getName());
            tvDegree.setText("🎓 " + applicant.getFormattedDegree());
            tvProject.setText("📁 " + applicant.getProjectTitle());

            // Bind interview details
            tvDate.setText(applicant.getInterviewDate() != null ? applicant.getInterviewDate() : "TBD");
            tvTime.setText(applicant.getInterviewTime() != null ? applicant.getInterviewTime() : "TBD");

            // Display interview type (Online/In-person)
            String interviewType = applicant.getInterviewMode();
            if (interviewType != null && !interviewType.equals("Not Set") && !interviewType.equals("Pending")) {
                tvMode.setText(interviewType);
            } else {
                tvMode.setText("Not Set");
            }

            // Display location based on interview type
            if ("In-person".equals(interviewType)) {
                tvLocation.setText(applicant.getInterviewLocation() != null ?
                        applicant.getInterviewLocation() : "Location TBD");
            } else if ("Online".equals(interviewType)) {
                if ("Zoom".equals(applicant.getInterviewMethod())) {
                    tvLocation.setText("Zoom Meeting");
                } else if ("Chat".equals(applicant.getInterviewMethod())) {
                    tvLocation.setText("Chat Interview");
                } else {
                    tvLocation.setText("Online");
                }
            } else {
                tvLocation.setText("Not Set");
            }

            // Handle notes visibility
            if (applicant.getInterviewNotes() != null && !applicant.getInterviewNotes().trim().isEmpty()) {
                layoutNotes.setVisibility(View.VISIBLE);
                tvNotes.setText(applicant.getInterviewNotes());
            } else {
                layoutNotes.setVisibility(View.GONE);
            }

            // Status chip
            String status = applicant.getInterviewStatus();
            String chipText;
            switch (status) {
                case "Scheduled":
                    chipText = "🟢 Scheduled";
                    break;
                case "Completed":
                    chipText = "✅ Completed";
                    break;
                case "Pending":
                default:
                    chipText = "🔵 Pending";
                    break;
            }
            chipStatus.setText(chipText);

            // Handle upcoming interviews highlight
            if (applicant.isUpcoming()) {
                itemView.setBackgroundResource(R.drawable.card_upcoming_interview);
            } else {
                itemView.setBackgroundResource(R.drawable.card_normal);
            }

            // Button states and text based on interview type
            configureChatAndJoinButtons(applicant);

            // Click listeners
            setupClickListeners(applicant);
        }

        private void configureChatAndJoinButtons(ShortlistedApplicant applicant) {
            String interviewType = applicant.getInterviewMode();
            String interviewMethod = applicant.getInterviewMethod();
            String interviewDate = applicant.getInterviewDate();
            String interviewTime = applicant.getInterviewTime();

            btnChat.setEnabled(false);
            btnJoinInterview.setEnabled(false);
            layoutReschedulePrompt.setVisibility(View.GONE);

            if ("Online".equals(interviewType) && interviewDate != null && interviewTime != null) {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
                    String fullDateTime = interviewDate + " " + interviewTime;
                    java.util.Date startTime = format.parse(fullDateTime);

                    if (startTime != null) {
                        long now = System.currentTimeMillis();
                        long interviewStart = startTime.getTime();
                        long interviewEnd = interviewStart + (5 * 60 * 1000); // 5 minutes

                        boolean isDuringInterview = now >= interviewStart && now <= interviewEnd;
                        boolean isAfterInterview = now > interviewEnd;

                        btnChat.setEnabled(isDuringInterview);
                        if ("Zoom".equals(interviewMethod)) {
                            String zoomLink = applicant.getInterviewLocation();
                            boolean validZoomLink = zoomLink != null && zoomLink.startsWith("http");
                            btnJoinInterview.setEnabled(isDuringInterview && validZoomLink);
                        }

                        if (isAfterInterview && "Scheduled".equals(applicant.getInterviewStatus())) {
                            layoutReschedulePrompt.setVisibility(View.VISIBLE);
                            setupReschedulePrompt(applicant);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("In-person".equals(interviewType)) {
                btnChat.setEnabled(true);
                btnJoinInterview.setEnabled(false);
            }
        }
        private void setupReschedulePrompt(ShortlistedApplicant applicant) {
            btnScheduleAgain.setOnClickListener(v -> {
                layoutReschedulePrompt.setVisibility(View.GONE);
                markInterviewCompleted(applicant); // Mark current interview as completed
                showScheduleNewInterviewDialog(applicant); // Show dialog for new interview
            });

            btnNoSchedule.setOnClickListener(v -> {
                layoutReschedulePrompt.setVisibility(View.GONE);
                markInterviewCompleted(applicant);
                // Optionally update status or leave as completed
            });
        }
        private void showScheduleNewInterviewDialog(ShortlistedApplicant applicant) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
            View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_schedule_interview, null);

            // Initialize dialog views
            TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
            TextView dialogApplicantName = dialogView.findViewById(R.id.dialog_applicant_name);
            EditText etInterviewDate = dialogView.findViewById(R.id.et_interview_date);
            EditText etInterviewTime = dialogView.findViewById(R.id.et_interview_time);
            android.widget.RadioGroup rgInterviewType = dialogView.findViewById(R.id.rg_interview_type);
            android.widget.RadioButton rbOnline = dialogView.findViewById(R.id.rb_online);
            android.widget.RadioButton rbInPerson = dialogView.findViewById(R.id.rb_in_person);
            LinearLayout llOnlineOptions = dialogView.findViewById(R.id.ll_online_options);
            LinearLayout llLocation = dialogView.findViewById(R.id.ll_location);
            android.widget.RadioGroup rgOnlineMethod = dialogView.findViewById(R.id.rg_online_method);
            android.widget.RadioButton rbChat = dialogView.findViewById(R.id.rb_chat);
            android.widget.RadioButton rbZoom = dialogView.findViewById(R.id.rb_zoom);
            LinearLayout llZoomLink = dialogView.findViewById(R.id.ll_zoom_link);
            EditText etZoomLink = dialogView.findViewById(R.id.et_zoom_link);
            EditText etLocation = dialogView.findViewById(R.id.et_location);
            android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
            androidx.appcompat.widget.AppCompatButton btnSchedule = dialogView.findViewById(R.id.btn_schedule);

            // Set dialog content
            dialogTitle.setText("Schedule New Interview");
            dialogApplicantName.setText("Schedule another interview with " + applicant.getName());

            // Setup date picker
            etInterviewDate.setOnClickListener(v -> {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                        activity,
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(year, month, dayOfMonth);
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault());
                            etInterviewDate.setText(sdf.format(calendar.getTime()));
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            });

            // Setup time picker
            etInterviewTime.setOnClickListener(v -> {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                        activity,
                        (view, hourOfDay, minute) -> {
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(java.util.Calendar.MINUTE, minute);
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                            etInterviewTime.setText(sdf.format(calendar.getTime()));
                        },
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        false
                );
                timePickerDialog.show();
            });

            // Setup interview type radio group
            rgInterviewType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_online) {
                    llOnlineOptions.setVisibility(View.VISIBLE);
                    llLocation.setVisibility(View.GONE);
                } else if (checkedId == R.id.rb_in_person) {
                    llOnlineOptions.setVisibility(View.GONE);
                    llLocation.setVisibility(View.VISIBLE);
                }
            });

            // Setup online method radio group
            rgOnlineMethod.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rb_zoom) {
                    llZoomLink.setVisibility(View.VISIBLE);
                } else {
                    llZoomLink.setVisibility(View.GONE);
                }
            });

            builder.setView(dialogView);
            android.app.AlertDialog dialog = builder.create();

            // Cancel button
            btnCancel.setOnClickListener(v -> dialog.dismiss());

            // Schedule button
            btnSchedule.setOnClickListener(v -> {
                String date = etInterviewDate.getText().toString().trim();
                String time = etInterviewTime.getText().toString().trim();

                if (date.isEmpty() || time.isEmpty()) {
                    android.widget.Toast.makeText(activity, "Please select date and time", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                String interviewType = "";
                String interviewMethod = "";
                String location = "";
                String zoomLink = "";

                // Get interview type
                if (rbOnline.isChecked()) {
                    interviewType = "Online";
                    if (rbChat.isChecked()) {
                        interviewMethod = "Chat";
                        location = "Chat";
                    } else if (rbZoom.isChecked()) {
                        interviewMethod = "Zoom";
                        zoomLink = etZoomLink.getText().toString().trim();
                        location = zoomLink;
                        if (zoomLink.isEmpty()) {
                            android.widget.Toast.makeText(activity, "Please enter Zoom meeting link", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                } else if (rbInPerson.isChecked()) {
                    interviewType = "In-person";
                    location = etLocation.getText().toString().trim();
                    if (location.isEmpty()) {
                        android.widget.Toast.makeText(activity, "Please enter interview location", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    android.widget.Toast.makeText(activity, "Please select interview type", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create new interview record
                scheduleNewInterview(applicant, date, time, interviewType, interviewMethod, location, zoomLink);
                dialog.dismiss();
            });

            dialog.show();
        }
        private void scheduleNewInterview(ShortlistedApplicant applicant, String date, String time,
                                          String interviewType, String interviewMethod, String location, String zoomLink) {

            // Create a new application entry for the additional interview
            com.google.firebase.database.DatabaseReference applicationsRef =
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("applications");

            String newApplicationId = applicationsRef.push().getKey();

            java.util.Map<String, Object> newInterviewData = new java.util.HashMap<>();
            newInterviewData.put("userId", applicant.getUserId());
            newInterviewData.put("projectId", applicant.getProjectId());
            newInterviewData.put("companyId", com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());
            newInterviewData.put("status", "Shortlisted");
            newInterviewData.put("interviewDate", date);
            newInterviewData.put("interviewTime", time);
            newInterviewData.put("interviewType", interviewType);
            newInterviewData.put("interviewMethod", interviewMethod);
            newInterviewData.put("interviewLocation", location);
            if (!zoomLink.isEmpty()) {
                newInterviewData.put("zoomLink", zoomLink);
            }
            newInterviewData.put("timestamp", System.currentTimeMillis());
            newInterviewData.put("lastUpdated", System.currentTimeMillis());
            newInterviewData.put("isAdditionalInterview", true); // Flag to identify additional interviews

            if (newApplicationId != null) {
                applicationsRef.child(newApplicationId).setValue(newInterviewData)
                        .addOnSuccessListener(aVoid -> {
                            android.widget.Toast.makeText(activity, "✅ New interview scheduled successfully!", android.widget.Toast.LENGTH_SHORT).show();

                            // Create announcement for the student
                            createNewInterviewAnnouncement(applicant, date, time, interviewType, location);

                            // Refresh the list to show the new interview
                            if (activity != null) {
                                activity.loadShortlistedApplicants();
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.widget.Toast.makeText(activity, "❌ Failed to schedule new interview", android.widget.Toast.LENGTH_SHORT).show();
                        });
            }
        }
        private void createNewInterviewAnnouncement(ShortlistedApplicant applicant, String date, String time,
                                                    String interviewType, String location) {
            com.google.firebase.database.DatabaseReference announcementsRef =
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("announcements_by_role").child("student");

            com.google.firebase.database.DatabaseReference companyRef =
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("users").child(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());

            companyRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot companySnapshot) {
                    String companyName = companySnapshot.child("name").getValue(String.class);

                    String message = "📅 A new interview has been scheduled for \"" + applicant.getProjectTitle() +
                            "\" with \"" + companyName + "\".\n\n" +
                            "📆 Date: " + date + "\n" +
                            "⏰ Time: " + time + "\n" +
                            "🌐 Mode: " + interviewType + "\n" +
                            "📍 Location: " + location + "\n\n[View Details]";

                    java.util.Map<String, Object> announceData = new java.util.HashMap<>();
                    announceData.put("title", "New Interview Scheduled");
                    announceData.put("message", message);
                    announceData.put("timestamp", System.currentTimeMillis());
                    announceData.put("applicant_status", "Shortlisted");
                    announceData.put("recipientId", applicant.getUserId());

                    announcementsRef.push().setValue(announceData);
                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
        }
        private void setupClickListeners(ShortlistedApplicant applicant) {
            // Edit interview (both icon and button)
            View.OnClickListener editListener = v -> activity.editInterview(applicant);
            ivEditInterview.setOnClickListener(editListener);
            btnEditInterview.setOnClickListener(editListener);

            // Chat button
            btnChat.setOnClickListener(v -> activity.startChat(applicant));

            // Join interview button - only for Zoom meetings
            btnJoinInterview.setOnClickListener(v -> {
                if ("Online".equals(applicant.getInterviewMode()) &&
                        "Zoom".equals(applicant.getInterviewMethod())) {
                    String zoomLink = applicant.getInterviewLocation();
                    if (zoomLink != null && zoomLink.startsWith("http")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(zoomLink));
                        context.startActivity(intent);
                    } else {
                        android.widget.Toast.makeText(context,
                                "Zoom link not available",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // View CV button
            btnViewCV.setOnClickListener(v -> activity.viewCV(applicant));

            // More options button
            btnMoreOptions.setOnClickListener(v -> showMoreOptionsMenu(applicant, v));
        }

        @SuppressLint("NonConstantResourceId")
        private void showMoreOptionsMenu(ShortlistedApplicant applicant, View anchorView) {
            PopupMenu popup = new PopupMenu(activity, anchorView);
            popup.getMenuInflater().inflate(R.menu.menu_shortlisted_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.menu_mark_completed) {
                    markInterviewCompleted(applicant);
                    return true;
                } else if (itemId == R.id.menu_send_reminder) {
                    sendInterviewReminder(applicant);
                    return true;
                } else if (itemId == R.id.menu_accept_applicant) {
                    acceptApplicant(applicant);
                    return true;
                } else if (itemId == R.id.menu_reject_applicant) {
                    rejectApplicant(applicant);
                    return true;
                } else if (itemId == R.id.menu_remove_shortlist) {
                    activity.removeApplicant(applicant);
                    return true;
                }

                return false;
            });

            popup.show();
        }

        private void markInterviewCompleted(ShortlistedApplicant applicant) {
            // Update interview status to completed
            applicant.setInterviewStatus("Completed");
            notifyItemChanged(getAdapterPosition());
            // You can also update Firebase here if needed
        }

        private void sendInterviewReminder(ShortlistedApplicant applicant) {
            // Get reference to announcements for students
            DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                    .getReference("announcements_by_role").child("student");

            // Get company name for the announcement
            DatabaseReference companyRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                    String companyName = companySnapshot.child("name").getValue(String.class);

                    // Create announcement message
                    String message = "🔔 Reminder: You have an upcoming interview for \"" + applicant.getProjectTitle() +
                            "\" with \"" + companyName + "\".\n\n" +
                            "📆 Date: " + applicant.getInterviewDate() + "\n" +
                            "⏰ Time: " + applicant.getInterviewTime() + "\n" +
                            "🌐 Mode: " + applicant.getInterviewMode() + "\n" +
                            "📍 Location: " + applicant.getInterviewLocation() + "\n\n[View Details]";

                    // Create announcement data
                    Map<String, Object> announceData = new HashMap<>();
                    announceData.put("title", "Interview Reminder");
                    announceData.put("message", message);
                    announceData.put("timestamp", System.currentTimeMillis());
                    announceData.put("applicant_status", "Shortlisted");
                    announceData.put("recipientId", applicant.getUserId());  // Include student's ID

                    // Save the announcement
                    announcementsRef.push().setValue(announceData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity,
                                        "✅ Reminder sent to " + applicant.getName(),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(activity,
                                        "❌ Failed to send reminder",
                                        Toast.LENGTH_SHORT).show();
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(activity,
                            "❌ Failed to send reminder",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void acceptApplicant(ShortlistedApplicant applicant) {
            // Update applicant status to accepted
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
            builder.setTitle("Accept Applicant")
                    .setMessage("Accept " + applicant.getName() + " for " + applicant.getProjectTitle() + "?")
                    .setPositiveButton("Accept", (dialog, which) -> {
                        // Update status in Firebase
                        updateApplicantStatus(applicant, "Accepted");
                        // Create acceptance announcement
                        createAcceptanceAnnouncement(applicant);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void createAcceptanceAnnouncement(ShortlistedApplicant applicant) {
            // Get reference to announcements for students
            DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                    .getReference("announcements_by_role").child("student");

            // Get company name for the announcement
            DatabaseReference companyRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                    String companyName = companySnapshot.child("name").getValue(String.class);

                    // Create announcement message
                    String message = "🎉 Congratulations! You have been accepted for \"" + applicant.getProjectTitle() +
                            "\" at \"" + companyName + "\".\n\n" +
                            "We look forward to having you join our team!\n\n" +
                            "[View Details]";

                    // Create announcement data
                    Map<String, Object> announceData = new HashMap<>();
                    announceData.put("title", "Application Accepted");
                    announceData.put("message", message);
                    announceData.put("timestamp", System.currentTimeMillis());
                    announceData.put("applicant_status", "Accepted");
                    announceData.put("recipientId", applicant.getUserId());  // Include student's ID

                    // Save the announcement
                    announcementsRef.push().setValue(announceData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity,
                                        "✅ Acceptance notification sent to " + applicant.getName(),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(activity,
                                        "❌ Failed to send acceptance notification",
                                        Toast.LENGTH_SHORT).show();
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(activity,
                            "❌ Failed to send acceptance notification",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void rejectApplicant(ShortlistedApplicant applicant) {
            // Update applicant status to rejected
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
            builder.setTitle("Reject Applicant")
                    .setMessage("Reject " + applicant.getName() + " for " + applicant.getProjectTitle() + "?")
                    .setPositiveButton("Reject", (dialog, which) -> {
                        // Update status in Firebase
                        updateApplicantStatus(applicant, "Rejected");
                        // Create rejection announcement
                        createRejectionAnnouncement(applicant);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void createRejectionAnnouncement(ShortlistedApplicant applicant) {
            // Get reference to announcements for students
            DatabaseReference announcementsRef = FirebaseDatabase.getInstance()
                    .getReference("announcements_by_role").child("student");

            // Get company name for the announcement
            DatabaseReference companyRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            companyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                    String companyName = companySnapshot.child("name").getValue(String.class);

                    // Create announcement message
                    String message = "We regret to inform you that your application for \"" + applicant.getProjectTitle() +
                            "\" at \"" + companyName + "\" was not successful.\n\n" +
                            "Thank you for your interest in our organization. We encourage you to apply for future opportunities.\n\n" +
                            "[View Details]";

                    // Create announcement data
                    Map<String, Object> announceData = new HashMap<>();
                    announceData.put("title", "Application Status Update");
                    announceData.put("message", message);
                    announceData.put("timestamp", System.currentTimeMillis());
                    announceData.put("applicant_status", "Rejected");
                    announceData.put("recipientId", applicant.getUserId());  // Include student's ID

                    // Save the announcement
                    announcementsRef.push().setValue(announceData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity,
                                        "✅ Status notification sent to " + applicant.getName(),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(activity,
                                        "❌ Failed to send status notification",
                                        Toast.LENGTH_SHORT).show();
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(activity,
                            "❌ Failed to send status notification",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void updateApplicantStatus(ShortlistedApplicant applicant, String newStatus) {
            com.google.firebase.database.DatabaseReference applicationRef =
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("applications")
                            .child(applicant.getApplicationId());

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("status", newStatus);
            updates.put("lastUpdated", System.currentTimeMillis());

            applicationRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        android.widget.Toast.makeText(activity,
                                applicant.getName() + " " + newStatus.toLowerCase(),
                                android.widget.Toast.LENGTH_SHORT).show();

                        // Remove from local list since they're no longer shortlisted
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            applicants.remove(position);
                            notifyItemRemoved(position);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.widget.Toast.makeText(activity,
                                "Failed to update status",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
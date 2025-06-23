package com.example.internlink;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

            // Check if interview needs to be automatically marked as completed
            checkAndMarkInterviewCompleted(applicant);

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

        private void checkAndMarkInterviewCompleted(ShortlistedApplicant applicant) {
            if (!"Scheduled".equals(applicant.getInterviewStatus())) {
                return; // Only process scheduled interviews
            }

            String interviewDate = applicant.getInterviewDate();
            String interviewTime = applicant.getInterviewTime();

            if (interviewDate == null || interviewTime == null) {
                return; // Can't process without date and time
            }

            try {
                SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
                String fullDateTime = interviewDate + " " + interviewTime;
                java.util.Date startTime = format.parse(fullDateTime);

                if (startTime != null) {
                    long now = System.currentTimeMillis();
                    long interviewEnd = startTime.getTime() + (5 * 60 * 1000); // 5 minutes after start time

                    if (now > interviewEnd) {
                        // It's been 5 minutes since the interview started, mark as completed
                        updateInterviewStatus(applicant, "Completed");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void updateInterviewStatus(ShortlistedApplicant applicant, String newStatus) {
            // Update the status in Firebase
            DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                    .getReference("applications")
                    .child(applicant.getApplicationId());

            Map<String, Object> updates = new HashMap<>();
            updates.put("interviewStatus", newStatus);
            updates.put("lastUpdated", System.currentTimeMillis());

            applicationRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Update the local model
                        applicant.setInterviewStatus(newStatus);
                        notifyItemChanged(getAdapterPosition());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity,
                                "Failed to update interview status",
                                Toast.LENGTH_SHORT).show();
                    });
        }

        private void configureChatAndJoinButtons(ShortlistedApplicant applicant) {
            String interviewType = applicant.getInterviewMode();
            String interviewMethod = applicant.getInterviewMethod();
            String interviewDate = applicant.getInterviewDate();
            String interviewTime = applicant.getInterviewTime();

            btnChat.setEnabled(false);
            btnJoinInterview.setEnabled(false);

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

                        btnChat.setEnabled(isDuringInterview);
                        if ("Zoom".equals(interviewMethod)) {
                            String zoomLink = applicant.getInterviewLocation();
                            boolean validZoomLink = zoomLink != null && zoomLink.startsWith("http");
                            btnJoinInterview.setEnabled(isDuringInterview && validZoomLink);
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

            // Update in Firebase
            DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                    .getReference("applications")
                    .child(applicant.getApplicationId());

            Map<String, Object> updates = new HashMap<>();
            updates.put("interviewStatus", "Completed");
            updates.put("lastUpdated", System.currentTimeMillis());

            applicationRef.updateChildren(updates)
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity,
                                "Failed to update interview status",
                                Toast.LENGTH_SHORT).show();
                    });
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
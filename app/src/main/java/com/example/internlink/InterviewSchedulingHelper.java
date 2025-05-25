package com.example.internlink;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class InterviewSchedulingHelper {

    private Context context;

    public InterviewSchedulingHelper(Context context) {
        this.context = context;
    }

    // Call this when an interview is scheduled (in your company interface)
    public void onInterviewScheduled(Application application) {
        InterviewReminderManager reminderManager = new InterviewReminderManager(context);
        reminderManager.scheduleInterviewReminder(application);

        Toast.makeText(context, "Interview scheduled! Reminder set for interview day.", Toast.LENGTH_SHORT).show();
    }

    // Call this when an interview is cancelled
    public void onInterviewCancelled(String applicationId) {
        InterviewReminderManager reminderManager = new InterviewReminderManager(context);
        reminderManager.cancelInterviewReminder(applicationId);

        Toast.makeText(context, "Interview reminder cancelled.", Toast.LENGTH_SHORT).show();
    }

    // Call this when an interview is rescheduled
    public void onInterviewRescheduled(Application application) {
        InterviewReminderManager reminderManager = new InterviewReminderManager(context);

        // Cancel old reminder
        reminderManager.cancelInterviewReminder(application.getApplicationId());

        // Schedule new reminder
        reminderManager.scheduleInterviewReminder(application);

        Toast.makeText(context, "Interview rescheduled! New reminder set.", Toast.LENGTH_SHORT).show();
    }

    // Complete method to schedule interview with Firebase update and reminder
    public void scheduleInterview(Application application, String date, String time) {
        // Update application with interview details
        Map<String, Object> updates = new HashMap<>();
        updates.put("interviewDate", date);
        updates.put("interviewTime", time);
        updates.put("status", "Shortlisted");
        updates.put("lastUpdated", System.currentTimeMillis());

        DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                .getReference("applications").child(application.getApplicationId());

        applicationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local application object
                    application.setInterviewDate(date);
                    application.setInterviewTime(time);
                    application.setStatus("Shortlisted");

                    // Schedule the reminder
                    onInterviewScheduled(application);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to schedule interview", Toast.LENGTH_SHORT).show();
                });
    }

    // Complete method to cancel interview with Firebase update and reminder cancellation
    public void cancelInterview(String applicationId) {
        // Cancel the reminder first
        onInterviewCancelled(applicationId);

        // Update application status
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Under Review");
        updates.put("interviewDate", null);
        updates.put("interviewTime", null);
        updates.put("lastUpdated", System.currentTimeMillis());

        DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                .getReference("applications").child(applicationId);

        applicationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Interview cancelled successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to cancel interview", Toast.LENGTH_SHORT).show();
                });
    }

    // Complete method to reschedule interview with Firebase update and reminder rescheduling
    public void rescheduleInterview(Application application, String newDate, String newTime) {
        InterviewReminderManager reminderManager = new InterviewReminderManager(context);

        // Cancel old reminder
        reminderManager.cancelInterviewReminder(application.getApplicationId());

        // Update application with new details
        Map<String, Object> updates = new HashMap<>();
        updates.put("interviewDate", newDate);
        updates.put("interviewTime", newTime);
        updates.put("lastUpdated", System.currentTimeMillis());

        DatabaseReference applicationRef = FirebaseDatabase.getInstance()
                .getReference("applications").child(application.getApplicationId());

        applicationRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local application object
                    application.setInterviewDate(newDate);
                    application.setInterviewTime(newTime);

                    // Schedule new reminder
                    reminderManager.scheduleInterviewReminder(application);

                    Toast.makeText(context, "Interview rescheduled! New reminder set.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to reschedule interview", Toast.LENGTH_SHORT).show();
                });
    }
}
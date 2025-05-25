package com.example.internlink;

import android.content. BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class InterviewReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.internlink.INTERVIEW_REMINDER".equals(intent.getAction())) {

            String applicationId = intent.getStringExtra("application_id");
            String projectTitle = intent.getStringExtra("project_title");
            String companyName = intent.getStringExtra("company_name");
            String studentName = intent.getStringExtra("student_name");
            String interviewTime = intent.getStringExtra("interview_time");
            String interviewDate = intent.getStringExtra("interview_date");
            String studentId = intent.getStringExtra("student_id");
            String companyId = intent.getStringExtra("company_id");

            Log.d("InterviewReminder", "Sending interview reminders for: " + projectTitle);

            // Send reminders to both student and company
            sendStudentReminder(studentId, projectTitle, companyName, interviewTime, interviewDate);
            sendCompanyReminder(companyId, studentName, projectTitle, interviewTime, interviewDate);
        }
    }

    private void sendStudentReminder(String studentId, String projectTitle, String companyName,
                                     String interviewTime, String interviewDate) {

        DatabaseReference studentAnnouncementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("student");

        String message = String.format(
                "🔔 Interview Reminder!\n\n" +
                        "Your interview for \"%s\" with \"%s\" is TODAY (%s) at %s.\n\n" +
                        "Good luck! Make sure to:\n" +
                        "✅ Be prepared and on time\n" +
                        "✅ Have your documents ready\n" +
                        "✅ Dress professionally\n" +
                        "✅ Review the project details\n\n" +
                        "[View Details]",
                projectTitle, companyName, interviewDate, interviewTime
        );

        String announcementId = studentAnnouncementsRef.push().getKey();
        if (announcementId != null) {
            Map<String, Object> announcement = new HashMap<>();
            announcement.put("title", "🎯 Interview Today!");
            announcement.put("message", message);
            announcement.put("timestamp", System.currentTimeMillis());
            announcement.put("recipientId", studentId);
            announcement.put("applicant_status", "Shortlisted");

            studentAnnouncementsRef.child(announcementId).setValue(announcement)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("InterviewReminder", "Student reminder sent successfully to: " + studentId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("InterviewReminder", "Failed to send student reminder", e);
                    });
        }
    }

    private void sendCompanyReminder(String companyId, String studentName, String projectTitle,
                                     String interviewTime, String interviewDate) {

        DatabaseReference companyAnnouncementsRef = FirebaseDatabase.getInstance()
                .getReference("announcements_by_role").child("company");

        String message = String.format(
                "🔔 Interview Reminder!\n\n" +
                        "Interview with %s for \"%s\" is scheduled TODAY (%s) at %s.\n\n" +
                        "Don't forget to:\n" +
                        "✅ Prepare interview questions\n" +
                        "✅ Review the candidate's application\n" +
                        "✅ Set up the meeting room/link\n" +
                        "✅ Have project details ready\n\n" +
                        "[View Applicants]",
                studentName, projectTitle, interviewDate, interviewTime
        );

        String announcementId = companyAnnouncementsRef.push().getKey();
        if (announcementId != null) {
            Map<String, Object> announcement = new HashMap<>();
            announcement.put("title", "📋 Interview Today!");
            announcement.put("message", message);
            announcement.put("timestamp", System.currentTimeMillis());
            announcement.put("recipientId", companyId);

            companyAnnouncementsRef.child(announcementId).setValue(announcement)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("InterviewReminder", "Company reminder sent successfully to: " + companyId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("InterviewReminder", "Failed to send company reminder", e);
                    });
        }
    }
}

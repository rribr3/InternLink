package com.example.internlink;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class InterviewReminderManager {

    private Context context;
    private DatabaseReference usersRef;
    private DatabaseReference projectsRef;

    public InterviewReminderManager(Context context) {
        this.context = context;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.usersRef = database.getReference("users");
        this.projectsRef = database.getReference("projects");
    }

    // Call this method whenever an interview is scheduled
    public void scheduleInterviewReminder(Application application) {
        // Get additional details needed for the reminder
        getProjectAndUserDetails(application);
    }

    private void getProjectAndUserDetails(Application application) {
        projectsRef.child(application.getProjectId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot projectSnapshot) {
                        String projectTitle = projectSnapshot.child("title").getValue(String.class);

                        // Get company name
                        usersRef.child(application.getCompanyId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot companySnapshot) {
                                        String companyName = companySnapshot.child("name").getValue(String.class);

                                        // Get student name
                                        usersRef.child(application.getUserId())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                                                        String studentName = studentSnapshot.child("name").getValue(String.class);

                                                        // Now schedule the actual reminder
                                                        scheduleAlarmForReminder(application, projectTitle, companyName, studentName);
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Log.e("InterviewReminder", "Failed to get student details");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("InterviewReminder", "Failed to get company details");
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("InterviewReminder", "Failed to get project details");
                    }
                });
    }

    private void scheduleAlarmForReminder(Application application, String projectTitle,
                                          String companyName, String studentName) {

        // Calculate when to send reminder (morning of interview day at 8 AM)
        long reminderTime = calculateReminderTime(application.getInterviewDate());

        if (reminderTime > System.currentTimeMillis()) {
            // Create intent for reminder broadcast receiver
            Intent intent = new Intent(context, InterviewReminderReceiver.class);
            intent.setAction("com.example.internlink.INTERVIEW_REMINDER");
            intent.putExtra("application_id", application.getApplicationId());
            intent.putExtra("project_title", projectTitle);
            intent.putExtra("company_name", companyName);
            intent.putExtra("student_name", studentName);
            intent.putExtra("interview_time", application.getInterviewTime());
            intent.putExtra("interview_date", application.getInterviewDate());
            intent.putExtra("student_id", application.getUserId());
            intent.putExtra("company_id", application.getCompanyId());

            // Use application ID hash as unique request code
            int requestCode = application.getApplicationId().hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // Schedule exact alarm for the reminder time
            if (alarmManager != null) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                    Log.d("InterviewReminder", "Interview reminder scheduled for: " + new Date(reminderTime));
                } catch (SecurityException e) {
                    Log.e("InterviewReminder", "Permission denied for exact alarm", e);
                    // Fallback to inexact alarm
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                }
            }
        }
    }

    // Cancel a scheduled reminder (when interview is cancelled/rescheduled)
    public void cancelInterviewReminder(String applicationId) {
        Intent intent = new Intent(context, InterviewReminderReceiver.class);
        intent.setAction("com.example.internlink.INTERVIEW_REMINDER");

        int requestCode = applicationId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("InterviewReminder", "Interview reminder cancelled for application: " + applicationId);
        }
    }

    private long calculateReminderTime(String interviewDate) {
        try {
            // Parse the interview date (format: "May 25, 2025")
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date date = sdf.parse(interviewDate);

            if (date != null) {
                Calendar reminderCalendar = Calendar.getInstance();
                reminderCalendar.setTime(date);

                // Set reminder time to 8:00 AM on the interview date
                reminderCalendar.set(Calendar.HOUR_OF_DAY, 8);
                reminderCalendar.set(Calendar.MINUTE, 0);
                reminderCalendar.set(Calendar.SECOND, 0);
                reminderCalendar.set(Calendar.MILLISECOND, 0);

                return reminderCalendar.getTimeInMillis();
            }
        } catch (ParseException e) {
            Log.e("InterviewReminder", "Failed to parse interview date: " + interviewDate, e);
        }

        return 0;
    }
}
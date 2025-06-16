package com.example.internlink;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudentScheduleActivity extends AppCompatActivity {

    private RecyclerView calendarRecyclerView;
    private TextView monthYearText;
    private CalendarAdapter calendarAdapter;
    private Calendar calendar;
    private List<InterviewEvent> interviews;
    private DatabaseReference applicationsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_schedule);

        initializeViews();
        setupFirebase();
        setupCalendar();
        loadInterviewData();
        checkUpcomingInterviews();
    }

    private void checkUpcomingInterviews() {
        // Check for interviews in the next 24 hours
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String tomorrowStr = dateFormat.format(tomorrow.getTime());
        String todayStr = dateFormat.format(new Date());

        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait for data to load
                runOnUiThread(() -> {
                    List<InterviewEvent> upcomingInterviews = new ArrayList<>();

                    for (InterviewEvent interview : interviews) {
                        if (interview.date.equals(todayStr) || interview.date.equals(tomorrowStr)) {
                            upcomingInterviews.add(interview);
                        }
                    }

                    if (!upcomingInterviews.isEmpty()) {
                        showUpcomingInterviewsNotification(upcomingInterviews);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showUpcomingInterviewsNotification(List<InterviewEvent> upcomingInterviews) {
        StringBuilder message = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String todayStr = dateFormat.format(new Date());

        List<InterviewEvent> todayInterviews = new ArrayList<>();
        List<InterviewEvent> tomorrowInterviews = new ArrayList<>();

        for (InterviewEvent interview : upcomingInterviews) {
            if (interview.date.equals(todayStr)) {
                todayInterviews.add(interview);
            } else {
                tomorrowInterviews.add(interview);
            }
        }

        if (!todayInterviews.isEmpty()) {
            message.append("🚨 TODAY:\n");
            for (InterviewEvent interview : todayInterviews) {
                message.append("• ").append(interview.time).append(" - ")
                        .append(interview.companyName != null ? interview.companyName : "Company")
                        .append("\n");
            }
            message.append("\n");
        }

        if (!tomorrowInterviews.isEmpty()) {
            message.append("📅 TOMORROW:\n");
            for (InterviewEvent interview : tomorrowInterviews) {
                message.append("• ").append(interview.time).append(" - ")
                        .append(interview.companyName != null ? interview.companyName : "Company")
                        .append("\n");
            }
        }

        if (message.length() > 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("⏰ Upcoming Interviews")
                    .setMessage(message.toString().trim())
                    .setPositiveButton("Got it!", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    private void initializeViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthYearText = findViewById(R.id.monthYearText);

        findViewById(R.id.prevMonthBtn).setOnClickListener(v -> navigateMonth(-1));
        findViewById(R.id.nextMonthBtn).setOnClickListener(v -> navigateMonth(1));
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        interviews = new ArrayList<>();
    }

    private void setupCalendar() {
        calendar = Calendar.getInstance();
        calendarAdapter = new CalendarAdapter();
        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        calendarRecyclerView.setAdapter(calendarAdapter);
        updateCalendarDisplay();
    }

    private void navigateMonth(int direction) {
        calendar.add(Calendar.MONTH, direction);
        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthYearText.setText(monthFormat.format(calendar.getTime()));
        calendarAdapter.updateCalendar(calendar, interviews);
    }

    private void loadInterviewData() {
        applicationsRef.orderByChild("userId").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        interviews.clear();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String status = snapshot.child("status").getValue(String.class);

                            if ("Shortlisted".equals(status)) {
                                String interviewDate = snapshot.child("interviewDate").getValue(String.class);
                                String interviewTime = snapshot.child("interviewTime").getValue(String.class);
                                String interviewType = snapshot.child("interviewType").getValue(String.class);
                                String interviewMethod = snapshot.child("interviewMethod").getValue(String.class);
                                String interviewLocation = snapshot.child("interviewLocation").getValue(String.class);
                                String projectId = snapshot.child("projectId").getValue(String.class);
                                String companyId = snapshot.child("companyId").getValue(String.class);
                                String zoomLink = snapshot.child("zoomLink").getValue(String.class);

                                if (interviewDate != null && interviewTime != null) {
                                    InterviewEvent event = new InterviewEvent();
                                    event.date = interviewDate;
                                    event.time = interviewTime;
                                    event.type = interviewType != null ? interviewType : "Online";
                                    event.method = interviewMethod != null ? interviewMethod : "Chat";
                                    event.location = interviewLocation != null ? interviewLocation : "";
                                    event.zoomLink = zoomLink != null ? zoomLink : "";
                                    event.projectId = projectId;
                                    event.companyId = companyId;

                                    // Load additional project and company details
                                    loadProjectDetails(event);
                                    interviews.add(event);
                                }
                            }
                        }

                        updateCalendarDisplay();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(StudentScheduleActivity.this,
                                "Error loading interviews: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProjectDetails(InterviewEvent event) {
        // Load project title
        FirebaseDatabase.getInstance().getReference("projects")
                .child(event.projectId).child("title")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        event.projectTitle = snapshot.getValue(String.class);
                        calendarAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });

        // Load company name
        FirebaseDatabase.getInstance().getReference("users")
                .child(event.companyId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        event.companyName = snapshot.getValue(String.class);
                        calendarAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // Calendar Adapter Class
    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
        private List<CalendarDay> days = new ArrayList<>();

        public void updateCalendar(Calendar cal, List<InterviewEvent> interviews) {
            days.clear();

            Calendar tempCal = (Calendar) cal.clone();
            tempCal.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

            // Add empty days for previous month
            for (int i = 0; i < firstDayOfWeek; i++) {
                days.add(new CalendarDay(0, false, new ArrayList<>()));
            }

            // Add days of current month
            for (int day = 1; day <= daysInMonth; day++) {
                List<InterviewEvent> dayInterviews = getInterviewsForDay(day, tempCal, interviews);
                boolean hasInterview = !dayInterviews.isEmpty();
                days.add(new CalendarDay(day, hasInterview, dayInterviews));
            }

            notifyDataSetChanged();
        }

        private List<InterviewEvent> getInterviewsForDay(int day, Calendar cal, List<InterviewEvent> interviews) {
            List<InterviewEvent> dayInterviews = new ArrayList<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

            cal.set(Calendar.DAY_OF_MONTH, day);
            String targetDate = dateFormat.format(cal.getTime());

            for (InterviewEvent interview : interviews) {
                try {
                    Date interviewDate = dateFormat.parse(interview.date);
                    Date targetDateObj = dateFormat.parse(targetDate);

                    if (interviewDate != null && targetDateObj != null &&
                            isSameDay(interviewDate, targetDateObj)) {
                        dayInterviews.add(interview);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            return dayInterviews;
        }

        private boolean isSameDay(Date date1, Date date2) {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

        @Override
        public CalendarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_calendar_day, parent, false);
            return new CalendarViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CalendarViewHolder holder, int position) {
            CalendarDay day = days.get(position);
            holder.bind(day);
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class CalendarViewHolder extends RecyclerView.ViewHolder {
            TextView dayNumber;
            View interviewIndicator;

            CalendarViewHolder(View itemView) {
                super(itemView);
                dayNumber = itemView.findViewById(R.id.dayNumber);
                interviewIndicator = itemView.findViewById(R.id.interviewIndicator);

                itemView.setOnClickListener(v -> {
                    CalendarDay day = days.get(getAdapterPosition());
                    if (day.hasInterview) {
                        showInterviewDetails(day.interviews);
                    }
                });
            }

            void bind(CalendarDay day) {
                if (day.dayNumber == 0) {
                    dayNumber.setText("");
                    dayNumber.setVisibility(View.INVISIBLE);
                    interviewIndicator.setVisibility(View.GONE);
                    itemView.setBackgroundResource(0);
                } else {
                    dayNumber.setText(String.valueOf(day.dayNumber));
                    dayNumber.setVisibility(View.VISIBLE);

                    // Check if this is today
                    Calendar today = Calendar.getInstance();
                    boolean isToday = (day.dayNumber == today.get(Calendar.DAY_OF_MONTH) &&
                            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR));

                    if (day.hasInterview) {
                        interviewIndicator.setVisibility(View.VISIBLE);

                        // Different colors for different interview urgency
                        if (isToday) {
                            interviewIndicator.setBackgroundColor(Color.parseColor("#FF5722")); // Red for today
                            dayNumber.setTextColor(Color.parseColor("#D32F2F"));
                        } else {
                            interviewIndicator.setBackgroundColor(Color.parseColor("#4CAF50")); // Green for future
                            dayNumber.setTextColor(Color.parseColor("#2E7D32"));
                        }
                        dayNumber.setTypeface(null, android.graphics.Typeface.BOLD);

                        // Multiple interviews indicator
                        if (day.interviews.size() > 1) {
                            interviewIndicator.setBackgroundResource(R.drawable.multiple_interviews_indicator);
                        }
                    } else {
                        interviewIndicator.setVisibility(View.GONE);
                        dayNumber.setTextColor(isToday ? Color.parseColor("#2196F3") : Color.parseColor("#333333"));
                        dayNumber.setTypeface(null, isToday ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                    }

                    // Highlight today
                    if (isToday) {
                        itemView.setBackgroundResource(R.drawable.today_background);
                    } else if (day.hasInterview) {
                        itemView.setBackgroundResource(R.drawable.interview_day_background);
                    } else {
                        itemView.setBackgroundResource(0);
                    }
                }
            }
        }
    }

    private void showInterviewDetails(List<InterviewEvent> interviews) {
        if (interviews.size() == 1) {
            showSingleInterviewDialog(interviews.get(0));
        } else {
            showMultipleInterviewsDialog(interviews);
        }
    }

    private void showSingleInterviewDialog(InterviewEvent interview) {
        StringBuilder details = new StringBuilder();
        details.append("📅 Date: ").append(interview.date).append("\n");
        details.append("🕐 Time: ").append(interview.time).append("\n");
        details.append("🏢 Project: ").append(interview.projectTitle != null ? interview.projectTitle : "Loading...").append("\n");
        details.append("🏛️ Company: ").append(interview.companyName != null ? interview.companyName : "Loading...").append("\n");
        details.append("💻 Type: ").append(interview.type).append(" (").append(interview.method).append(")\n");

        if ("In-person".equals(interview.type) && !interview.location.isEmpty()) {
            details.append("📍 Location: ").append(interview.location).append("\n");
        }

        if ("Online".equals(interview.type) && "Zoom".equals(interview.method) && !interview.zoomLink.isEmpty()) {
            details.append("🔗 Zoom Link: ").append(interview.zoomLink).append("\n");
        }

        // Check if it's the interview day and within time range
        boolean isInterviewTime = isInterviewTimeNow(interview);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("📋 Interview Details")
                .setMessage(details.toString())
                .setPositiveButton("✓ Got it", null);

        // Add conditional buttons based on interview type and timing
        if (isInterviewTime) {
            // Interview is happening now or very soon
            if ("Online".equals(interview.type) && "Zoom".equals(interview.method) && !interview.zoomLink.isEmpty()) {
                // Online Zoom interview: Show both chat and zoom buttons
                builder.setNeutralButton("🔗 Join Zoom", (dialog, which) -> {
                    openZoomLink(interview.zoomLink);
                });
                builder.setNegativeButton("💬 Chat with Company", (dialog, which) -> {
                    openChatWithCompany(interview);
                });
            } else if ("Online".equals(interview.type) && "Messages".equalsIgnoreCase(interview.method)) {
                // Online Messages/Chat only interview: Show chat button
                builder.setNegativeButton("💬 Start Chat Interview", (dialog, which) -> {
                    openChatWithCompany(interview);
                });
            } else if ("In-person".equals(interview.type)) {
                // In-person interview: Show chat button for coordination
                builder.setNegativeButton("💬 Chat with Company", (dialog, which) -> {
                    openChatWithCompany(interview);
                });
            }
        } else {
            // Interview is not happening now: Show calendar add button
            builder.setNegativeButton("📅 Add to Calendar", (dialog, which) -> {
                addToCalendar(interview);
            });

            // Always show chat option for coordination
            builder.setNeutralButton("💬 Chat with Company", (dialog, which) -> {
                openChatWithCompany(interview);
            });
        }

        builder.show();
    }

    private boolean isInterviewTimeNow(InterviewEvent interview) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            // Check if it's the interview date
            Date interviewDate = dateFormat.parse(interview.date);
            Date today = new Date();

            Calendar interviewCal = Calendar.getInstance();
            interviewCal.setTime(interviewDate);

            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);

            boolean isSameDay = (interviewCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    interviewCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR));

            if (!isSameDay) {
                return false;
            }

            // Check if it's within the interview time window (30 minutes before to 2 hours after)
            Date interviewTime = timeFormat.parse(interview.time);
            Calendar interviewTimeCal = Calendar.getInstance();
            interviewTimeCal.setTime(interviewTime);

            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            int interviewHour = interviewTimeCal.get(Calendar.HOUR_OF_DAY);
            int interviewMinute = interviewTimeCal.get(Calendar.MINUTE);

            // Convert to minutes for easier comparison
            int currentTotalMinutes = currentHour * 60 + currentMinute;
            int interviewTotalMinutes = interviewHour * 60 + interviewMinute;

            // Allow access 30 minutes before to 2 hours (120 minutes) after
            return (currentTotalMinutes >= interviewTotalMinutes - 30 &&
                    currentTotalMinutes <= interviewTotalMinutes + 120);

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openZoomLink(String zoomLink) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(zoomLink));

            // Try to open with Zoom app first
            intent.setPackage("us.zoom.videomeetings");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to browser if Zoom app is not installed
                intent.setPackage(null);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening Zoom link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatWithCompany(InterviewEvent interview) {
        if (interview.companyId == null || interview.companyName == null) {
            Toast.makeText(this, "Company information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent chatIntent = new Intent(this, ChatActivity.class);
        chatIntent.putExtra("CHAT_WITH_ID", interview.companyId);
        chatIntent.putExtra("CHAT_WITH_NAME", interview.companyName);
        startActivity(chatIntent);
    }

    private void showMultipleInterviewsDialog(List<InterviewEvent> interviews) {
        StringBuilder details = new StringBuilder();
        details.append("You have ").append(interviews.size()).append(" interviews scheduled:\n\n");

        for (int i = 0; i < interviews.size(); i++) {
            InterviewEvent interview = interviews.get(i);
            details.append("Interview ").append(i + 1).append(":\n");
            details.append("🏢 ").append(interview.companyName != null ? interview.companyName : "Loading...").append("\n");
            details.append("📋 ").append(interview.projectTitle != null ? interview.projectTitle : "Loading...").append("\n");
            details.append("🕐 ").append(interview.time).append("\n");
            details.append("💻 ").append(interview.type).append(" (").append(interview.method).append(")\n");

            if (i < interviews.size() - 1) {
                details.append("\n");
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📅 Multiple Interviews")
                .setMessage(details.toString())
                .setPositiveButton("✓ Got it", null)
                .show();
    }

    private void addToCalendar(InterviewEvent interview) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date interviewDate = dateFormat.parse(interview.date);
            Date interviewTime = timeFormat.parse(interview.time);

            Calendar cal = Calendar.getInstance();
            cal.setTime(interviewDate);

            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(interviewTime);

            cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));

            // Create calendar intent
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal.getTimeInMillis())
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, cal.getTimeInMillis() + (60 * 60 * 1000)) // 1 hour duration
                    .putExtra(android.provider.CalendarContract.Events.TITLE, "Interview: " + (interview.projectTitle != null ? interview.projectTitle : "Project"))
                    .putExtra(android.provider.CalendarContract.Events.DESCRIPTION,
                            "Company: " + (interview.companyName != null ? interview.companyName : "Loading...") + "\n" +
                                    "Type: " + interview.type + " (" + interview.method + ")\n" +
                                    ("In-person".equals(interview.type) && !interview.location.isEmpty() ? "Location: " + interview.location : "") +
                                    ("Online".equals(interview.type) && !interview.zoomLink.isEmpty() ? "Zoom: " + interview.zoomLink : ""))
                    .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION,
                            "In-person".equals(interview.type) ? interview.location : "Online");

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error adding to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper Classes
    private static class CalendarDay {
        int dayNumber;
        boolean hasInterview;
        List<InterviewEvent> interviews;

        CalendarDay(int dayNumber, boolean hasInterview, List<InterviewEvent> interviews) {
            this.dayNumber = dayNumber;
            this.hasInterview = hasInterview;
            this.interviews = interviews;
        }
    }

    private static class InterviewEvent {
        String date;
        String time;
        String type;
        String method;
        String location;
        String zoomLink;
        String projectId;
        String companyId;
        String projectTitle;
        String companyName;
    }
}
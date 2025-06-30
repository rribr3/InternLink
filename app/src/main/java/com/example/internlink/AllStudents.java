package com.example.internlink;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllStudents extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private EditText searchEditText;
    private StudentAdapter studentAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Lists for search functionality
    private List<User> originalStudentList = new ArrayList<>();
    private List<User> filteredStudentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_students);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupSwipeRefresh();
        setupToolbar();
        setupSearchFunctionality();
        loadStudentsData();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rv_all_students);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        searchEditText = findViewById(R.id.message_search);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with empty list
        studentAdapter = new StudentAdapter(this, filteredStudentList);
        recyclerView.setAdapter(studentAdapter);
    }
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            // Set custom colors for the refresh indicator
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.blue_500,
                    R.color.green,
                    R.color.red,
                    R.color.yellow
            );

            // Set the listener for refresh action
            swipeRefreshLayout.setOnRefreshListener(this::refreshStudentsData);
        }
    }

    private void refreshStudentsData() {
        // Clear existing data
        originalStudentList.clear();
        filteredStudentList.clear();

        // If we have an adapter, notify it about the cleared data
        if (studentAdapter != null) {
            studentAdapter.notifyDataSetChanged();
        }

        // Then load fresh data
        loadStudentsData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        if (toolbar != null) {

            // Enable back button
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

            // Set navigation click listener
            toolbar.setNavigationOnClickListener(v -> {
                // Handle back button click
                onBackPressed();
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Clear search if active, otherwise go back
        if (searchEditText != null && !searchEditText.getText().toString().trim().isEmpty()) {
            clearSearch();
        } else {
            super.onBackPressed();
            finish();
        }
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void loadStudentsData() {
        String companyId = getIntent().getStringExtra("COMPANY_ID");
        if (companyId == null) {
            showEmptyState();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        applicationsRef.orderByChild("companyId").equalTo(companyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot appsSnapshot) {
                        Set<String> acceptedStudentIds = new HashSet<>();
                        for (DataSnapshot app : appsSnapshot.getChildren()) {
                            String status = app.child("status").getValue(String.class);
                            String studentId = app.child("userId").getValue(String.class);
                            if ("Accepted".equalsIgnoreCase(status) && studentId != null) {
                                acceptedStudentIds.add(studentId);
                            }
                        }

                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                                originalStudentList.clear();

                                for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                                    String uid = userSnap.getKey();
                                    String role = userSnap.child("role").getValue(String.class);

                                    if ("student".equals(role) && acceptedStudentIds.contains(uid)) {
                                        User student = userSnap.getValue(User.class);
                                        if (student != null) {
                                            student.setUid(uid);
                                            originalStudentList.add(student);
                                        }
                                    }
                                }

                                // Initialize filtered list with all students
                                filterStudents("");
                                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                showEmptyState();
                                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        showEmptyState();
                        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
    }

    /**
     * Enhanced search function with comprehensive character-level matching
     */
    private void filterStudents(String query) {
        filteredStudentList.clear();

        // If query is empty, show all students
        if (query.trim().isEmpty()) {
            filteredStudentList.addAll(originalStudentList);
        } else {
            // Enhanced search - search through multiple fields with character-level matching
            String searchQuery = query.toLowerCase().trim();

            for (User student : originalStudentList) {
                if (matchesSearchQuery(student, searchQuery)) {
                    filteredStudentList.add(student);
                }
            }
        }

        // Update UI
        runOnUiThread(() -> {
            studentAdapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private boolean matchesSearchQuery(User student, String searchQuery) {
        // Search in student name - character by character matching
        String name = student.getName() != null ? student.getName().toLowerCase() : "";
        if (containsAllCharacters(name, searchQuery)) {
            return true;
        }

        // Search in email - character by character matching
        String email = student.getEmail() != null ? student.getEmail().toLowerCase() : "";
        if (containsAllCharacters(email, searchQuery)) {
            return true;
        }

        // Search in university - character by character matching
        String university = student.getUniversity() != null ? student.getUniversity().toLowerCase() : "";
        if (containsAllCharacters(university, searchQuery)) {
            return true;
        }

        // Search in field of study - character by character matching
        String fieldOfStudy = student.getFieldOfStudy() != null ? student.getFieldOfStudy().toLowerCase() : "";
        if (containsAllCharacters(fieldOfStudy, searchQuery)) {
            return true;
        }

        // Search in graduation year - character by character matching
        String graduationYear = student.getGraduationYear() != null ? student.getGraduationYear().toLowerCase() : "";
        if (containsAllCharacters(graduationYear, searchQuery)) {
            return true;
        }

        // Search in phone number - character by character matching
        String phone = student.getPhone() != null ? student.getPhone().toLowerCase() : "";
        if (containsAllCharacters(phone, searchQuery)) {
            return true;
        }

        // Search in bio/description - character by character matching
        String bio = student.getBio() != null ? student.getBio().toLowerCase() : "";
        if (containsAllCharacters(bio, searchQuery)) {
            return true;
        }

        // Search in skills if available
        if (student.getSkills() != null) {
            for (String skill : student.getSkills()) {
                String skillLower = skill != null ? skill.toLowerCase() : "";
                if (containsAllCharacters(skillLower, searchQuery)) {
                    return true;
                }
            }
        }

        // Search in location/address
        String location = student.getLocation() != null ? student.getLocation().toLowerCase() : "";
        if (containsAllCharacters(location, searchQuery)) {
            return true;
        }

        // Add special search terms (exact matches for these)
        if (searchQuery.equals("student")) {
            return true; // All users here are students
        }

        // Additional character-based search for any combination
        // Create a combined searchable string with all student data
        String combinedText = (name + " " +
                email + " " +
                university + " " +
                fieldOfStudy + " " +
                graduationYear + " " +
                phone + " " +
                bio + " " +
                location).toLowerCase();

        return containsAllCharacters(combinedText, searchQuery);
    }

    /**
     * Enhanced character-level search that matches all characters in the query
     * against the target string, allowing for non-consecutive matches
     */
    private boolean containsAllCharacters(String target, String query) {
        if (target == null || query == null || query.isEmpty()) {
            return query == null || query.isEmpty();
        }

        // First check for direct substring match (fastest)
        if (target.contains(query)) {
            return true;
        }

        // Then check for character sequence matching (allows for gaps)
        return containsCharacterSequence(target, query);
    }

    /**
     * Checks if target contains all characters from query in the same order,
     * but not necessarily consecutive (fuzzy matching)
     */
    private boolean containsCharacterSequence(String target, String query) {
        if (target == null || query == null) {
            return false;
        }

        int targetIndex = 0;
        int queryIndex = 0;

        while (targetIndex < target.length() && queryIndex < query.length()) {
            if (target.charAt(targetIndex) == query.charAt(queryIndex)) {
                queryIndex++;
            }
            targetIndex++;
        }

        // Return true if we've matched all characters in the query
        return queryIndex == query.length();
    }

    /**
     * Alternative method for exact character matching (every character must be present)
     */
    private boolean containsAllCharactersExact(String target, String query) {
        if (target == null || query == null) {
            return false;
        }

        // Convert to char arrays for faster processing
        char[] targetChars = target.toCharArray();
        char[] queryChars = query.toCharArray();

        // Count character frequencies in target
        Map<Character, Integer> targetCharCount = new HashMap<>();
        for (char c : targetChars) {
            targetCharCount.put(c, targetCharCount.getOrDefault(c, 0) + 1);
        }

        // Check if target contains enough of each character from query
        Map<Character, Integer> queryCharCount = new HashMap<>();
        for (char c : queryChars) {
            queryCharCount.put(c, queryCharCount.getOrDefault(c, 0) + 1);
        }

        for (Map.Entry<Character, Integer> entry : queryCharCount.entrySet()) {
            char queryChar = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = targetCharCount.getOrDefault(queryChar, 0);

            if (availableCount < requiredCount) {
                return false;
            }
        }

        return true;
    }

    /**
     * Alternative fuzzy search method using edit distance (Levenshtein distance)
     * This can be used for even more flexible matching
     */
    private boolean fuzzyMatch(String target, String query, int maxDistance) {
        if (target == null || query == null) {
            return false;
        }

        target = target.toLowerCase();
        query = query.toLowerCase();

        // Simple implementation - you can optimize this further
        int[][] dp = new int[target.length() + 1][query.length() + 1];

        for (int i = 0; i <= target.length(); i++) {
            for (int j = 0; j <= query.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (target.charAt(i - 1) == query.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }

        return dp[target.length()][query.length()] <= maxDistance;
    }

    private void updateEmptyState() {
        if (filteredStudentList.isEmpty()) {
            String searchQuery = searchEditText.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                // No students at all
                showEmptyState();
            } else {
                // No search results
                showNoSearchResults();
            }
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showNoSearchResults() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        // You might want to update the empty state text to indicate no search results
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Clear search and show all students
     */
    public void clearSearch() {
        searchEditText.setText("");
        searchEditText.clearFocus();

        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (originalStudentList.isEmpty()) {
            loadStudentsData();
        }
    }
}
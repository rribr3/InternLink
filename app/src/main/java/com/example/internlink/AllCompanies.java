package com.example.internlink;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class AllCompanies extends AppCompatActivity {

    private String studentId;
    private DatabaseReference applicationsRef;
    private DatabaseReference chatsRef;
    private DatabaseReference usersRef;

    // Search related variables
    private EditText etSearch;
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private CompanyAdapter companyAdapter;
    private List<User> allCompaniesList = new ArrayList<>();
    private List<User> originalCompaniesList = new ArrayList<>();
    private ImageView btnClearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_companies);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        studentId = getIntent().getStringExtra("STUDENT_ID");

        // Initialize views
        initializeViews();

        // Initialize Firebase references
        applicationsRef = FirebaseDatabase.getInstance().getReference("applications");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadAcceptedCompaniesWithoutChat();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.message_search);
        recyclerView = findViewById(R.id.rv_all_companies);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCompanies(s.toString());
                // Show/hide clear button based on search text
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup clear search button
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
            // Hide keyboard
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        });

        // Setup back navigation
        findViewById(R.id.topAppBar).setOnClickListener(v -> onBackPressed());
    }

    /**
     * Enhanced search function with comprehensive character-level matching
     */
    private void filterCompanies(String query) {
        List<User> filteredList = new ArrayList<>();

        // If query is empty, show all companies
        if (query.trim().isEmpty()) {
            filteredList.addAll(originalCompaniesList);
        } else {
            // Enhanced search - search through multiple fields with character-level matching
            String searchQuery = query.toLowerCase().trim();

            for (User company : originalCompaniesList) {
                boolean matches = false;

                // Search in company name - character by character matching
                String name = company.getName() != null ? company.getName().toLowerCase() : "";
                if (containsAllCharacters(name, searchQuery)) {
                    matches = true;
                }

                // Search in company email
                if (!matches) {
                    String email = company.getEmail() != null ? company.getEmail().toLowerCase() : "";
                    if (containsAllCharacters(email, searchQuery)) {
                        matches = true;
                    }
                }

                // Search in company location/address
                if (!matches) {
                    String location = company.getLocation() != null ? company.getLocation().toLowerCase() : "";
                    if (containsAllCharacters(location, searchQuery)) {
                        matches = true;
                    }
                }

                // Search in company description
                if (!matches) {
                    String description = company.getDescription() != null ? company.getDescription().toLowerCase() : "";
                    if (containsAllCharacters(description, searchQuery)) {
                        matches = true;
                    }
                }

                // Search in company industry/category
                if (!matches) {
                    String industry = company.getIndustry() != null ? company.getIndustry().toLowerCase() : "";
                    if (containsAllCharacters(industry, searchQuery)) {
                        matches = true;
                    }
                }

                // Combined search - search across all fields
                if (!matches) {
                    String combinedText = (name + " " +
                            (company.getEmail() != null ? company.getEmail() : "") + " " +
                            (company.getLocation() != null ? company.getLocation() : "") + " " +
                            (company.getDescription() != null ? company.getDescription() : "") + " " +
                            (company.getIndustry() != null ? company.getIndustry() : "")).toLowerCase();
                    if (containsAllCharacters(combinedText, searchQuery)) {
                        matches = true;
                    }
                }

                if (matches) {
                    filteredList.add(company);
                }
            }
        }

        // Update adapter with filtered results
        if (companyAdapter != null) {
            companyAdapter.updateList(filteredList);
        }

        // Update empty state
        updateEmptyState(filteredList.isEmpty(), !query.trim().isEmpty());
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

    private void updateEmptyState(boolean isEmpty, boolean isSearching) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadAcceptedCompaniesWithoutChat() {
        applicationsRef.orderByChild("userId").equalTo(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot appsSnapshot) {
                        Set<String> acceptedCompanyIds = new HashSet<>();

                        for (DataSnapshot app : appsSnapshot.getChildren()) {
                            String status = app.child("status").getValue(String.class);
                            String companyId = app.child("companyId").getValue(String.class);

                            if ("Accepted".equalsIgnoreCase(status) && companyId != null) {
                                acceptedCompanyIds.add(companyId);
                            }
                        }

                        loadChattedCompaniesThenFilter(acceptedCompanyIds);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadChattedCompaniesThenFilter(Set<String> acceptedCompanyIds) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {
                Set<String> chattedCompanyIds = new HashSet<>();
                for (DataSnapshot chat : chatsSnapshot.getChildren()) {
                    String user1 = chat.child("user1").getValue(String.class);
                    String user2 = chat.child("user2").getValue(String.class);

                    if ((studentId.equals(user1) || studentId.equals(user2))) {
                        String otherUser = studentId.equals(user1) ? user2 : user1;
                        chattedCompanyIds.add(otherUser);
                    }
                }

                Set<String> finalCompanyIds = new HashSet<>();
                for (String companyId : acceptedCompanyIds) {
                    if (!chattedCompanyIds.contains(companyId)) {
                        finalCompanyIds.add(companyId);
                    }
                }

                loadCompanyDetails(finalCompanyIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCompanyDetails(Set<String> companyIds) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                List<User> companyList = new ArrayList<>();
                for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                    String role = userSnap.child("role").getValue(String.class);
                    String uid = userSnap.getKey();

                    if ("company".equals(role) && companyIds.contains(uid)) {
                        User company = userSnap.getValue(User.class);
                        if (company != null) {
                            company.setUid(uid); // ✅ Add UID from Firebase key
                            companyList.add(company);
                        }
                    }
                }

                // Store original list for search functionality
                allCompaniesList.clear();
                allCompaniesList.addAll(companyList);

                originalCompaniesList.clear();
                originalCompaniesList.addAll(companyList);

                // Setup adapter
                companyAdapter = new CompanyAdapter(AllCompanies.this, allCompaniesList);
                recyclerView.setAdapter(companyAdapter);

                // Update empty state
                updateEmptyState(companyList.isEmpty(), false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
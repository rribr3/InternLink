package com.example.internlink;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompanyMessagesActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private static final String TAG = "CompanyMessagesActivity";

    // Filter options enum
    private enum SortOrder {
        NEWEST_FIRST,
        OLDEST_FIRST,
        NAME_ASC,
        NAME_DESC,
        UNREAD_FIRST
    }

    private RecyclerView rvConversations;
    private LinearLayout tvEmptyState;
    private ConversationsAdapter conversationsAdapter;
    private List<Conversation> conversationsList = new ArrayList<>();
    private List<Conversation> originalConversationsList = new ArrayList<>(); // Store original list

    private String currentUserId;
    private DatabaseReference chatMetadataRef, usersRef, chatsRef, notificationsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;
    private LinearLayout tabActive, tabArchive;
    private TextView tabActiveText, tabArchiveText;
    private View tabActiveIndicator, tabArchiveIndicator;
    private RecyclerView rvArchivedConversations;
    private LinearLayout tvEmptyStateArchived;
    private ConversationsAdapter archivedConversationsAdapter;
    private List<Conversation> archivedConversationsList = new ArrayList<>();
    private List<Conversation> originalArchivedConversationsList = new ArrayList<>(); // Store original archived list
    private boolean isShowingActiveTab = true;

    // Filter related variables
    private ImageView filterIcon;
    private SortOrder currentSortOrder = SortOrder.NEWEST_FIRST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_messages);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        FloatingActionButton fabNewMessage = findViewById(R.id.fab_new_message);
        fabNewMessage.setVisibility(View.VISIBLE);
        fabNewMessage.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyMessagesActivity.this, AllStudents.class);
            intent.putExtra("COMPANY_ID", currentUserId);
            startActivity(intent);
        });

        setupRecyclerView();
        setupSwipeGestures();
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        etSearch = findViewById(R.id.message_search);
        ImageView menuIcon = findViewById(R.id.menu_icon);
        tabActive = findViewById(R.id.tab_active);
        tabArchive = findViewById(R.id.tab_archive);
        tabActiveText = findViewById(R.id.tab_active_text);
        tabArchiveText = findViewById(R.id.tab_archive_text);
        tabActiveIndicator = findViewById(R.id.tab_active_indicator);
        tabArchiveIndicator = findViewById(R.id.tab_archive_indicator);
        rvArchivedConversations = findViewById(R.id.archived_messages_recycler_view);
        tvEmptyStateArchived = findViewById(R.id.tv_empty_state_archive);
        filterIcon = findViewById(R.id.ic_filter);

        tabActive.setOnClickListener(v -> showActiveTab());
        tabArchive.setOnClickListener(v -> showArchiveTab());

        swipeRefreshLayout.setOnRefreshListener(this::loadConversations);
        menuIcon.setOnClickListener(v -> onBackPressed());

        // Setup filter icon click listener
        filterIcon.setOnClickListener(v -> showFilterDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showFilterDialog() {
        // Create a custom menu-style popup
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, filterIcon);

        // Add menu items
        popupMenu.getMenu().add(0, 1, 0, " Newest First");
        popupMenu.getMenu().add(0, 2, 1, " Oldest First");
        popupMenu.getMenu().add(0, 3, 2, " Name A-Z");
        popupMenu.getMenu().add(0, 4, 3, " Name Z-A");
        popupMenu.getMenu().add(0, 5, 4, " Unread First");

        // Mark current selection with checkmark
        android.view.Menu menu = popupMenu.getMenu();
        switch (currentSortOrder) {
            case NEWEST_FIRST:
                menu.findItem(1).setTitle("✓ Newest First");
                break;
            case OLDEST_FIRST:
                menu.findItem(2).setTitle("✓ Oldest First");
                break;
            case NAME_ASC:
                menu.findItem(3).setTitle("✓ Name A-Z");
                break;
            case NAME_DESC:
                menu.findItem(4).setTitle("✓ Name Z-A");
                break;
            case UNREAD_FIRST:
                menu.findItem(5).setTitle("✓ Unread First");
                break;
        }

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    if (currentSortOrder != SortOrder.NEWEST_FIRST) {
                        currentSortOrder = SortOrder.NEWEST_FIRST;
                        applySortOrder();
                        Toast.makeText(this, "Sorted by newest first", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 2:
                    if (currentSortOrder != SortOrder.OLDEST_FIRST) {
                        currentSortOrder = SortOrder.OLDEST_FIRST;
                        applySortOrder();
                        Toast.makeText(this, "Sorted by oldest first", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 3:
                    if (currentSortOrder != SortOrder.NAME_ASC) {
                        currentSortOrder = SortOrder.NAME_ASC;
                        applySortOrder();
                        Toast.makeText(this, "Sorted by name A-Z", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 4:
                    if (currentSortOrder != SortOrder.NAME_DESC) {
                        currentSortOrder = SortOrder.NAME_DESC;
                        applySortOrder();
                        Toast.makeText(this, "Sorted by name Z-A", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 5:
                    if (currentSortOrder != SortOrder.UNREAD_FIRST) {
                        currentSortOrder = SortOrder.UNREAD_FIRST;
                        applySortOrder();
                        Toast.makeText(this, "Sorted by unread first", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case 6:
                    // Clear search
                    etSearch.setText("");
                    etSearch.clearFocus();
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    }
                    Toast.makeText(this, "Search cleared", Toast.LENGTH_SHORT).show();
                    return true;
                default:
                    return false;
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void applySortOrder() {
        // Sort both original lists
        applySortOrderToList(originalConversationsList);
        applySortOrderToList(originalArchivedConversationsList);

        // Update current displayed lists from original lists
        conversationsList.clear();
        conversationsList.addAll(originalConversationsList);

        archivedConversationsList.clear();
        archivedConversationsList.addAll(originalArchivedConversationsList);

        // Update adapters
        runOnUiThread(() -> {
            conversationsAdapter.notifyDataSetChanged();
            archivedConversationsAdapter.notifyDataSetChanged();
        });

        // Apply search filter if there's text in search box
        String searchQuery = etSearch.getText().toString();
        if (!searchQuery.isEmpty()) {
            filterConversations(searchQuery);
        }
    }

    private void applySortOrderToList(List<Conversation> list) {
        switch (currentSortOrder) {
            case NEWEST_FIRST:
                Collections.sort(list, (c1, c2) ->
                        Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                break;
            case OLDEST_FIRST:
                Collections.sort(list, (c1, c2) ->
                        Long.compare(c1.getLastMessageTime(), c2.getLastMessageTime()));
                break;
            case NAME_ASC:
                Collections.sort(list, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case NAME_DESC:
                Collections.sort(list, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                break;
            case UNREAD_FIRST:
                Collections.sort(list, (c1, c2) -> {
                    // First sort by unread count (higher first), then by time (newer first)
                    int unreadCompare = Integer.compare(c2.getUnreadCount(), c1.getUnreadCount());
                    if (unreadCompare != 0) return unreadCompare;
                    return Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime());
                });
                break;
        }
    }

    /**
     * Enhanced search function with comprehensive character-level matching
     */
    private void filterConversations(String query) {
        List<Conversation> filteredActiveList = new ArrayList<>();
        List<Conversation> filteredArchivedList = new ArrayList<>();

        // If query is empty, show all conversations with current sort order
        if (query.trim().isEmpty()) {
            filteredActiveList.addAll(originalConversationsList);
            filteredArchivedList.addAll(originalArchivedConversationsList);
        } else {
            // Enhanced search - search through multiple fields with character-level matching
            String searchQuery = query.toLowerCase().trim();

            // Filter active conversations
            for (Conversation convo : originalConversationsList) {
                if (matchesSearchQuery(convo, searchQuery)) {
                    filteredActiveList.add(convo);
                }
            }

            // Filter archived conversations
            for (Conversation convo : originalArchivedConversationsList) {
                if (matchesSearchQuery(convo, searchQuery)) {
                    filteredArchivedList.add(convo);
                }
            }
        }

        // Apply current sort order to filtered lists
        applySortOrderToList(filteredActiveList);
        applySortOrderToList(filteredArchivedList);

        // Update displayed lists
        conversationsList.clear();
        conversationsList.addAll(filteredActiveList);

        archivedConversationsList.clear();
        archivedConversationsList.addAll(filteredArchivedList);

        // Update adapters
        conversationsAdapter.notifyDataSetChanged();
        archivedConversationsAdapter.notifyDataSetChanged();

        // Update empty state visibility
        updateEmptyStates();
    }

    private boolean matchesSearchQuery(Conversation convo, String searchQuery) {
        // Search in user name - character by character matching
        String name = convo.getOtherUserName() != null ? convo.getOtherUserName().toLowerCase() : "";
        if (containsAllCharacters(name, searchQuery)) {
            return true;
        }

        // Search in project title - character by character matching
        String project = convo.getProjectTitle() != null ? convo.getProjectTitle().toLowerCase() : "";
        if (containsAllCharacters(project, searchQuery)) {
            return true;
        }

        // Search in last message content - character by character matching
        String lastMessage = convo.getLastMessage() != null ? convo.getLastMessage().toLowerCase() : "";
        if (containsAllCharacters(lastMessage, searchQuery)) {
            return true;
        }

        // Search in user role - character by character matching
        String role = convo.getOtherUserRole() != null ? convo.getOtherUserRole().toLowerCase() : "";
        if (containsAllCharacters(role, searchQuery)) {
            return true;
        }

        // Add special search terms (exact matches for these)
        if (searchQuery.equals("unread") && convo.getUnreadCount() > 0) {
            return true;
        } else if (searchQuery.equals("student") && "student".equals(convo.getOtherUserRole())) {
            return true;
        } else if (searchQuery.equals("company") && "company".equals(convo.getOtherUserRole())) {
            return true;
        } else if (searchQuery.equals("typing") && convo.isTyping()) {
            return true;
        }

        // Additional character-based search for any combination
        // Create a combined searchable string with all conversation data
        String combinedText = (name + " " +
                (convo.getProjectTitle() != null ? convo.getProjectTitle() : "") + " " +
                (convo.getLastMessage() != null ? convo.getLastMessage() : "") + " " +
                (convo.getOtherUserRole() != null ? convo.getOtherUserRole() : "")).toLowerCase();

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

    private void insertIntoSortedList(List<Conversation> list, Conversation conversation) {
        int insertPosition = 0;

        switch (currentSortOrder) {
            case NEWEST_FIRST:
                // Find position where this conversation should go (newest first)
                for (int i = 0; i < list.size(); i++) {
                    if (conversation.getLastMessageTime() > list.get(i).getLastMessageTime()) {
                        insertPosition = i;
                        break;
                    }
                    insertPosition = i + 1;
                }
                break;
            case OLDEST_FIRST:
                // Find position where this conversation should go (oldest first)
                for (int i = 0; i < list.size(); i++) {
                    if (conversation.getLastMessageTime() < list.get(i).getLastMessageTime()) {
                        insertPosition = i;
                        break;
                    }
                    insertPosition = i + 1;
                }
                break;
            case NAME_ASC:
                String convName = conversation.getOtherUserName() != null ? conversation.getOtherUserName() : "";
                for (int i = 0; i < list.size(); i++) {
                    String listName = list.get(i).getOtherUserName() != null ? list.get(i).getOtherUserName() : "";
                    if (convName.compareToIgnoreCase(listName) < 0) {
                        insertPosition = i;
                        break;
                    }
                    insertPosition = i + 1;
                }
                break;
            case NAME_DESC:
                String convNameDesc = conversation.getOtherUserName() != null ? conversation.getOtherUserName() : "";
                for (int i = 0; i < list.size(); i++) {
                    String listNameDesc = list.get(i).getOtherUserName() != null ? list.get(i).getOtherUserName() : "";
                    if (convNameDesc.compareToIgnoreCase(listNameDesc) > 0) {
                        insertPosition = i;
                        break;
                    }
                    insertPosition = i + 1;
                }
                break;
            case UNREAD_FIRST:
                for (int i = 0; i < list.size(); i++) {
                    int unreadCompare = Integer.compare(conversation.getUnreadCount(), list.get(i).getUnreadCount());
                    if (unreadCompare > 0) {
                        insertPosition = i;
                        break;
                    } else if (unreadCompare == 0 && conversation.getLastMessageTime() > list.get(i).getLastMessageTime()) {
                        insertPosition = i;
                        break;
                    }
                    insertPosition = i + 1;
                }
                break;
        }

        list.add(insertPosition, conversation);
    }

    private void setupSwipeGestures() {
        setupSwipeGestureForRecyclerView(rvConversations, conversationsList, conversationsAdapter, false);
        setupSwipeGestureForRecyclerView(rvArchivedConversations, archivedConversationsList, archivedConversationsAdapter, true);
    }

    private void setupSwipeGestureForRecyclerView(RecyclerView recyclerView, List<Conversation> list, ConversationsAdapter adapter, boolean isArchived) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Conversation conversation = list.get(position);
                showSwipeActionDialog(conversation, position, isArchived);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().clearView(foregroundView);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showSwipeActionDialog(Conversation conversation, int position, boolean isFromArchived) {
        String archiveAction = isFromArchived ? "Unarchive" : "Archive";

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(new CharSequence[]{archiveAction, "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        if (isFromArchived) {
                            unarchiveConversation(conversation, position);
                        } else {
                            archiveConversation(conversation, position);
                        }
                    } else {
                        deleteConversation(conversation, position, isFromArchived);
                    }
                })
                .setOnDismissListener(dialog -> {
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                }).show();
    }

    private void archiveConversation(Conversation conversation, int position) {
        chatMetadataRef.child(conversation.getChatId()).child("archivedBy").child(currentUserId).setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation archived", Toast.LENGTH_SHORT).show();
                    conversation.setArchived(true);

                    // Update original lists
                    originalConversationsList.remove(conversation);
                    originalArchivedConversationsList.add(conversation);

                    // Move from active to archived list
                    conversationsList.remove(position);

                    // Insert into archived list based on current sort order
                    insertIntoSortedList(archivedConversationsList, conversation);

                    // Update UI
                    conversationsAdapter.notifyItemRemoved(position);
                    archivedConversationsAdapter.notifyDataSetChanged();
                    updateEmptyStates();

                    // Re-apply search if there's an active search
                    String searchQuery = etSearch.getText().toString();
                    if (!searchQuery.isEmpty()) {
                        filterConversations(searchQuery);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to archive", Toast.LENGTH_SHORT).show();
                    conversationsAdapter.notifyItemChanged(position);
                });
    }

    private void unarchiveConversation(Conversation conversation, int position) {
        chatMetadataRef.child(conversation.getChatId()).child("archivedBy").child(currentUserId).removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation unarchived", Toast.LENGTH_SHORT).show();
                    conversation.setArchived(false);

                    // Update original lists
                    originalArchivedConversationsList.remove(conversation);
                    originalConversationsList.add(conversation);

                    // Move from archived to active list
                    archivedConversationsList.remove(position);

                    // Insert into active list based on current sort order
                    insertIntoSortedList(conversationsList, conversation);

                    // Update UI
                    archivedConversationsAdapter.notifyItemRemoved(position);
                    conversationsAdapter.notifyDataSetChanged();
                    updateEmptyStates();

                    // Re-apply search if there's an active search
                    String searchQuery = etSearch.getText().toString();
                    if (!searchQuery.isEmpty()) {
                        filterConversations(searchQuery);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to unarchive", Toast.LENGTH_SHORT).show();
                    archivedConversationsAdapter.notifyItemChanged(position);
                });
    }

    private void deleteConversation(Conversation conversation, int position, boolean isFromArchived) {
        chatMetadataRef.child(conversation.getChatId()).child("deletedBy").child(currentUserId).setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();

                    // Update original lists
                    if (isFromArchived) {
                        originalArchivedConversationsList.remove(conversation);
                        archivedConversationsList.remove(position);
                        archivedConversationsAdapter.notifyItemRemoved(position);
                    } else {
                        originalConversationsList.remove(conversation);
                        conversationsList.remove(position);
                        conversationsAdapter.notifyItemRemoved(position);
                    }

                    updateEmptyStates();

                    // Re-apply search if there's an active search
                    String searchQuery = etSearch.getText().toString();
                    if (!searchQuery.isEmpty()) {
                        filterConversations(searchQuery);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                });
    }

    private int findInsertPosition(Conversation convo) {
        for (int i = 0; i < conversationsList.size(); i++) {
            if (convo.getLastMessageTime() > conversationsList.get(i).getLastMessageTime()) {
                return i;
            }
        }
        return conversationsList.size();
    }

    private void showActiveTab() {
        isShowingActiveTab = true;
        rvConversations.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
        rvArchivedConversations.setVisibility(View.GONE);
        tvEmptyStateArchived.setVisibility(View.GONE);
        tabActiveText.setTextColor(getResources().getColor(R.color.blue));
        tabActiveIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
        tabArchiveText.setTextColor(Color.parseColor("#808080"));
        tabArchiveIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void showArchiveTab() {
        isShowingActiveTab = false;
        rvConversations.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        rvArchivedConversations.setVisibility(View.VISIBLE);
        tvEmptyStateArchived.setVisibility(archivedConversationsList.isEmpty() ? View.VISIBLE : View.GONE);
        tabArchiveText.setTextColor(getResources().getColor(R.color.blue));
        tabArchiveIndicator.setBackgroundColor(getResources().getColor(R.color.blue));
        tabActiveText.setTextColor(Color.parseColor("#808080"));
        tabActiveIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void setupRecyclerView() {
        conversationsAdapter = new ConversationsAdapter(conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationsAdapter);

        archivedConversationsAdapter = new ConversationsAdapter(archivedConversationsList, this);
        rvArchivedConversations.setLayoutManager(new LinearLayoutManager(this));
        rvArchivedConversations.setAdapter(archivedConversationsAdapter);
    }

    private void setupFirebaseReferences() {
        chatMetadataRef = FirebaseDatabase.getInstance().getReference("chat_metadata");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }

    private void loadConversations() {
        chatMetadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationsList.clear();
                archivedConversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                    if (Boolean.TRUE.equals(deleted)) {
                        continue; // Skip this conversation
                    }

                    DataSnapshot participantsSnapshot = chatSnapshot.child("participants");

                    if (participantsSnapshot.hasChild(currentUserId)) {
                        String otherUserId = null;
                        for (DataSnapshot participant : participantsSnapshot.getChildren()) {
                            String participantId = participant.getKey();
                            if (!participantId.equals(currentUserId)) {
                                otherUserId = participantId;
                                break;
                            }
                        }

                        if (otherUserId != null) {
                            Conversation conversation = new Conversation();
                            Boolean archived = chatSnapshot.child("archivedBy").child(currentUserId).getValue(Boolean.class);
                            conversation.setArchived(Boolean.TRUE.equals(archived));

                            if (conversation.isArchived()) {
                                archivedConversationsList.add(conversation);
                            } else {
                                conversationsList.add(conversation);
                            }

                            conversation.setChatId(chatId);
                            conversation.setOtherUserId(otherUserId);
                            conversation.setLastMessage(chatSnapshot.child("lastMessage").getValue(String.class));
                            Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);
                            conversation.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);
                            conversation.setLastSenderId(chatSnapshot.child("lastSenderId").getValue(String.class));
                            calculateUnreadCount(conversation, otherUserId);
                            processedChatIds.add(chatId);
                        }
                    }
                }

                loadDirectChats(processedChatIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDirectChats(new HashSet<>());
            }
        });
    }

    private void updateEmptyStates() {
        runOnUiThread(() -> {
            if (isShowingActiveTab) {
                tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                tvEmptyStateArchived.setVisibility(archivedConversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadDirectChats(Set<String> alreadyProcessed) {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                    if (Boolean.TRUE.equals(deleted)) {
                        continue; // Skip this conversation
                    }

                    if (alreadyProcessed.contains(chatId)) continue;
                    if (chatId != null && chatId.contains(currentUserId)) {
                        String otherUserId = getOtherUserIdFromChatId(chatId);
                        if (otherUserId == null) continue;

                        DataSnapshot messagesSnapshot = chatSnapshot.child("messages");
                        if (!messagesSnapshot.exists()) continue;

                        Conversation conversation = new Conversation();
                        conversation.setChatId(chatId);
                        conversation.setOtherUserId(otherUserId);
                        conversation.setLastMessage("No messages yet");
                        conversation.setLastMessageTime(0L);
                        conversation.setArchived(false); // Direct chats default to not archived

                        // Get the last message from this chat
                        DataSnapshot lastMessageSnapshot = null;
                        for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                            lastMessageSnapshot = messageSnapshot;
                        }

                        if (lastMessageSnapshot != null) {
                            String msgText = lastMessageSnapshot.child("text").getValue(String.class);
                            String messageType = lastMessageSnapshot.child("messageType").getValue(String.class);
                            String fileName = lastMessageSnapshot.child("fileName").getValue(String.class);
                            Long timestamp = lastMessageSnapshot.child("timestamp").getValue(Long.class);

                            conversation.setLastMessageTime(timestamp != null ? timestamp : 0);
                            conversation.setLastSenderId(lastMessageSnapshot.child("senderId").getValue(String.class));

                            // Format message based on type
                            if ("image".equals(messageType)) {
                                conversation.setLastMessage("📷 Photo");
                            } else if ("file".equals(messageType)) {
                                conversation.setLastMessage("📎 " + (fileName != null ? fileName : "File"));
                            } else {
                                conversation.setLastMessage(msgText != null ? msgText : "No messages yet");
                            }
                        }

                        calculateUnreadCount(conversation, otherUserId);
                        conversationsList.add(conversation);
                    }
                }

                // Finish loading and apply sort order
                finishLoadingConversations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                finishLoadingConversations();
            }
        });
    }

    private void finishLoadingConversations() {
        // Store original lists before applying any filters or sorts
        originalConversationsList.clear();
        originalConversationsList.addAll(conversationsList);

        originalArchivedConversationsList.clear();
        originalArchivedConversationsList.addAll(archivedConversationsList);

        // Sort conversations by time initially
        Collections.sort(originalConversationsList, (c1, c2) ->
                Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
        Collections.sort(originalArchivedConversationsList, (c1, c2) ->
                Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

        // Apply current sort order
        applySortOrder();

        // Load user details
        loadUserDetails();
    }

    private String getOtherUserIdFromChatId(String chatId) {
        if (chatId == null || currentUserId == null) return null;

        String[] ids = chatId.split("_");
        if (ids.length == 2) {
            return ids[0].equals(currentUserId) ? ids[1] : ids[0];
        }
        return null;
    }

    private void calculateUnreadCount(Conversation conversation, String otherUserId) {
        notificationsRef.child(currentUserId).orderByChild("chatId").equalTo(conversation.getChatId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unread = 0;
                        for (DataSnapshot notif : snapshot.getChildren()) {
                            Boolean read = notif.child("read").getValue(Boolean.class);
                            String senderId = notif.child("senderId").getValue(String.class);
                            if (!Boolean.TRUE.equals(read) && otherUserId.equals(senderId)) {
                                unread++;
                            }
                        }
                        conversation.setUnreadCount(unread);

                        runOnUiThread(() -> {
                            conversationsAdapter.notifyDataSetChanged();
                            archivedConversationsAdapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to calculate unread count", error.toException());
                    }
                });
    }

    private void loadUserDetails() {
        List<Conversation> all = new ArrayList<>();
        all.addAll(conversationsList);
        all.addAll(archivedConversationsList);

        if (all.isEmpty()) {
            updateEmptyStates();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        final int totalConversations = all.size();
        final int[] loadedCount = {0};

        for (Conversation conversation : all) {
            usersRef.child(conversation.getOtherUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        conversation.setOtherUserName(snapshot.child("name").getValue(String.class));
                        conversation.setOtherUserRole(snapshot.child("role").getValue(String.class));
                        conversation.setOtherUserLogoUrl(snapshot.child("logoUrl").getValue(String.class));

                        // Add typing listener
                        DatabaseReference typingRef = FirebaseDatabase.getInstance()
                                .getReference("user_status")
                                .child(conversation.getOtherUserId())
                                .child("isTyping");

                        typingRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Boolean typing = snapshot.getValue(Boolean.class);
                                conversation.setTyping(Boolean.TRUE.equals(typing));
                                conversationsAdapter.notifyDataSetChanged();
                                archivedConversationsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });

                        // Load project information if this is a student
                        if ("student".equals(conversation.getOtherUserRole())) {
                            loadProjectInfoForConversation(conversation);
                        }
                    } else {
                        // User doesn't exist, set default values
                        conversation.setOtherUserName("Unknown User");
                        conversation.setOtherUserRole("user");
                        Log.w(TAG, "User not found: " + conversation.getOtherUserId());
                    }

                    loadedCount[0]++;
                    if (loadedCount[0] == totalConversations) {
                        runOnUiThread(() -> {
                            // Update original lists after user details are loaded
                            originalConversationsList.clear();
                            originalConversationsList.addAll(conversationsList);

                            originalArchivedConversationsList.clear();
                            originalArchivedConversationsList.addAll(archivedConversationsList);

                            updateEmptyStates();
                            conversationsAdapter.notifyDataSetChanged();
                            archivedConversationsAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user details for " + conversation.getOtherUserId(), error.toException());
                    conversation.setOtherUserName("Unknown User");
                    conversation.setOtherUserRole("user");

                    loadedCount[0]++;
                    if (loadedCount[0] == totalConversations) {
                        runOnUiThread(() -> {
                            updateEmptyStates();
                            conversationsAdapter.notifyDataSetChanged();
                            archivedConversationsAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }
            });
        }
    }

    private void loadProjectInfoForConversation(Conversation conversation) {
        // Look for applications between current company and this student
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("companyId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String userId = appSnapshot.child("userId").getValue(String.class);

                            if (conversation.getOtherUserId().equals(userId)) {
                                String projectId = appSnapshot.child("projectId").getValue(String.class);
                                String applicationId = appSnapshot.getKey();

                                conversation.setProjectId(projectId);
                                conversation.setApplicationId(applicationId);

                                Log.d(TAG, "Found project info for conversation: " + projectId);

                                // Load project title
                                if (projectId != null) {
                                    loadProjectTitle(conversation, projectId);
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load project info", error.toException());
                    }
                });
    }

    private void loadProjectTitle(Conversation conversation, String projectId) {
        DatabaseReference projectRef = FirebaseDatabase.getInstance()
                .getReference("projects").child(projectId);

        projectRef.child("title").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String projectTitle = snapshot.getValue(String.class);
                conversation.setProjectTitle(projectTitle);

                Log.d(TAG, "Loaded project title: " + projectTitle);

                runOnUiThread(() -> {
                    conversationsAdapter.notifyDataSetChanged();
                    archivedConversationsAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load project title", error.toException());
            }
        });
    }

    private void showEmptyState() {
        rvConversations.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvConversations.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        // Mark notifications as read
        notificationsRef.child(currentUserId)
                .orderByChild("chatId")
                .equalTo(conversation.getChatId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            notifSnapshot.getRef().child("read").setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to mark notifications as read: " + error.getMessage());
                    }
                });
        conversation.setUnreadCount(0);
        conversationsAdapter.notifyDataSetChanged();
        archivedConversationsAdapter.notifyDataSetChanged();

        // Navigate to ChatActivity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_WITH_ID", conversation.getOtherUserId());
        intent.putExtra("CHAT_WITH_NAME", conversation.getOtherUserName());

        if (conversation.getProjectId() != null)
            intent.putExtra("PROJECT_ID", conversation.getProjectId());

        if (conversation.getApplicationId() != null)
            intent.putExtra("APPLICATION_ID", conversation.getApplicationId());

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }
}
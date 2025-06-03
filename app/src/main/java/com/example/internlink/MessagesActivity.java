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

public class MessagesActivity extends AppCompatActivity implements ConversationsAdapter.OnConversationClickListener {

    private static final String TAG = "MessagesActivity";

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
    private List<Conversation> originalConversationsList = new ArrayList<>(); // Keep original order

    private String currentUserId;
    private DatabaseReference chatMetadataRef;
    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;
    private DatabaseReference notificationsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;
    private LinearLayout tabActive, tabArchive;
    private TextView tabActiveText, tabArchiveText;
    private View tabActiveIndicator, tabArchiveIndicator;
    private RecyclerView rvArchivedConversations;
    private LinearLayout tvEmptyStateArchived;
    private ConversationsAdapter archivedConversationsAdapter;
    private List<Conversation> archivedConversationsList = new ArrayList<>();
    private List<Conversation> originalArchivedConversationsList = new ArrayList<>(); // Keep original order
    private boolean isShowingActiveTab = true;

    // Filter related variables
    private ImageView filterIcon;
    private SortOrder currentSortOrder = SortOrder.NEWEST_FIRST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Current User ID: " + currentUserId);

        initializeViews();
        setupRecyclerViews();
        setupSwipeGestures();
        setupFirebaseReferences();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.messages_recycler_view);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tabActive = findViewById(R.id.tab_active);
        tabArchive = findViewById(R.id.tab_archive);
        tabActiveText = findViewById(R.id.tab_active_text);
        tabArchiveText = findViewById(R.id.tab_archive_text);
        tabActiveIndicator = findViewById(R.id.tab_active_indicator);
        tabArchiveIndicator = findViewById(R.id.tab_archive_indicator);
        rvArchivedConversations = findViewById(R.id.archived_messages_recycler_view);
        tvEmptyStateArchived = findViewById(R.id.tv_empty_state_archive);

        tabActive.setOnClickListener(v -> showActiveTab());
        tabArchive.setOnClickListener(v -> showArchiveTab());

        ImageView menuIcon = findViewById(R.id.menu_icon);
        filterIcon = findViewById(R.id.ic_filter);
        FloatingActionButton fabNewMessage = findViewById(R.id.fab_new_message);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadConversations();
        });
        etSearch = findViewById(R.id.message_search);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        menuIcon.setOnClickListener(v -> onBackPressed());

        // Setup filter icon click listener
        filterIcon.setOnClickListener(v -> showFilterDialog());

        fabNewMessage.setVisibility(View.VISIBLE);
        fabNewMessage.setOnClickListener(v -> {
            Intent intent = new Intent(MessagesActivity.this, AllCompanies.class);
            intent.putExtra("STUDENT_ID", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
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
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
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
        // Sort active conversations
        switch (currentSortOrder) {
            case NEWEST_FIRST:
                Collections.sort(conversationsList, (c1, c2) ->
                        Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                Collections.sort(archivedConversationsList, (c1, c2) ->
                        Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
                break;
            case OLDEST_FIRST:
                Collections.sort(conversationsList, (c1, c2) ->
                        Long.compare(c1.getLastMessageTime(), c2.getLastMessageTime()));
                Collections.sort(archivedConversationsList, (c1, c2) ->
                        Long.compare(c1.getLastMessageTime(), c2.getLastMessageTime()));
                break;
            case NAME_ASC:
                Collections.sort(conversationsList, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                Collections.sort(archivedConversationsList, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;
            case NAME_DESC:
                Collections.sort(conversationsList, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                Collections.sort(archivedConversationsList, (c1, c2) -> {
                    String name1 = c1.getOtherUserName() != null ? c1.getOtherUserName() : "";
                    String name2 = c2.getOtherUserName() != null ? c2.getOtherUserName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                break;
            case UNREAD_FIRST:
                Collections.sort(conversationsList, (c1, c2) -> {
                    // First sort by unread count (higher first), then by time (newer first)
                    int unreadCompare = Integer.compare(c2.getUnreadCount(), c1.getUnreadCount());
                    if (unreadCompare != 0) return unreadCompare;
                    return Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime());
                });
                Collections.sort(archivedConversationsList, (c1, c2) -> {
                    int unreadCompare = Integer.compare(c2.getUnreadCount(), c1.getUnreadCount());
                    if (unreadCompare != 0) return unreadCompare;
                    return Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime());
                });
                break;
        }

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

    private void setupRecyclerViews() {
        // Setup active conversations RecyclerView
        conversationsAdapter = new ConversationsAdapter(conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationsAdapter);

        // Setup archived conversations RecyclerView
        archivedConversationsAdapter = new ConversationsAdapter(archivedConversationsList, this);
        rvArchivedConversations.setLayoutManager(new LinearLayoutManager(this));
        rvArchivedConversations.setAdapter(archivedConversationsAdapter);
    }

    private void setupSwipeGestures() {
        // Setup swipe for active conversations
        setupSwipeGestureForRecyclerView(rvConversations, conversationsList, conversationsAdapter, false);

        // Setup swipe for archived conversations
        setupSwipeGestureForRecyclerView(rvArchivedConversations, archivedConversationsList, archivedConversationsAdapter, true);
    }

    private void setupSwipeGestureForRecyclerView(RecyclerView recyclerView, List<Conversation> conversations,
                                                  ConversationsAdapter adapter, boolean isArchived) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Conversation conversation = conversations.get(position);

                // Show custom action options
                showSwipeActionDialog(conversation, position, isArchived);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
                        actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                View foregroundView = ((ConversationsAdapter.ConversationViewHolder) viewHolder).itemView.findViewById(R.id.foreground_layout);
                getDefaultUIUtil().clearView(foregroundView);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showSwipeActionDialog(Conversation conversation, int position, boolean isFromArchived) {
        String archiveAction = isFromArchived ? "Unarchive" : "Archive";

        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(new CharSequence[]{archiveAction, "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Archive/Unarchive
                            if (isFromArchived) {
                                unarchiveConversation(conversation, position);
                            } else {
                                archiveConversation(conversation, position);
                            }
                            break;
                        case 1:
                            // Delete
                            deleteConversation(conversation, position, isFromArchived);
                            break;
                    }
                })
                .setOnDismissListener(dialog -> {
                    // Reset swipe for the appropriate adapter
                    if (isFromArchived) {
                        archivedConversationsAdapter.notifyItemChanged(position);
                    } else {
                        conversationsAdapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private void archiveConversation(Conversation conversation, int position) {
        // Update Firebase
        chatMetadataRef.child(conversation.getChatId())
                .child("archivedBy")
                .child(currentUserId)
                .setValue(true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation archived", Toast.LENGTH_SHORT).show();

                    // Update local state
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
        // Update Firebase
        chatMetadataRef.child(conversation.getChatId())
                .child("archivedBy")
                .child(currentUserId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conversation unarchived", Toast.LENGTH_SHORT).show();

                    // Update local state
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

    private void deleteConversation(Conversation conversation, int position, boolean isFromArchived) {
        // Soft-delete: mark this chat as deleted for this user in chat_metadata
        chatMetadataRef.child(conversation.getChatId())
                .child("deletedBy")
                .child(currentUserId)
                .setValue(true)
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

    private void updateEmptyStates() {
        runOnUiThread(() -> {
            if (isShowingActiveTab) {
                tvEmptyState.setVisibility(conversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                tvEmptyStateArchived.setVisibility(archivedConversationsList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Enhanced search function with comprehensive character-level matching
     */
    private void filterConversations(String query) {
        List<Conversation> filteredList = new ArrayList<>();
        List<Conversation> sourceList = isShowingActiveTab ? conversationsList : archivedConversationsList;

        // If query is empty, show all conversations with current sort order
        if (query.trim().isEmpty()) {
            filteredList.addAll(sourceList);
        } else {
            // Enhanced search - search through multiple fields with character-level matching
            String searchQuery = query.toLowerCase().trim();

            for (Conversation convo : sourceList) {
                boolean matches = false;

                // Search in user name - character by character matching
                String name = convo.getOtherUserName() != null ? convo.getOtherUserName().toLowerCase() : "";
                if (containsAllCharacters(name, searchQuery)) {
                    matches = true;
                }

                // Search in project title - character by character matching
                if (!matches) {
                    String project = convo.getProjectTitle() != null ? convo.getProjectTitle().toLowerCase() : "";
                    if (containsAllCharacters(project, searchQuery)) {
                        matches = true;
                    }
                }

                // Search in last message content - character by character matching
                if (!matches) {
                    String lastMessage = convo.getLastMessage() != null ? convo.getLastMessage().toLowerCase() : "";
                    if (containsAllCharacters(lastMessage, searchQuery)) {
                        matches = true;
                    }
                }

                // Search in user role - character by character matching
                if (!matches) {
                    String role = convo.getOtherUserRole() != null ? convo.getOtherUserRole().toLowerCase() : "";
                    if (containsAllCharacters(role, searchQuery)) {
                        matches = true;
                    }
                }

                // Add special search terms (exact matches for these)
                if (!matches) {
                    if (searchQuery.equals("unread") && convo.getUnreadCount() > 0) {
                        matches = true;
                    } else if (searchQuery.equals("company") && "company".equals(convo.getOtherUserRole())) {
                        matches = true;
                    } else if (searchQuery.equals("student") && "student".equals(convo.getOtherUserRole())) {
                        matches = true;
                    } else if (searchQuery.equals("typing") && convo.isTyping()) {
                        matches = true;
                    }
                }

                // Additional character-based search for any combination
                if (!matches) {
                    // Create a combined searchable string with all conversation data
                    String combinedText = (name + " " +
                            (convo.getProjectTitle() != null ? convo.getProjectTitle() : "") + " " +
                            (convo.getLastMessage() != null ? convo.getLastMessage() : "") + " " +
                            (convo.getOtherUserRole() != null ? convo.getOtherUserRole() : "")).toLowerCase();
                    if (containsAllCharacters(combinedText, searchQuery)) {
                        matches = true;
                    }
                }

                if (matches) {
                    filteredList.add(convo);
                }
            }
        }

        // Apply current sort order to filtered list
        applySortOrderToList(filteredList);

        // Update the appropriate adapter
        if (isShowingActiveTab) {
            conversationsAdapter.updateList(filteredList);
        } else {
            archivedConversationsAdapter.updateList(filteredList);
        }

        // Update empty state visibility based on filtered results
        updateEmptyStatesForSearch(filteredList.isEmpty() && !query.trim().isEmpty());
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
                    int unreadCompare = Integer.compare(c2.getUnreadCount(), c1.getUnreadCount());
                    if (unreadCompare != 0) return unreadCompare;
                    return Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime());
                });
                break;
        }
    }

    private void updateEmptyStatesForSearch(boolean noSearchResults) {
        runOnUiThread(() -> {
            if (noSearchResults) {
                // Show "No search results" message
                if (isShowingActiveTab) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    // You might want to change the empty state text temporarily for search
                } else {
                    tvEmptyStateArchived.setVisibility(View.VISIBLE);
                }
            } else {
                updateEmptyStates(); // Use normal empty state logic
            }
        });
    }

    private void setupFirebaseReferences() {
        chatMetadataRef = FirebaseDatabase.getInstance().getReference("chat_metadata");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    }

    private void loadConversations() {
        Log.d(TAG, "Loading conversations for user: " + currentUserId);

        // Show loading initially
        showEmptyState();

        // Load from chat_metadata first, then fallback to direct chats
        chatMetadataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Chat metadata exists: " + snapshot.exists());
                Log.d(TAG, "Chat metadata children count: " + snapshot.getChildrenCount());

                conversationsList.clear();
                archivedConversationsList.clear();
                Set<String> processedChatIds = new HashSet<>();

                if (snapshot.exists()) {
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        Log.d(TAG, "Processing chat metadata: " + chatId);

                        Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                        if (Boolean.TRUE.equals(deleted)) {
                            Log.d(TAG, "Conversation hidden for this user: " + chatId);
                            continue; // Skip this conversation
                        }

                        // Check if current user is a participant in this chat
                        DataSnapshot participantsSnapshot = chatSnapshot.child("participants");

                        if (participantsSnapshot.hasChild(currentUserId)) {
                            Log.d(TAG, "User is participant in chat: " + chatId);

                            // Get the other participant
                            String otherUserId = null;
                            for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                                String participantId = participantSnapshot.getKey();
                                if (!participantId.equals(currentUserId)) {
                                    otherUserId = participantId;
                                    break;
                                }
                            }

                            if (otherUserId != null) {
                                Log.d(TAG, "Found other user: " + otherUserId);

                                // Create conversation object
                                Conversation conversation = new Conversation();
                                conversation.setChatId(chatId);
                                conversation.setOtherUserId(otherUserId);

                                String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                                conversation.setLastMessage(lastMessage != null ? lastMessage : "No messages yet");

                                Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);
                                conversation.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);

                                conversation.setLastSenderId(chatSnapshot.child("lastSenderId").getValue(String.class));

                                // Check if conversation is archived
                                Boolean archived = chatSnapshot.child("archivedBy").child(currentUserId).getValue(Boolean.class);
                                conversation.setArchived(Boolean.TRUE.equals(archived));

                                // Calculate unread count from notifications
                                calculateUnreadCount(conversation, otherUserId);

                                // Add to appropriate list
                                if (conversation.isArchived()) {
                                    archivedConversationsList.add(conversation);
                                } else {
                                    conversationsList.add(conversation);
                                }

                                processedChatIds.add(chatId);

                                Log.d(TAG, "Added conversation with user: " + otherUserId + " (archived: " + conversation.isArchived() + ")");
                            }
                        }
                    }
                }

                Log.d(TAG, "Found " + conversationsList.size() + " active and " + archivedConversationsList.size() + " archived conversations in metadata");

                // Also check direct chats that might not be in metadata
                loadDirectChats(processedChatIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat metadata: " + error.getMessage());
                // Try loading direct chats as fallback
                loadDirectChats(new HashSet<>());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void calculateUnreadCount(Conversation conversation, String otherUserId) {
        // Count unread chat message notifications from this user
        notificationsRef.child(currentUserId).orderByChild("type").equalTo("chat_message")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = 0;
                        for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                            String senderId = notifSnapshot.child("senderId").getValue(String.class);
                            Boolean read = notifSnapshot.child("read").getValue(Boolean.class);
                            String chatId = notifSnapshot.child("chatId").getValue(String.class);

                            if (otherUserId.equals(senderId) &&
                                    (read == null || !read) &&
                                    conversation.getChatId().equals(chatId)) {
                                unreadCount++;
                            }
                        }
                        conversation.setUnreadCount(unreadCount);

                        // Update adapter on main thread
                        runOnUiThread(() -> {
                            if (conversationsAdapter != null) {
                                conversationsAdapter.notifyDataSetChanged();
                            }
                            if (archivedConversationsAdapter != null) {
                                archivedConversationsAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load notifications for unread count: " + error.getMessage());
                    }
                });
    }

    private void loadDirectChats(Set<String> alreadyProcessed) {
        Log.d(TAG, "Loading direct chats, already processed: " + alreadyProcessed.size());

        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Direct chats exist: " + snapshot.exists());
                Log.d(TAG, "Direct chats children count: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                        String chatId = chatSnapshot.getKey();
                        Boolean deleted = chatSnapshot.child("deletedBy").child(currentUserId).getValue(Boolean.class);
                        if (Boolean.TRUE.equals(deleted)) {
                            Log.d(TAG, "Conversation hidden for this user: " + chatId);
                            continue; // Skip this conversation
                        }

                        Log.d(TAG, "Checking direct chat: " + chatId);

                        // Skip if already processed
                        if (alreadyProcessed.contains(chatId)) {
                            Log.d(TAG, "Skipping already processed chat: " + chatId);
                            continue;
                        }

                        // Check if current user is part of this chat ID
                        if (chatId != null && chatId.contains(currentUserId)) {
                            String otherUserId = getOtherUserIdFromChatId(chatId, currentUserId);

                            if (otherUserId != null) {
                                Log.d(TAG, "Found direct chat with: " + otherUserId);

                                // Check if there are actually messages in this chat
                                DataSnapshot messagesSnapshot = chatSnapshot.child("messages");
                                if (!messagesSnapshot.exists()) {
                                    Log.d(TAG, "No messages in chat " + chatId + ", skipping");
                                    continue;
                                }

                                // Get the last message from this chat
                                String lastMessage = "No messages yet";
                                long lastMessageTime = 0;
                                String lastSenderId = "";

                                // Get the last message
                                DataSnapshot lastMessageSnapshot = null;
                                for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                    lastMessageSnapshot = messageSnapshot;
                                }

                                if (lastMessageSnapshot != null) {
                                    String msgText = lastMessageSnapshot.child("text").getValue(String.class);
                                    String messageType = lastMessageSnapshot.child("messageType").getValue(String.class);
                                    String fileName = lastMessageSnapshot.child("fileName").getValue(String.class);
                                    Long timestamp = lastMessageSnapshot.child("timestamp").getValue(Long.class);
                                    lastMessageTime = timestamp != null ? timestamp : 0;
                                    lastSenderId = lastMessageSnapshot.child("senderId").getValue(String.class);

                                    // Format message based on type
                                    if ("image".equals(messageType)) {
                                        lastMessage = "📷 Photo";
                                    } else if ("file".equals(messageType)) {
                                        lastMessage = "📎 " + (fileName != null ? fileName : "File");
                                    } else {
                                        lastMessage = msgText != null ? msgText : "No messages yet";
                                    }
                                }

                                // Create conversation object
                                Conversation conversation = new Conversation();
                                conversation.setChatId(chatId);
                                conversation.setOtherUserId(otherUserId);
                                conversation.setLastMessage(lastMessage);
                                conversation.setLastMessageTime(lastMessageTime);
                                conversation.setLastSenderId(lastSenderId);
                                conversation.setArchived(false); // Direct chats default to not archived

                                // Calculate unread count
                                calculateUnreadCount(conversation, otherUserId);

                                conversationsList.add(conversation);

                                Log.d(TAG, "Added direct conversation with user: " + otherUserId);
                            }
                        }
                    }
                }

                Log.d(TAG, "Total conversations found: " + (conversationsList.size() + archivedConversationsList.size()));

                // Sort and load user details
                finishLoadingConversations();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load direct chats: " + error.getMessage());
                finishLoadingConversations();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void finishLoadingConversations() {
        Log.d(TAG, "Finishing loading conversations, active: " + conversationsList.size() + ", archived: " + archivedConversationsList.size());

        // Store original lists before applying any filters or sorts
        originalConversationsList.clear();
        originalConversationsList.addAll(conversationsList);

        originalArchivedConversationsList.clear();
        originalArchivedConversationsList.addAll(archivedConversationsList);

        // Apply current sort order
        applySortOrder();

        // Load user details for each conversation
        loadUserDetailsForConversations();
    }

    private String getOtherUserIdFromChatId(String chatId, String currentUserId) {
        if (chatId == null || currentUserId == null) return null;

        String[] userIds = chatId.split("_");
        if (userIds.length == 2) {
            if (userIds[0].equals(currentUserId)) {
                return userIds[1];
            } else if (userIds[1].equals(currentUserId)) {
                return userIds[0];
            }
        }
        return null;
    }

    private void loadUserDetailsForConversations() {
        List<Conversation> allConversations = new ArrayList<>();
        allConversations.addAll(conversationsList);
        allConversations.addAll(archivedConversationsList);

        Log.d(TAG, "Loading user details for " + allConversations.size() + " total conversations");

        if (allConversations.isEmpty()) {
            showEmptyState();
            return;
        }

        final int totalConversations = allConversations.size();
        final int[] loadedCount = {0};

        for (Conversation conversation : allConversations) {
            Log.d(TAG, "Loading user details for: " + conversation.getOtherUserId());

            usersRef.child(conversation.getOtherUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        String logoUrl = snapshot.child("logoUrl").getValue(String.class);

                        conversation.setOtherUserName(name != null ? name : "Unknown User");
                        conversation.setOtherUserRole(role != null ? role : "user");
                        conversation.setOtherUserLogoUrl(logoUrl);

                        // Add typing status listener
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
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to listen to typing status: " + error.getMessage());
                            }
                        });

                        Log.d(TAG, "Loaded user details for: " + name + " (role: " + role + ")");

                        // If this is a company, try to get project info from applications
                        if ("company".equals(role)) {
                            loadProjectInfoForConversation(conversation);
                        }
                    } else {
                        // User doesn't exist, set default values
                        conversation.setOtherUserName("Unknown User");
                        conversation.setOtherUserRole("user");
                        Log.w(TAG, "User not found: " + conversation.getOtherUserId());
                    }

                    loadedCount[0]++;
                    Log.d(TAG, "Loaded " + loadedCount[0] + " of " + totalConversations + " user details");

                    if (loadedCount[0] == totalConversations) {
                        // All user details loaded, update UI
                        Log.d(TAG, "All user details loaded, updating UI");
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
                    Log.e(TAG, "Failed to load user details for " + conversation.getOtherUserId() + ": " + error.getMessage());
                    // Set default values on error
                    conversation.setOtherUserName("Unknown User");
                    conversation.setOtherUserRole("user");

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
            });
        }
    }

    private void loadProjectInfoForConversation(Conversation conversation) {
        // Look for applications between current user and this company
        DatabaseReference applicationsRef = FirebaseDatabase.getInstance().getReference("applications");

        applicationsRef.orderByChild("userId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot appSnapshot : snapshot.getChildren()) {
                            String companyId = appSnapshot.child("companyId").getValue(String.class);

                            if (conversation.getOtherUserId().equals(companyId)) {
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
                        Log.e(TAG, "Failed to load project info: " + error.getMessage());
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
                    if (conversationsAdapter != null) {
                        conversationsAdapter.notifyDataSetChanged();
                    }
                    if (archivedConversationsAdapter != null) {
                        archivedConversationsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load project title: " + error.getMessage());
            }
        });
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        runOnUiThread(() -> {
            updateEmptyStates();
        });
    }

    private void hideEmptyState() {
        Log.d(TAG, "Hiding empty state");
        runOnUiThread(() -> {
            updateEmptyStates();
        });
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Log.d(TAG, "Conversation clicked: " + conversation.getOtherUserName());
        // Mark all notifications for this chat as read
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

        Intent intent = new Intent(this, StudentChatActivity.class);
        intent.putExtra("COMPANY_ID", conversation.getOtherUserId());
        intent.putExtra("COMPANY_NAME", conversation.getOtherUserName());

        if (conversation.getProjectId() != null) {
            intent.putExtra("PROJECT_ID", conversation.getProjectId());
        }
        if (conversation.getProjectTitle() != null) {
            intent.putExtra("PROJECT_TITLE", conversation.getProjectTitle());
        }
        if (conversation.getApplicationId() != null) {
            intent.putExtra("APPLICATION_ID", conversation.getApplicationId());
        }

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh conversations when returning to this activity
        if (conversationsAdapter != null) {
            loadConversations();
        }
    }
}
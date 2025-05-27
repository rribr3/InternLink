package com.example.internlink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private TextView tvNoChats;
    private ChatListAdapter chatListAdapter;
    private List<ChatItem> chatItems = new ArrayList<>();

    private String currentUserId;
    private DatabaseReference chatMetadataRef;
    private ValueEventListener chatListListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        initializeViews();
        setupRecyclerView();
        loadChats();
    }

    private void initializeViews() {
        rvChats = findViewById(R.id.rv_chats);
        tvNoChats = findViewById(R.id.tv_no_chats);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatMetadataRef = FirebaseDatabase.getInstance().getReference("chat_metadata");
    }

    private void setupRecyclerView() {
        chatListAdapter = new ChatListAdapter(chatItems, this::openChat);
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        rvChats.setAdapter(chatListAdapter);
    }

    private void loadChats() {
        chatListListener = chatMetadataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatItems.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    // Check if current user is participant
                    if (chatSnapshot.child("participants").hasChild(currentUserId)) {
                        String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                        Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);
                        String lastSenderId = chatSnapshot.child("lastSenderId").getValue(String.class);

                        // Get other user's ID
                        String otherUserId = null;
                        for (DataSnapshot participant : chatSnapshot.child("participants").getChildren()) {
                            String participantId = participant.getKey();
                            if (!participantId.equals(currentUserId)) {
                                otherUserId = participantId;
                                break;
                            }
                        }

                        if (otherUserId != null) {
                            // Get unread count
                            Long unreadCount = chatSnapshot.child("unreadCount")
                                    .child(currentUserId).getValue(Long.class);

                            // Create chat item
                            ChatItem chatItem = new ChatItem();
                            chatItem.setChatId(chatId);
                            chatItem.setOtherUserId(otherUserId);
                            chatItem.setLastMessage(lastMessage);
                            chatItem.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);
                            chatItem.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
                            chatItem.setLastMessageFromMe(currentUserId.equals(lastSenderId));

                            // Load other user's info
                            loadUserInfo(chatItem);
                        }
                    }
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadUserInfo(ChatItem chatItem) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(chatItem.getOtherUserId());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String userType = snapshot.child("userType").getValue(String.class);

                chatItem.setOtherUserName(name != null ? name : "Unknown");
                chatItem.setOtherUserType(userType);

                // Add to list and sort
                chatItems.add(chatItem);
                Collections.sort(chatItems,
                        (a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

                chatListAdapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void updateUI() {
        if (chatItems.isEmpty()) {
            tvNoChats.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.GONE);
        } else {
            tvNoChats.setVisibility(View.GONE);
            rvChats.setVisibility(View.VISIBLE);
        }
    }

    private void openChat(ChatItem chatItem) {
        String userType = FirebaseAuth.getInstance().getCurrentUser()
                .getDisplayName(); // Assuming userType is stored in display name

        if ("company".equals(userType)) {
            // Company opening chat with student
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("CHAT_WITH_ID", chatItem.getOtherUserId());
            intent.putExtra("CHAT_WITH_NAME", chatItem.getOtherUserName());
            startActivity(intent);
        } else {
            // Student opening chat with company
            Intent intent = new Intent(this, StudentChatActivity.class);
            intent.putExtra("COMPANY_ID", chatItem.getOtherUserId());
            intent.putExtra("COMPANY_NAME", chatItem.getOtherUserName());
            // You can add project info if needed
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListListener != null && chatMetadataRef != null) {
            chatMetadataRef.removeEventListener(chatListListener);
        }
    }
}
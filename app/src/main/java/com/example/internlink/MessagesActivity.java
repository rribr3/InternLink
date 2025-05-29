package com.example.internlink;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etSearch;
    private FloatingActionButton fabNewMessage;

    private List<Message> messages = new ArrayList<>();
    private MessagesAdapter adapter;

    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;
    private String currentUserId, chatWithId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatWithId = getIntent().getStringExtra("CHAT_WITH_ID");

        if (chatWithId == null || chatWithId.isEmpty()) {
            Toast.makeText(this, "No chat partner specified.", Toast.LENGTH_SHORT).show();
            finish(); // or navigate back safely
            return;
        }


        initViews();
        setupRecyclerView();
        loadMessages();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etSearch = findViewById(R.id.etSearch);
        fabNewMessage = findViewById(R.id.fabNewMessage);

        findViewById(R.id.btnMenu).setOnClickListener(v -> onBackPressed());

        fabNewMessage.setOnClickListener(v -> {
            // optional: open new message activity or compose
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                filterMessages(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new MessagesAdapter(messages, currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);
    }

    private void loadMessages() {
        String chatId = generateChatId(currentUserId, chatWithId);
        messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId).child("messages");

        messagesListener = messagesRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MessagesActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterMessages(String query) {
        List<Message> filtered = new ArrayList<>();
        for (Message m : messages) {
            if (m.getText() != null && m.getText().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(m);
            }
        }
        adapter = new MessagesAdapter(filtered, currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private String generateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null && messagesRef != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}

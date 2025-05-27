package com.example.internlink;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token: " + token);

        // Send token to your server
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String type = remoteMessage.getData().get("type");

            if ("chat_message".equals(type)) {
                String title = remoteMessage.getData().get("title");
                String message = remoteMessage.getData().get("message");
                String senderId = remoteMessage.getData().get("senderId");
                String senderName = remoteMessage.getData().get("senderName");
                String chatId = remoteMessage.getData().get("chatId");

                // Show notification
                NotificationHelper.showChatNotification(
                        this, title, message, senderId, senderName, chatId
                );
            }
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void sendTokenToServer(String token) {
        // Save FCM token to Firebase Database for the current user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(userId).child("fcmToken");
            tokenRef.setValue(token);
        }
    }
}
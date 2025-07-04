package com.example.internlink;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileAttachmentHelper {

    public static final int PICK_FILE_REQUEST = 1001;
    public static final int PICK_IMAGE_REQUEST = 1002;
    public static final int PICK_DOCUMENT_REQUEST = 1003;

    // ImgBB API key - Replace with your actual API key
    private static final String IMGBB_API_KEY = "93a9e7c9a933826963d704e128929b30";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    // Dropbox access token - Replace with your actual token
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AF0p4s-AA_2SxQPlpstEqB-uKW9OoZmPN8EBqQbsCDm-hcrJk3zEw9RTV_wRBTf_49_7Gdpg6_6Phu8DoO06_LIeh865B-7E4WpzYOzw3puvGF8zKp7qOz59kYBIo5Cs4bdrub8NNHu4pMgVw8FfxdsTu7Ebb12D1y0LARMc8ZBKNcYv5SgfnyWC_i5yyfkC1XDa4DADgATKXcrAeMNYJ8bscJOSwpnTwM-qPcRNzT-2OdNG35fpNNmMdbizGX_KELCA4K0AazC-RM_2kNNhmEvgozYuxfbDBv58xpruqI6xBHOOyFOVltlkWfnTd7ulK01LGDg5rSGN5vDF1J7_8fwrrv7sBIRzx1x1gdHOUQ6zNivAuZHLSFeWdJqX6l4BqDfqBQGfiRixfQXE5l8vPAFg4iSMlQ9hE8OxQcK2COwU5GgQSylpaxL493XfEa0hy6PeUKo65d_lSP3E6s6mxX0qVAf4v98ij6N1PKh0pBvRtJIsaUT3l5N0bbgIJTb7Gz1SrczG-S5MVgyuY8W2GeIzG1bS7fPpDtN_zTpBvK-3iBiBo3GiVcpsdVMcbUCzydClb8o5ctrhLK7JEWtnFezkMeMuwL_c9FywSj-xS-cTxQ4UtQ0Z3_hOR0ZQMkqJP7r5OQ2KW0oqiSoIY7gnQcuN7U5h0WhUTEKqOra9mYtOwi7McpyryNB0oHYE7MJDHF-OmfDswlVq1BXjcoiomOth1Bz1W_Vyly-gkqo3SfYnOp68Y1XFRtaLOibfCikP3aOAAmBUVw7dJuDvKWj43t7jWXZDLXSUdymfQT1TqqUU_KZ8maQthUV5Sw34cquVURa-LkYl3LIC9AMFvx2ZkXq2wV0If1XcQDJBVDgbeIA9FY1X8WCTIX9NgEBFMeC3gy0ZokAaOqz9hHcvhKss7WoMC3pdsK-lqO5OHOZu1uJlCqFE_IP6WcqeolDNTMi-3lapY3CK-SO3hSzJrWkywMTGZQmSpt6c2YM6DKMn0AYSELOQHlTqKeYNoRXYd9v2Jpl35v4U0BmwLzwjg3Ehc--fxJe_7FlVSpZUd9VcEsodN5tiy102KmX6LTx4Zco0tDaJqgH-WMU9_vzLWbv5YJiLHQxR2vJj0LKfLGF-zA0SpXjE8TUvBbY0nBzjDYOg98ilONxR3FLVYcM9lRPRwrx6mKej-bKOOv-P_JBa_cfVBO-9B68ZZUlCPko6VwFpMf01flAlYNuZ1IF5wDl8IF-WnWiPRyqMVEEwjQuXMnuhYrl7fa-GPd_xpfXVAT4UJA9xdsJZjVpf-xkQl1VcMK_AFKfeKO9hO6F3tHoZIjrQjvjLmqZ0JGRrl0mfsIwPKhZVUCAjFa57njo6ajuOtzLDigeBqLNcvZUm3vpz3tNqT_p-huInG8uExQss2MG1eC73Jn5s0AyJ4Y3vm_yWtU-qyK6tlT3Mf9wR-MkR7AKUOQ";
    private static final String DROPBOX_UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String DROPBOX_SHARED_LINK_URL = "https://api.dropboxapi.com/2/sharing/create_shared_link_with_settings";

    private Activity activity;
    private OnFileUploadListener listener;
    private OkHttpClient client;

    public interface OnFileUploadListener {
        void onUploadProgress(int progress);
        void onUploadSuccess(String fileUrl, String fileName, long fileSize, String fileType);
        void onUploadFailure(String error);
    }

    public FileAttachmentHelper(Activity activity, OnFileUploadListener listener) {
        this.activity = activity;
        this.listener = listener;
        this.client = new OkHttpClient();
    }

    // Pick image from gallery
    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        activity.startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                PICK_IMAGE_REQUEST
        );
    }

    // Pick document (PDF, DOC, etc.)
    public void pickDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"
        });
        activity.startActivityForResult(
                Intent.createChooser(intent, "Select Document"),
                PICK_DOCUMENT_REQUEST
        );
    }

    // Pick any file
    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                PICK_FILE_REQUEST
        );
    }

    // Handle file selection result
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();

            // Get file info
            String fileName = getFileName(fileUri);
            long fileSize = getFileSize(fileUri);
            String mimeType = activity.getContentResolver().getType(fileUri);

            // Check if it's an image or other file
            if (mimeType != null && mimeType.startsWith("image/")) {
                uploadToImgBB(fileUri, fileName, fileSize, mimeType);
            } else {
                uploadToDropbox(fileUri, fileName, fileSize, mimeType);
            }
        }
    }

    // Upload image to ImgBB
    private void uploadToImgBB(Uri imageUri, String fileName, long fileSize, String mimeType) {
        try {
            // Convert image to base64
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Create request
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", imageBase64)
                    .build();

            Request request = new Request.Builder()
                    .url(IMGBB_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            // Execute request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onUploadFailure("ImgBB upload failed: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    activity.runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            if (json.getBoolean("success")) {
                                JSONObject data = json.getJSONObject("data");
                                String imageUrl = data.getString("url");

                                if (listener != null) {
                                    listener.onUploadSuccess(imageUrl, fileName, fileSize, mimeType);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onUploadFailure("ImgBB upload failed");
                                }
                            }
                        } catch (Exception e) {
                            if (listener != null) {
                                listener.onUploadFailure("Failed to parse ImgBB response: " + e.getMessage());
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            if (listener != null) {
                listener.onUploadFailure("Failed to process image: " + e.getMessage());
            }
        }
    }

    // Upload file to Dropbox
    private void uploadToDropbox(Uri fileUri, String fileName, long fileSize, String mimeType) {
        try {
            // Read file content
            InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] fileBytes = baos.toByteArray();
            inputStream.close();

            // Create unique file path
            String timestamp = String.valueOf(System.currentTimeMillis());
            String dropboxPath = "/InternLink/chat_attachments/" + timestamp + "_" + fileName;

            // Dropbox API args
            JSONObject args = new JSONObject();
            args.put("path", dropboxPath);
            args.put("mode", "add");
            args.put("autorename", true);
            args.put("mute", false);

            // Create request
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/octet-stream"),
                    fileBytes
            );

            Request uploadRequest = new Request.Builder()
                    .url(DROPBOX_UPLOAD_URL)
                    .header("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .header("Dropbox-API-Arg", args.toString())
                    .header("Content-Type", "application/octet-stream")
                    .post(requestBody)
                    .build();

            // Execute upload
            client.newCall(uploadRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onUploadFailure("Dropbox upload failed: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Create shared link
                        createDropboxSharedLink(dropboxPath, fileName, fileSize, mimeType);
                    } else {
                        String error = response.body().string();
                        activity.runOnUiThread(() -> {
                            if (listener != null) {
                                listener.onUploadFailure("Dropbox upload failed: " + error);
                            }
                        });
                    }
                }
            });

        } catch (Exception e) {
            if (listener != null) {
                listener.onUploadFailure("Failed to upload file: " + e.getMessage());
            }
        }
    }

    // Create shared link for Dropbox file
    private void createDropboxSharedLink(String dropboxPath, String fileName, long fileSize, String mimeType) {
        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("path", dropboxPath);

            JSONObject settings = new JSONObject();
            settings.put("requested_visibility", "public");
            requestJson.put("settings", settings);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Request request = new Request.Builder()
                    .url(DROPBOX_SHARED_LINK_URL)
                    .header("Authorization", "Bearer " + DROPBOX_ACCESS_TOKEN)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onUploadFailure("Failed to create shared link: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    activity.runOnUiThread(() -> {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String sharedUrl = json.getString("url");

                            // Convert to direct download link
                            String directUrl = sharedUrl.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                                    .replace("?dl=0", "");

                            if (listener != null) {
                                listener.onUploadSuccess(directUrl, fileName, fileSize, mimeType);
                            }
                        } catch (Exception e) {
                            if (listener != null) {
                                listener.onUploadFailure("Failed to parse Dropbox response: " + e.getMessage());
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            if (listener != null) {
                listener.onUploadFailure("Failed to create shared link: " + e.getMessage());
            }
        }
    }

    // Get file name from URI
    private String getFileName(Uri uri) {
        String fileName = null;
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        if (fileName == null) {
            fileName = "attachment_" + System.currentTimeMillis();
        }

        return fileName;
    }

    // Get file size from URI
    private long getFileSize(Uri uri) {
        long fileSize = 0;
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1) {
                fileSize = cursor.getLong(sizeIndex);
            }
            cursor.close();
        }

        return fileSize;
    }

    // Get file extension
    private String getFileExtension(Uri uri) {
        String extension = null;
        String mimeType = activity.getContentResolver().getType(uri);

        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        if (extension == null) {
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot != -1) {
                    extension = path.substring(lastDot + 1);
                }
            }
        }

        return extension != null ? extension : "";
    }

    // Format file size for display
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
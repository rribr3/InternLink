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
    private static final String DROPBOX_ACCESS_TOKEN = "sl.u.AFwcQhtqr3YxI9mEiLWqUAnAzAjp50veHgNFNVV4xo-a0er8jRhe9l55hGz8VfIQHtJMoJvjUOhOoMDkGQWOaJMdi-ydz6AQoRJvoDJyUSJc3tBNmwlzRwGO_M6XT4QYKJE6WWV_bvNx84g0ANq9bcl4w1GQJYCCSgIwxrVpGdSKpfSY3u5CjmvqTZ9t0BJdPUoyzK-_IJTnDit62Sazr3OPP5cmhz2Pzq4yzdmhvL2v6CsNM7KZMkAoVVup9OuUb8OvQuVLZM8PuniaYfZvcjy29AO9a6eq4V2_2G5b2AdQnFed-hV32kdQ_MVm3NUSFfsAX_DEpKVzvv0rMMexsyCvZOEVq8KPYb_FjDns10x7zcI3dhn3FJrxaANGXo39tSM_O8SWZm_UwE-85zWwKmao7-x2yYoWv6rrfVh4VNFS10MQ1aY2xNvcxOQEHxoKkVVSZnfJkdNYJs2Zmp4v_6dY8SZp4A6BbeQeyRGs2Refitwavwr50DtAZtvnQQUT11V7EHrhWIHnHTcCTH4ALMOltfQujwuuPuzFioBp4fJDZHb_ZL1ssf_b7-euEXdkWfZaKIIxs6bp8rrUmYsKc0RbKLkbRnXaN4TkQvU07P94l50jxHZcMPtdrCTfwJgpvwR7ctuYU_Mc-58lp-_LDRcDOwncgRABVJZ55uALuV-Qq8jCYqwW1WfHZc1lVdAcrsUZuuqCIrDWYMlEdPCxFxiR6tndrmKX0nCrPTFKyRBL425qJrzKD17Y-HzCtYMpkNtXZrhxjp-uPz9hBsoT6hUIs0fwd4A1oMhEL6j_1OIpgZ0Hyg6Tk4tCZyaH_BT43A5jE5GEHKVYF87I_yYnnPfWBsp4WW9QJAYQH1zSLKUAmhXgZt0qLcQLPyhAJEGV-G3Kr4uzu0Gs4adSLeOSw0TL4iaaQGbmZeXQ9TJlIEHicVgU74kLClERSpTWD-FH6BnwhKAM-7WlvGSBV1r4dLjdLTmRKGODIh4YulIz0lOg17IsvhKlYa2iuclMDLBavcfiQ_BpyiQZK1d5eSHMu5PjuOXJNuHS2MNLTAWYBYg6d1MEaN_0aos9vFD5zZ2aNXOxVeb-G3cr0bdysDH2Fk97XaBli2o4Me5NphGjDrxqj4rezsuJOqBO3i7LPfAuQetjoRsV6MjA9Ilts5Q7Qnsaq-wrvt55dxvnrvGqi9IcIJkQizcIXmlnG2YBangiuj-bhzo0n6k8KuyjhPyFZYzC5kcWCWbEYR8QnCVkiPI1T4zJujYJoIaJ0vvwSPiRr40o-NM04YqynJfH6mrBPP-C34dmfH_KQF1vLN7NPVZFSoAhMaNuGoDJTAPuVDzV7ZXfw5GaGwm3_twpvYLRfCsr4S55acubuXcL-6EzbvM1GyA1yrn5FjO9jlq2feb97w6bjt8Zy6OiuPrHJ_QSRxcQi2PrQqLPVodSp-pO_WUbhcuTUOWPLvVbEdlU9z-ZSnA";

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
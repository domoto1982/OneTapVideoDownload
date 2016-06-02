package com.phantom.onetapvideodownload.utils;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import com.phantom.onetapvideodownload.R;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Global {
    public static final String TAG = "Global";
    public static final String VIDEO_MIME = "video/*";
    public static final String DEVELOPER_EMAIL = "onetapvideodownload@gmail.com";

    public static String getDeveloperEmail() {
        return DEVELOPER_EMAIL;
    }

    public static String getFilenameFromUrl(String url) {
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }

    public static String getDomain(String url) {
        Uri uri = Uri.parse(url);
        return uri.getHost();
    }

    public static String getNewFilename(String filename) {
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1) {
            dotPos = filename.length() - 1;
        }

        int openingBracketPos = filename.lastIndexOf('(');
        int closingBracketPos = filename.lastIndexOf(')');
        if (openingBracketPos != -1 && closingBracketPos != -1) {
            String numberString = filename.substring(openingBracketPos + 1, closingBracketPos);
            try {
                Integer number = Integer.parseInt(numberString);
                number = number + 1;
                filename = filename.substring(0, openingBracketPos + 1) + number.toString()
                        + filename.substring(closingBracketPos);
            } catch (Exception e) {
                filename = filename.substring(0, dotPos) + "(1)" + filename.substring(dotPos);
            }
        } else {
            filename = filename.substring(0, dotPos) + "(1)" + filename.substring(dotPos);
        }
        return filename;
    }

    public static String suggestName(String location, String filename) {
        File downloadFile = new File(location, filename);
        if (!downloadFile.exists()) {
            return downloadFile.getName();
        }

        filename = getNewFilename(filename);
        return suggestName(location, filename);
    }

    public static boolean isResourceAvailable(String urlString) {
        URL url;
        HttpURLConnection urlConnection;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();

            // HttpURLConnection will follow up to five HTTP redirects.
            if (responseCode/100 == 2) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getResourceMime(String urlString) {
        URL url;
        HttpURLConnection urlConnection;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();

            // HttpURLConnection will follow up to five HTTP redirects.
            if (responseCode/100 == 2) {
                return urlConnection.getHeaderField("Content-Type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getValidatedFilename(String filename) {
        StringBuilder filenameBuilder = new StringBuilder(filename);
        for(int i = 0; i < filename.length(); i++) {
            char j = filename.charAt(i);
            String reservedChars = "?:\"*|/\\<>";
            if(reservedChars.indexOf(j) != -1) {
                filenameBuilder.setCharAt(i, ' ');
            }
        }
        return filenameBuilder.toString().trim();
    }

    public static void startOpenIntent(Context context, String fileLocation) {
        try {
            Intent openIntent = new Intent();
            openIntent.setAction(android.content.Intent.ACTION_VIEW);
            openIntent.setDataAndType(Uri.parse(fileLocation), "video/*");
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(openIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    context.getResources().getString(R.string.play_video_activity_not_found),
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void startFileShareIntent(Context context, String fileLocation) {
        try {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);

            Uri fileUri = FileProvider.getUriForFile(context, "com.phantom.fileprovider",
                    new File(fileLocation));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType(Global.VIDEO_MIME);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(shareIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    context.getResources().getString(R.string.share_video_activity_not_found),
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void deleteFile(Context context, String fileLocation) {
        File file = new File(fileLocation);
        boolean result = file.delete();
        if (result) {
            Toast.makeText(context, R.string.file_deleted_successfully,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.unable_to_delete_file,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static String getHumanReadableSize(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1000));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1000, exp), pre);
    }

    public static String getResponseBody(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            if (response.body().contentLength() < 3*1000*1000L) {
                return response.body().string();
            } else {
                throw new IllegalArgumentException("Body content size is very large");
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void sendEmail(Context context, String to, String subject, String body) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", to, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    public static boolean isLocalFile(String path) {
        return path.startsWith("file://") || path.startsWith("/");
    }

    public static void copyUrlToClipboard(Context context, String url) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Uri copyUri = Uri.parse(url);
        ClipData clip = ClipData.newUri(context.getContentResolver(), "URI", copyUri);
        clipboard.setPrimaryClip(clip);
    }

    public static boolean isPlaystoreAvailable(@NonNull Context context) {
        List<String> packages = new ArrayList<>();
        packages.add("com.google.market");
        packages.add("com.android.vending");

        PackageManager packageManager = context.getPackageManager();
        for (String packageName : packages) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        Log.v(TAG, "Playstore not available on the device!");
        return false;
    }

}

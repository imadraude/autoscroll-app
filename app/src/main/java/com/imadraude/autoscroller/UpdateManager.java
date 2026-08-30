package com.imadraude.autoscroller;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class UpdateManager {

    interface Listener {
        void onUpdateStatus(String status);
    }

    private static final String RELEASE_API_URL =
            "https://api.github.com/repos/imadraude/autoscroll-app/releases/latest";
    private static final String TRUSTED_DOWNLOAD_PREFIX =
            "https://github.com/imadraude/autoscroll-app/releases/download/";
    private static final String RELEASE_ASSET_NAME = "AutoScroller-Lite-v1.0.apk";
    private static final String TAG_PREFIX = "build-";
    private static final String UPDATE_MIME = "application/vnd.android.package-archive";

    private final Activity activity;
    private final Listener listener;
    private final SharedPreferences preferences;
    private final DownloadManager downloadManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long pendingDownloadId = -1L;
    private int pendingBuild = -1;
    private boolean receiverRegistered;
    private boolean checking;
    private boolean destroyed;
    private boolean waitingForInstallPermission;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                return;
            }
            long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (completedId == pendingDownloadId) {
                handlePendingDownload();
            }
        }
    };

    UpdateManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        preferences = activity.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE);
        downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    void start() {
        registerDownloadReceiver();
        pendingDownloadId = preferences.getLong(AppPreferences.KEY_UPDATE_DOWNLOAD_ID, -1L);
        pendingBuild = preferences.getInt(AppPreferences.KEY_UPDATE_BUILD, -1);

        if (pendingDownloadId >= 0L && pendingBuild > 0) {
            if (pendingBuild <= BuildConfig.VERSION_CODE) {
                clearPendingDownload(true);
                checkForUpdates();
            } else {
                handlePendingDownload();
            }
        } else {
            checkForUpdates();
        }
    }

    void onResume() {
        if (waitingForInstallPermission && activity.getPackageManager().canRequestPackageInstalls()) {
            waitingForInstallPermission = false;
            handlePendingDownload();
        }
    }

    void checkForUpdates() {
        if (checking || destroyed || pendingDownloadId >= 0L) {
            return;
        }

        checking = true;
        setStatus(activity.getString(R.string.update_status_checking));
        executor.execute(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                activity.runOnUiThread(() -> {
                    checking = false;
                    if (destroyed) {
                        return;
                    }
                    if (release.buildNumber <= BuildConfig.VERSION_CODE) {
                        setStatus(activity.getString(
                                R.string.update_status_latest,
                                BuildConfig.VERSION_CODE
                        ));
                    } else {
                        startDownload(release);
                    }
                });
            } catch (IOException | JSONException | IllegalArgumentException exception) {
                activity.runOnUiThread(() -> {
                    checking = false;
                    if (!destroyed) {
                        setStatus(activity.getString(R.string.update_status_error));
                    }
                });
            }
        });
    }

    void destroy() {
        destroyed = true;
        executor.shutdownNow();
        if (receiverRegistered) {
            try {
                activity.unregisterReceiver(downloadReceiver);
            } catch (IllegalArgumentException ignored) {
                // Receiver was already unregistered by the framework.
            }
            receiverRegistered = false;
        }
    }

    private void registerDownloadReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(downloadReceiver, filter);
        }
        receiverRegistered = true;
    }

    private ReleaseInfo fetchLatestRelease() throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_API_URL).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(8000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "AutoScroller-Lite/" + BuildConfig.VERSION_CODE);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub release API returned HTTP " + responseCode);
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8
            ))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            JSONObject root = new JSONObject(body.toString());
            int buildNumber = parseBuildNumber(root.getString("tag_name"));
            JSONArray assets = root.getJSONArray("assets");
            String downloadUrl = null;

            for (int index = 0; index < assets.length(); index++) {
                JSONObject asset = assets.getJSONObject(index);
                if (RELEASE_ASSET_NAME.equals(asset.optString("name"))) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }

            if (downloadUrl == null || !downloadUrl.startsWith(TRUSTED_DOWNLOAD_PREFIX)) {
                throw new IOException("Trusted release APK was not found");
            }
            return new ReleaseInfo(buildNumber, downloadUrl);
        } finally {
            connection.disconnect();
        }
    }

    static int parseBuildNumber(String tag) {
        if (tag == null || !tag.startsWith(TAG_PREFIX)) {
            throw new IllegalArgumentException("Unexpected release tag");
        }
        int buildNumber = Integer.parseInt(tag.substring(TAG_PREFIX.length()));
        if (buildNumber <= 0) {
            throw new IllegalArgumentException("Invalid build number");
        }
        return buildNumber;
    }

    private void startDownload(ReleaseInfo release) {
        File directory = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) {
            setStatus(activity.getString(R.string.update_status_error));
            return;
        }

        File apk = updateFile(directory, release.buildNumber);
        if (apk.exists() && !apk.delete()) {
            setStatus(activity.getString(R.string.update_status_error));
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(release.downloadUrl));
        request.setTitle(activity.getString(R.string.update_download_title));
        request.setDescription(activity.getString(R.string.update_download_description));
        request.setMimeType(UPDATE_MIME);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );
        request.setDestinationInExternalFilesDir(
                activity,
                Environment.DIRECTORY_DOWNLOADS,
                apk.getName()
        );

        try {
            pendingDownloadId = downloadManager.enqueue(request);
            pendingBuild = release.buildNumber;
            preferences.edit()
                    .putLong(AppPreferences.KEY_UPDATE_DOWNLOAD_ID, pendingDownloadId)
                    .putInt(AppPreferences.KEY_UPDATE_BUILD, pendingBuild)
                    .apply();
            setStatus(activity.getString(R.string.update_status_downloading, pendingBuild));
        } catch (RuntimeException exception) {
            pendingDownloadId = -1L;
            pendingBuild = -1;
            setStatus(activity.getString(R.string.update_status_error));
        }
    }

    private void handlePendingDownload() {
        if (pendingDownloadId < 0L || pendingBuild <= 0 || destroyed) {
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(pendingDownloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (!cursor.moveToFirst()) {
                clearPendingDownload(false);
                checkForUpdates();
                return;
            }

            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_PENDING
                    || status == DownloadManager.STATUS_PAUSED
                    || status == DownloadManager.STATUS_RUNNING) {
                setStatus(activity.getString(R.string.update_status_downloading, pendingBuild));
                return;
            }

            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                clearPendingDownload(true);
                setStatus(activity.getString(R.string.update_status_error));
                return;
            }
        }

        File directory = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) {
            setStatus(activity.getString(R.string.update_status_error));
            return;
        }

        File apk = updateFile(directory, pendingBuild);
        if (!apk.isFile() || !isTrustedUpdate(apk, pendingBuild)) {
            clearPendingDownload(true);
            setStatus(activity.getString(R.string.update_status_security_error));
            return;
        }

        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            waitingForInstallPermission = true;
            setStatus(activity.getString(R.string.update_status_install_permission));
            Intent settings = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())
            );
            activity.startActivity(settings);
            return;
        }

        Uri apkUri = downloadManager.getUriForDownloadedFile(pendingDownloadId);
        if (apkUri == null) {
            setStatus(activity.getString(R.string.update_status_error));
            return;
        }

        setStatus(activity.getString(R.string.update_status_install_ready, pendingBuild));
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, UPDATE_MIME);
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(install);
    }

    private boolean isTrustedUpdate(File apk, int expectedBuild) {
        PackageManager packageManager = activity.getPackageManager();
        try {
            int signatureFlags = signatureFlags();
            PackageInfo current = packageManager.getPackageInfo(
                    activity.getPackageName(),
                    signatureFlags
            );
            PackageInfo archive = packageManager.getPackageArchiveInfo(
                    apk.getAbsolutePath(),
                    signatureFlags
            );

            if (archive == null
                    || !activity.getPackageName().equals(archive.packageName)
                    || packageVersionCode(archive) != expectedBuild
                    || packageVersionCode(archive) <= BuildConfig.VERSION_CODE) {
                return false;
            }

            Signature[] currentSignatures = packageSignatures(current);
            Signature[] archiveSignatures = packageSignatures(archive);
            if (currentSignatures.length != 1 || archiveSignatures.length != 1) {
                return false;
            }
            return currentSignatures[0].equals(archiveSignatures[0]);
        } catch (PackageManager.NameNotFoundException | RuntimeException exception) {
            return false;
        }
    }

    private static int signatureFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }
        return legacySignatureFlags();
    }

    @SuppressWarnings("deprecation")
    private static int legacySignatureFlags() {
        return PackageManager.GET_SIGNATURES;
    }

    private static Signature[] packageSignatures(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (packageInfo.signingInfo == null) {
                return new Signature[0];
            }
            return packageInfo.signingInfo.getApkContentsSigners();
        }
        return legacyPackageSignatures(packageInfo);
    }

    @SuppressWarnings("deprecation")
    private static Signature[] legacyPackageSignatures(PackageInfo packageInfo) {
        if (packageInfo.signatures == null) {
            return new Signature[0];
        }
        return packageInfo.signatures;
    }

    private static long packageVersionCode(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        return legacyVersionCode(packageInfo);
    }

    @SuppressWarnings("deprecation")
    private static long legacyVersionCode(PackageInfo packageInfo) {
        return packageInfo.versionCode;
    }

    private void clearPendingDownload(boolean removeDownload) {
        if (removeDownload && pendingDownloadId >= 0L) {
            downloadManager.remove(pendingDownloadId);
        }
        pendingDownloadId = -1L;
        pendingBuild = -1;
        waitingForInstallPermission = false;
        preferences.edit()
                .remove(AppPreferences.KEY_UPDATE_DOWNLOAD_ID)
                .remove(AppPreferences.KEY_UPDATE_BUILD)
                .apply();
    }

    private static File updateFile(File directory, int buildNumber) {
        return new File(directory, "AutoScroller-Lite-update-" + buildNumber + ".apk");
    }

    private void setStatus(String status) {
        listener.onUpdateStatus(status);
    }

    private static final class ReleaseInfo {
        private final int buildNumber;
        private final String downloadUrl;

        private ReleaseInfo(int buildNumber, String downloadUrl) {
            this.buildNumber = buildNumber;
            this.downloadUrl = downloadUrl;
        }
    }
}

package com.winhub.hernandez.store;

import com.winhub.hernandez.R
import com.winhub.hernandez.util.I18n

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Amazon Games SDK DLL manager.
 *
 * Downloads FuelSDK_x64.dll and AmazonGamesSDK_*.dll from the Amazon launcher
 * channel manifest, caches them in filesDir/amazon_sdk/, and deploys them to
 * the Wine prefix before launching a game.
 *
 * Cache: filesDir/amazon_sdk/ with .sdk_version sentinel file.
 * Deploy target (Wine prefix ProgramData):
 *   C:\ProgramData\Amazon Games Services\Legacy\FuelSDK_x64.dll
 *   C:\ProgramData\Amazon Games Services\AmazonGamesSDK\AmazonGamesSDK_*.dll
 *
 * SDK manifest uses the same pipeline as game manifests:
 *   GET SDK channel spec → downloadUrl
 *   GET {downloadUrl}/manifest.proto → parse → download files
 */
public class AmazonSdkManager {

    private static final String TAG = "BH_AMAZON";

    private static final String SDK_DIR          = "amazon_sdk";
    private static final String SDK_SERVICES_DIR = "Amazon Games Services";
    private static final String VERSION_FILE      = ".sdk_version";
    private static final String LEGACY_DIR        = "Legacy";
    private static final String AGSSDK_DIR        = "AmazonGamesSDK";

    /**
     * Ensures SDK DLLs are cached in filesDir/amazon_sdk/.
     * Idempotent — skips if version file exists AND at least one file in
     * the Amazon Games Services subdirectory (matches GameNative's isSdkCached()).
     *
     * @return true if SDK is ready (already cached or just downloaded)
     */
    public static boolean ensureSdkFiles(Context ctx, String accessToken) {
        if (isSdkCached(ctx)) {
            Log.d(TAG, I18n.s(R.string.ui_sdk_already_cached_skipping_download));
            return true;
        }

        Log.d(TAG, I18n.s(R.string.ui_downloading_amazon_sdk_dlls));
        try {
            // Step 1: Get SDK channel download spec
            String specJson = AmazonApiClient.getSdkChannelSpec(accessToken);
            if (specJson == null) {
                Log.e(TAG, I18n.s(R.string.ui_sdk_channel_spec_fetch_failed));
                return false;
            }

            org.json.JSONObject spec = new org.json.JSONObject(specJson);
            String downloadUrl = spec.optString("downloadUrl", "");
            String versionId   = spec.optString("versionId", "");
            if (downloadUrl.isEmpty()) {
                Log.e(TAG, I18n.s(R.string.ui_sdk_channel_downloadurl_is_empty));
                return false;
            }

            // Step 2: Download and parse manifest
            String manifestUrl = AmazonApiClient.appendPath(downloadUrl, "manifest.proto");
            byte[] manifestBytes = AmazonApiClient.getBytes(manifestUrl, accessToken);
            if (manifestBytes == null) {
                Log.e(TAG, I18n.s(R.string.ui_sdk_manifest_download_failed));
                return false;
            }

            AmazonManifest.ParsedManifest manifest = AmazonManifest.parse(manifestBytes);

            // Step 3: Filter files — only "Amazon Games Services" path entries
            File sdkCacheDir = new File(ctx.getFilesDir(), SDK_DIR);
            sdkCacheDir.mkdirs();

            int downloaded = 0;
            for (AmazonManifest.ManifestFile file : manifest.allFiles) {
                String unixPath = file.unixPath();

                // Skip macOS resource forks
                if (unixPath.contains("._")) continue;
                // Only Amazon Games Services files
                if (!unixPath.contains(SDK_SERVICES_DIR)) continue;

                String filename = new File(unixPath).getName();
                boolean isFuelSdk  = filename.equals("FuelSDK_x64.dll");
                boolean isAgsSdk   = filename.startsWith("AmazonGamesSDK");
                if (!isFuelSdk && !isAgsSdk) continue;

                // Download to appropriate subdirectory
                String subDir = isFuelSdk ? LEGACY_DIR : AGSSDK_DIR;
                File destFile = new File(new File(sdkCacheDir, SDK_SERVICES_DIR + "/" + subDir),
                        filename);
                destFile.getParentFile().mkdirs();

                if (destFile.exists() && destFile.length() == file.size) {
                    Log.d(TAG, I18n.s(R.string.ui_sdk_file_already_cached) + filename);
                    downloaded++;
                    continue;
                }

                String hashHex = file.hashHex();
                String fileUrl = AmazonApiClient.appendPath(downloadUrl, "files/" + hashHex);

                if (downloadSdkFile(fileUrl, accessToken, destFile)) {
                    downloaded++;
                    Log.d(TAG, I18n.s(R.string.ui_downloaded_sdk_file) + filename);
                } else {
                    Log.e(TAG, I18n.s(R.string.ui_failed_to_download_sdk_file) + filename);
                }
            }

            if (downloaded > 0) {
                // Write version sentinel
                File vf = new File(sdkCacheDir, VERSION_FILE);
                try (FileOutputStream fos = new FileOutputStream(vf)) {
                    fos.write(versionId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                Log.d(TAG, I18n.s(R.string.ui_sdk_cached_ok) + downloaded + " file(s), version=" + versionId);
                return true;
            } else {
                Log.e(TAG, I18n.s(R.string.ui_no_sdk_files_were_downloaded));
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, I18n.s(R.string.ui_ensuresdkfiles_failed), e);
            return false;
        }
    }

    /**
     * Deploys cached SDK DLLs to the Wine prefix ProgramData directory.
     * Idempotent — skips individual files if dest exists and size matches.
     *
     * @param prefixProgramData  File pointing to {wine_prefix}/.wine/drive_c/ProgramData
     */
    public static void deploySdkToPrefix(Context ctx, File prefixProgramData) {
        File sdkCacheServices = new File(ctx.getFilesDir(),
                SDK_DIR + "/" + SDK_SERVICES_DIR);
        if (!sdkCacheServices.isDirectory()) {
            Log.w(TAG, I18n.s(R.string.ui_sdk_cache_dir_does_not_exist) + sdkCacheServices.getAbsolutePath());
            return;
        }

        File prefixServices = new File(prefixProgramData, "Amazon Games Services");
        prefixServices.mkdirs();
        new File(prefixServices, LEGACY_DIR).mkdirs();
        new File(prefixServices, AGSSDK_DIR).mkdirs();

        copyDir(sdkCacheServices, prefixServices);
        Log.d(TAG, I18n.s(R.string.ui_sdk_deployed_to_wine_prefix) + prefixProgramData.getAbsolutePath());
    }

    // ── isSdkCached ───────────────────────────────────────────────────────────

    private static boolean isSdkCached(Context ctx) {
        File sdkDir = new File(ctx.getFilesDir(), SDK_DIR);
        File vf     = new File(sdkDir, VERSION_FILE);
        if (!vf.exists()) return false;

        // At least one file in amazon_sdk/Amazon Games Services/
        File servicesDir = new File(sdkDir, SDK_SERVICES_DIR);
        if (!servicesDir.isDirectory()) return false;

        File[] files = servicesDir.listFiles();
        if (files == null || files.length == 0) return false;

        // Recurse into subdirs to find at least one file
        return hasAnyFile(servicesDir);
    }

    private static boolean hasAnyFile(File dir) {
        File[] entries = dir.listFiles();
        if (entries == null) return false;
        for (File f : entries) {
            if (f.isFile()) return true;
            if (f.isDirectory() && hasAnyFile(f)) return true;
        }
        return false;
    }

    // ── File download ─────────────────────────────────────────────────────────

    private static boolean downloadSdkFile(String urlStr, String accessToken, File dest) {
        try {
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("User-Agent", "nile/0.1 Amazon");
            if (accessToken != null)
                conn.setRequestProperty("x-amzn-token", accessToken);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                Log.e(TAG, I18n.s(R.string.ui_sdk_file_http) + code + ": " + StoreLog.redactUrl(urlStr));
                conn.disconnect();
                return false;
            }

            try (java.io.InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            }
            conn.disconnect();
            return true;
        } catch (Exception e) {
            Log.e(TAG, I18n.s(R.string.ui_downloadsdkfile_failed) + dest.getName(), e);
            return false;
        }
    }

    // ── Directory copy ────────────────────────────────────────────────────────

    private static void copyDir(File src, File dst) {
        File[] entries = src.listFiles();
        if (entries == null) return;
        for (File f : entries) {
            File destFile = new File(dst, f.getName());
            if (f.isDirectory()) {
                destFile.mkdirs();
                copyDir(f, destFile);
            } else {
                // Idempotent: skip if dest exists and size matches
                if (destFile.exists() && destFile.length() == f.length()) continue;
                try {
                    copyFile(f, destFile);
                } catch (IOException e) {
                    Log.e(TAG, I18n.s(R.string.ui_sdk_copyfile_failed) + f.getName(), e);
                }
            }
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = fis.read(buf)) >= 0) fos.write(buf, 0, n);
        }
    }
}

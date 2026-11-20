package com.winhub.hernandez.restore;

import com.winhub.hernandez.R
import com.winhub.hernandez.util.I18n

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.winhub.hernandez.R;
import com.winhub.hernandez.core.AppUtils;
import com.winhub.hernandez.core.FileUtils;
import com.winhub.hernandez.core.PreloaderDialog;
import com.winhub.hernandez.core.TarCompressorUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

public class RestoreActivity extends Activity {

    private PreloaderDialog preloaderDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preloaderDialog = new PreloaderDialog(this);
        preloaderDialog.showOnUiThread(R.string.restoring_data);

        Uri backupUri = getIntent().getData();
        if (backupUri != null) {
            startRestoreOperation(backupUri);
        } else {
            finish(); // Close the activity if no URI is passed
        }
    }

    private void startRestoreOperation(Uri backupUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File dataDir = getFilesDir().getParentFile(); // App's data directory
                File backupFile = FileUtils.getFileFromUri(this, backupUri);

                if (backupFile != null) {
                    boolean success = TarCompressorUtils.extractTar(backupFile, dataDir, (file, size) -> {
                        // Skip symbolic links pointing to critical directories
                        if (isCriticalSymbolicLink(file)) {
                            Log.d("RestoreOp", I18n.s(R.string.ui_skipping_symbolic_link) + file.getAbsolutePath());
                            return null; // Return null to skip extraction
                        }
                        Log.d("RestoreOp", I18n.s(R.string.ui_extracting_file) + file.getAbsolutePath());
                        return file;
                    });

                    backupFile.delete(); // Clean up the temp file if it was created

                    if (success) {
                        onRestoreSuccess();
                    } else {
                        throw new IOException(I18n.s(R.string.ui_extraction_failed));
                    }
                } else {
                    throw new IOException(I18n.s(R.string.ui_failed_to_obtain_file_from_uri));
                }
            } catch (Exception e) {
                Log.e("RestoreOp", I18n.s(R.string.ui_restore_failed), e);
                onRestoreFailed();
            }
        });
    }

    private boolean isCriticalSymbolicLink(File file) {
        if (FileUtils.isSymlink(file)) {
            String targetPath = FileUtils.readSymlink(file);
            // Check if the symlink points to the Downloads or Storage directories
            if (targetPath.contains("Download") || targetPath.contains("storage")) {
                return true;
            }
        }
        return false;
    }


    private void onRestoreSuccess() {
        runOnUiThread(() -> {
            preloaderDialog.closeOnUiThread();
            AppUtils.showToast(this, I18n.s(R.string.ui_data_restored_successfully));
            finish();
            // Restart the main application after restore
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                startActivity(intent);
            }
        });
    }

    private void onRestoreFailed() {
        runOnUiThread(() -> {
            preloaderDialog.closeOnUiThread();
            AppUtils.showToast(this, I18n.s(R.string.ui_data_restore_failed));
            finish();
        });
    }
        }

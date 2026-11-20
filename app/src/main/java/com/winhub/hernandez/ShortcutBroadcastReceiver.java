package com.winhub.hernandez;

import com.winhub.hernandez.R;
import com.winhub.hernandez.util.I18n;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class ShortcutBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "ShortcutBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals("com.winhub.hernandez.SHORTCUT_ADDED")) {
            boolean isShortcutAdded = intent.getBooleanExtra("shortcut_added", false);
            if (isShortcutAdded) {
                Log.d(LOG_TAG, I18n.s(R.string.ui_shortcut_added_successfully));
                Toast.makeText(context, I18n.s(R.string.ui_sorry_your_device_may_not_be_supported), Toast.LENGTH_SHORT).show(); // yeah. I'm at a loss here.
            } else {
                Log.d(LOG_TAG, I18n.s(R.string.ui_shortcut_addition_failed));
                Toast.makeText(context, I18n.s(R.string.ui_failed_to_add_shortcut), Toast.LENGTH_SHORT).show();

                // Attempt to add the shortcut here if it failed
                addShortcutToHomeScreen(context, intent);
            }
        } else {
            Log.d(LOG_TAG, I18n.s(R.string.ui_unexpected_broadcast_action_received));
        }
    }

    private void addShortcutToHomeScreen(Context context, Intent originalIntent) {
        String shortcutName = originalIntent.getStringExtra("shortcut_name");
        Bitmap shortcutIcon = originalIntent.getParcelableExtra("shortcut_icon");
        Intent shortcutIntent = originalIntent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

        if (shortcutName != null && shortcutIcon != null && shortcutIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

                if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                    ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, shortcutName)
                            .setShortLabel(shortcutName)
                            .setIcon(Icon.createWithBitmap(shortcutIcon))
                            .setIntent(shortcutIntent)
                            .build();

                    Log.d(LOG_TAG, I18n.s(R.string.ui_requesting_pin_shortcut_from_the_broadcastreceiver));
                    boolean result = shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                    Log.d(LOG_TAG, I18n.s(R.string.ui_pin_shortcut_requested_with_result) + result);

                    if (result) {
                        Toast.makeText(context, I18n.s(R.string.ui_shortcut_added_successfully_from_broadcastreceiver), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(LOG_TAG, I18n.s(R.string.ui_failed_to_add_shortcut_from_broadcastreceiver));
                    }
                }
            } else {
                // Fallback for older versions (API < 26)
                Intent addIntent = new Intent();
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, shortcutIcon);
                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

                try {
                    context.sendBroadcast(addIntent);
                    Log.d(LOG_TAG, I18n.s(R.string.ui_sent_broadcast_to_install_shortcut_from_broadcastreceive));
                    Toast.makeText(context, I18n.s(R.string.ui_shortcut_added_successfully_broadcast), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(LOG_TAG, I18n.s(R.string.ui_error_sending_broadcast_for_installing_shortcut) + e.getMessage(), e);
                    Toast.makeText(context, I18n.s(R.string.ui_failed_to_add_shortcut_via_broadcast), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.e(LOG_TAG, I18n.s(R.string.ui_missing_shortcut_data_cannot_add_to_home_screen));
        }
    }
}




package com.winhub.hernandez.store;

import com.winhub.hernandez.R
import com.winhub.hernandez.util.I18n

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Persists Epic OAuth2 credentials in SharedPreferences "bh_epic_prefs".
 *
 * Keys: access_token, refresh_token, account_id, display_name, expires_at (epoch ms)
 */
public class EpicCredentialStore {

    private static final String TAG        = "BH_EPIC";
    private static final String PREFS_NAME = "bh_epic_prefs";

    public static class Credentials {
        public String accessToken;
        public String refreshToken;
        public String accountId;
        public String displayName;
        public long   expiresAt;   // epoch millis
    }

    public static void save(Context ctx, Credentials creds) {
        try {
            ctx.getSharedPreferences(PREFS_NAME, 0).edit()
                    .putString("access_token",  creds.accessToken)
                    .putString("refresh_token", creds.refreshToken)
                    .putString("account_id",    creds.accountId)
                    .putString("display_name",  creds.displayName != null ? creds.displayName : "")
                    .putLong("expires_at",      creds.expiresAt)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, I18n.s(R.string.ui_failed_to_save_epic_credentials) + e.getClass().getSimpleName());
        }
    }

    public static Credentials load(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, 0);
            String token = sp.getString("access_token", null);
            if (token == null || token.isEmpty()) return null;

            Credentials creds = new Credentials();
            creds.accessToken  = token;
            creds.refreshToken = sp.getString("refresh_token", null);
            creds.accountId    = sp.getString("account_id",    "");
            creds.displayName  = sp.getString("display_name",  "");
            creds.expiresAt    = sp.getLong("expires_at",      0L);
            return creds;
        } catch (Exception e) {
            Log.e(TAG, I18n.s(R.string.ui_failed_to_load_epic_credentials) + e.getClass().getSimpleName());
            return null;
        }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, 0).edit()
                .remove("access_token")
                .remove("refresh_token")
                .remove("account_id")
                .remove("display_name")
                .remove("expires_at")
                .apply();
    }

    public static boolean isLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, 0);
        String token = sp.getString("access_token", null);
        return token != null && !token.isEmpty();
    }

    /**
     * Returns a valid access token (auto-refreshes if within 5 minutes of expiry).
     * Returns null if not logged in or refresh fails.
     */
    public static String getValidAccessToken(Context ctx) {
        Credentials creds = load(ctx);
        if (creds == null || creds.accessToken == null) return null;

        long fiveMinutesMs = 5L * 60L * 1000L;
        if ((creds.expiresAt - System.currentTimeMillis()) < fiveMinutesMs
                && creds.refreshToken != null) {
            Log.d(TAG, I18n.s(R.string.ui_epic_token_near_expiry_refreshing));
            EpicAuthClient.TokenResult result = EpicAuthClient.refreshToken(creds.refreshToken);
            if (result != null) {
                creds.accessToken  = result.accessToken;
                creds.refreshToken = result.refreshToken != null ? result.refreshToken : creds.refreshToken;
                creds.expiresAt    = result.expiresAt;
                save(ctx, creds);
                Log.d(TAG, I18n.s(R.string.ui_epic_token_refreshed_ok));
            }
        }

        return creds.accessToken;
    }
}

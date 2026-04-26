package com.example.project;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.UUID;

/**
 * Centralised configuration: server endpoints, on-disk paths, and the
 * per-install user identifier.
 *
 * <h3>User identifier</h3>
 * Replaces {@code Settings.Secure.ANDROID_ID} with a v4 UUID generated on
 * first launch and stored in private {@link SharedPreferences}. ANDROID_ID
 * is shared across apps signed by the same key, can leak PII, and is
 * deprecated as a tracking primitive. The UUID is opaque, scoped to this
 * install, and reset when the user clears app data.
 *
 * <h3>Server URL</h3>
 * Read from {@code BuildConfig.SERVER_BASE_URL}, which is wired in via
 * {@code app/build.gradle} from the {@code SERVER_BASE_URL} gradle property.
 * The runtime network policy ({@code res/xml/networkset.xml}) refuses
 * cleartext, so the URL must use HTTPS.
 */
public final class Config {

    private static final String PREFS = "app_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String RECOMMENDER_PREFIX = "/recommender/";

    /** Cached so callers can hit {@link #userId(Context)} freely. */
    private static volatile String cachedUserId;

    private Config() {}

    /** Base URL of the Django server, with any trailing slash trimmed. */
    public static String serverBaseUrl() {
        String url = BuildConfig.SERVER_BASE_URL;
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("SERVER_BASE_URL not configured");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String sendUserFileUrl(String userId) {
        return serverBaseUrl() + RECOMMENDER_PREFIX + "SendUserFile/" + userId;
    }

    public static String getUserFileUrl(String userId) {
        return serverBaseUrl() + RECOMMENDER_PREFIX + "GetUserFile/" + userId;
    }

    /**
     * Absolute path to the app's databases directory. Survives package
     * renames and matches {@link Context#getDatabasePath} semantics.
     */
    public static String databasesDir(Context context) {
        File dir = new File(context.getApplicationInfo().dataDir, "databases");
        // mkdirs() is idempotent and a no-op once the directory exists.
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir.getAbsolutePath();
    }

    /**
     * Stable, per-install user identifier. Generated on first call and
     * cached for the lifetime of the process. Clearing app data resets it,
     * which is the desired privacy trade-off.
     */
    public static String userId(Context context) {
        String id = cachedUserId;
        if (id != null) {
            return id;
        }
        synchronized (Config.class) {
            if (cachedUserId != null) {
                return cachedUserId;
            }
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String stored = prefs.getString(KEY_USER_ID, null);
            if (stored == null) {
                stored = UUID.randomUUID().toString().replace("-", "");
                prefs.edit().putString(KEY_USER_ID, stored).apply();
            }
            cachedUserId = stored;
            return stored;
        }
    }
}

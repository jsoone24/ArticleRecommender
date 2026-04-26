package com.example.project;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Adds HMAC-SHA256 request signing headers expected by the server.
 *
 * Signature payload: METHOD + "\n" + PATH + "\n" + TIMESTAMP
 * Timestamp prevents indefinite replay; the server rejects skew > 5 minutes.
 *
 * The shared secret is baked in via BuildConfig.REQUEST_SIGNING_SECRET, so
 * a dedicated attacker can pull it out of the APK. This is intentionally only
 * a speed bump, not a real auth system — but it stops anyone who only learns
 * a user's UUID from impersonating them.
 */
public final class RequestSigner {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private RequestSigner() {}

    public static void sign(HttpURLConnection conn) {
        String secret = BuildConfig.REQUEST_SIGNING_SECRET;
        if (secret == null || secret.isEmpty()) {
            // Debug builds can be configured without a secret so a fresh
            // checkout opens cleanly. Release builds fail at gradle time
            // (see app/build.gradle) so this branch is unreachable in
            // production APKs.
            return;
        }

        URL url = conn.getURL();
        // The canonical payload MUST match the server byte-for-byte
        // (see server/recommender/auth.py::_expected_signature). URL.getPath()
        // omits the query string; if you ever sign query parameters or the
        // request body, update both sides in lockstep or every request 403s.
        String path = url.getPath();
        String method = conn.getRequestMethod();
        // Server uses seconds since epoch.
        String timestamp = Long.toString(System.currentTimeMillis() / 1000L);
        String payload = method + "\n" + path + "\n" + timestamp;

        String signature;
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            signature = toHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            // HmacSHA256 is mandated by the JCE spec; this should never happen.
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }

        conn.setRequestProperty("X-Timestamp", timestamp);
        conn.setRequestProperty("X-Signature", signature);
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(out);
    }
}

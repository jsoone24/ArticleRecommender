package com.example.project;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads the per-user recommendation SQLite from the server on app launch.
 */
public class RecommendRequester extends AsyncTask<Void, Void, Void> {

    private final String url;
    private final File destination;

    public RecommendRequester(String url, File destination) {
        this.url = url;
        this.destination = destination;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        HttpURLConnection conn = null;
        try {
            URL endpoint = new URL(url);
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            RequestSigner.sign(conn);
            conn.connect();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }

            try (InputStream input = new BufferedInputStream(conn.getInputStream(), 8192);
                 OutputStream output = new FileOutputStream(destination)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = input.read(buf)) != -1) {
                    output.write(buf, 0, n);
                }
                output.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}

package com.example.project;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Uploads the user's reading-history SQLite to the server when the app closes
 * (either via {@link MainActivity#onDestroy} or {@link ForecdTerminationService}).
 */
public class RecordSender extends AsyncTask<Void, Void, Void> {

    private static final String BOUNDARY = "----RecordSenderBoundary";
    private static final String LINE_END = "\r\n";
    private static final int BUFFER_SIZE = 16 * 1024;

    private final String url;
    private final File source;

    public RecordSender(String url, File source) {
        this.url = url;
        this.source = source;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!source.exists()) {
            return null;
        }

        HttpURLConnection conn = null;
        try {
            URL endpoint = new URL(url);
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + BOUNDARY);
            RequestSigner.sign(conn);

            try (FileInputStream input = new FileInputStream(source);
                 DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {

                out.writeBytes("--" + BOUNDARY + LINE_END);
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + source.getName() + "\"" + LINE_END);
                out.writeBytes("Content-Type: application/octet-stream" + LINE_END);
                out.writeBytes("Content-Transfer-Encoding: binary" + LINE_END);
                out.writeBytes(LINE_END);

                byte[] buffer = new byte[BUFFER_SIZE];
                int n;
                while ((n = input.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }

                out.writeBytes(LINE_END);
                out.writeBytes("--" + BOUNDARY + "--" + LINE_END);
                out.flush();
            }

            // Force the request to flush; response body is empty.
            conn.getResponseCode();
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

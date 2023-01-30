package com.example.project;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RecordSender { //어플 종료 시 사용자 기록 파일을 서버로 보내기 위한 클래스
    RecordTask http;

    public void request(String url, String dirPath, String fileName) {
        http = new RecordTask(url, dirPath, fileName);
        http.execute();
    }

    private class RecordTask extends AsyncTask<Void, Void, Void> {
        String url;
        String dirPath;
        String fileName;
        String filePath;

        public RecordTask(String url, String dirPath, String fileName) {
            this.url = url;
            this.dirPath = dirPath;
            this.fileName = fileName;
            this.filePath = dirPath+"/"+fileName;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File dir = new File(this.dirPath);
            File sourceFile = new File(this.filePath);
            if (!dir.exists() && !dir.isDirectory()) { dir.mkdirs();  } //저장할려고 하는 폴더가 없다면 새로 만들기
            else {
                if (sourceFile.exists()) { //만약 파일이 없다면 안함, 있으면 전송.
                    String boundary = "*****";
                    String twoHyphens = "--";
                    String lineEnd = "\r\n";
                    HttpURLConnection conn = null;
                    DataOutputStream dos = null;
                    int bytesRead, bytesAvailable, bufferSize;
                    byte[] buffer;
                    int maxBufferSize = 1 * 1024 * 1024;

                    try {
                        // open a URL connection to the Servlet
                        URL url = new URL(this.url);

                        // Open a HTTP  connection to  the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("enctype", "multipart/form-data");
                        conn.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);

                        FileInputStream fileInputStream = new FileInputStream(sourceFile);
                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                        dos.writeBytes("Content-Type: multipart/form-data" + lineEnd);
                        dos.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.flush();

                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                        dos.flush();

                        int serverResponseCode = conn.getResponseCode();

                        fileInputStream.close();
                        dos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
}

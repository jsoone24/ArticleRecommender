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

public class RecommendRequester { //어플 시작시 서버에 요청하여 추천 기사 목록을 받아오는 클래스
    RecommendTask http;

    public void request(String url, String dirPath, String fileName) {
        http = new RecommendTask(url, dirPath, fileName);
        http.execute();
    }

    private class RecommendTask extends AsyncTask<Void, Integer, Void> { //서버에서 파일을 다운로드 받는 것을 스레드를 추가해 백그라운드에서 실행한다.
        String url;
        String dirPath;
        String fileName;
        String filePath;

        public RecommendTask(String url, String dirPath, String fileName) {
            this.url = url;
            this.dirPath = dirPath;
            this.fileName = fileName;
            this.filePath = dirPath+"/"+fileName;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File dir = new File(this.dirPath);
            File file = new File(this.filePath);

            if (!dir.exists() && !dir.isDirectory()) { dir.mkdirs();  } //저장할려고 하는 폴더가 없다면 새로 만들기

            if (!file.exists()) { //만약 파일이 없다면, 파일 생성. 생성한 뒤에 덮어 씌움.
                try { file.createNewFile();}
                catch (IOException e) { e.printStackTrace(); }
            }

            try {
                URL text = new URL(url);
                HttpURLConnection http = (HttpURLConnection) text.openConnection();
                http.setRequestProperty("Content-type", "application/x-www-form-urlencoded;charset=UTF-8");
                http.setConnectTimeout(10000);
                http.setReadTimeout(10000);
                http.setRequestMethod("POST");
                http.setDoInput(true);
                http.setDoOutput(true);
                http.connect();

                InputStream input = new BufferedInputStream(text.openStream(), 8192); //서버로 인풋 스트림 열기
                OutputStream output = new FileOutputStream(file); //파일 경로로 아웃풋 스트림 열기

                byte[] data = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close(); //버퍼에 있던 내용 flush하고 스트림 닫아준다.
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

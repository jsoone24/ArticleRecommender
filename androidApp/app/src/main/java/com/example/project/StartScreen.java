package com.example.project;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class StartScreen extends AppCompatActivity { //어플을 실행하자마자 보이는 초기화 화면입니다.
    private String android_id;
    private String dirPath = "/data/data/com.example.project/databases"; //데이터베이스의 경로
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        startService(new Intent(this, ForecdTerminationService.class)); //서비스를 실행한다. 앱이 강제 종료되는 것을 백그라운드에서 감지하여 후에 동작을 수행한다.
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        textView = findViewById(R.id.initializeStatusText);

        Realm.init(this);//Realm 초기화
        textView.setText("DownloadingFile...");
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.execute(); //파일 확인 시작 전 recommend 파일 다운로드
    }

    class DownloadFile extends AsyncTask<Void, String, Void> { //앱 실행시 recommend파일 서버로부터 다운로드

        @Override
        protected Void doInBackground(Void... voids) {
            Realm mrealm = Realm.getDefaultInstance(); //기록을 앱을 켠 순간부터 다시 하기 위해서 다 지운다.
            final RealmResults<ArticleVO> results = mrealm.where(ArticleVO.class).findAll();
            mrealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    results.deleteAllFromRealm();
                }
            });

            RecommendRequester recommendRequester = new RecommendRequester(); //서버로부터 파일을 다운로드 받는다.
            recommendRequester.request("http://URL:8080/recommender/SendUserFile/".concat(android_id), dirPath, "recommenddb");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new On().execute();
        } //서버로부터 데이터베이스 파일을 다운로드 받은 후, 데이터베이스를 ArrayList형태로 만든다.
    }

    class On extends AsyncTask<Void, String, Void> {
        String filePath = dirPath + "/" + "recommenddb";
        ArrayList<ArticleVO> datas = new ArrayList<>();

        @Override
        protected void onProgressUpdate(String... values) {
            textView.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("data", datas);
            startActivity(intent);
            finish();
        }

        @Override
        protected Void doInBackground(Void... voids) { //데이터베이스의 파일 내용 중 링크를 파싱하여 기사마다의 정보를 파악한다.
            File file = new File(filePath);
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
            Cursor cursor = db.rawQuery("select link, similarity from tblink order by similarity desc", null);
            int i = 0;
            while (cursor.moveToNext()) {
                String link = cursor.getString(0);
                String similarity = cursor.getString(1);
                ArticleVO articleVO = new ArticleVO();

                Document doc = null;
                try {
                    doc = Jsoup.connect(link).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert doc != null;
                try {
                    Elements getTitle = doc.select("#articleTitle");   // 아이디가 _article인 div 태그 선택
                    Elements getArticleDateTime = doc.select("#main_content > div.article_header > div.article_info > div > span:nth-child(1)");
                    Elements getArticleModifyTime = doc.select("#main_content > div.article_header > div.article_info > div > span:nth-child(2)");
                    Elements getPublisher = doc.select("#main_content > div.article_header > div.press_logo > a > img");
                    Elements getImageUri = doc.select("#articleBodyContents").select("img");

                    articleVO.link = link;
                    articleVO.similarity = similarity;
                    articleVO.title = getTitle.text();
                    articleVO.date = "입력 : " + getArticleDateTime.text();
                    articleVO.publisher = getPublisher.attr("title");

                    try {
                        articleVO.imageUri = getImageUri.first().attr("src");
                    } catch (NullPointerException e) {
                        articleVO.imageUri = "";
                    }

                    datas.add(articleVO);
                    publishProgress("IndexingData..." + i);

                    i += 1;

                    if (i > 50)
                        break;
                }catch (Exception e){}
            }
            cursor.close();

            return null;
        }
    }
}
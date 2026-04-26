package com.example.project;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;

/**
 * Splash screen. Pulls a fresh per-user recommendation database from the
 * server, then scrapes article metadata for each link before handing off to
 * {@link MainActivity}.
 */
public class StartScreen extends AppCompatActivity {
    /** Cap on the number of articles indexed before opening MainActivity. */
    private static final int MAX_INDEXED = 50;

    private File recommendDbFile;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        startService(new Intent(this, ForecdTerminationService.class));
        recommendDbFile = new File(Config.databasesDir(this), "recommenddb");
        textView = findViewById(R.id.initializeStatusText);

        Realm.init(this);
        textView.setText("DownloadingFile...");
        new DownloadFile().execute();
    }

    /** Wipes stale per-launch caches, then pulls the latest recommend DB. */
    class DownloadFile extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            // Stale recommendations from a previous session would otherwise
            // bleed into the new list before the server response lands.
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> r.where(ArticleVO.class).findAll().deleteAllFromRealm());
            }

            String url = Config.sendUserFileUrl(Config.userId(StartScreen.this));
            new RecommendRequester(url, recommendDbFile).execute();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new On().execute();
        }
    }

    /** Reads the freshly downloaded SQLite and scrapes article metadata. */
    class On extends AsyncTask<Void, String, Void> {
        final ArrayList<ArticleVO> datas = new ArrayList<>();

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
        protected Void doInBackground(Void... voids) {
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(recommendDbFile, null);
                 Cursor cursor = db.rawQuery(
                     "select link, similarity from tblink order by similarity desc", null)) {

                int i = 0;
                while (cursor.moveToNext() && i < MAX_INDEXED) {
                    String link = cursor.getString(0);
                    String similarity = cursor.getString(1);

                    Document doc;
                    try {
                        doc = Jsoup.connect(link).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (doc == null) {
                        continue;
                    }

                    try {
                        ArticleVO articleVO = new ArticleVO();
                        articleVO.link = link;
                        articleVO.similarity = similarity;
                        articleVO.title = doc.select("#articleTitle").text();
                        articleVO.date = "입력 : " + doc.select(
                            "#main_content > div.article_header > div.article_info > div > span:nth-child(1)"
                        ).text();
                        articleVO.publisher = doc.select(
                            "#main_content > div.article_header > div.press_logo > a > img"
                        ).attr("title");

                        Elements images = doc.select("#articleBodyContents").select("img");
                        articleVO.imageUri = images.first() != null ? images.first().attr("src") : "";

                        datas.add(articleVO);
                        publishProgress("IndexingData..." + i);
                        i += 1;
                    } catch (Exception ignored) {
                        // Skip articles whose markup doesn't match Naver's expected layout.
                    }
                }
            }
            return null;
        }
    }
}

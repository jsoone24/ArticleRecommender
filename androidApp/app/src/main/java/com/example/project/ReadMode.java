package com.example.project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import io.realm.Realm;

/**
 * Article reader. Renders the scraped article HTML in a WebView (with JS
 * disabled — the body comes from a third party), records the read in the
 * local SQLite, and lets the user toggle the bookmark state.
 */
public class ReadMode extends AppCompatActivity implements View.OnClickListener {
    ArrayList<ArticleVO> datas;
    String link;
    String readDate;
    String article;

    Button bookmarkButtonView;
    Button shareButtonView;
    TextView titleView;
    TextView dateView;
    TextView publisherView;
    WebView webView;

    Integer pos;
    boolean bookmarkStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_mode);

        Intent intent = getIntent(); //인텐트로부터 넘어온 정보를 받는다.

        datas = (ArrayList<ArticleVO>) intent.getSerializableExtra("data");
        pos = intent.getIntExtra("pos", -1);
        this.link = datas.get(pos).link;

        bookmarkButtonView = findViewById(R.id.bookmarkButton);
        bookmarkButtonView.setOnClickListener(this);
        shareButtonView = findViewById(R.id.shareButton);
        shareButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", link);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "기사 링크가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        titleView = findViewById(R.id.title);
        webView = findViewById(R.id.article);
        publisherView = findViewById(R.id.publisher);

        dateView = findViewById(R.id.date);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
        readDate = dateFormat.format(System.currentTimeMillis());

        RecordDBHelper helper = new RecordDBHelper(getApplicationContext());  //읽은 기사들을 데이터로 저장해둔다.
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            db.execSQL(
                "insert into tb_Record (readdate, articledate, link) values (?,?,?)",
                new String[]{readDate, datas.get(pos).date, link}
            );
        }

        try (Realm mRealm = Realm.getDefaultInstance()) {
            //북마크 목록에 이 기사가 있는지 확인한다. 있으면 북마크를 눌린 상태로 표시한다.
            BookmarkVO vo = mRealm.where(BookmarkVO.class).equalTo("link", link).findFirst();
            if (vo != null && vo.link != null) {
                bookmarkStatus = true;
                bookmarkButtonView.setSelected(true);
            }
        }

        new Parser().execute();

        titleView.setText(datas.get(pos).title);
        dateView.setText(datas.get(pos).date);
        publisherView.setText(datas.get(pos).publisher);
    }

    @Override
    public void onBackPressed() {
        // Reconcile the bookmark store with the user's final toggle state.
        try (Realm realm = Realm.getDefaultInstance()) {
            BookmarkVO existing = realm.where(BookmarkVO.class).equalTo("link", link).findFirst();
            boolean existsInStore = existing != null && existing.link != null;

            if (existsInStore && !bookmarkStatus) {
                final BookmarkVO toDelete = existing;
                realm.executeTransaction(r -> toDelete.deleteFromRealm());
                Intent intent = getIntent();
                intent.putExtra("posreturn", pos);
                setResult(RESULT_OK, intent);
                finish();
            } else if (!existsInStore && bookmarkStatus) {
                realm.executeTransaction(r -> {
                    BookmarkVO created = r.createObject(BookmarkVO.class);
                    created.title = datas.get(pos).title;
                    created.link = link;
                    created.readdate = readDate;
                    created.date = datas.get(pos).date;
                    created.publisher = datas.get(pos).publisher;
                    created.article = article;
                    created.summary = datas.get(pos).summary;
                    created.imageUri = datas.get(pos).imageUri;
                });
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) { //북마크 저장여부에 따라 버튼 모양을 변경시킨다.
        if (view == bookmarkButtonView) {
            if (bookmarkStatus) {
                view.setSelected(false);
            } else {
                view.setSelected(true);
            }
            bookmarkStatus = !bookmarkStatus;
        }
    }

    public class MyWebClient extends WebViewClient { //파싱된 기사내용을 webview를 통해 보여준다.
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    class Parser extends AsyncTask<Void, Void, Void> { //기사 내용을 파싱해온다.
        protected Void doInBackground(Void... voids) {
            Document doc;
            try {
                doc = Jsoup.connect(link).get();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String imageSize = "<style> img{display: inline; height: auto; max-width: 100%;} </style>";
            Elements html = doc.getElementsByClass("_article_body_contents");
            article = imageSize + html.toString();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            WebSettings settings = webView.getSettings();
            // JS is intentionally disabled: article HTML is scraped from a third party
            // and rendered inside the WebView. Enabling JS would let crafted markup
            // run scripts in our app context.
            settings.setJavaScriptEnabled(false);
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            settings.setLoadWithOverviewMode(true);
            webView.setVerticalScrollBarEnabled(false);
            webView.setWebViewClient(new MyWebClient());
            String body = article == null ? "" : article;
            webView.loadData(body, "text/html; charset=UTF-8", null);
        }
    }
}
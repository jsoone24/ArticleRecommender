package com.example.project;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements View.OnClickListener { //시작화면에서 앱이 추천 기사 목록 인덱싱을 마치고 프래그먼트 뷰를 관리하는 부분이다.
    private String android_id;
    private String dirPath = "/data/data/com.example.project/databases";
    ArrayList<ArticleVO> datas;

    ImageButton articleIcon;
    ImageButton bookmarkIcon;
    ImageButton userIcon;

    FragmentManager manager;
    ArticleFragment articleFragment;
    BookmarkFragment bookmarkFragment;
    UserFragment userFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        datas = (ArrayList<ArticleVO>) intent.getSerializableExtra("data");
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        articleIcon = findViewById(R.id.articleIcon);
        bookmarkIcon = findViewById(R.id.bookmarkIcon);
        userIcon = findViewById(R.id.userIcon);

        articleIcon.setOnClickListener(this);
        bookmarkIcon.setOnClickListener(this);
        userIcon.setOnClickListener(this);

        manager = getSupportFragmentManager(); //프래그먼트 뷰를 관리하기 위한 매니저
        articleFragment = new ArticleFragment(datas);
        bookmarkFragment = new BookmarkFragment();
        userFragment = new UserFragment();
        articleIcon.setSelected(true);

        FragmentTransaction ft = manager.beginTransaction();
        ft.addToBackStack(null);
        ft.add(R.id.main_container, articleFragment);
        ft.commit();

        Realm.init(this);
    }

    @Override
    public void onClick(View view) { //프래그먼트 버튼에 리스너 설정
        if (view == articleIcon) { //각 프래그먼트 뷰들이 버튼을 누름에 따라 포커싱 됨
            if (!articleFragment.isVisible()) {
                FragmentTransaction ft = manager.beginTransaction();
                ft.addToBackStack(null);
                ft.replace(R.id.main_container, articleFragment);
                ft.commit();
                articleIcon.setSelected(true);
                bookmarkIcon.setSelected(false);
                userIcon.setSelected(false);
            }
        } else if (view == bookmarkIcon) {
            if (!bookmarkFragment.isVisible()) {
                FragmentTransaction ft = manager.beginTransaction();
                ft.addToBackStack(null);
                ft.replace(R.id.main_container, bookmarkFragment);
                ft.commit();
                articleIcon.setSelected(false);
                bookmarkIcon.setSelected(true);
                userIcon.setSelected(false);
            }
        } else if (view == userIcon) {
            if (!userFragment.isVisible()) {
                FragmentTransaction ft = manager.beginTransaction();
                ft.addToBackStack(null);
                ft.replace(R.id.main_container, userFragment);
                ft.commit();
                articleIcon.setSelected(false);
                bookmarkIcon.setSelected(false);
                userIcon.setSelected(true);
            }
        }
    }

    @Override
    public void onBackPressed() { //프래그먼트 뷰에서 뒤로 가기를 눌렀을 때,
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (!(fragment instanceof IOnBackPressed) || !((IOnBackPressed) fragment).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() { //앱이 자연스레 종료될때 서버로 데이터를 전송한다.
        SendFile sendFile = new SendFile();
        sendFile.execute(); //파일 확인 시작 전 recommend 파일 다운로드
        super.onDestroy();
    }

    class SendFile extends AsyncTask<Void, String, Void> { //앱 종료시 record파일 서버로 전송
        @Override
        protected Void doInBackground(Void... voids) {
            RecordSender recordSender = new RecordSender();
            recordSender.request("http://URL:8080/recommender/GetUserFile/".concat(android_id), dirPath, "recorddb");
            return null;
        }
    }
    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - initTime > 3000) {
                showToast("종료하려면 한번 더 누르세요.");
                initTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }*/

}
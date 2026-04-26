package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.util.ArrayList;

import io.realm.Realm;

/**
 * Three-tab fragment host (Articles / Bookmarks / User).
 *
 * On natural shutdown, dispatches the user's reading-history SQLite to the
 * server. {@link ForecdTerminationService} handles the swipe-killed case.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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

        articleIcon = findViewById(R.id.articleIcon);
        bookmarkIcon = findViewById(R.id.bookmarkIcon);
        userIcon = findViewById(R.id.userIcon);

        articleIcon.setOnClickListener(this);
        bookmarkIcon.setOnClickListener(this);
        userIcon.setOnClickListener(this);

        manager = getSupportFragmentManager();
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
    public void onClick(View view) {
        if (view == articleIcon) {
            switchFragment(articleFragment, true, false, false);
        } else if (view == bookmarkIcon) {
            switchFragment(bookmarkFragment, false, true, false);
        } else if (view == userIcon) {
            switchFragment(userFragment, false, false, true);
        }
    }

    private void switchFragment(Fragment target, boolean a, boolean b, boolean u) {
        if (target.isVisible()) return;
        FragmentTransaction ft = manager.beginTransaction();
        ft.addToBackStack(null);
        ft.replace(R.id.main_container, target);
        ft.commit();
        articleIcon.setSelected(a);
        bookmarkIcon.setSelected(b);
        userIcon.setSelected(u);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (!(fragment instanceof IOnBackPressed) || !((IOnBackPressed) fragment).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        // Best-effort upload on graceful shutdown. The OS may kill us before
        // it lands; ForecdTerminationService.onTaskRemoved handles task-swipe.
        File recordDb = new File(Config.databasesDir(this), "recorddb");
        new RecordSender(Config.getUserFileUrl(Config.userId(this)), recordDb).execute();
        super.onDestroy();
    }
}
